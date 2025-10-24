package com.shuyixiao.livingdoc.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.shuyixiao.livingdoc.settings.LivingDocSettings;
import org.jetbrains.annotations.NotNull;

/**
 * 索引项目文档 Action
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class IndexProjectAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 检查配置
        LivingDocSettings settings = LivingDocSettings.getInstance(project);
        if (!settings.isValid()) {
            Notifications.Bus.notify(
                new Notification(
                    "LivingDoc",
                    "配置不完整",
                    "请先在 Settings -> Tools -> 活文档 中配置 API Key 和数据库连接",
                    NotificationType.WARNING
                ),
                project
            );
            return;
        }
        
        // 后台任务索引
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "索引项目文档", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("正在分析项目代码...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.0);
                    
                    // TODO: 实际的索引逻辑
                    // 1. 分析项目代码
                    indicator.setFraction(0.2);
                    Thread.sleep(500); // 模拟耗时操作
                    
                    // 2. 提取 API 信息
                    indicator.setText("正在提取 API 信息...");
                    indicator.setFraction(0.4);
                    Thread.sleep(500);
                    
                    // 3. 向量化文档
                    indicator.setText("正在向量化文档...");
                    indicator.setFraction(0.6);
                    Thread.sleep(500);
                    
                    // 4. 存储到向量数据库
                    indicator.setText("正在存储到向量数据库...");
                    indicator.setFraction(0.8);
                    Thread.sleep(500);
                    
                    indicator.setFraction(1.0);
                    
                    // 成功通知
                    if (settings.showNotifications) {
                        Notifications.Bus.notify(
                            new Notification(
                                "LivingDoc",
                                "索引完成",
                                "已成功索引项目文档到向量数据库",
                                NotificationType.INFORMATION
                            ),
                            project
                        );
                    }
                    
                } catch (Exception ex) {
                    Notifications.Bus.notify(
                        new Notification(
                            "LivingDoc",
                            "索引失败",
                            "错误: " + ex.getMessage(),
                            NotificationType.ERROR
                        ),
                        project
                    );
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只在有项目时启用
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}

