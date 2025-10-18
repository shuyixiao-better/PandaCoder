package com.shuyixiao.sql.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.shuyixiao.sql.ui.SqlToolWindow;
import org.jetbrains.annotations.NotNull;

/**
 * SQL Monitor 工具窗口工厂
 * 负责创建 SQL Monitor 工具窗口
 */
public class SqlToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        SqlToolWindow sqlToolWindow = new SqlToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(sqlToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // SQL Monitor 对所有项目都可用
        return true;
    }
}

