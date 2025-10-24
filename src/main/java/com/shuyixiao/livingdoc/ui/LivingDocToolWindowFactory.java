package com.shuyixiao.livingdoc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 活文档工具窗口工厂
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class LivingDocToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LivingDocToolWindowPanel panel = new LivingDocToolWindowPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

