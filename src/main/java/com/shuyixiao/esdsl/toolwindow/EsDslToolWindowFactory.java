package com.shuyixiao.esdsl.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.shuyixiao.esdsl.ui.EsDslToolWindow;
import org.jetbrains.annotations.NotNull;

/**
 * ES DSL 工具窗口工厂
 * 用于在 IntelliJ IDEA 中创建和注册 ES DSL 工具窗口
 */
public class EsDslToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建 ES DSL UI 组件
        EsDslToolWindow esDslToolWindow = new EsDslToolWindow(project);
        
        // 创建内容并添加到工具窗口
        Content content = toolWindow.getContentManager().getFactory()
                .createContent(esDslToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // 对所有项目都可用
        return true;
    }
}

