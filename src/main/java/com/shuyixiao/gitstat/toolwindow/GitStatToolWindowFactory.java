package com.shuyixiao.gitstat.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.shuyixiao.gitstat.ui.GitStatToolWindow;
import org.jetbrains.annotations.NotNull;

/**
 * Git 统计工具窗口工厂
 * 负责创建 Git 统计工具窗口
 */
public class GitStatToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建 Git 统计 UI 组件
        GitStatToolWindow gitStatToolWindow = new GitStatToolWindow(project);
        
        // 创建内容并添加到工具窗口
        Content content = toolWindow.getContentManager().getFactory()
                .createContent(gitStatToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Git 统计对所有项目都可用
        return true;
    }
}

