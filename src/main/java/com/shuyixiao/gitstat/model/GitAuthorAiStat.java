package com.shuyixiao.gitstat.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Git 作者 AI 使用统计模型
 * 记录每个作者的 AI 工具使用情况
 */
public class GitAuthorAiStat {
    
    private String authorName;          // 作者姓名
    private String authorEmail;         // 作者邮箱
    
    // 总体统计
    private int totalCommits;           // 总提交次数
    private int aiCommits;              // AI 辅助的提交次数
    private int manualCommits;          // 纯人工的提交次数
    
    private int totalAiAdditions;       // 总 AI 新增行数
    private int totalManualAdditions;   // 总人工新增行数
    private int totalAdditions;         // 总新增行数
    
    private int totalAiDeletions;       // 总 AI 删除行数
    private int totalManualDeletions;   // 总人工删除行数
    private int totalDeletions;         // 总删除行数
    
    // 百分比
    private double aiCommitPercentage;  // AI 提交占比
    private double aiCodePercentage;    // AI 代码占比
    
    // AI 工具使用统计
    private Map<String, Integer> aiToolUsage;  // 各种 AI 工具的使用次数
    
    // 时间范围
    private LocalDate firstAiCommit;    // 第一次使用 AI 的时间
    private LocalDate lastAiCommit;     // 最后一次使用 AI 的时间
    
    public GitAuthorAiStat(String authorName, String authorEmail) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.aiToolUsage = new HashMap<>();
    }
    
    /**
     * 添加 AI 提交统计
     */
    public void addAiCommit(String aiTool, int additions, int deletions, LocalDate date) {
        this.aiCommits++;
        this.totalAiAdditions += additions;
        this.totalAiDeletions += deletions;
        
        if (aiTool != null) {
            this.aiToolUsage.put(aiTool, this.aiToolUsage.getOrDefault(aiTool, 0) + 1);
        }
        
        if (this.firstAiCommit == null || date.isBefore(this.firstAiCommit)) {
            this.firstAiCommit = date;
        }
        if (this.lastAiCommit == null || date.isAfter(this.lastAiCommit)) {
            this.lastAiCommit = date;
        }
    }
    
    /**
     * 添加人工提交统计
     */
    public void addManualCommit(int additions, int deletions) {
        this.manualCommits++;
        this.totalManualAdditions += additions;
        this.totalManualDeletions += deletions;
    }
    
    /**
     * 计算百分比
     */
    public void calculatePercentages() {
        this.totalCommits = this.aiCommits + this.manualCommits;
        this.totalAdditions = this.totalAiAdditions + this.totalManualAdditions;
        this.totalDeletions = this.totalAiDeletions + this.totalManualDeletions;
        
        if (this.totalCommits > 0) {
            this.aiCommitPercentage = (double) this.aiCommits / this.totalCommits * 100;
        }
        
        if (this.totalAdditions > 0) {
            this.aiCodePercentage = (double) this.totalAiAdditions / this.totalAdditions * 100;
        }
    }
    
    /**
     * 获取最常用的 AI 工具
     */
    public String getMostUsedAiTool() {
        if (aiToolUsage.isEmpty()) {
            return null;
        }
        
        return aiToolUsage.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    // Getters and Setters
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getAuthorEmail() {
        return authorEmail;
    }
    
    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }
    
    public int getTotalCommits() {
        return totalCommits;
    }
    
    public void setTotalCommits(int totalCommits) {
        this.totalCommits = totalCommits;
    }
    
    public int getAiCommits() {
        return aiCommits;
    }
    
    public void setAiCommits(int aiCommits) {
        this.aiCommits = aiCommits;
    }
    
    public int getManualCommits() {
        return manualCommits;
    }
    
    public void setManualCommits(int manualCommits) {
        this.manualCommits = manualCommits;
    }
    
    public int getTotalAiAdditions() {
        return totalAiAdditions;
    }
    
    public void setTotalAiAdditions(int totalAiAdditions) {
        this.totalAiAdditions = totalAiAdditions;
    }
    
    public int getTotalManualAdditions() {
        return totalManualAdditions;
    }
    
    public void setTotalManualAdditions(int totalManualAdditions) {
        this.totalManualAdditions = totalManualAdditions;
    }
    
    public int getTotalAdditions() {
        return totalAdditions;
    }
    
    public void setTotalAdditions(int totalAdditions) {
        this.totalAdditions = totalAdditions;
    }
    
    public int getTotalAiDeletions() {
        return totalAiDeletions;
    }
    
    public void setTotalAiDeletions(int totalAiDeletions) {
        this.totalAiDeletions = totalAiDeletions;
    }
    
    public int getTotalManualDeletions() {
        return totalManualDeletions;
    }
    
    public void setTotalManualDeletions(int totalManualDeletions) {
        this.totalManualDeletions = totalManualDeletions;
    }
    
    public int getTotalDeletions() {
        return totalDeletions;
    }
    
    public void setTotalDeletions(int totalDeletions) {
        this.totalDeletions = totalDeletions;
    }
    
    public double getAiCommitPercentage() {
        return aiCommitPercentage;
    }
    
    public void setAiCommitPercentage(double aiCommitPercentage) {
        this.aiCommitPercentage = aiCommitPercentage;
    }
    
    public double getAiCodePercentage() {
        return aiCodePercentage;
    }
    
    public void setAiCodePercentage(double aiCodePercentage) {
        this.aiCodePercentage = aiCodePercentage;
    }
    
    public Map<String, Integer> getAiToolUsage() {
        return aiToolUsage;
    }
    
    public void setAiToolUsage(Map<String, Integer> aiToolUsage) {
        this.aiToolUsage = aiToolUsage;
    }
    
    public LocalDate getFirstAiCommit() {
        return firstAiCommit;
    }
    
    public void setFirstAiCommit(LocalDate firstAiCommit) {
        this.firstAiCommit = firstAiCommit;
    }
    
    public LocalDate getLastAiCommit() {
        return lastAiCommit;
    }
    
    public void setLastAiCommit(LocalDate lastAiCommit) {
        this.lastAiCommit = lastAiCommit;
    }
    
    @Override
    public String toString() {
        return "GitAuthorAiStat{" +
                "authorName='" + authorName + '\'' +
                ", totalCommits=" + totalCommits +
                ", aiCommits=" + aiCommits +
                ", aiCodePercentage=" + String.format("%.1f%%", aiCodePercentage) +
                '}';
    }
}

