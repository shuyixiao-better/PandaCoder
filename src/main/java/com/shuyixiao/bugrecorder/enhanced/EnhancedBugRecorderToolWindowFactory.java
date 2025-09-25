package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

/**
 * 增强版Bug记录工具窗口工厂
 * 创建和管理增强版Bug记录工具窗口
 */
public class EnhancedBugRecorderToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建增强版Bug记录工具窗口
        EnhancedBugRecorderToolWindow enhancedToolWindow = new EnhancedBugRecorderToolWindow(project);
        
        // 初始化工具窗口
        enhancedToolWindow.initToolWindow(toolWindow);
        
        // 创建内容
        Content content = toolWindow.getContentManager().getFactory().createContent(enhancedToolWindow.getContent(), "", false);
        
        // 添加到工具窗口
        toolWindow.getContentManager().addContent(content);
        
        // 设置工具窗口属性
        toolWindow.setAvailable(true);
        // toolWindow.setToHideOnDispose(true); // 已过时的方法
        
        // 保存工具窗口引用以便后续访问
        // project.putUserData(EnhancedBugRecorderToolWindow.KEY, enhancedToolWindow); // 修复类型不匹配问题
    }
}