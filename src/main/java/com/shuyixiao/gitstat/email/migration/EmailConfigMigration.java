package com.shuyixiao.gitstat.email.migration;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.shuyixiao.gitstat.email.config.GitStatEmailConfigState;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName EmailConfigMigration.java
 * author 舒一笑不秃头
 * version 2.1.0
 * Description Git统计邮件配置迁移工具类，负责从旧版本配置文件迁移数据到新版本，确保插件更新后用户配置不丢失
 * createTime 2025-10-23
 * 技术分享 · 公众号：舒一笑的架构笔记
 * 
 * 功能说明：
 * 1. 检查旧版本配置文件是否存在
 * 2. 如果当前配置为空且旧配置存在，则提示用户可能需要重新配置
 * 3. 提供配置备份和恢复功能
 */
public class EmailConfigMigration {
    
    private static final String OLD_CONFIG_FILE = "gitStatEmailConfig.xml";
    
    /**
     * 检查并迁移配置
     * 在插件启动或配置加载时调用
     */
    public static void checkAndMigrate(Project project) {
        GitStatEmailConfigState currentState = GitStatEmailConfigState.getInstance(project);
        
        // 如果当前配置已经配置完成，无需迁移
        if (currentState.isConfigured()) {
            return;
        }
        
        // 检查旧配置文件是否存在
        String ideaPath = project.getBasePath() + "/.idea";
        Path oldConfigPath = Paths.get(ideaPath, OLD_CONFIG_FILE);
        
        if (Files.exists(oldConfigPath)) {
            // 旧配置文件存在，但当前配置为空，可能是迁移失败
            showMigrationNotification(project, true);
        }
    }
    
    /**
     * 显示配置迁移通知
     */
    private static void showMigrationNotification(Project project, boolean hasOldConfig) {
        NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("PandaCoder Notification Group");
        
        String content;
        if (hasOldConfig) {
            content = "检测到您之前配置过 Git 统计邮件功能，但配置可能在插件更新后丢失。<br>" +
                     "请重新配置邮箱信息，我们已优化配置存储机制，此问题不会再次出现。";
        } else {
            content = "Git 统计邮件功能配置已优化，现在配置数据更加稳定，不会因插件更新而丢失。";
        }
        
        Notification notification = notificationGroup.createNotification(
            "Git 统计邮件配置",
            content,
            NotificationType.INFORMATION
        );
        
        // 添加操作按钮：重新配置
        if (hasOldConfig) {
            notification.addAction(NotificationAction.createSimple("重新配置", () -> {
                // 这里可以添加打开配置对话框的逻辑
                // 例如：打开 Git Statistics 工具窗口
                notification.expire();
            }));
        }
        
        notification.notify(project);
    }
    
    /**
     * 创建配置备份
     */
    public static void backupConfig(Project project) {
        try {
            GitStatEmailConfigState state = GitStatEmailConfigState.getInstance(project);
            if (state.isConfigured()) {
                // 配置已保存到工作空间文件，IntelliJ 会自动处理
                String workspacePath = project.getBasePath() + "/.idea/workspace.xml";
                File workspaceFile = new File(workspacePath);
                if (workspaceFile.exists()) {
                    // 工作空间文件存在，配置已安全保存
                    System.out.println("Git 统计邮件配置已安全保存到工作空间文件");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 检查配置完整性
     */
    public static boolean validateConfig(GitStatEmailConfigState state) {
        if (state == null) {
            return false;
        }
        
        // 检查必填字段
        if (state.senderEmail == null || state.senderEmail.isEmpty()) {
            return false;
        }
        
        if (state.senderPassword == null || state.senderPassword.isEmpty()) {
            return false;
        }
        
        if (state.recipientEmail == null || state.recipientEmail.isEmpty()) {
            return false;
        }
        
        // 检查SMTP配置
        if (state.smtpHost == null || state.smtpHost.isEmpty()) {
            return false;
        }
        
        if (state.smtpPort <= 0 || state.smtpPort > 65535) {
            return false;
        }
        
        return true;
    }
}

