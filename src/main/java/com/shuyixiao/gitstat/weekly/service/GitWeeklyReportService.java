package com.shuyixiao.gitstat.weekly.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.gitstat.weekly.model.WeeklyReportConfig;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Git 周报生成服务
 * 负责获取一周的 Git 提交日志并调用 AI 生成周报
 */
@Service(Service.Level.PROJECT)
public final class GitWeeklyReportService {
    
    private static final Logger LOG = Logger.getInstance(GitWeeklyReportService.class);
    private static final Gson GSON = new Gson();
    private final Project project;
    
    public GitWeeklyReportService(Project project) {
        this.project = project;
    }

    /**
     * 获取所有作者列表
     * 从 Git 仓库中提取所有提交过代码的作者
     */
    public List<String> getAllAuthors() {
        Set<String> authors = new LinkedHashSet<>();

        try {
            // 获取项目的 Git 仓库
            Collection<GitRepository> repositories = GitUtil.getRepositories(project);
            if (repositories.isEmpty()) {
                LOG.warn("No Git repositories found in project");
                return new ArrayList<>();
            }

            // 遍历所有 Git 仓库
            for (GitRepository repository : repositories) {
                VirtualFile root = repository.getRoot();
                String repoPath = root.getPath();

                // 执行 git log 命令获取所有作者
                String[] command = {
                    "git",
                    "-C", repoPath,
                    "log",
                    "--all",
                    "--format=%an <%ae>",
                };

                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        authors.add(line.trim());
                    }
                }

                reader.close();
                process.waitFor();
            }

        } catch (Exception e) {
            LOG.error("Failed to get all authors", e);
        }

        return new ArrayList<>(authors);
    }

    /**
     * 获取本周的 Git 提交日志
     * 本周定义为：从本周一到本周日（包含今天）
     */
    public String getWeeklyCommits() {
        return getWeeklyCommits(null);
    }

    /**
     * 获取本周的 Git 提交日志（支持按作者筛选）
     * 本周定义为：从本周一到本周日（包含今天）
     *
     * @param authorFilter 作者筛选条件，格式："作者名 <邮箱>"，null 表示不筛选
     */
    public String getWeeklyCommits(String authorFilter) {
        StringBuilder commits = new StringBuilder();
        
        try {
            // 计算本周的开始和结束日期
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String since = weekStart.format(formatter);
            String until = weekEnd.format(formatter);
            
            // 获取项目的 Git 仓库
            Collection<GitRepository> repositories = GitUtil.getRepositories(project);
            if (repositories.isEmpty()) {
                LOG.warn("No Git repositories found in project");
                return "未找到 Git 仓库";
            }
            
            // 遍历所有 Git 仓库
            for (GitRepository repository : repositories) {
                VirtualFile root = repository.getRoot();
                String repoPath = root.getPath();

                // 构建 git log 命令
                List<String> commandList = new ArrayList<>();
                commandList.add("git");
                commandList.add("-C");
                commandList.add(repoPath);
                commandList.add("log");
                commandList.add("--all");
                commandList.add("--since=" + since);
                commandList.add("--until=" + until + " 23:59:59");

                // 如果指定了作者筛选，添加 --author 参数
                if (authorFilter != null && !authorFilter.trim().isEmpty()) {
                    // 提取作者名（去掉邮箱部分）
                    String authorName = authorFilter;
                    if (authorFilter.contains("<")) {
                        authorName = authorFilter.substring(0, authorFilter.indexOf("<")).trim();
                    }
                    commandList.add("--author=" + authorName);
                }

                commandList.add("--pretty=format:%ad | %an | %s");
                commandList.add("--date=format:%Y-%m-%d %H:%M:%S");

                String[] command = commandList.toArray(new String[0]);

                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    commits.append(line).append("\n");
                }

                reader.close();
                process.waitFor();
            }
            
            if (commits.length() == 0) {
                if (authorFilter != null && !authorFilter.trim().isEmpty()) {
                    return "本周暂无该作者的提交记录（" + since + " 至 " + until + "）\n作者：" + authorFilter;
                } else {
                    return "本周暂无提交记录（" + since + " 至 " + until + "）";
                }
            }

            // 添加统计信息头部
            StringBuilder result = new StringBuilder();
            result.append("=== 本周提交统计 ===\n");
            result.append("时间范围：").append(since).append(" 至 ").append(until).append("\n");
            if (authorFilter != null && !authorFilter.trim().isEmpty()) {
                result.append("筛选作者：").append(authorFilter).append("\n");
            } else {
                result.append("筛选作者：全部作者\n");
            }
            result.append("提交记录：\n\n");
            result.append(commits);

            return result.toString();
            
        } catch (Exception e) {
            LOG.error("Failed to get weekly commits", e);
            return "获取提交日志失败: " + e.getMessage();
        }
    }
    
    /**
     * 调用 AI API 生成周报
     * 支持流式响应
     * 
     * @param config 周报配置
     * @param commits Git 提交日志
     * @param onChunk 接收每个数据块的回调函数
     * @param onComplete 完成时的回调函数
     * @param onError 错误时的回调函数
     */
    public void generateWeeklyReport(
            @NotNull WeeklyReportConfig config,
            @NotNull String commits,
            @NotNull Consumer<String> onChunk,
            @NotNull Runnable onComplete,
            @NotNull Consumer<String> onError) {
        
        // 在后台线程执行 API 调用
        new Thread(() -> {
            try {
                // 构建提示词
                String prompt = config.getPromptTemplate().replace("{commits}", commits);
                
                // 构建请求 JSON
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", config.getModel());
                requestBody.addProperty("stream", true);
                requestBody.addProperty("temperature", config.getTemperature());
                requestBody.addProperty("max_tokens", config.getMaxTokens());
                
                JsonArray messages = new JsonArray();
                JsonObject message = new JsonObject();
                message.addProperty("role", "user");
                message.addProperty("content", prompt);
                messages.add(message);
                requestBody.add("messages", messages);
                
                // 发送 HTTP 请求
                URL url = java.net.URI.create(config.getApiUrl()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
                conn.setDoOutput(true);
                
                // 写入请求体
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = GSON.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // 读取流式响应
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    );
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            
                            try {
                                JsonObject json = GSON.fromJson(data, JsonObject.class);
                                if (json.has("choices")) {
                                    JsonArray choices = json.getAsJsonArray("choices");
                                    if (choices.size() > 0) {
                                        JsonObject choice = choices.get(0).getAsJsonObject();
                                        if (choice.has("delta")) {
                                            JsonObject delta = choice.getAsJsonObject("delta");
                                            if (delta.has("content")) {
                                                String content = delta.get("content").getAsString();
                                                onChunk.accept(content);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOG.warn("Failed to parse SSE data: " + data, e);
                            }
                        }
                    }
                    
                    reader.close();
                    onComplete.run();
                    
                } else {
                    // 读取错误响应
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)
                    );
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                    
                    onError.accept("API 调用失败 (HTTP " + responseCode + "): " + errorResponse.toString());
                }
                
            } catch (Exception e) {
                LOG.error("Failed to generate weekly report", e);
                onError.accept("生成周报失败: " + e.getMessage());
            }
        }).start();
    }
}

