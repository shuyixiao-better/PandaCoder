package com.shuyixiao.gitstat.email.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.gitstat.email.model.GitStatEmailConfig;
import com.shuyixiao.gitstat.email.model.GitStatEmailContent;
import com.shuyixiao.gitstat.email.model.GitStatEmailRecord;
import com.shuyixiao.gitstat.email.util.PasswordEncryptor;
import com.shuyixiao.gitstat.model.GitAuthorDailyStat;
import com.shuyixiao.gitstat.model.GitDailyStat;
import com.shuyixiao.gitstat.service.GitStatService;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName GitStatEmailService.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description Git统计邮件服务核心类，提供手动发送、定时发送、SMTP连接测试等功能，负责统计数据收集、邮件内容生成、定时任务调度、发送历史记录等
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
@Service(Service.Level.PROJECT)
public final class GitStatEmailService {
    
    private static final Logger LOG = Logger.getInstance(GitStatEmailService.class);
    
    private final Project project;
    private final GitStatService gitStatService;
    private final EmailTemplateService templateService;
    
    private GitStatEmailConfig config;
    private ScheduledExecutorService scheduler;
    private final List<GitStatEmailRecord> emailHistory = new ArrayList<>();
    
    public GitStatEmailService(Project project) {
        this.project = project;
        this.gitStatService = project.getService(GitStatService.class);
        this.templateService = new EmailTemplateService();
        this.config = new GitStatEmailConfig();
    }
    
    /**
     * 手动发送今日统计邮件
     */
    public boolean sendTodayEmail() {
        return sendEmail(LocalDate.now());
    }
    
    /**
     * 发送指定日期的统计邮件
     */
    public boolean sendEmail(LocalDate date) {
        if (!config.isValid()) {
            LOG.warn("Email configuration is not valid");
            return false;
        }
        
        try {
            LOG.info("Sending email for date: " + date);
            
            // 1. 收集统计数据
            GitStatEmailContent content = collectStatistics(date);
            
            // 2. 生成邮件内容
            String emailBody = templateService.generateEmailBody(content, config);
            
            // 3. 发送邮件
            sendViaSMTP(emailBody, date);
            
            // 4. 记录发送历史
            recordSendHistory(content, true, null);
            
            LOG.info("Email sent successfully for date: " + date);
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to send email", e);
            recordSendHistory(null, false, e.getMessage());
            return false;
        }
    }
    
    /**
     * 收集统计数据
     */
    private GitStatEmailContent collectStatistics(LocalDate date) {
        GitStatEmailContent content = new GitStatEmailContent();
        content.setStatisticsDate(date);
        
        String filterAuthor = config.getFilterAuthor();
        
        if (filterAuthor != null && !filterAuthor.isEmpty()) {
            // 筛选特定作者
            GitAuthorDailyStat authorStat = gitStatService.getAllAuthorDailyStats()
                .stream()
                .filter(s -> s.getDate().equals(date))
                .filter(s -> s.getAuthorName().equals(filterAuthor))
                .findFirst()
                .orElse(null);
            
            if (authorStat != null) {
                content.setAuthorName(authorStat.getAuthorName());
                content.setAuthorEmail(authorStat.getAuthorEmail());
                content.setTodayCommits(authorStat.getCommits());
                content.setTodayAdditions(authorStat.getAdditions());
                content.setTodayDeletions(authorStat.getDeletions());
                content.setTodayNetChanges(authorStat.getNetChanges());
            } else {
                // 没有数据时填充默认值
                content.setAuthorName(filterAuthor);
                content.setAuthorEmail("");
                content.setTodayCommits(0);
                content.setTodayAdditions(0);
                content.setTodayDeletions(0);
                content.setTodayNetChanges(0);
            }
        } else {
            // 统计所有作者
            GitDailyStat dailyStat = gitStatService.getAllDailyStats()
                .stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst()
                .orElse(null);
            
            if (dailyStat != null) {
                content.setAuthorName("所有开发者");
                content.setAuthorEmail("");
                content.setTodayCommits(dailyStat.getCommits());
                content.setTodayAdditions(dailyStat.getAdditions());
                content.setTodayDeletions(dailyStat.getDeletions());
                content.setTodayNetChanges(dailyStat.getNetChanges());
            } else {
                // 没有数据时填充默认值
                content.setAuthorName("所有开发者");
                content.setAuthorEmail("");
                content.setTodayCommits(0);
                content.setTodayAdditions(0);
                content.setTodayDeletions(0);
                content.setTodayNetChanges(0);
            }
        }
        
        // 如果需要趋势分析
        if (config.isIncludeTrends()) {
            content.setLast7Days(getLast7DaysStats(date));
            content.setLast30Days(getLast30DaysStats(date));
        }
        
        // 计算排名（如果筛选了特定作者）
        if (filterAuthor != null && !filterAuthor.isEmpty()) {
            calculateRanking(content, filterAuthor);
        }
        
        return content;
    }
    
    /**
     * 获取最近7天统计
     */
    private Map<LocalDate, GitDailyStat> getLast7DaysStats(LocalDate endDate) {
        Map<LocalDate, GitDailyStat> stats = new LinkedHashMap<>();
        List<GitDailyStat> allStats = gitStatService.getAllDailyStats();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            GitDailyStat stat = allStats.stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst()
                .orElse(new GitDailyStat(date));
            stats.put(date, stat);
        }
        
