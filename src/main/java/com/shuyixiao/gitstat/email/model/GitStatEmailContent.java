package com.shuyixiao.gitstat.email.model;

import com.shuyixiao.gitstat.model.GitDailyStat;
import java.time.LocalDate;
import java.util.Map;

/**
 * Git 统计邮件内容
 * 包含统计数据、趋势分析、排名信息等
 */
public class GitStatEmailContent {
    
    private String authorName;
    private String authorEmail;
    private LocalDate statisticsDate;
    
    // 当日统计
    private int todayCommits;
    private int todayAdditions;
    private int todayDeletions;
    private int todayNetChanges;
    
    // 趋势数据
    private Map<LocalDate, GitDailyStat> last7Days;
    private Map<LocalDate, GitDailyStat> last30Days;
    
    // 排名信息
    private int rankByCommits = -1;
    private int rankByAdditions = -1;
    private int totalAuthors = 0;
    
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
    
    public LocalDate getStatisticsDate() {
        return statisticsDate;
    }
    
    public void setStatisticsDate(LocalDate statisticsDate) {
        this.statisticsDate = statisticsDate;
    }
    
    public int getTodayCommits() {
        return todayCommits;
    }
    
    public void setTodayCommits(int todayCommits) {
        this.todayCommits = todayCommits;
    }
    
    public int getTodayAdditions() {
        return todayAdditions;
    }
    
    public void setTodayAdditions(int todayAdditions) {
        this.todayAdditions = todayAdditions;
    }
    
    public int getTodayDeletions() {
        return todayDeletions;
    }
    
    public void setTodayDeletions(int todayDeletions) {
        this.todayDeletions = todayDeletions;
    }
    
    public int getTodayNetChanges() {
        return todayNetChanges;
    }
    
    public void setTodayNetChanges(int todayNetChanges) {
        this.todayNetChanges = todayNetChanges;
    }
    
    public Map<LocalDate, GitDailyStat> getLast7Days() {
        return last7Days;
    }
    
    public void setLast7Days(Map<LocalDate, GitDailyStat> last7Days) {
        this.last7Days = last7Days;
    }
    
    public Map<LocalDate, GitDailyStat> getLast30Days() {
        return last30Days;
    }
    
    public void setLast30Days(Map<LocalDate, GitDailyStat> last30Days) {
        this.last30Days = last30Days;
    }
    
    public int getRankByCommits() {
        return rankByCommits;
    }
    
    public void setRankByCommits(int rankByCommits) {
        this.rankByCommits = rankByCommits;
    }
    
    public int getRankByAdditions() {
        return rankByAdditions;
    }
    
    public void setRankByAdditions(int rankByAdditions) {
        this.rankByAdditions = rankByAdditions;
    }
    
    public int getTotalAuthors() {
        return totalAuthors;
    }
    
    public void setTotalAuthors(int totalAuthors) {
        this.totalAuthors = totalAuthors;
    }
}

