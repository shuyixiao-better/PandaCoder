package com.shuyixiao.gitstat.email.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName GitStatEmailConfigState.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description Git统计邮件配置持久化状态类，使用IntelliJ Platform的PersistentStateComponent机制将邮件配置保存到gitStatEmailConfig.xml文件中，支持项目级配置存储
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
@State(
    name = "GitStatEmailConfig",
    storages = @Storage("gitStatEmailConfig.xml")
)
public class GitStatEmailConfigState implements PersistentStateComponent<GitStatEmailConfigState> {
    
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
        XmlSerializerUtil.copyBean(state, this);
    }
    
    public static GitStatEmailConfigState getInstance(Project project) {
        return project.getService(GitStatEmailConfigState.class);
    }
}

