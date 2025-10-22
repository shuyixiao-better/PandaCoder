package com.shuyixiao.gitstat.email.model;

import java.time.LocalDateTime;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName GitStatEmailRecord.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description Git统计邮件发送历史记录模型类，记录每次邮件发送的时间、接收者、发送状态、统计数据快照等信息，用于历史查询和问题排查
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class GitStatEmailRecord {
    
    private String recordId;
    private LocalDateTime sendTime;
    private String recipient;
    private String subject;
    private boolean success;
    private String errorMessage;
    
    // 统计数据快照
    private int commits;
    private int additions;
    private int deletions;
    private int netChanges;
    
    // Getters and Setters
    
    public String getRecordId() {
        return recordId;
    }
    
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    
    public LocalDateTime getSendTime() {
        return sendTime;
    }
    
    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}

