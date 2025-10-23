package com.shuyixiao.gitstat.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuyixiao.gitstat.ai.config.GitStatAiConfigState;
import com.shuyixiao.gitstat.ai.model.AiCodeRecord;
import com.shuyixiao.gitstat.ai.storage.AiCodeRecordStorage;
import com.shuyixiao.gitstat.model.GitAuthorAiStat;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 代码统计服务
 * 负责识别和统计 AI 生成的代码
 * 
 * 采用混合识别法：
 * 1. 实时监控数据（最高优先级，95%+ 准确度）
 * 2. Commit Message 标记（高优先级，100% 准确度）
 * 3. Git Diff 分析（中优先级，70-80% 准确度）
 */
@Service(Service.Level.PROJECT)
public final class GitAiStatService {
    
    private static final Logger LOG = Logger.getInstance(GitAiStatService.class);
    private final AiCodeRecordStorage recordStorage;
    
    // AI 识别关键词模式
    private List<Pattern> aiPatterns = new ArrayList<>();
    
    // AI 工具识别映射
    private Map<String, String> aiToolMapping = new HashMap<>();
    
    // 缓存
    private final Map<String, GitAuthorAiStat> authorAiStatsCache = new LinkedHashMap<>();
    
    public GitAiStatService(Project project) {
        this.recordStorage = project.getService(AiCodeRecordStorage.class);
        initializePatterns();
        LOG.info("GitAiStatService initialized for project: " + project.getName());
    }
    
    /**
     * 初始化 AI 识别模式
     */
    private void initializePatterns() {
        GitStatAiConfigState config = GitStatAiConfigState.getInstance();
        
        // 构建关键词模式
        aiPatterns.clear();
        for (String keyword : config.aiKeywords) {
            aiPatterns.add(Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE));
        }
        
