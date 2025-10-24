package com.shuyixiao.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.shuyixiao.service.PandaCoderSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * PandaCoder Tool Window 工厂类
 * 负责创建和初始化 Tool Window
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class PandaCoderToolWindowFactory implements ToolWindowFactory, DumbAware {
    
    private static final int AUTO_HIDE_DELAY_MS = 3000; // 首次展开3秒后自动折叠
    
    @Override
    public void createToolWindowContent(@NotNull Project project, 
                                       @NotNull ToolWindow toolWindow) {
        // 创建 Tool Window 内容
        PandaCoderToolWindow window = new PandaCoderToolWindow(project, toolWindow);
        
        Content content = ContentFactory.getInstance()
            .createContent(window.getContent(), "", false);
        
        toolWindow.getContentManager().addContent(content);
        
        // 首次安装：自动展开 3 秒后自动折叠
        handleFirstInstall(project, toolWindow);
    }
    
    /**
     * 处理首次安装的自动展开逻辑
     */
    private void handleFirstInstall(Project project, ToolWindow toolWindow) {
        PandaCoderSettings settings = PandaCoderSettings.getInstance(project);
        
        // 如果Tool Window还未自动展开过，则自动展开
        if (!settings.isToolWindowAutoShown()) {
            // 延迟一下，确保UI已经准备好
            SwingUtilities.invokeLater(() -> {
                toolWindow.show(() -> {
                    // 标记已展开过
                    settings.setToolWindowAutoShown();
                    
                    // 3秒后自动折叠
                    Timer timer = new Timer(AUTO_HIDE_DELAY_MS, e -> {
                        toolWindow.hide(null);
                    });
                    timer.setRepeats(false);
                    timer.start();
                });
            });
        }
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // 对所有项目可用
        return true;
    }
}

