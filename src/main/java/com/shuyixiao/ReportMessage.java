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
 * Description PandaCoder欢迎信息
 * createTime 2024年08月21日 21:53:00
 */
public class ReportMessage extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取国际化的消息和标题，如果没有可以使用默认值
        String message = getLocalizedMessage("report.message", "欢迎使用 PandaCoder - 中文开发者的智能编码助手！\n\n" +
                "版本：1.1.2\n" +
                "功能特性：\n" +
                "- 智能中文转小驼峰\n" +
                "- 中文类名自动转换\n" +
                "- 智能中文类生成\n\n" +
                "作者：舒一笑");
        String title = getLocalizedMessage("report.title", "PandaCoder 熊猫编码助手");

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
