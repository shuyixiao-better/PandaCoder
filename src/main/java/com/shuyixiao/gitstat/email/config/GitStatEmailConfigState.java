package com.shuyixiao.gitstat.email.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName GitStatEmailConfigState.java
 * author 舒一笑不秃头
 * version 2.1.0
 * Description Git统计邮件配置持久化状态类，使用IntelliJ Platform的PersistentStateComponent机制将邮件配置保存到工作空间文件中，确保插件更新后配置不丢失
 * createTime 2025-10-22
 * updateTime 2025-10-23
 * 技术分享 · 公众号：舒一笑的架构笔记
 * 
 * 修复说明：
 * 1. 使用 StoragePathMacros.WORKSPACE_FILE 确保配置存储在稳定的工作空间文件中
 * 2. 添加配置版本号，支持未来的配置迁移
 * 3. 增强配置持久化稳定性，防止插件更新后配置丢失
 */
@State(
    name = "GitStatEmailConfig",
    storages = {
        @Storage(value = StoragePathMacros.WORKSPACE_FILE),
        @Storage(value = "gitStatEmailConfig.xml", deprecated = true)  // 保留旧配置作为备用
    }
)
public class GitStatEmailConfigState implements PersistentStateComponent<GitStatEmailConfigState> {
    
    // 配置版本号，用于未来的配置迁移
    public int configVersion = 1;
    
    // SMTP 配置
    public String smtpHost = "smtp.gmail.com";
    public int smtpPort = 587;
    public boolean enableTLS = true;
    public boolean enableSSL = false;
    
    // 认证信息
    public String senderEmail = "";
    public String senderPassword = "";  // 加密存储
    public String senderName = "Git 统计";
    public String recipientEmail = "";
    
    // 定时发送配置
    public boolean enableScheduled = false;
    public String scheduledTime = "18:00";
    
    // 统计配置
    public String filterAuthor = "";
    public boolean includeTrends = true;
    public boolean sendHtml = true;
    public String emailSubject = "📊 Git 统计日报 - {DATE}";
    
    @Override
    public @Nullable GitStatEmailConfigState getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull GitStatEmailConfigState state) {
        // 复制配置
        XmlSerializerUtil.copyBean(state, this);
        
        // 配置迁移逻辑：如果版本号为0（旧版本），升级到版本1
        if (this.configVersion == 0) {
            this.configVersion = 1;
        }
        
        // 确保关键字段不为null
        if (this.smtpHost == null) {
            this.smtpHost = "smtp.gmail.com";
        }
        if (this.senderEmail == null) {
            this.senderEmail = "";
        }
        if (this.senderPassword == null) {
            this.senderPassword = "";
        }
        if (this.senderName == null) {
            this.senderName = "Git 统计";
        }
        if (this.recipientEmail == null) {
            this.recipientEmail = "";
        }
        if (this.scheduledTime == null) {
            this.scheduledTime = "18:00";
        }
        if (this.filterAuthor == null) {
            this.filterAuthor = "";
        }
        if (this.emailSubject == null) {
            this.emailSubject = "📊 Git 统计日报 - {DATE}";
        }
    }
    
    /**
     * 获取配置状态实例
     * 注意：此方法会自动从多个存储位置加载配置，优先使用工作空间文件
     */
    public static GitStatEmailConfigState getInstance(Project project) {
        return project.getService(GitStatEmailConfigState.class);
    }
    
    /**
     * 检查配置是否已初始化（是否有有效的邮箱配置）
     */
    public boolean isConfigured() {
        return senderEmail != null && !senderEmail.isEmpty() 
            && senderPassword != null && !senderPassword.isEmpty()
            && recipientEmail != null && !recipientEmail.isEmpty();
    }
}

