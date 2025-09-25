package com.shuyixiao;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.shuyixiao.ui.WelcomeDialog;
/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName ReportMessage.java
 * author 舒一笑不秃头 yixiaoshu88@163.com
 * version 1.0.0
 * Description PandaCoder欢迎信息
 * createTime 2024年08月21日 21:53:00
 */
public class ReportMessage extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 显示现代化的欢迎对话框
        WelcomeDialog.show(e.getProject());
    }
}
