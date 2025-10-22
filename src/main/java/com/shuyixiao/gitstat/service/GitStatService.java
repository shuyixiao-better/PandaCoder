package com.shuyixiao.gitstat.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.gitstat.model.GitAuthorDailyStat;
import com.shuyixiao.gitstat.model.GitAuthorStat;
import com.shuyixiao.gitstat.model.GitDailyStat;
import com.shuyixiao.gitstat.model.GitProjectStat;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Git 统计服务
 * 负责从项目的 Git 仓库中获取统计数据
 */
@Service(Service.Level.PROJECT)
public final class GitStatService {
    
    private static final Logger LOG = Logger.getInstance(GitStatService.class);
    private final Project project;
    private final Map<String, GitAuthorStat> authorStatsCache = new LinkedHashMap<>();
    private final Map<LocalDate, GitDailyStat> dailyStatsCache = new LinkedHashMap<>();
    private final List<GitAuthorDailyStat> authorDailyStatsCache = new ArrayList<>();
    private GitProjectStat projectStat = new GitProjectStat();
    private LocalDate lastRefreshDate;
    
    public GitStatService(Project project) {
        this.project = project;
    }
    
    /**
     * 刷新统计数据
     */
    public void refreshStatistics() {
        try {
            authorStatsCache.clear();
            dailyStatsCache.clear();
            authorDailyStatsCache.clear();
            projectStat = new GitProjectStat();
            
            // 获取项目的 Git 仓库
            Collection<GitRepository> repositories = GitUtil.getRepositories(project);
            if (repositories.isEmpty()) {
                LOG.warn("No Git repositories found in project");
                return;
            }
            
            // 遍历所有 Git 仓库
            for (GitRepository repository : repositories) {
                VirtualFile root = repository.getRoot();
                processRepository(root);
                calculateProjectStats(root);
            }
            
            lastRefreshDate = LocalDate.now();
            
        } catch (Exception e) {
            LOG.error("Failed to refresh Git statistics", e);
        }
    }
    
