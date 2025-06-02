package com.shuyixiao.notification;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;

/**
 * 通知工具类
 * 提供各种类型的通知显示
 */
public class NotificationUtil {

    private static final NotificationGroup GROUP = NotificationGroupManager.getInstance()
            .getNotificationGroup("PandaCoder Notification Group");

    /**
     * 显示信息通知
     *
     * @param project 当前项目
     * @param title 通知标题
     * @param content 通知内容
     */
    public static void showInfoNotification(Project project, String title, String content) {
        Notification notification = GROUP.createNotification(
                title,
                content,
                NotificationType.INFORMATION);

        notification.notify(project);
    }

    /**
     * 显示警告通知
     *
     * @param project 当前项目
     * @param title 通知标题
     * @param content 通知内容
     */
    public static void showWarningNotification(Project project, String title, String content) {
        Notification notification = GROUP.createNotification(
                title,
                content,
                NotificationType.WARNING);

        notification.notify(project);
    }

    /**
     * 显示错误通知
     *
     * @param project 当前项目
     * @param title 通知标题
     * @param content 通知内容
     */
    public static void showErrorNotification(Project project, String title, String content) {
        Notification notification = GROUP.createNotification(
                title,
                content,
                NotificationType.ERROR);

        notification.notify(project);
    }
}
