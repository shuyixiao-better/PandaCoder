package com.shuyixiao.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.shuyixiao.update.UpdateCheckService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateNotificationDialog.java
 * author 舒一笑
 * version 1.0.0
 * Description 版本更新提示对话框 显示新版本信息和更新选项
 * createTime 2025年07月31日 11:50:01
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class UpdateNotificationDialog extends DialogWrapper {

    private final String currentVersion;
    private final String latestVersion;

    public UpdateNotificationDialog(@Nullable Project project, String currentVersion, String latestVersion) {
        super(project);
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;

        setTitle("🎉 PandaCoder 有新版本可用");
        setResizable(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(500, 300));
        mainPanel.setBorder(JBUI.Borders.empty(20));

        // 创建头部面板
        JBPanel<?> headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建内容面板
        JBPanel<?> contentPanel = createContentPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 创建底部面板
        JBPanel<?> footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JBPanel<?> createHeaderPanel() {
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.emptyBottom(15));

        // 左侧：图标和标题
        JBPanel<?> titlePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // 加载插件图标
        Icon pluginIcon;
        try {
            pluginIcon = IconLoader.getIcon("/META-INF/pluginIcon.svg", UpdateNotificationDialog.class);
        } catch (Exception e) {
            pluginIcon = UIUtil.getInformationIcon();
        }

        JBLabel iconLabel = new JBLabel(pluginIcon);
        iconLabel.setBorder(JBUI.Borders.emptyRight(10));
        titlePanel.add(iconLabel);

        JBPanel<?> textPanel = new JBPanel<>(new BorderLayout());
        JBLabel titleLabel = new JBLabel("PandaCoder 版本更新");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        textPanel.add(titleLabel, BorderLayout.NORTH);

        JBLabel subtitleLabel = new JBLabel("发现新版本，建议立即更新");
        subtitleLabel.setForeground(UIUtil.getContextHelpForeground());
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(12f));
        textPanel.add(subtitleLabel, BorderLayout.CENTER);

        titlePanel.add(textPanel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        return headerPanel;
    }

    private JBPanel<?> createContentPanel() {
        JBPanel<?> contentPanel = new JBPanel<>(new BorderLayout());

        // 版本信息
        JBPanel<?> versionPanel = new JBPanel<>(new BorderLayout());
        versionPanel.setBorder(JBUI.Borders.emptyBottom(15));

        JBLabel versionInfoLabel = new JBLabel("<html><body style='width: 400px'>" +
                "<p style='margin-bottom: 10px; color: #4A90E2; font-size: 14px'>" +
                "📦 版本信息</p>" +
                "<p style='margin-bottom: 5px; color: #666666'>" +
                "当前版本：<span style='color: #FF6B35; font-weight: bold'>" + currentVersion + "</span></p>" +
                "<p style='margin-bottom: 15px; color: #666666'>" +
                "最新版本：<span style='color: #4CAF50; font-weight: bold'>" + latestVersion + "</span></p>" +
                "</body></html>");
        versionPanel.add(versionInfoLabel, BorderLayout.NORTH);

        // 更新内容
        JBPanel<?> updateContentPanel = new JBPanel<>(new BorderLayout());
        updateContentPanel.setBorder(JBUI.Borders.emptyTop(10));

        JBLabel updateContentTitle = new JBLabel("✨ 更新内容");
        updateContentTitle.setFont(updateContentTitle.getFont().deriveFont(Font.BOLD, 14f));
        updateContentTitle.setBorder(JBUI.Borders.emptyBottom(8));
        updateContentPanel.add(updateContentTitle, BorderLayout.NORTH);

        String[] updateFeatures = {
                "🐛 修复已知问题，提升稳定性",
                "🚀 新增功能特性，增强用户体验",
                "⚡ 性能优化，提升响应速度",
                "🎨 界面优化，提供更好的视觉效果",
                "🔧 代码重构，提高代码质量"
        };

        JBPanel<?> featuresList = new JBPanel<>();
        featuresList.setLayout(new BoxLayout(featuresList, BoxLayout.Y_AXIS));

        for (String feature : updateFeatures) {
            JBLabel featureLabel = new JBLabel("<html><body style='width: 380px'>" +
                    "<p style='margin: 3px 0; padding: 5px 0'>" + feature + "</p>" +
                    "</body></html>");
            featureLabel.setBorder(JBUI.Borders.emptyLeft(15));
            featuresList.add(featureLabel);
        }

        updateContentPanel.add(featuresList, BorderLayout.CENTER);
        contentPanel.add(versionPanel, BorderLayout.NORTH);
        contentPanel.add(updateContentPanel, BorderLayout.CENTER);

        return contentPanel;
    }

    private JBPanel<?> createFooterPanel() {
        JBPanel<?> footerPanel = new JBPanel<>(new BorderLayout());
        footerPanel.setBorder(JBUI.Borders.emptyTop(15));

        // 操作按钮
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 10, 0));

        // 立即更新按钮
        JButton updateButton = new JButton("🚀 立即更新");
        updateButton.setPreferredSize(new Dimension(120, 35));
        updateButton.addActionListener(e -> {
            openUpdatePage();
            UpdateCheckService.markUpdateNotificationShown();
            doOKAction();
        });
        buttonPanel.add(updateButton);

        // 稍后提醒按钮
        JButton laterButton = new JButton("⏰ 稍后提醒");
        laterButton.setPreferredSize(new Dimension(120, 35));
        laterButton.addActionListener(e -> {
            UpdateCheckService.markUpdateNotificationShown();
            doCancelAction();
        });
        buttonPanel.add(laterButton);

        footerPanel.add(buttonPanel, BorderLayout.CENTER);

        return footerPanel;
    }

    /**
     * 打开更新页面
     */
    private void openUpdatePage() {
        try {
            // 打开插件页面或下载页面
            Desktop.getDesktop().browse(new URI("https://plugins.jetbrains.com/plugin/your-plugin-id"));
        } catch (Exception e) {
            showNotification("无法打开浏览器，请手动访问插件页面进行更新");
        }
    }

    private void showNotification(String message) {
        JOptionPane.showMessageDialog(
                getContentPanel(),
                message,
                "提示",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{
                new AbstractAction("关闭") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doCancelAction();
                    }
                }
        };
    }

    /**
     * 静态方法：显示更新提示对话框
     */
    public static void show(@Nullable Project project, String currentVersion, String latestVersion) {
        UpdateNotificationDialog dialog = new UpdateNotificationDialog(project, currentVersion, latestVersion);
        dialog.show();
    }
}
