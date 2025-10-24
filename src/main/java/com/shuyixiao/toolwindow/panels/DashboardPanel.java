package com.shuyixiao.toolwindow.panels;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.shuyixiao.service.PandaCoderSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 仪表盘面板 - 显示版本信息和使用统计
 * 支持动态更新使用次数
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class DashboardPanel extends JBPanel<DashboardPanel> {
    
    private static final String VERSION = com.shuyixiao.version.VersionInfo.getVersion();
    private final Project project;
    private JBLabel usageLabel;
    private Timer refreshTimer;
    
    public DashboardPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        setBorder(JBUI.Borders.empty(12, 10));
        setOpaque(false);
        
        // 初始化UI
        initUI();
        
        // 启动定时刷新（每3秒刷新一次）
        startRefreshTimer();
        
        // 头部区域
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        // 左侧：品牌信息
        JBPanel<?> brandPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);
        
        // 图标
        try {
            Icon icon = IconLoader.getIcon("/icons/pluginIcon.svg", DashboardPanel.class);
            // 缩放图标到合适大小
            JBLabel iconLabel = new JBLabel(icon);
            iconLabel.setBorder(JBUI.Borders.emptyRight(10));
            brandPanel.add(iconLabel);
        } catch (Exception e) {
            // 如果图标加载失败，使用emoji
            JBLabel iconLabel = new JBLabel("🐼");
            iconLabel.setFont(iconLabel.getFont().deriveFont(32f));
            iconLabel.setBorder(JBUI.Borders.emptyRight(10));
            brandPanel.add(iconLabel);
        }
        
        // 标题和副标题
        JBPanel<?> titlePanel = new JBPanel<>();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JBLabel titleLabel = new JBLabel("PandaCoder");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        
        JBLabel subtitleLabel = new JBLabel("中文开发者的智能编码助手");
        subtitleLabel.setForeground(UIUtil.getContextHelpForeground());
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(11f));
        titlePanel.add(subtitleLabel);
        
        brandPanel.add(titlePanel);
        headerPanel.add(brandPanel, BorderLayout.WEST);
        
        // 右侧：版本信息和统计
        JBPanel<?> infoPanel = new JBPanel<>();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        // 版本号
        JBLabel versionLabel = new JBLabel("v" + VERSION);
        versionLabel.setForeground(UIUtil.getContextHelpForeground());
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 10f));
        versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        infoPanel.add(versionLabel);
        
        // 使用统计（保存引用以便后续更新）
        int usageCount = PandaCoderSettings.getInstance(project).getUsageCount();
        usageLabel = new JBLabel("使用 " + usageCount + " 次");
        usageLabel.setForeground(UIUtil.getContextHelpForeground());
        usageLabel.setFont(usageLabel.getFont().deriveFont(Font.PLAIN, 9f));
        usageLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        infoPanel.add(usageLabel);
        
        headerPanel.add(infoPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.CENTER);
        
        // 添加一个细分隔线
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setBorder(JBUI.Borders.emptyTop(10));
        add(separator, BorderLayout.SOUTH);
    }
    
    /**
     * 初始化UI（将原来的构造函数逻辑提取到这里）
     */
    private void initUI() {
        // UI 初始化逻辑已在构造函数中
    }
    
    /**
     * 启动定时刷新
     * 每3秒刷新一次使用统计
     */
    private void startRefreshTimer() {
        refreshTimer = new Timer(3000, e -> refreshUsageCount());
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }
    
    /**
     * 刷新使用次数显示
     */
    public void refreshUsageCount() {
        if (usageLabel != null) {
            int usageCount = PandaCoderSettings.getInstance(project).getUsageCount();
            SwingUtilities.invokeLater(() -> {
                usageLabel.setText("使用 " + usageCount + " 次");
            });
        }
    }
    
    /**
     * 停止刷新定时器（在面板销毁时调用）
     */
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }
}

