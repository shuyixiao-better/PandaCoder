package com.shuyixiao.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

/**
 * 现代化的PandaCoder欢迎对话框
 * 提供美观的用户界面和更好的用户体验
 */
public class WelcomeDialog extends DialogWrapper {
    
    private static final String VERSION = "1.1.6";
    
    public WelcomeDialog(@Nullable Project project) {
        super(project);
        setTitle("🐼 PandaCoder - 熊猫编码助手");
        setResizable(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(500, 400));
        mainPanel.setBorder(JBUI.Borders.empty(20));
        
        // 创建头部面板
        JBPanel<?> headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // 创建内容面板
        JBPanel<?> contentPanel = createContentPanel();
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
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
            pluginIcon = IconLoader.getIcon("/META-INF/pluginIcon.svg", WelcomeDialog.class);
        } catch (Exception e) {
            pluginIcon = UIUtil.getInformationIcon();
        }
        
        JBLabel iconLabel = new JBLabel(pluginIcon);
        iconLabel.setBorder(JBUI.Borders.emptyRight(10));
        titlePanel.add(iconLabel);
        
        JBPanel<?> textPanel = new JBPanel<>(new BorderLayout());
        JBLabel titleLabel = new JBLabel("PandaCoder");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        textPanel.add(titleLabel, BorderLayout.NORTH);
        
        JBLabel subtitleLabel = new JBLabel("中文开发者的智能编码助手");
        subtitleLabel.setForeground(UIUtil.getContextHelpForeground());
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(12f));
        textPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        titlePanel.add(textPanel);
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // 右侧：版本号
        JBLabel versionLabel = new JBLabel("v" + VERSION);
        versionLabel.setForeground(UIUtil.getContextHelpForeground());
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        versionLabel.setBorder(JBUI.Borders.empty(5));
        headerPanel.add(versionLabel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JBPanel<?> createContentPanel() {
        JBPanel<?> contentPanel = new JBPanel<>(new BorderLayout());
        
        // 欢迎信息
        JBLabel welcomeLabel = new JBLabel("<html><body style='width: 400px'>" +
                "<p style='margin-bottom: 10px; color: #4A90E2; font-size: 14px'>" +
                "🎉 欢迎使用 PandaCoder！您的智能编码伙伴已就绪</p>" +
                "<p style='margin-bottom: 15px; color: #666666'>专为中文开发者设计，让编码更高效、更智能</p>" +
                "</body></html>");
        contentPanel.add(welcomeLabel, BorderLayout.NORTH);
        
        // 功能特性列表
        JBPanel<?> featuresPanel = new JBPanel<>(new BorderLayout());
        featuresPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        JBLabel featuresTitle = new JBLabel("✨ 核心功能");
        featuresTitle.setFont(featuresTitle.getFont().deriveFont(Font.BOLD, 14f));
        featuresTitle.setBorder(JBUI.Borders.emptyBottom(8));
        featuresPanel.add(featuresTitle, BorderLayout.NORTH);
        
        String[] features = {
            "🔤 智能中文转大小驼峰 - 一键转换，支持多种命名规范",
            "📝 智能中文转大写带下划线 - 常量命名的最佳选择", 
            "🏷️ 中文类名自动转换 - 让类名更规范、更专业",
            "🤖 智能中文类生成 - AI驱动的代码生成体验",
            "🌐 多引擎翻译支持 - 国内大模型、Google、百度三级备用",
            "⚙️ Jenkins Pipeline增强 - 语法高亮、智能补全",
            "🍃 SpringBoot配置图标 - 技术栈可视化识别",
            "🎯 类名前缀识别 - 支持Service:用户管理等格式",
            "📝 自定义文件模板 - 支持用户自定义Java注释模板"
        };
        
        JBPanel<?> featuresList = new JBPanel<>();
        featuresList.setLayout(new BoxLayout(featuresList, BoxLayout.Y_AXIS));
        
        for (String feature : features) {
            JBLabel featureLabel = new JBLabel("<html><body style='width: 380px'>" +
                    "<p style='margin: 3px 0; padding: 5px 0'>" + feature + "</p>" +
                    "</body></html>");
            featureLabel.setBorder(JBUI.Borders.emptyLeft(15));
            featuresList.add(featureLabel);
        }
        
        featuresPanel.add(featuresList, BorderLayout.CENTER);
        contentPanel.add(featuresPanel, BorderLayout.CENTER);
        
        return contentPanel;
    }
    
        private JBPanel<?> createFooterPanel() {
        JBPanel<?> footerPanel = new JBPanel<>(new BorderLayout());
        footerPanel.setBorder(JBUI.Borders.emptyTop(15));
        
        // 作者信息
        JBPanel<?> authorPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        
        JBLabel authorLabel = new JBLabel("<html>" +
                "<span style='color: #666666'>作者：</span>" +
                "<span style='color: #4A90E2; font-weight: bold'>舒一笑不秃头</span>" +
                "</html>");
        authorPanel.add(authorLabel);
        
        footerPanel.add(authorPanel, BorderLayout.WEST);
        
        // 操作按钮
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        // 公众号按钮
        JButton wechatButton = new JButton("📱 关注公众号");
        wechatButton.putClientProperty("JButton.buttonType", "borderless");
        wechatButton.setToolTipText("舒一笑的架构笔记");
        wechatButton.addActionListener(e -> {
            // 显示微信公众号二维码对话框
            QRCodeDialog.showWechatQRCode(null);
        });
        buttonPanel.add(wechatButton);
        
        // 问题反馈按钮
        JButton feedbackButton = new JButton("💬 问题反馈");
        feedbackButton.putClientProperty("JButton.buttonType", "borderless");
        feedbackButton.addActionListener(e -> {
            showNotification("如有问题或建议，请联系微信：Tobeabetterman1001,备注来意-PandaCoder问题交流");
        });
        buttonPanel.add(feedbackButton);
        
        footerPanel.add(buttonPanel, BorderLayout.EAST);
        
        return footerPanel;
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
            new AbstractAction("开始使用") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doOKAction();
                }
            },
            new AbstractAction("稍后再说") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doCancelAction();
                }
            }
        };
    }
    
    /**
     * 静态方法：显示欢迎对话框
     */
    public static void show(@Nullable Project project) {
        WelcomeDialog dialog = new WelcomeDialog(project);
        dialog.show();
    }
} 