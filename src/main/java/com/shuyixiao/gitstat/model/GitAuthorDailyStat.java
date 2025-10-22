package com.shuyixiao.gitstat.model;

import java.time.LocalDate;

/**
 * Git 作者每日统计数据模型
 * 记录每个作者每天的代码提交统计信息
 */
public class GitAuthorDailyStat {
    
    private String authorName;      // 作者姓名
    private String authorEmail;     // 作者邮箱
    private LocalDate date;         // 日期
    private int commits;            // 当天提交次数
    private int additions;          // 当天新增行数
    private int deletions;          // 当天删除行数
    private int netChanges;         // 净变化（新增 - 删除）
    
    public GitAuthorDailyStat(String authorName, String authorEmail, LocalDate date) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.date = date;
        this.commits = 0;
        this.additions = 0;
        this.deletions = 0;
        this.netChanges = 0;
    }
    
    public void addStats(int additions, int deletions) {
        this.commits++;
        this.additions += additions;
        this.deletions += deletions;
        this.netChanges = this.additions - this.deletions;
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
    
    @Override
    public String toString() {
        return "GitAuthorDailyStat{" +
                "authorName='" + authorName + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", date=" + date +
                ", commits=" + commits +
                ", additions=" + additions +
                ", deletions=" + deletions +
                ", netChanges=" + netChanges +
                '}';
    }
}

