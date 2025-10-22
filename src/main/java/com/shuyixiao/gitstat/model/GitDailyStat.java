package com.shuyixiao.gitstat.model;

import java.time.LocalDate;

/**
 * Git 每日统计数据模型
 * 记录每天的代码提交统计信息
 */
public class GitDailyStat {
    
    private LocalDate date;         // 日期
    private int commits;            // 提交次数
    private int additions;          // 新增行数
    private int deletions;          // 删除行数
    private int netChanges;         // 净变化（新增 - 删除）
    private int activeAuthors;      // 活跃作者数
    
    public GitDailyStat(LocalDate date) {
        this.date = date;
        this.commits = 0;
        this.additions = 0;
        this.deletions = 0;
        this.netChanges = 0;
        this.activeAuthors = 0;
    }
    
    public void addStats(int additions, int deletions) {
        this.commits++;
        this.additions += additions;
        this.deletions += deletions;
        this.netChanges = this.additions - this.deletions;
    }
    
    // Getters and Setters
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public int getCommits() {
        return commits;
    }
    
    public void setCommits(int commits) {
        this.commits = commits;
    }
    
    public int getAdditions() {
        return additions;
    }
    
    public void setAdditions(int additions) {
        this.additions = additions;
    }
    
    public int getDeletions() {
        return deletions;
    }
    
    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }
    
    public int getNetChanges() {
        return netChanges;
    }
    
    public void setNetChanges(int netChanges) {
        this.netChanges = netChanges;
    }
    
    public int getActiveAuthors() {
        return activeAuthors;
    }
    
    public void setActiveAuthors(int activeAuthors) {
        this.activeAuthors = activeAuthors;
    }
    
    @Override
    public String toString() {
        return "GitDailyStat{" +
                "date=" + date +
                ", commits=" + commits +
                ", additions=" + additions +
                ", deletions=" + deletions +
                ", netChanges=" + netChanges +
                ", activeAuthors=" + activeAuthors +
                '}';
    }
}

