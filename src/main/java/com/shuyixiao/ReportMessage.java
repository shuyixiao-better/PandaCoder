package com.shuyixiao;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName ReportMessage.java
 * author 舒一笑 yixiaoshu88@163.com
 * version 1.0.0
 * Description IDEA插件开发消息提示成功
 * createTime 2024年08月21日 21:53:00
 */
public class ReportMessage extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取国际化的消息和标题，如果没有可以使用默认值
        String message = getLocalizedMessage("report.message", "欢迎使用中文类转化插件");
        String title = getLocalizedMessage("report.title", "一笑友情提醒");

        // 显示消息对话框
        showMessageDialogOnUIThread(message, title);
    }

    private String getLocalizedMessage(String key, String defaultMessage) {
        // 这里可以添加从资源文件中读取的逻辑，目前简单返回默认值
        // 实际实现时可以使用 ResourceBundle.getBundle("messages.Bundle").getString(key)
        return defaultMessage;
    }

    private void showMessageDialogOnUIThread(String message, String title) {
        // 确保消息对话框是在 UI 线程中显示
        ApplicationManager.getApplication().invokeLater(() ->
                Messages.showMessageDialog(message, title, Messages.getInformationIcon())
        );
    }
}
