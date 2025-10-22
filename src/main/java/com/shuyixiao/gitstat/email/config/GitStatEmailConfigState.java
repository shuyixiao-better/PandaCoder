package com.shuyixiao.gitstat.email.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Git 统计邮件配置状态持久化
 * 使用 IntelliJ Platform 的 PersistentStateComponent 机制
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

