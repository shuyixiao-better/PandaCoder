package com.shuyixiao.gitstat.model;

import java.time.LocalDate;

/**
 * Git 作者统计数据模型
 * 记录每个作者的代码提交统计信息
 */
public class GitAuthorStat {
    
    private String authorName;      // 作者姓名
    private String authorEmail;     // 作者邮箱
    private int totalCommits;       // 总提交次数
    private int totalAdditions;     // 总新增行数
    private int totalDeletions;     // 总删除行数
    private int netChanges;         // 净变化（新增 - 删除）
    private LocalDate firstCommit;  // 第一次提交时间
    private LocalDate lastCommit;   // 最后一次提交时间
    
    public GitAuthorStat(String authorName, String authorEmail) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.totalCommits = 0;
        this.totalAdditions = 0;
        this.totalDeletions = 0;
        this.netChanges = 0;
    }
    
    /**
     * 增加一次提交（只计数，不统计代码行数）
     */
    public void incrementCommit(LocalDate commitDate) {
        this.totalCommits++;
        
        if (this.firstCommit == null || commitDate.isBefore(this.firstCommit)) {
            this.firstCommit = commitDate;
        }
        
        if (this.lastCommit == null || commitDate.isAfter(this.lastCommit)) {
            this.lastCommit = commitDate;
        }
    }
    
    /**
     * 添加代码变更统计（只统计行数，不计数提交次数）
     */
    public void addCodeStats(int additions, int deletions) {
        this.totalAdditions += additions;
        this.totalDeletions += deletions;
        this.netChanges = this.totalAdditions - this.totalDeletions;
    }
    
    /**
     * 添加提交统计（同时计数提交和统计代码行数）
     * @deprecated 使用 incrementCommit() 和 addCodeStats() 代替
     */
    @Deprecated
    public void addCommitStats(int additions, int deletions, LocalDate commitDate) {
        incrementCommit(commitDate);
        addCodeStats(additions, deletions);
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
    
    public int getTotalAdditions() {
        return totalAdditions;
    }
    
    public void setTotalAdditions(int totalAdditions) {
        this.totalAdditions = totalAdditions;
    }
    
    public int getTotalDeletions() {
        return totalDeletions;
    }
    
    public void setTotalDeletions(int totalDeletions) {
        this.totalDeletions = totalDeletions;
    }
    
    public int getNetChanges() {
        return netChanges;
    }
    
    public void setNetChanges(int netChanges) {
        this.netChanges = netChanges;
    }
    
    public LocalDate getFirstCommit() {
        return firstCommit;
    }
    
    public void setFirstCommit(LocalDate firstCommit) {
        this.firstCommit = firstCommit;
    }
    
    public LocalDate getLastCommit() {
        return lastCommit;
    }
    
    public void setLastCommit(LocalDate lastCommit) {
        this.lastCommit = lastCommit;
    }
    
    @Override
    public String toString() {
        return "GitAuthorStat{" +
                "authorName='" + authorName + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", totalCommits=" + totalCommits +
                ", totalAdditions=" + totalAdditions +
                ", totalDeletions=" + totalDeletions +
                ", netChanges=" + netChanges +
                ", firstCommit=" + firstCommit +
                ", lastCommit=" + lastCommit +
                '}';
    }
}

