package com.shuyixiao.ui;

import com.intellij.openapi.project.Project;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.shuyixiao.util.TranslationUtil;

/**
 * 用户体验帮助类
 * 统一管理插件的用户交互风格和体验
 */
public class UserExperienceHelper {

    /**
     * 显示状态栏消息
     */
    public static void showStatusMessage(Project project, String message) {
        if (project != null) {
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("PandaCoder.Notifications")
                    .createNotification("PandaCoder: " + message, NotificationType.INFORMATION)
                    .notify(project);
        }
    }

    /**
     * 显示翻译成功的用户反馈
     */
    public static void showTranslationSuccess(Project project, String original, String translated, String engine) {
        // 显示状态栏消息
        showStatusMessage(project, String.format("已翻译: %s → %s", original, translated));
        
        // 显示通知
        TranslationUtil.showTranslationSuccess(project, original, translated, engine);
    }

    /**
     * 显示转换成功的用户反馈
     */
    public static void showConversionSuccess(Project project, String original, String converted, String format) {
        // 显示状态栏消息
        showStatusMessage(project, String.format("已转换为%s: %s → %s", format, original, converted));
        
        // 显示通知
        EnhancedNotificationUtil.showSimpleInfo(project, "转换完成", 
            String.format("已将 \"%s\" 转换为%s格式: \"%s\"", original, format, converted));
    }

    /**
     * 显示文件创建成功的反馈
     */
    public static void showFileCreationSuccess(Project project, String filename, String type) {
        showStatusMessage(project, String.format("已创建%s: %s", type, filename));
        EnhancedNotificationUtil.showSimpleInfo(project, "文件创建成功", 
            String.format("已成功创建%s: %s", type, filename));
    }

    /**
     * 检查API配置的用户友好版本
     */
    public static boolean checkApiConfigurationWithFeedback(Project project) {
        boolean isConfigured = TranslationUtil.checkApiConfiguration(project);
        
        if (!isConfigured) {
            showStatusMessage(project, "需要配置翻译API才能使用此功能");
        }
        
        return isConfigured;
    }

    /**
     * 显示功能首次使用提示
     */
    public static void showFeatureIntroduction(Project project, String featureName, String description) {
        EnhancedNotificationUtil.showSimpleInfo(project, 
            "✨ " + featureName, 
            description + "\n\n💡 提示：您可以在设置中配置更多选项。");
    }

    /**
     * 显示键盘快捷键提示
     */
    public static void showShortcutTip(Project project, String action, String shortcut) {
        EnhancedNotificationUtil.showSimpleInfo(project, 
            "⌨️ 快捷键提示", 
            String.format("%s 的快捷键是 %s", action, shortcut));
    }

    /**
     * 显示欢迎信息（仅在第一次使用时）
     */
    public static void showWelcomeMessageIfFirstTime(Project project) {
        // 这里可以添加逻辑来检查是否是第一次使用
        // 例如检查用户设置或创建标记文件
        WelcomeDialog.show(project);
    }

    /**
     * 统一的错误处理
     */
    public static void handleError(Project project, String operation, Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : "未知错误";
        
        showStatusMessage(project, operation + "失败: " + errorMessage);
        
        EnhancedNotificationUtil.showEnhancedError(
            project,
            operation + "错误",
            "操作未能完成",
            errorMessage,
            () -> checkApiConfigurationWithFeedback(project)
        );
    }

    /**
     * 显示操作进度
     */
    public static void showProgress(Project project, String operation) {
        showStatusMessage(project, operation + "中...");
        EnhancedNotificationUtil.showProgress(project, "⏳ 处理中", operation + "，请稍候...");
    }

    /**
     * 用户交互常量
     */
    public static class Constants {
        public static final String PLUGIN_NAME = "PandaCoder";
        public static final String WELCOME_TITLE = "🐼 " + PLUGIN_NAME + " - 熊猫编码助手";
        
        public static class Messages {
            public static final String API_CONFIG_REQUIRED = "需要配置翻译API才能使用此功能";
            public static final String TRANSLATION_SUCCESS = "翻译完成";
            public static final String CONVERSION_SUCCESS = "转换完成";
            public static final String FILE_CREATION_SUCCESS = "文件创建成功";
        }
    }
} 