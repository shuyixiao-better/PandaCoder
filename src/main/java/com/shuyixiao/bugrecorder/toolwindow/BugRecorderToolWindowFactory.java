package com.shuyixiao.bugrecorder.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.shuyixiao.bugrecorder.ui.BugRecorderToolWindow;
import org.jetbrains.annotations.NotNull;

/**
 * Bug记录器工具窗口工厂
 * 用于在IntelliJ IDEA中创建和注册Bug记录器工具窗口
 */
public class BugRecorderToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建Bug记录器UI组件
        BugRecorderToolWindow bugRecorderToolWindow = new BugRecorderToolWindow(project);

        // 创建内容并添加到工具窗口
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(bugRecorderToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // 对所有项目都可用
        return true;
    }
}