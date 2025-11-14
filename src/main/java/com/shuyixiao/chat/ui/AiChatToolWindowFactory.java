package com.shuyixiao.chat.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class AiChatToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        AiChatToolWindowPanel panel = new AiChatToolWindowPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