    /**
     * 处理单个 Git 仓库
     */
    private void processRepository(VirtualFile root) {
        try {
            String repoPath = root.getPath();
            
            // 执行 git log 命令获取统计数据
            // 格式：%an|%ae|%ad|%s (作者名|作者邮箱|日期|提交信息)
            String[] command = {
                    "git",
                    "-C", repoPath,
                    "log",
                    "--all",
                    "--numstat",
                    "--date=short",
                    "--pretty=format:COMMIT|%an|%ae|%ad|%s"
            };
            
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            final String[] currentAuthorName = {null};
            final String[] currentAuthorEmail = {null};
            final LocalDate[] currentDate = {null};
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("COMMIT|")) {
                    // 解析提交信息
                    String[] parts = line.substring(7).split("\\|", 4);
                    if (parts.length >= 3) {
                        currentAuthorName[0] = parts[0];
                        currentAuthorEmail[0] = parts[1];
                        currentDate[0] = LocalDate.parse(parts[2], DateTimeFormatter.ISO_DATE);
                    }
                } else if (!line.trim().isEmpty() && currentAuthorName[0] != null) {
                    // 解析文件变更统计
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int additions = "-".equals(parts[0]) ? 0 : Integer.parseInt(parts[0]);
                            int deletions = "-".equals(parts[1]) ? 0 : Integer.parseInt(parts[1]);
                            
                            // 更新作者统计
                            String authorKey = currentAuthorEmail[0];
                            GitAuthorStat authorStat = authorStatsCache.computeIfAbsent(
                                    authorKey,
                                    k -> new GitAuthorStat(currentAuthorName[0], currentAuthorEmail[0])
                            );
                            authorStat.addCommitStats(additions, deletions, currentDate[0]);
                            
                            // 更新每日统计
                            GitDailyStat dailyStat = dailyStatsCache.computeIfAbsent(
                                    currentDate[0],
                                    GitDailyStat::new
                            );
                            dailyStat.addStats(additions, deletions);
                            
                            // 更新作者每日统计
                            updateAuthorDailyStats(currentAuthorName[0], currentAuthorEmail[0], currentDate[0], additions, deletions);
                            
                        } catch (NumberFormatException e) {
                            // 忽略无法解析的行
                        }
                    }
                }
            }
            
            reader.close();
            process.waitFor();
            
            // 更新每日活跃作者数
            updateDailyActiveAuthors();
            
        } catch (Exception e) {
            LOG.error("Failed to process Git repository", e);
        }
    }
    
    /**
     * 更新每日活跃作者数
     */
    private void updateDailyActiveAuthors() {
        // 统计每天有多少不同的作者提交了代码
        Map<LocalDate, Set<String>> dailyAuthors = new HashMap<>();
        
        for (GitAuthorStat authorStat : authorStatsCache.values()) {
            LocalDate firstCommit = authorStat.getFirstCommit();
            LocalDate lastCommit = authorStat.getLastCommit();
            
            if (firstCommit != null && lastCommit != null) {
                // 简化处理：只标记第一次和最后一次提交日期
                dailyAuthors.computeIfAbsent(firstCommit, k -> new HashSet<>())
                        .add(authorStat.getAuthorEmail());
                dailyAuthors.computeIfAbsent(lastCommit, k -> new HashSet<>())
                        .add(authorStat.getAuthorEmail());
            }
        }
        
        // 更新每日统计中的活跃作者数
        for (Map.Entry<LocalDate, Set<String>> entry : dailyAuthors.entrySet()) {
            GitDailyStat dailyStat = dailyStatsCache.get(entry.getKey());
            if (dailyStat != null) {
                dailyStat.setActiveAuthors(entry.getValue().size());
            }
        }
    }
    
    /**
     * 获取所有作者统计数据
     */
    @NotNull
    public List<GitAuthorStat> getAllAuthorStats() {
        return new ArrayList<>(authorStatsCache.values());
    }
    
    /**
     * 获取按提交数排序的作者统计
     */
    @NotNull
    public List<GitAuthorStat> getAuthorStatsSortedByCommits() {
        return authorStatsCache.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalCommits(), a.getTotalCommits()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取按代码行数排序的作者统计
     */
    @NotNull
    public List<GitAuthorStat> getAuthorStatsSortedByLines() {
        return authorStatsCache.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalAdditions(), a.getTotalAdditions()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有每日统计数据
     */
    @NotNull
    public List<GitDailyStat> getAllDailyStats() {
        return dailyStatsCache.values().stream()
                .sorted(Comparator.comparing(GitDailyStat::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最近 N 天的每日统计
     */
    @NotNull
    public List<GitDailyStat> getRecentDailyStats(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return dailyStatsCache.values().stream()
                .filter(stat -> !stat.getDate().isBefore(startDate))
                .sorted(Comparator.comparing(GitDailyStat::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取总体统计信息
     */
    @NotNull
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalCommits = authorStatsCache.values().stream()
                .mapToInt(GitAuthorStat::getTotalCommits)
                .sum();
        
        int totalAdditions = authorStatsCache.values().stream()
                .mapToInt(GitAuthorStat::getTotalAdditions)
                .sum();
        
        int totalDeletions = authorStatsCache.values().stream()
                .mapToInt(GitAuthorStat::getTotalDeletions)
                .sum();
        
        int totalAuthors = authorStatsCache.size();
        
        stats.put("totalCommits", totalCommits);
        stats.put("totalAdditions", totalAdditions);
        stats.put("totalDeletions", totalDeletions);
        stats.put("netChanges", totalAdditions - totalDeletions);
        stats.put("totalAuthors", totalAuthors);
        stats.put("lastRefreshDate", lastRefreshDate);
        
        return stats;
    }
    
    /**
     * 获取最后刷新时间
     */
    public LocalDate getLastRefreshDate() {
        return lastRefreshDate;
    }
    
    /**
     * 更新作者每日统计
     */
    private void updateAuthorDailyStats(String authorName, String authorEmail, LocalDate date, int additions, int deletions) {
        // 查找或创建作者每日统计
        GitAuthorDailyStat authorDailyStat = authorDailyStatsCache.stream()
                .filter(stat -> stat.getAuthorEmail().equals(authorEmail) && stat.getDate().equals(date))
                .findFirst()
                .orElse(null);
        
        if (authorDailyStat == null) {
            authorDailyStat = new GitAuthorDailyStat(authorName, authorEmail, date);
            authorDailyStatsCache.add(authorDailyStat);
        }
        
        authorDailyStat.addStats(additions, deletions);
    }
    
    /**
     * 获取所有作者每日统计数据
     */
    @NotNull
    public List<GitAuthorDailyStat> getAllAuthorDailyStats() {
        return new ArrayList<>(authorDailyStatsCache);
    }
    
    /**
     * 获取指定作者的每日统计数据（通过邮箱）
     */
    @NotNull
    public List<GitAuthorDailyStat> getAuthorDailyStatsByAuthor(String authorEmail) {
        return authorDailyStatsCache.stream()
                .filter(stat -> stat.getAuthorEmail().equals(authorEmail))
                .sorted(Comparator.comparing(GitAuthorDailyStat::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定作者的每日统计数据（通过姓名）
     */
    @NotNull
    public List<GitAuthorDailyStat> getAuthorDailyStatsByAuthorName(String authorName) {
        return authorDailyStatsCache.stream()
                .filter(stat -> stat.getAuthorName().equals(authorName))
                .sorted(Comparator.comparing(GitAuthorDailyStat::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有作者的名称列表（用于下拉选择）
     */
    @NotNull
    public List<String> getAllAuthorNames() {
        return authorStatsCache.values().stream()
                .map(GitAuthorStat::getAuthorName)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定时间范围内指定作者的每日统计
     */
    @NotNull
    public List<GitAuthorDailyStat> getAuthorDailyStatsByAuthorAndDays(String authorName, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return authorDailyStatsCache.stream()
                .filter(stat -> stat.getAuthorName().equals(authorName))
                .filter(stat -> !stat.getDate().isBefore(startDate))
                .sorted(Comparator.comparing(GitAuthorDailyStat::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最近 N 天的作者每日统计
     */
    @NotNull
    public List<GitAuthorDailyStat> getRecentAuthorDailyStats(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return authorDailyStatsCache.stream()
                .filter(stat -> !stat.getDate().isBefore(startDate))
                .sorted(Comparator.comparing(GitAuthorDailyStat::getDate).thenComparing(GitAuthorDailyStat::getAuthorName))
                .collect(Collectors.toList());
    }
    
    /**
     * 计算项目当前代码统计
     */
    private void calculateProjectStats(VirtualFile root) {
        try {
            Path rootPath = Paths.get(root.getPath());
            
            // 定义需要统计的代码文件扩展名
            Set<String> codeExtensions = new HashSet<>(Arrays.asList(
                    "java", "kt", "groovy", "scala",
                    "js", "ts", "jsx", "tsx", "vue",
                    "py", "rb", "go", "rs", "c", "cpp", "h", "hpp",
                    "cs", "php", "swift", "m", "mm",
                    "xml", "html", "css", "scss", "less",
                    "sql", "sh", "bat", "ps1",
                    "json", "yaml", "yml", "properties", "gradle"
            ));
            
            // 定义需要排除的目录
            Set<String> excludeDirs = new HashSet<>(Arrays.asList(
                    ".git", ".idea", ".vscode", "node_modules", "build", "target",
                    "dist", "out", "bin", ".gradle", "gradle"
            ));
            
            // 遍历项目文件
            try (Stream<Path> paths = Files.walk(rootPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> {
                            // 检查是否在排除目录中
                            for (String excludeDir : excludeDirs) {
                                if (path.toString().contains(java.io.File.separator + excludeDir + java.io.File.separator)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            String extension = "";
                            int lastDot = fileName.lastIndexOf('.');
                            if (lastDot > 0) {
                                extension = fileName.substring(lastDot + 1);
                            }
                            
                            if (codeExtensions.contains(extension)) {
                                analyzeFile(path, extension);
                            }
                        });
            }
            
        } catch (Exception e) {
            LOG.error("Failed to calculate project statistics", e);
        }
    }
    
    /**
     * 分析单个文件
     */
    private void analyzeFile(Path file, String extension) {
        try {
            List<String> lines = Files.readAllLines(file);
            int totalLines = lines.size();
            int codeLines = 0;
            int blankLines = 0;
            int commentLines = 0;
            
            boolean inBlockComment = false;
            
            for (String line : lines) {
                String trimmed = line.trim();
                
                if (trimmed.isEmpty()) {
                    blankLines++;
                } else if (isComment(trimmed, extension, inBlockComment)) {
                    commentLines++;
                    if (trimmed.contains("/*")) {
                        inBlockComment = true;
                    }
                    if (trimmed.contains("*/")) {
                        inBlockComment = false;
                    }
                } else {
                    codeLines++;
                }
            }
            
            projectStat.addFile(extension, totalLines, codeLines, blankLines, commentLines);
            
        } catch (Exception e) {
            LOG.warn("Failed to analyze file: " + file, e);
        }
    }
    
    /**
     * 判断是否为注释行
     */
    private boolean isComment(String line, String extension, boolean inBlockComment) {
        if (inBlockComment) {
            return true;
        }
        
        // Java/C/C++/JavaScript/TypeScript 风格注释
        if (Arrays.asList("java", "kt", "groovy", "scala", "js", "ts", "jsx", "tsx", "c", "cpp", "h", "hpp", "cs", "go", "rs", "swift").contains(extension)) {
            return line.startsWith("//") || line.startsWith("/*") || line.startsWith("*");
        }
        
        // Python/Shell/Ruby 风格注释
        if (Arrays.asList("py", "rb", "sh", "yml", "yaml", "properties").contains(extension)) {
            return line.startsWith("#");
        }
        
        // XML/HTML 注释
        if (Arrays.asList("xml", "html").contains(extension)) {
            return line.startsWith("<!--");
        }
        
        // CSS 注释
        if (Arrays.asList("css", "scss", "less").contains(extension)) {
            return line.startsWith("/*") || line.startsWith("*");
        }
        
        return false;
    }
    
    /**
     * 获取项目代码统计
     */
    @NotNull
    public GitProjectStat getProjectStat() {
        return projectStat;
    }
}

