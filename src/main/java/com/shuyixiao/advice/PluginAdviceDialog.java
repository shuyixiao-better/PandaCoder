package com.shuyixiao.advice;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.mail.MessagingException;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName: PluginAdviceDialog.java
 * author: 舒一笑不秃头
 * version: 2.2.0
 * Description: 插件建议反馈对话框，提供优雅的用户界面供用户提交意见、建议和Bug反馈
 * createTime: 2025-10-24
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class PluginAdviceDialog extends DialogWrapper {
    
    private final Project project;
    private final PluginAdviceService adviceService;
    
    private JComboBox<String> feedbackTypeCombo;
    private JBTextArea contentArea;
    private JTextField contactField;
    private JBLabel remainingCountLabel;
    
    public PluginAdviceDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        this.adviceService = new PluginAdviceService();
        
        setTitle("💌 PandaCoder 建议反馈");
        setResizable(true);
        init();
        
        updateRemainingCount();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(550, 450));
        mainPanel.setBorder(JBUI.Borders.empty(15));
        
        // 顶部引导区
        JBPanel<?> headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // 表单区
        JBPanel<?> formPanel = createFormPanel();
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // 底部提示区
        JBPanel<?> footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    /**
     * 创建头部引导面板
     */
    private JBPanel<?> createHeaderPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(102, 126, 234, 20));
        panel.setBorder(JBUI.Borders.empty(15));
        
        JBLabel titleLabel = new JBLabel(
            "<html>" +
            "<div style='text-align: center;'>" +
            "<h2 style='color: #667eea; margin: 0;'>✨ 您的反馈让 PandaCoder 更好</h2>" +
            "<p style='color: #666; margin-top: 8px; font-size: 12px;'>" +
            "感谢您使用 PandaCoder！我们非常重视您的意见和建议<br/>" +
            "无论是功能建议、Bug反馈还是使用体验，都欢迎告诉我们" +
            "</p>" +
            "</div>" +
            "</html>"
        );
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(titleLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建表单面板
     */
    private JBPanel<?> createFormPanel() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.emptyTop(15));
        
        // 反馈类型
        panel.add(createLabel("反馈类型", "请选择您要反馈的类型"));
        String[] types = {"功能建议", "Bug反馈", "使用体验", "其他"};
        feedbackTypeCombo = new JComboBox<>(types);
        feedbackTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(feedbackTypeCombo);
        panel.add(Box.createVerticalStrut(15));
        
        // 反馈内容
        panel.add(createLabel("反馈内容", "请详细描述您的问题、建议或想法"));
        contentArea = new JBTextArea();
        contentArea.setRows(8);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(JBUI.Borders.empty(8));
        
        // 添加占位符效果
        contentArea.setForeground(UIUtil.getContextHelpForeground());
        contentArea.setText("例如：\n" +
                "• 希望增加某个功能...\n" +
                "• 在使用XX功能时遇到了XX问题...\n" +
                "• 建议优化XX体验...\n" +
                "• 希望支持XX场景...");
        
        contentArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (contentArea.getForeground().equals(UIUtil.getContextHelpForeground())) {
                    contentArea.setText("");
                    contentArea.setForeground(UIUtil.getLabelForeground());
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (contentArea.getText().trim().isEmpty()) {
                    contentArea.setForeground(UIUtil.getContextHelpForeground());
                    contentArea.setText("例如：\n" +
                            "• 希望增加某个功能...\n" +
                            "• 在使用XX功能时遇到了XX问题...\n" +
                            "• 建议优化XX体验...\n" +
                            "• 希望支持XX场景...");
                }
            }
        });
        
        JBScrollPane scrollPane = new JBScrollPane(contentArea);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        panel.add(scrollPane);
        panel.add(Box.createVerticalStrut(15));
        
        // 联系方式
        panel.add(createLabel("联系方式（可选）", "留下您的微信、QQ或邮箱，方便我们与您沟通"));
        contactField = new JTextField();
        contactField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(contactField);
        
        return panel;
    }
    
    /**
     * 创建标签
     */
    private JComponent createLabel(String title, String description) {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout(5, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        
        JBLabel descLabel = new JBLabel("(" + description + ")");
        descLabel.setForeground(UIUtil.getContextHelpForeground());
        descLabel.setFont(descLabel.getFont().deriveFont(10f));
        
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(descLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建底部提示面板
     */
    private JBPanel<?> createFooterPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyTop(15));
        panel.setOpaque(false);
        
        // 左侧：剩余次数提示
        remainingCountLabel = new JBLabel();
        remainingCountLabel.setFont(remainingCountLabel.getFont().deriveFont(10f));
        panel.add(remainingCountLabel, BorderLayout.WEST);
        
        // 右侧：隐私提示
        JBLabel privacyLabel = new JBLabel(
            "<html><span style='color: #888; font-size: 10px;'>🔒 您的反馈将通过加密邮件发送</span></html>"
        );
        panel.add(privacyLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * 更新剩余次数显示
     */
    private void updateRemainingCount() {
        int remaining = FeedbackRateLimiter.getRemainingCount();
        String color = remaining > 3 ? "#4caf50" : (remaining > 0 ? "#ff9800" : "#f44336");
        remainingCountLabel.setText(
            String.format("<html><span style='color: %s; font-size: 10px;'>📊 今日剩余反馈次数：%d/6</span></html>", 
                    color, remaining)
        );
    }
    
    @Override
    protected Action[] createActions() {
        return new Action[]{
                new AbstractAction("📧 发送反馈") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doOKAction();
                    }
                },
                new AbstractAction("取消") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doCancelAction();
                    }
                }
        };
    }
    
    @Override
    protected void doOKAction() {
        // 验证输入
        String content = contentArea.getText().trim();
        if (content.isEmpty() || content.startsWith("例如：")) {
            Messages.showWarningDialog(
                    getContentPanel(),
                    "请填写反馈内容哦 😊",
                    "温馨提示"
            );
            return;
        }
        
        if (content.length() < 10) {
            Messages.showWarningDialog(
                    getContentPanel(),
                    "反馈内容至少需要10个字符，请详细描述您的问题或建议 📝",
                    "温馨提示"
            );
            return;
        }
        
        // 检查限流
        if (!FeedbackRateLimiter.canSendToday()) {
            int sendCount = FeedbackRateLimiter.getTodaySendCount();
            Messages.showWarningDialog(
                    getContentPanel(),
                    String.format("您今天已发送 %d 次反馈，已达到每日上限（6次）\n明天再来分享您的想法吧！😊", sendCount),
                    "温馨提示"
            );
            return;
        }
        
        // 发送反馈
        try {
            String feedbackType = (String) feedbackTypeCombo.getSelectedItem();
            String contactInfo = contactField.getText().trim();
            
            // 显示发送中提示
            getOKAction().setEnabled(false);
            getCancelAction().setEnabled(false);
            
            // 在后台线程发送
            new Thread(() -> {
                try {
                    adviceService.sendFeedback(feedbackType, content, contactInfo, project);
                    
                    // 成功后在UI线程显示提示
                    SwingUtilities.invokeLater(() -> {
                        Messages.showInfoMessage(
                                getContentPanel(),
                                "感谢您的反馈！✨\n" +
                                        "我们已经收到您的" + feedbackType + "，会尽快处理。\n\n" +
                                        "今日剩余反馈次数：" + FeedbackRateLimiter.getRemainingCount() + "/6",
                                "发送成功"
                        );
                        close(OK_EXIT_CODE);
                    });
                } catch (MessagingException ex) {
                    // 失败后在UI线程显示错误提示
                    SwingUtilities.invokeLater(() -> {
                        getOKAction().setEnabled(true);
                        getCancelAction().setEnabled(true);
                        showSendErrorDialog(content, ex.getMessage());
                    });
                }
            }).start();
            
        } catch (Exception ex) {
            getOKAction().setEnabled(true);
            getCancelAction().setEnabled(true);
            showSendErrorDialog(content, ex.getMessage());
        }
    }
    
    /**
     * 显示发送失败对话框（提供备用方案）
     */
    private void showSendErrorDialog(String content, String errorMessage) {
        String receiverEmail = adviceService.getReceiverEmail();
        
        int choice = Messages.showDialog(
                getContentPanel(),
                "邮件发送失败 😢\n\n" +
                        "可能原因：网络连接问题或邮件服务暂时不可用\n" +
                        "错误信息：" + errorMessage + "\n\n" +
                        "💡 您可以：\n" +
                        "1. 点击\"复制内容\"，稍后通过其他方式发送给作者\n" +
                        "2. 点击\"重试\"再次尝试发送\n" +
                        "3. 直接发送邮件到：" + receiverEmail,
                "发送失败",
                new String[]{"复制内容", "重试", "取消"},
                0,
                Messages.getWarningIcon()
        );
        
        if (choice == 0) {
            // 复制内容到剪贴板
            String feedbackType = (String) feedbackTypeCombo.getSelectedItem();
            String contactInfo = contactField.getText().trim();
            
            StringBuilder clipboardContent = new StringBuilder();
            clipboardContent.append("【PandaCoder 反馈】\n\n");
            clipboardContent.append("反馈类型：").append(feedbackType).append("\n\n");
            clipboardContent.append("反馈内容：\n").append(content).append("\n\n");
            if (!contactInfo.isEmpty()) {
                clipboardContent.append("联系方式：").append(contactInfo).append("\n\n");
            }
            clipboardContent.append("作者邮箱：").append(receiverEmail);
            
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(clipboardContent.toString()), null);
            
            Messages.showInfoMessage(
                    getContentPanel(),
                    "内容已复制到剪贴板！📋\n\n" +
                            "您可以通过以下方式将反馈发送给作者：\n" +
                            "1. 邮件：" + receiverEmail + "\n" +
                            "2. 微信：扫描\"关注公众号\"二维码联系\n" +
                            "3. GitHub：提交 Issue\n\n" +
                            "感谢您的反馈！💕",
                    "已复制"
            );
        } else if (choice == 1) {
            // 重试
            doOKAction();
        }
    }
    
    /**
     * 显示建议反馈对话框
     */
    public static void show(@Nullable Project project) {
        PluginAdviceDialog dialog = new PluginAdviceDialog(project);
        dialog.show();
    }
}

