package com.shuyixiao.livingdoc.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * 打开搜索对话框 Action
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class SearchDocAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 打开 Tool Window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow("活文档");
        
        if (toolWindow != null) {
            toolWindow.show();
            // 切换到搜索 Tab
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}