        // 构建工具映射
        aiToolMapping.clear();
        for (GitStatAiConfigState.AiToolMapping mapping : config.aiToolMappings) {
            aiToolMapping.put(mapping.keyword.toLowerCase(), mapping.toolName);
        }
    }
    
    /**
     * 检测 commit message 是否表明使用了 AI
     */
    public boolean isAiGeneratedCommit(String commitMessage) {
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return false;
        }
        
        for (Pattern pattern : aiPatterns) {
            if (pattern.matcher(commitMessage).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 从 commit message 中提取 AI 工具名称
     */
    public String extractAiToolName(String commitMessage) {
        if (commitMessage == null) {
            return "AI Assistant";
        }
        
        String lowerMessage = commitMessage.toLowerCase();
        
        for (Map.Entry<String, String> entry : aiToolMapping.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "AI Assistant";
    }
    
    /**
     * 从 Git 日志中分析 AI 代码统计
     * 混合识别法：结合实时数据 + Commit Message + Git Diff
     */
    public void analyzeAiStatistics(VirtualFile root) {
        try {
            String repoPath = root.getPath();
            
            // 执行 git log 命令
            String[] command = {
                "git",
                "-C", repoPath,
                "log",
                "--all",
                "--numstat",
                "--date=short",
                "--pretty=format:COMMIT|%H|%an|%ae|%ad|%s"
            };
            
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            String currentCommitHash = null;
            String currentAuthorName = null;
            String currentAuthorEmail = null;
            LocalDate currentDate = null;
            String currentMessage = null;
            int commitAdditions = 0;
            int commitDeletions = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("COMMIT|")) {
                    // 处理上一个 commit 的统计
                    if (currentCommitHash != null) {
                        processCommitAiStats(
                            currentCommitHash,
                            currentAuthorName,
                            currentAuthorEmail,
                            currentDate,
                            currentMessage,
                            commitAdditions,
                            commitDeletions
                        );
                    }
                    
                    // 解析新的 commit
                    String[] parts = line.substring(7).split("\\|", 6);
                    if (parts.length >= 5) {
                        currentCommitHash = parts[0];
                        currentAuthorName = parts[1];
                        currentAuthorEmail = parts[2];
                        currentDate = LocalDate.parse(parts[3], DateTimeFormatter.ISO_DATE);
                        currentMessage = parts.length > 4 ? parts[4] : "";
                        commitAdditions = 0;
                        commitDeletions = 0;
                    }
                } else if (!line.trim().isEmpty() && currentCommitHash != null) {
                    // 解析文件变更统计
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int additions = "-".equals(parts[0]) ? 0 : Integer.parseInt(parts[0]);
                            int deletions = "-".equals(parts[1]) ? 0 : Integer.parseInt(parts[1]);
                            commitAdditions += additions;
                            commitDeletions += deletions;
                        } catch (NumberFormatException e) {
                            // 忽略
                        }
                    }
                }
            }
            
            // 处理最后一个 commit
            if (currentCommitHash != null) {
                processCommitAiStats(
                    currentCommitHash,
                    currentAuthorName,
                    currentAuthorEmail,
                    currentDate,
                    currentMessage,
                    commitAdditions,
                    commitDeletions
                );
            }
            
            reader.close();
            process.waitFor();
            
            // 计算百分比
            calculateAiPercentages();
            
            LOG.info("AI statistics analysis completed");
            
        } catch (Exception e) {
            LOG.error("Failed to analyze AI statistics", e);
        }
    }
    
    /**
     * 处理单个 commit 的 AI 统计
     * 混合识别：实时数据 > Commit Message > Git Diff
     */
    private void processCommitAiStats(
        String commitHash,
        String authorName,
        String authorEmail,
        LocalDate date,
        String commitMessage,
        int additions,
        int deletions
    ) {
        // 获取或创建作者 AI 统计
        String authorKey = authorEmail;
        GitAuthorAiStat authorAiStat = authorAiStatsCache.computeIfAbsent(
            authorKey,
            k -> new GitAuthorAiStat(authorName, authorEmail)
        );
        
        // 混合识别判断
        boolean isAiGenerated = false;
        String aiTool = null;
        
        // 优先级 1: 检查实时监控数据
        List<AiCodeRecord> realtimeRecords = getRealtimeRecordsForCommit(commitHash, date);
        if (!realtimeRecords.isEmpty()) {
            isAiGenerated = true;
            aiTool = getMostCommonAiTool(realtimeRecords);
            LOG.debug("Using realtime data for commit: " + commitHash);
        }
        
        // 优先级 2: 检查 Commit Message 标记
        if (!isAiGenerated && isAiGeneratedCommit(commitMessage)) {
            isAiGenerated = true;
            aiTool = extractAiToolName(commitMessage);
            LOG.debug("Using commit message for commit: " + commitHash);
        }
        
        // 优先级 3: Git Diff 启发式分析
        if (!isAiGenerated && shouldUseHeuristicDetection()) {
            if (additions >= GitStatAiConfigState.getInstance().heuristicLinesThreshold) {
                isAiGenerated = true;
                aiTool = "Unknown AI";
                LOG.debug("Using heuristic for commit: " + commitHash);
            }
        }
        
        // 更新统计
        if (isAiGenerated) {
            authorAiStat.addAiCommit(aiTool, additions, deletions, date);
        } else {
            authorAiStat.addManualCommit(additions, deletions);
        }
    }
    
    /**
     * 获取 commit 对应的实时监控记录
     */
    private List<AiCodeRecord> getRealtimeRecordsForCommit(String commitHash, LocalDate date) {
        // 获取该日期的所有记录
        long startTime = date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = date.plusDays(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        return recordStorage.getRecordsByDateRange(startTime, endTime).stream()
                .filter(r -> commitHash.equals(r.getCommitHash()) || r.getCommitHash() == null)
                .collect(Collectors.toList());
    }
    
    /**
     * 计算平均 AI 概率
     */
    private int calculateAverageAiProbability(List<AiCodeRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }
        
        return (int) records.stream()
                .mapToInt(AiCodeRecord::getAiProbability)
                .average()
                .orElse(0);
    }
    
    /**
     * 获取最常见的 AI 工具
     */
    private String getMostCommonAiTool(List<AiCodeRecord> records) {
        Map<String, Long> toolCounts = records.stream()
                .collect(Collectors.groupingBy(AiCodeRecord::getAiTool, Collectors.counting()));
        
        return toolCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("AI Assistant");
    }
    
    /**
     * 是否应该使用启发式检测
     */
    private boolean shouldUseHeuristicDetection() {
        return GitStatAiConfigState.getInstance().enableHeuristicDetection;
    }
    
    /**
     * 计算 AI 百分比
     */
    private void calculateAiPercentages() {
        for (GitAuthorAiStat stat : authorAiStatsCache.values()) {
            stat.calculatePercentages();
        }
    }
    
    /**
     * 获取所有作者的 AI 使用统计
     */
    @NotNull
    public List<GitAuthorAiStat> getAllAuthorAiStats() {
        return new ArrayList<>(authorAiStatsCache.values());
    }
    
    /**
     * 获取 AI 使用率最高的作者
     */
    @NotNull
    public List<GitAuthorAiStat> getTopAiUsers(int limit) {
        return authorAiStatsCache.values().stream()
                .sorted(Comparator.comparing(GitAuthorAiStat::getAiCodePercentage).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取整体 AI 统计信息
     */
    @NotNull
    public Map<String, Object> getOverallAiStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAiCommits = authorAiStatsCache.values().stream()
                .mapToInt(GitAuthorAiStat::getAiCommits)
                .sum();
        
        int totalManualCommits = authorAiStatsCache.values().stream()
                .mapToInt(GitAuthorAiStat::getManualCommits)
                .sum();
        
        int totalCommits = totalAiCommits + totalManualCommits;
        
        int totalAiAdditions = authorAiStatsCache.values().stream()
                .mapToInt(GitAuthorAiStat::getTotalAiAdditions)
                .sum();
        
        int totalManualAdditions = authorAiStatsCache.values().stream()
                .mapToInt(GitAuthorAiStat::getTotalManualAdditions)
                .sum();
        
        int totalAdditions = totalAiAdditions + totalManualAdditions;
        
        double aiCommitPercentage = totalCommits > 0 
                ? (double) totalAiCommits / totalCommits * 100 
                : 0;
        
        double aiCodePercentage = totalAdditions > 0 
                ? (double) totalAiAdditions / totalAdditions * 100 
                : 0;
        
        // 统计 AI 工具使用情况
        Map<String, Integer> toolUsage = new HashMap<>();
        for (GitAuthorAiStat authorStat : authorAiStatsCache.values()) {
            for (Map.Entry<String, Integer> entry : authorStat.getAiToolUsage().entrySet()) {
                toolUsage.put(
                        entry.getKey(),
                        toolUsage.getOrDefault(entry.getKey(), 0) + entry.getValue()
                );
            }
        }
        
        stats.put("totalAiCommits", totalAiCommits);
        stats.put("totalManualCommits", totalManualCommits);
        stats.put("totalCommits", totalCommits);
        stats.put("aiCommitPercentage", aiCommitPercentage);
        
        stats.put("totalAiAdditions", totalAiAdditions);
        stats.put("totalManualAdditions", totalManualAdditions);
        stats.put("totalAdditions", totalAdditions);
        stats.put("aiCodePercentage", aiCodePercentage);
        
        stats.put("aiToolUsage", toolUsage);
        stats.put("aiUserCount", authorAiStatsCache.size());
        
        return stats;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        authorAiStatsCache.clear();
    }
}

