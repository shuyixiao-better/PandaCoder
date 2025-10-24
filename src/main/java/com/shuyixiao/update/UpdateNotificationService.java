package com.shuyixiao.update;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.net.URI;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateNotificationService.java
 * author 舒一笑不秃头
 * version 1.0.0
 * Description 更新通知服务 在右下角显示优雅的Balloon通知
 * createTime 2025年10月24日 15:30:00
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class UpdateNotificationService {

    private static final Logger LOG = Logger.getInstance(UpdateNotificationService.class);
    private static final String NOTIFICATION_GROUP_ID = "PandaCoder.Notifications";
    private static final String PLUGIN_PAGE_URL = "https://plugins.jetbrains.com/plugin/27533-pandacoder";

    /**
     * 显示更新通知（右下角Balloon）
     * 
     * @param project 项目实例
     * @param currentVersion 当前版本
     * @param latestVersion 最新版本
     * @param releaseNotes 更新说明
     */
    public static void showUpdateNotification(@Nullable Project project, 
                                             @NotNull String currentVersion,
                                             @NotNull String latestVersion,
                                             @Nullable String releaseNotes) {
        
        UpdateSettingsState settings = UpdateSettingsState.getInstance();
        
        // 检查是否应该显示通知
        if (!settings.shouldShowNotification()) {
            LOG.info("跳过更新通知显示，未到提醒时间");
            return;
        }

        // 检查版本是否已被忽略
        if (settings.isVersionIgnored(latestVersion)) {
            LOG.info("跳过更新通知显示，版本已被忽略: " + latestVersion);
            return;
        }

        // 构建通知内容
        String content = buildNotificationContent(currentVersion, latestVersion, releaseNotes);
        
        // 创建通知
        Notification notification = new Notification(
            NOTIFICATION_GROUP_ID,
            "🎉 PandaCoder 新版本可用",
            content,
            NotificationType.INFORMATION
        );

        // 添加"立即更新"操作
        notification.addAction(new NotificationAction("💡 立即更新") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                openPluginPage();
                settings.markNotificationShown(latestVersion);
                n.expire();
            }
        });

        // 添加"明天提醒"操作
        notification.addAction(new NotificationAction("⏰ 明天提醒") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                // 设置24小时后再次提醒
                settings.setLastShownTime(System.currentTimeMillis());
                n.expire();
                LOG.info("用户选择明天提醒");
            }
        });

        // 添加"忽略此版本"操作
        notification.addAction(new NotificationAction("🔕 忽略此版本") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                settings.addIgnoredVersion(latestVersion);
                settings.markNotificationShown(latestVersion);
                n.expire();
                LOG.info("用户选择忽略版本: " + latestVersion);
                
                // 显示确认提示
                showIgnoreConfirmation(project, latestVersion);
            }
        });

        // 设置通知在15秒后自动消失（如果用户没有交互）
        notification.setImportant(false);
        
        // 显示通知
        Notifications.Bus.notify(notification, project);
        
        LOG.info("已显示更新通知: " + currentVersion + " -> " + latestVersion);
    }

    /**
     * 构建通知内容
     */
    private static String buildNotificationContent(@NotNull String currentVersion,
                                                   @NotNull String latestVersion,
                                                   @Nullable String releaseNotes) {
        StringBuilder content = new StringBuilder();
        
        // 版本信息
        content.append("<p style='margin: 5px 0;'>📦 当前版本：<b style='color: #FF6B35;'>")
               .append(currentVersion)
               .append("</b></p>");
        
        content.append("<p style='margin: 5px 0;'>✨ 最新版本：<b style='color: #4CAF50;'>")
               .append(latestVersion)
               .append("</b></p>");
        
        // 更新内容
        if (releaseNotes != null && !releaseNotes.isEmpty()) {
            content.append("<p style='margin: 10px 0 5px 0;'>🚀 <b>更新亮点：</b></p>");
            
            // 解析并格式化更新内容
            String[] features = parseReleaseNotes(releaseNotes);
            for (String feature : features) {
                if (!feature.trim().isEmpty()) {
                    content.append("<p style='margin: 2px 0 2px 15px;'>• ")
                           .append(feature)
                           .append("</p>");
                }
            }
        } else {
            // 默认更新内容
            content.append("<p style='margin: 10px 0 5px 0;'>🚀 <b>更新亮点：</b></p>")
                   .append("<p style='margin: 2px 0 2px 15px;'>• Bug修复和性能优化</p>")
                   .append("<p style='margin: 2px 0 2px 15px;'>• 提升稳定性和用户体验</p>");
        }
        
        return content.toString();
    }

    /**
     * 解析更新说明
     */
    private static String[] parseReleaseNotes(@NotNull String releaseNotes) {
        // 简单解析，支持换行符分隔
        String[] lines = releaseNotes.split("[\n\r]+");
        
        // 只取前5条
        int count = Math.min(lines.length, 5);
        String[] result = new String[count];
        
        for (int i = 0; i < count; i++) {
            String line = lines[i].trim();
            // 移除markdown列表符号
            line = line.replaceAll("^[•\\-*]\\s*", "");
            result[i] = line;
        }
        
        return result;
    }

    /**
     * 打开插件页面
     */
    private static void openPluginPage() {
        try {
            Desktop.getDesktop().browse(new URI(PLUGIN_PAGE_URL));
            LOG.info("已打开插件页面");
        } catch (Exception e) {
            LOG.warn("打开插件页面失败", e);
        }
    }

    /**
     * 显示忽略确认提示
     */
    private static void showIgnoreConfirmation(@Nullable Project project, @NotNull String version) {
        Notification confirmation = new Notification(
            NOTIFICATION_GROUP_ID,
            "已忽略版本 " + version,
            "您不会再收到此版本的更新提醒。如需恢复，请在设置中配置。",
            NotificationType.INFORMATION
        );
        
        confirmation.setImportant(false);
        Notifications.Bus.notify(confirmation, project);
    }

    /**
     * 显示重大版本更新通知
     */
    public static void showMajorUpdateNotification(@Nullable Project project,
                                                   @NotNull String currentVersion,
                                                   @NotNull String latestVersion,
                                                   @Nullable String releaseNotes) {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();
        
        if (!settings.shouldShowNotification() || settings.isVersionIgnored(latestVersion)) {
            return;
        }

        String content = "<p style='margin: 5px 0;'>🎊 <b style='color: #E74C3C;'>重大版本更新！</b></p>" +
                        "<p style='margin: 5px 0;'>📦 当前版本：<b>" + currentVersion + "</b></p>" +
                        "<p style='margin: 5px 0;'>✨ 最新版本：<b style='color: #4CAF50;'>" + latestVersion + "</b></p>" +
                        "<p style='margin: 10px 0 5px 0;'>🌟 <b>重磅功能：</b></p>" +
                        "<p style='margin: 2px 0 2px 15px;'>" + (releaseNotes != null ? releaseNotes : "全新体验，强烈建议更新！") + "</p>";

        Notification notification = new Notification(
            NOTIFICATION_GROUP_ID,
            "🎊 PandaCoder 重大版本更新！",
            content,
            NotificationType.INFORMATION
        );

        notification.addAction(new NotificationAction("🚀 立即更新") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                openPluginPage();
                settings.markNotificationShown(latestVersion);
                n.expire();
            }
        });

        notification.addAction(new NotificationAction("稍后") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                settings.setLastShownTime(System.currentTimeMillis());
                n.expire();
            }
        });

        notification.setImportant(true); // 重要通知，不会自动消失
        Notifications.Bus.notify(notification, project);
    }

    /**
     * 显示Bug修复版本通知
     */
    public static void showBugFixUpdateNotification(@Nullable Project project,
                                                    @NotNull String currentVersion,
                                                    @NotNull String latestVersion) {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();
        
        if (!settings.shouldShowNotification() || settings.isVersionIgnored(latestVersion)) {
            return;
        }

        String content = "<p style='margin: 5px 0;'>🔧 发现重要Bug修复版本</p>" +
                        "<p style='margin: 5px 0;'>📦 " + currentVersion + " → <b style='color: #4CAF50;'>" + latestVersion + "</b></p>" +
                        "<p style='margin: 10px 0 5px 0;'>建议尽快更新以获得最佳体验</p>";

        Notification notification = new Notification(
            NOTIFICATION_GROUP_ID,
            "🔧 PandaCoder Bug修复版本",
            content,
            NotificationType.INFORMATION
        );

        notification.addAction(new NotificationAction("立即更新") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                openPluginPage();
                settings.markNotificationShown(latestVersion);
                n.expire();
            }
        });

        notification.addAction(new NotificationAction("稍后") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                settings.setLastShownTime(System.currentTimeMillis());
                n.expire();
            }
        });

        Notifications.Bus.notify(notification, project);
    }
}

