package com.shuyixiao.toolwindow.panels;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 功能卡片面板 - 快速访问主要功能
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class FunctionCardsPanel extends JBPanel<FunctionCardsPanel> {
    
    private final Project project;
    
    public FunctionCardsPanel(@NotNull Project project) {
        super();
        this.project = project;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(JBUI.Borders.empty(10, 10, 15, 10));
        setOpaque(false);
        
        // 标题
        JBLabel titleLabel = new JBLabel("⚡ 快速功能");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setBorder(JBUI.Borders.emptyBottom(10));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);
        
        // 功能卡片
        add(createFunctionCard(
            "📊",
            "Git 统计",
            "代码提交统计与分析",
            () -> openToolWindow("Git Statistics")
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "🔍",
            "ES DSL Monitor",
            "Elasticsearch 查询监控",
            () -> openToolWindow("ES DSL Monitor")
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "📝",
            "SQL Monitor",
            "SQL 查询监控与分析",
            () -> openToolWindow("SQL Monitor")
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "🚀",
            "Jenkins 增强",
            "Pipeline 语法高亮与补全",
            () -> showJenkinsInfo()
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "📚",
            "活文档",
            "智能项目文档管理",
            () -> openToolWindow("活文档")
        ));
    }
    
    /**
     * 创建功能卡片
     */
    private JComponent createFunctionCard(String emoji,
                                         String title, 
                                         String description, 
                                         Runnable action) {
        JBPanel<?> card = new JBPanel<>(new BorderLayout(10, 5));
        card.setBorder(JBUI.Borders.empty(12, 12));
        card.setBackground(UIUtil.getPanelBackground());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        // 添加悬停效果
        Color defaultBg = UIUtil.getPanelBackground();
        Color hoverBg = UIUtil.getListSelectionBackground(true);
        
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
                card.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(defaultBg);
                card.repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        
        // 左侧图标
        JBLabel iconLabel = new JBLabel(emoji);
        iconLabel.setFont(iconLabel.getFont().deriveFont(20f));
        card.add(iconLabel, BorderLayout.WEST);
        
        // 中间内容
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        
        JBLabel descLabel = new JBLabel(description);
        descLabel.setForeground(UIUtil.getContextHelpForeground());
        descLabel.setFont(descLabel.getFont().deriveFont(11f));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(descLabel);
        
        card.add(contentPanel, BorderLayout.CENTER);
        
        // 右侧箭头
        JBLabel arrowLabel = new JBLabel("→");
        arrowLabel.setForeground(UIUtil.getContextHelpForeground());
        arrowLabel.setFont(arrowLabel.getFont().deriveFont(16f));
        card.add(arrowLabel, BorderLayout.EAST);
        
        return card;
    }
    
    /**
     * 打开指定的 Tool Window
     */
    private void openToolWindow(String toolWindowId) {
        try {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = manager.getToolWindow(toolWindowId);
            
            if (toolWindow != null) {
                toolWindow.activate(null);
            } else {
                showNotification(
                    "Tool Window 未找到",
                    "无法找到 \"" + toolWindowId + "\" 工具窗口",
                    NotificationType.WARNING
                );
            }
        } catch (Exception e) {
            showNotification(
                "打开失败",
                "无法打开 \"" + toolWindowId + "\" 工具窗口: " + e.getMessage(),
                NotificationType.ERROR
            );
        }
    }
    
    /**
     * 显示 Jenkins 功能信息
     */
    private void showJenkinsInfo() {
        showNotification(
            "Jenkins 增强功能",
            "Jenkins 增强功能已自动启用！\n" +
            "在 Jenkinsfile 中自动提供：\n" +
            "• 语法高亮（11种颜色）\n" +
            "• 环境变量补全\n" +
            "• 参数智能提示\n" +
            "• 文档快速查看",
            NotificationType.INFORMATION
        );
    }
    
    /**
     * 显示通知
     */
    private void showNotification(String title, String content, NotificationType type) {
        Notification notification = new Notification(
            "PandaCoder.Notifications",
            title,
            content,
            type
        );
        Notifications.Bus.notify(notification, project);
    }
}