        return stats;
    }
    
    /**
     * 获取最近30天统计
     */
    private Map<LocalDate, GitDailyStat> getLast30DaysStats(LocalDate endDate) {
        Map<LocalDate, GitDailyStat> stats = new LinkedHashMap<>();
        List<GitDailyStat> allStats = gitStatService.getAllDailyStats();
        
        for (int i = 29; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            GitDailyStat stat = allStats.stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst()
                .orElse(new GitDailyStat(date));
            stats.put(date, stat);
        }
        
        return stats;
    }
    
    /**
     * 计算作者排名
     */
    private void calculateRanking(GitStatEmailContent content, String authorName) {
        // 按提交次数排名
        List<String> authorsByCommits = new ArrayList<>();
        gitStatService.getAuthorStatsSortedByCommits().forEach(a -> 
            authorsByCommits.add(a.getAuthorName())
        );
        int rankByCommits = authorsByCommits.indexOf(authorName) + 1;
        content.setRankByCommits(rankByCommits > 0 ? rankByCommits : -1);
        
        // 按代码量排名
        List<String> authorsByAdditions = new ArrayList<>();
        gitStatService.getAuthorStatsSortedByLines().forEach(a -> 
            authorsByAdditions.add(a.getAuthorName())
        );
        int rankByAdditions = authorsByAdditions.indexOf(authorName) + 1;
        content.setRankByAdditions(rankByAdditions > 0 ? rankByAdditions : -1);
        
        // 总作者数
        content.setTotalAuthors(gitStatService.getAllAuthorStats().size());
    }
    
    /**
     * 通过 SMTP 发送邮件
     */
    private void sendViaSMTP(String emailBody, LocalDate date) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.auth", "true");
        
        if (config.isEnableTLS()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (config.isEnableSSL()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        
        // 创建会话
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String decryptedPassword = PasswordEncryptor.decrypt(
                    config.getSenderPassword(), 
                    project
                );
                return new PasswordAuthentication(
                    config.getSenderEmail(), 
                    decryptedPassword
                );
            }
        });
        
        // 创建消息
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(
            config.getSenderEmail(), 
            config.getSenderName()
        ));
        message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(config.getRecipientEmail())
        );
        
        // 设置主题（替换日期占位符）
        String subject = config.getEmailSubject()
            .replace("{DATE}", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        message.setSubject(subject);
        
        // 设置内容
        if (config.isSendHtml()) {
            message.setContent(emailBody, "text/html; charset=utf-8");
        } else {
            message.setText(emailBody);
        }
        
        // 发送
        Transport.send(message);
    }
    
    /**
     * 启动定时任务
     */
    public void startScheduledTask() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (!config.isEnableScheduled() || !config.isValid()) {
            LOG.info("Scheduled task not started: " + 
                     (config.isEnableScheduled() ? "configuration invalid" : "scheduled disabled"));
            return;
        }
        
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 计算首次执行延迟
        long initialDelay = calculateInitialDelay();
        long period = TimeUnit.DAYS.toMillis(1);
        
        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    // 发送昨天的统计
                    sendEmail(LocalDate.now().minusDays(1));
                } catch (Exception e) {
                    LOG.error("Scheduled email task failed", e);
                }
            },
            initialDelay,
            period,
            TimeUnit.MILLISECONDS
        );
        
        LOG.info("Scheduled email task started, first run in " + 
                 (initialDelay / 1000 / 60) + " minutes");
    }
    
    /**
     * 停止定时任务
     */
    public void stopScheduledTask() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
            LOG.info("Scheduled email task stopped");
        }
    }
    
    /**
     * 计算首次执行延迟（毫秒）
     */
    private long calculateInitialDelay() {
        String[] parts = config.getScheduledTime().split(":");
        int targetHour = Integer.parseInt(parts[0]);
        int targetMinute = Integer.parseInt(parts[1]);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now
            .withHour(targetHour)
            .withMinute(targetMinute)
            .withSecond(0)
            .withNano(0);
        
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        
        return Duration.between(now, nextRun).toMillis();
    }
    
    /**
     * 测试 SMTP 连接
     */
    public boolean testConnection() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getSmtpHost());
            props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");
            
            if (config.isEnableTLS()) {
                props.put("mail.smtp.starttls.enable", "true");
            }
            if (config.isEnableSSL()) {
                props.put("mail.smtp.ssl.enable", "true");
            }
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String decryptedPassword = PasswordEncryptor.decrypt(
                        config.getSenderPassword(), 
                        project
                    );
                    return new PasswordAuthentication(
                        config.getSenderEmail(), 
                        decryptedPassword
                    );
                }
            });
            
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            
            LOG.info("SMTP connection test successful");
            return true;
        } catch (Exception e) {
            LOG.error("SMTP connection test failed", e);
            return false;
        }
    }
    
    /**
     * 记录发送历史
     */
    private void recordSendHistory(GitStatEmailContent content, boolean success, String errorMessage) {
        GitStatEmailRecord record = new GitStatEmailRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setSendTime(LocalDateTime.now());
        record.setRecipient(config.getRecipientEmail());
        record.setSubject(config.getEmailSubject());
        record.setSuccess(success);
        record.setErrorMessage(errorMessage);
        
        if (content != null) {
            record.setCommits(content.getTodayCommits());
            record.setAdditions(content.getTodayAdditions());
            record.setDeletions(content.getTodayDeletions());
            record.setNetChanges(content.getTodayNetChanges());
        }
        
        emailHistory.add(0, record); // 添加到开头
        
        // 保留最近 100 条记录
        if (emailHistory.size() > 100) {
            emailHistory.subList(100, emailHistory.size()).clear();
        }
    }
    
    // Getters and Setters
    
    public GitStatEmailConfig getConfig() {
        return config;
    }
    
    public void setConfig(GitStatEmailConfig config) {
        this.config = config;
        
        // 如果配置变更，重启定时任务
        if (config.isEnableScheduled()) {
            startScheduledTask();
        } else {
            stopScheduledTask();
        }
    }
    
    public List<GitStatEmailRecord> getEmailHistory() {
        return new ArrayList<>(emailHistory);
    }
}

