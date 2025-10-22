package com.shuyixiao.gitstat.email.model;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName GitStatEmailConfig.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description Git统计邮件配置模型类，用于存储SMTP服务器配置、发送者信息、接收者信息、定时发送配置等邮件发送相关的所有配置参数
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class GitStatEmailConfig {
    
    // SMTP 服务器配置
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private boolean enableTLS = true;
    private boolean enableSSL = false;
    
    // 认证信息
    private String senderEmail = "";
    private String senderPassword = "";  // 加密存储
    private String senderName = "Git 统计";
    private String recipientEmail = "";
    
    // 定时发送配置
    private boolean enableScheduled = false;
    private String scheduledTime = "18:00";
    
    // 统计配置
    private String filterAuthor = null;  // null 表示所有作者
    private boolean includeTrends = true;
    private boolean sendHtml = true;
    private String emailSubject = "📊 Git 统计日报 - {DATE}";
    
    // Getters and Setters
    
    public String getSmtpHost() {
        return smtpHost;
    }
    
    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }
    
    public int getSmtpPort() {
        return smtpPort;
    }
    
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }
    
    public boolean isEnableTLS() {
        return enableTLS;
    }
    
    public void setEnableTLS(boolean enableTLS) {
        this.enableTLS = enableTLS;
    }
    
    public boolean isEnableSSL() {
        return enableSSL;
    }
    
    public void setEnableSSL(boolean enableSSL) {
        this.enableSSL = enableSSL;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getSenderPassword() {
        return senderPassword;
    }
    
    public void setSenderPassword(String senderPassword) {
        this.senderPassword = senderPassword;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public boolean isEnableScheduled() {
        return enableScheduled;
    }
    
    public void setEnableScheduled(boolean enableScheduled) {
        this.enableScheduled = enableScheduled;
    }
    
    public String getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public String getFilterAuthor() {
        return filterAuthor;
    }
    
    public void setFilterAuthor(String filterAuthor) {
        this.filterAuthor = filterAuthor;
    }
    
    public boolean isIncludeTrends() {
        return includeTrends;
    }
    
    public void setIncludeTrends(boolean includeTrends) {
        this.includeTrends = includeTrends;
    }
    
    public boolean isSendHtml() {
        return sendHtml;
    }
    
    public void setSendHtml(boolean sendHtml) {
        this.sendHtml = sendHtml;
    }
    
    public String getEmailSubject() {
        return emailSubject;
    }
    
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }
    
    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        return smtpHost != null && !smtpHost.isEmpty()
            && smtpPort > 0
            && senderEmail != null && !senderEmail.isEmpty()
            && senderPassword != null && !senderPassword.isEmpty()
            && recipientEmail != null && !recipientEmail.isEmpty();
    }
}

