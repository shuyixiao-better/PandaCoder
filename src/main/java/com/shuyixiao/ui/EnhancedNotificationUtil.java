package com.shuyixiao.ui;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 增强的通知工具类
 * 提供现代化的错误提示、警告和信息通知
 */
public class EnhancedNotificationUtil {

    private static final NotificationGroup GROUP = NotificationGroupManager.getInstance()
            .getNotificationGroup("PandaCoder Notification Group");

    /**
     * 显示增强的错误通知
     * 支持操作按钮和更详细的错误信息
     */
    public static void showEnhancedError(Project project, String title, String message, 
                                        String details, Runnable configAction) {
        Notification notification = GROUP.createNotification(
                title,
                message,
                NotificationType.ERROR);
        
        // 添加配置操作
        if (configAction != null) {
            notification.addAction(new NotificationAction("⚙️ 去配置") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    configAction.run();
                    notification.expire();
                }
            });
        }
        
        // 添加查看详情操作
        if (details != null && !details.trim().isEmpty()) {
            notification.addAction(new NotificationAction("📋 查看详情") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    showDetailDialog(project, title, details);
                    notification.expire();
                }
            });
        }
        
        notification.notify(project);
    }

    /**
     * 显示API配置错误的特殊通知
     */
    public static void showApiConfigError(Project project, String apiName, String errorMessage, 
                                         String configUrl, Runnable openSettingsAction) {
        String title = String.format("%s API 配置错误", apiName);
        String content = String.format("无法连接到 %s：%s", apiName, errorMessage);
        
        Notification notification = GROUP.createNotification(
                title,
                content,
                NotificationType.ERROR);
        
        // 添加打开设置的操作
        notification.addAction(new NotificationAction("⚙️ 打开设置") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                if (openSettingsAction != null) {
                    openSettingsAction.run();
                }
                notification.expire();
            }
        });
        
        // 添加帮助文档操作
        if (configUrl != null) {
            notification.addAction(new NotificationAction("📖 查看文档") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    try {
                        Desktop.getDesktop().browse(java.net.URI.create(configUrl));
                    } catch (Exception ex) {
                        showSimpleInfo(project, "文档链接", configUrl);
                    }
                    notification.expire();
                }
            });
        }
        
        notification.notify(project);
    }

    /**
     * 显示翻译引擎配置向导通知
     */
    public static void showTranslationSetupGuide(Project project, Runnable openSettingsAction) {
        String title = "🌐 翻译引擎配置向导";
        String content = "检测到您还没有配置任何翻译引擎，让我们来快速设置一个！";
        
        Notification notification = GROUP.createNotification(
                title,
                content,
                NotificationType.INFORMATION);
        
        notification.addAction(new NotificationAction("🚀 立即配置") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                if (openSettingsAction != null) {
                    openSettingsAction.run();
                }
                notification.expire();
            }
        });
        
        notification.addAction(new NotificationAction("🎯 查看支持的引擎") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                showTranslationEngineInfo(project);
                notification.expire();
            }
        });
        
        notification.notify(project);
    }

    /**
     * 显示翻译成功的通知
     */
    public static void showTranslationSuccess(Project project, String original, String translated, String engine) {
        String title = "✅ 翻译完成";
        String content = String.format("\"%s\" → \"%s\" (使用 %s)", original, translated, engine);
        
        Notification notification = GROUP.createNotification(
                title,
                content,
                NotificationType.INFORMATION);
        
        // 添加复制结果的操作
        notification.addAction(new NotificationAction("📋 复制结果") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(translated), null);
                showSimpleInfo(project, "复制成功", "翻译结果已复制到剪贴板");
                notification.expire();
            }
        });
        
        notification.notify(project);
    }

    /**
     * 显示进度通知
     */
    public static void showProgress(Project project, String title, String message) {
        Notification notification = GROUP.createNotification(
                title,
                message,
                NotificationType.INFORMATION);
        
        // 设置较短的自动消失时间
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                Thread.sleep(2000);
                notification.expire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        notification.notify(project);
    }

    /**
     * 显示简单的信息通知
     */
    public static void showSimpleInfo(Project project, String title, String message) {
        Notification notification = GROUP.createNotification(
                title,
                message,
                NotificationType.INFORMATION);

        notification.notify(project);
    }

    /**
     * 显示复制成功的自动消失通知
     * 2秒后自动消失，无需手动点击确定
     *
     * @param project 项目
     * @param message 复制成功的消息，例如 "DSL 已复制到剪贴板"
     */
    public static void showCopySuccess(Project project, String message) {
        Notification notification = GROUP.createNotification(
                "✅ " + message,
                NotificationType.INFORMATION);

        notification.notify(project);

        // 在后台线程中等待2秒后自动消失，避免阻塞UI线程
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Thread.sleep(2000);
                // 在EDT线程上执行expire操作
                ApplicationManager.getApplication().invokeLater(() -> {
                    notification.expire();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 显示警告通知
     */
    public static void showWarning(Project project, String title, String message, Runnable action) {
        Notification notification = GROUP.createNotification(
                title,
                message,
                NotificationType.WARNING);
        
        if (action != null) {
            notification.addAction(new NotificationAction("🔧 修复") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    action.run();
                    notification.expire();
                }
            });
        }
        
        notification.notify(project);
    }

    /**
     * 显示详情对话框
     */
    private static void showDetailDialog(Project project, String title, String details) {
        new DetailDialog(project, title, details).show();
    }

    /**
     * 显示翻译引擎信息
     */
    private static void showTranslationEngineInfo(Project project) {
        String info = "PandaCoder 支持以下翻译引擎：\n\n" +
                      "🥇 国内大模型翻译（推荐）\n" +
                      "   • 通义千问 (Qianwen)\n" +
                      "   • 文心一言 (Wenxin)\n" +
                      "   • 智谱AI (GLM-4)\n\n" +
                      "🥈 Google Cloud Translation\n" +
                      "   • 支持多种语言\n" +
                      "   • 翻译质量优秀\n\n" +
                      "🥉 百度翻译 API\n" +
                      "   • 备用翻译引擎\n" +
                      "   • 稳定可靠\n\n" +
                      "建议优先配置国内大模型，访问速度更快！";
        
        new DetailDialog(project, "支持的翻译引擎", info).show();
    }

    /**
     * 详情对话框
     */
    private static class DetailDialog extends DialogWrapper {
        private final String title;
        private final String content;

        public DetailDialog(@Nullable Project project, String title, String content) {
            super(project);
            this.title = title;
            this.content = content;
            setTitle(title);
            setResizable(true);
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JBPanel<?> panel = new JBPanel<>(new BorderLayout());
            panel.setPreferredSize(JBUI.size(500, 300));
            panel.setBorder(JBUI.Borders.empty(15));

            JTextArea textArea = new JTextArea(content);
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setFont(textArea.getFont().deriveFont(12f));
            textArea.setBackground(UIUtil.getPanelBackground());
            textArea.setBorder(JBUI.Borders.empty(10));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(JBUI.Borders.empty());
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            panel.add(scrollPane, BorderLayout.CENTER);

            return panel;
        }

        @Override
        protected Action[] createActions() {
            return new Action[]{getOKAction()};
        }
    }
} 