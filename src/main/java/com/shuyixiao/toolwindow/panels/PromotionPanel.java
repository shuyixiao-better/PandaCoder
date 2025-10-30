package com.shuyixiao.toolwindow.panels;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.shuyixiao.advice.PluginAdviceDialog;
import com.shuyixiao.ui.QRCodeDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 推广面板 - 可折叠的作者信息和推广内容
 * 优雅地展示公众号、社交链接等商业化内容
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class PromotionPanel extends JBPanel<PromotionPanel> {
    
    private final Project project;
    private boolean expanded = false;
    private final JBPanel<?> contentPanel;
    private final JBLabel expandIcon;
    private final JButton expandButton;
    
    // 企业服务区域
    private boolean enterpriseExpanded = false;
    private JBPanel<?> enterpriseContentPanel;
    private JBLabel expandIconEnterprise;
    
    public PromotionPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1, 0, 0, 0),
            JBUI.Borders.empty(12, 10)
        ));
        setOpaque(false);
        
        // 头部（可点击展开/折叠）
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JBLabel titleLabel = new JBLabel("🌟 跟随作者成长");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        expandIcon = new JBLabel("▼");
        expandIcon.setForeground(UIUtil.getContextHelpForeground());
        expandIcon.setFont(expandIcon.getFont().deriveFont(10f));
        headerPanel.add(expandIcon, BorderLayout.EAST);
        
        // 点击展开/折叠
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
        });
        
        // 添加一个按钮来展开
        expandButton = new JButton("点击展开");
        expandButton.putClientProperty("JButton.buttonType", "borderless");
        expandButton.setFont(expandButton.getFont().deriveFont(10f));
        expandButton.addActionListener(e -> toggleExpanded());
        expandButton.setVisible(false); // 默认隐藏，用图标就够了
        
        add(headerPanel, BorderLayout.NORTH);
        
        // 内容面板（默认折叠）
        contentPanel = createContentPanel();
        contentPanel.setVisible(false);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * 切换展开/折叠状态
     */
    private void toggleExpanded() {
        expanded = !expanded;
        contentPanel.setVisible(expanded);
        expandIcon.setText(expanded ? "▲" : "▼");
        revalidate();
        repaint();
    }
    
    /**
     * 创建内容面板
     */
    private JBPanel<?> createContentPanel() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.emptyTop(12));
        panel.setOpaque(false);
        
        // 作者信息
        JBPanel<?> authorPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        authorPanel.setOpaque(false);
        authorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JBLabel authorLabel = new JBLabel(
            "<html>" +
            "作者：<b>@舒一笑不秃头</b><br/>" +
            "<span style='color: #888; font-size: 10px;'>TorchV AI 工程师 | 专注于大模型应用与插件开发</span>" +
            "</html>"
        );
        authorLabel.setFont(authorLabel.getFont().deriveFont(11f));
        authorPanel.add(authorLabel);
        
        panel.add(authorPanel);
        panel.add(Box.createVerticalStrut(12));
        
        // 公众号按钮
        JButton wechatButton = new JButton("📱 关注公众号");
        wechatButton.putClientProperty("JButton.buttonType", "borderless");
        wechatButton.setFont(wechatButton.getFont().deriveFont(12f));
        wechatButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        wechatButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wechatButton.addActionListener(e -> {
            QRCodeDialog.showWechatQRCode(project);
        });
        panel.add(wechatButton);
        JButton coffeeButton = new JButton("☕️ 请作者喝杯");
        coffeeButton.putClientProperty("JButton.buttonType", "borderless");
        coffeeButton.setFont(coffeeButton.getFont().deriveFont(12f));
        coffeeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        coffeeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        coffeeButton.addActionListener(e -> {
            QRCodeDialog.showCoffeeQRCode(project);
        });
        panel.add(coffeeButton);
        JButton adviceButton = new JButton("✍️ 插件的建议");
        adviceButton.putClientProperty("JButton.buttonType", "borderless");
        adviceButton.setFont(coffeeButton.getFont().deriveFont(12f));
        adviceButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        adviceButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adviceButton.addActionListener(e -> {
            // 显示插件建议反馈对话框
            PluginAdviceDialog.show(project);
        });
        panel.add(adviceButton);
        JButton versionHistoryButton = new JButton("📋 历史版本说明");
        versionHistoryButton.putClientProperty("JButton.buttonType", "borderless");
        versionHistoryButton.setFont(coffeeButton.getFont().deriveFont(12f));
        versionHistoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionHistoryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        versionHistoryButton.addActionListener(e -> {
            // 打开历史版本说明链接
            openUrl("https://www.poeticcoder.com/articles/panda-coder-intro.html");
        });
        panel.add(versionHistoryButton);
        
        panel.add(Box.createVerticalStrut(10));
        
        // 社交链接
        JBPanel<?> linksPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linksPanel.setOpaque(false);
        linksPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        linksPanel.add(createLinkLabel("🐙 GitHub", 
            "https://github.com/shuyixiao-better/PandaCoder"));
        linksPanel.add(new JBLabel("|"));
        linksPanel.add(createLinkLabel("🐱 Gitee",
                "https://gitee.com/shuyixiao-only/PandaCoder"));
        linksPanel.add(new JBLabel("|"));
        linksPanel.add(createLinkLabel("📝 博客", 
            "https://www.poeticcoder.com"));
        
        panel.add(linksPanel);
        
        panel.add(Box.createVerticalStrut(15));
        
        // 分隔线
        JSeparator separator1 = new JSeparator(JSeparator.HORIZONTAL);
        separator1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator1);
        
        panel.add(Box.createVerticalStrut(12));
        
        // 企业服务区域
        panel.add(createEnterpriseSection());
        
        panel.add(Box.createVerticalStrut(12));
        
        // 分隔线
        JSeparator separator2 = new JSeparator(JSeparator.HORIZONTAL);
        separator2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator2);
        
        panel.add(Box.createVerticalStrut(12));
        
        // 未来规划区域
        panel.add(createFuturePlanSection());
        
        return panel;
    }
    
    /**
     * 创建链接标签
     */
    private JComponent createLinkLabel(String text, String url) {
        JBLabel label = new JBLabel(text);
        label.setForeground(new Color(30, 144, 255)); // 蓝色
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setFont(label.getFont().deriveFont(10f));
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUrl(url);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(new Color(0, 100, 200)); // 深蓝色
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(new Color(30, 144, 255)); // 恢复蓝色
            }
        });
        
        return label;
    }
    
    /**
     * 创建企业服务区域（可折叠）
     */
    private JComponent createEnterpriseSection() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 标题（可点击展开）
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JBLabel titleLabel = new JBLabel("🏢 企业 AI 解决方案");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        expandIconEnterprise = new JBLabel("▼");
        expandIconEnterprise.setForeground(UIUtil.getContextHelpForeground());
        expandIconEnterprise.setFont(expandIconEnterprise.getFont().deriveFont(10f));
        headerPanel.add(expandIconEnterprise, BorderLayout.EAST);
        
        // 点击展开/折叠
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleEnterpriseSection();
            }
        });
        
        panel.add(headerPanel);
        
        // 内容面板（默认折叠）
        enterpriseContentPanel = createEnterpriseContent();
        enterpriseContentPanel.setVisible(false);
        panel.add(enterpriseContentPanel);
        
        return panel;
    }
    
    /**
     * 创建企业服务内容
     */
    private JBPanel<?> createEnterpriseContent() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.emptyTop(10));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // TorchV 介绍
        JBLabel introLabel = new JBLabel(
            "<html>" +
            "<div style='width: 250px;'>" +
            "<b>TorchV AIS</b> - 大模型知识协作系统<br/><br/>" +
            "<span style='font-size: 10px;'>" +
            "✨ <b>核心能力：</b><br/>" +
            "• 快速搭建 RAG 应用<br/>" +
            "• 智能客服机器人<br/>" +
            "• 企业知识库管理<br/>" +
            "• 支持私有化部署<br/><br/>" +
            "🎯 <b>适用场景：</b><br/>" +
            "客服问答、内部知识管理、<br/>研发文档助手、合同预审助手" +
            "</span>" +
            "</div>" +
            "</html>"
        );
        introLabel.setFont(introLabel.getFont().deriveFont(10f));
        introLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(introLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        // 按钮组
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton learnMoreButton = new JButton("了解详情");
        learnMoreButton.putClientProperty("JButton.buttonType", "borderless");
        learnMoreButton.setFont(learnMoreButton.getFont().deriveFont(10f));
        learnMoreButton.addActionListener(e -> {
            openUrl("https://torchv.com/?utm_source=pandacoder&utm_medium=plugin&utm_campaign=enterprise");
        });
        
        JButton demoButton = new JButton("商务联系");
        demoButton.putClientProperty("JButton.buttonType", "borderless");
        demoButton.setFont(demoButton.getFont().deriveFont(10f));
        demoButton.addActionListener(e -> {
            showTorchVContactDialog();
        });
        
        buttonPanel.add(learnMoreButton);
        buttonPanel.add(demoButton);
        
        panel.add(buttonPanel);
        
        return panel;
    }
    
    /**
     * 切换企业服务区域展开/折叠
     */
    private void toggleEnterpriseSection() {
        enterpriseExpanded = !enterpriseExpanded;
        enterpriseContentPanel.setVisible(enterpriseExpanded);
        expandIconEnterprise.setText(enterpriseExpanded ? "▲" : "▼");
        revalidate();
        repaint();
    }
    
    /**
     * 创建未来规划区域
     */
    private JComponent createFuturePlanSection() {
        JBPanel<?> panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JBLabel label = new JBLabel(
                "<html>" +
                        "<span style='color: #888; font-size: 10px;'>💡 更多高级功能开发中...</span>" +
                        "</html>"
        );
        label.setFont(label.getFont().deriveFont(10f));
        panel.add(label);
        
        return panel;
    }
    
    /**
     * 显示 TorchV 商务联系图片对话框
     */
    private void showTorchVContactDialog() {
        try {
            // 加载图片
            java.net.URL imageUrl = getClass().getResource("/images/torchv-business.png");
            if (imageUrl == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "无法找到商务联系图片",
                    "错误",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            
            ImageIcon icon = new ImageIcon(imageUrl);
            
            // 创建自定义对话框
            JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), "TorchV 商务联系", true);
            dialog.setLayout(new BorderLayout());
            
            // 图片标签
            JBLabel imageLabel = new JBLabel(icon);
            imageLabel.setBorder(JBUI.Borders.empty(10));
            
            // 添加到滚动面板（以防图片太大）
            com.intellij.ui.components.JBScrollPane scrollPane = new com.intellij.ui.components.JBScrollPane(imageLabel);
            scrollPane.setBorder(JBUI.Borders.empty());
            
            // 说明文字
            JBPanel<?> infoPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER));
            infoPanel.setBorder(JBUI.Borders.empty(5, 10));
            JBLabel infoLabel = new JBLabel(
                "<html>" +
                "<div style='text-align: center;'>" +
                "扫描二维码或添加微信/手机号联系 TorchV 商务团队<br/>" +
                "<span style='color: #888; font-size: 10px;'>了解企业级 AI 解决方案</span>" +
                "</div>" +
                "</html>"
            );
            infoPanel.add(infoLabel);
            
            // 按钮面板
            JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 10, 10));
            
            JButton visitWebsiteButton = new JButton("访问官网");
            visitWebsiteButton.addActionListener(e -> {
                openUrl("https://torchv.com/?utm_source=pandacoder&utm_medium=plugin&utm_campaign=contact");
                dialog.dispose();
            });
            
            JButton closeButton = new JButton("关闭");
            closeButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(visitWebsiteButton);
            buttonPanel.add(closeButton);
            
            // 组装对话框
            JBPanel<?> contentPanel = new JBPanel<>(new BorderLayout());
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            contentPanel.add(infoPanel, BorderLayout.NORTH);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.add(contentPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "显示商务联系信息失败: " + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * 打开 URL
     */
    private void openUrl(String url) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
}

