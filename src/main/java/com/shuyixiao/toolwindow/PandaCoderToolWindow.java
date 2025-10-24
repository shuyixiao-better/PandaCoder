package com.shuyixiao.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.toolwindow.panels.DashboardPanel;
import com.shuyixiao.toolwindow.panels.FunctionCardsPanel;
import com.shuyixiao.toolwindow.panels.PromotionPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * PandaCoder Tool Window 主窗口
 * 整合仪表盘、功能卡片、推广面板等组件
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class PandaCoderToolWindow {
    
    private final Project project;
    private final ToolWindow toolWindow;
    private final JBPanel<?> mainPanel;
    
    public PandaCoderToolWindow(@NotNull Project project, 
                                @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.mainPanel = createMainPanel();
    }
    
    /**
     * 获取内容组件（供外部使用）
     */
    @NotNull
    public JComponent getContent() {
        // 使用滚动面板包装，以防内容过长
        JBScrollPane scrollPane = new JBScrollPane(mainPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        return scrollPane;
    }
    
    /**
     * 创建主面板
     */
    private JBPanel<?> createMainPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        
        // 创建内容面板（垂直布局）
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        
        // 1. 仪表盘（顶部）
        DashboardPanel dashboard = new DashboardPanel(project);
        dashboard.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(dashboard);
        
        // 2. 功能卡片（中部）
        FunctionCardsPanel functionCards = new FunctionCardsPanel(project);
        functionCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(functionCards);
        
        // 3. 推广面板（底部，可折叠）
        PromotionPanel promotion = new PromotionPanel(project);
        promotion.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(promotion);
        
        // 4. 弹性空间（将推广面板推到底部）
        contentPanel.add(Box.createVerticalGlue());
        
        panel.add(contentPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    /**
     * 获取项目引用
     */
    @NotNull
    public Project getProject() {
        return project;
    }
    
    /**
     * 获取 Tool Window 引用
     */
    @NotNull
    public ToolWindow getToolWindow() {
        return toolWindow;
    }
}

