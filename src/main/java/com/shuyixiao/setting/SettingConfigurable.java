package com.shuyixiao.setting;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName SettingConfigurable.java
 * author 舒一笑 yixiaoshu88@163.com
 * version 1.0.0
 * Description 设置面板增加配置项
 * createTime 2024年09月07日 19:39:00
 */
public class SettingConfigurable implements SearchableConfigurable {

    private JTextArea templateTextArea;
    private JTextField prefixesTextField;
    private JPasswordField baiduApiKeyField;
    private JTextField baiduAppIdField;
    private JPanel panel;

    @NotNull
    @Override
    public String getId() {
        return "FileAnnotationInformationConfiguration";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "PandaCoder";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // 使用GridBagLayout可以更灵活地排列组件
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 创建模板区域
        JLabel templateLabel = new JLabel("文件注释模板：");
        templateTextArea = new JTextArea(10, 40);
        templateTextArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(templateTextArea);

        // 确保从设置中加载模板
        String template = PluginSettings.getInstance().getTemplate();
        if (template != null) {
            templateTextArea.setText(template);
        }

        // 创建百度应用ID输入框
        JLabel appIdLabel = new JLabel("百度应用ID：");
        baiduAppIdField = new JTextField(40);
        baiduAppIdField.setText(PluginSettings.getInstance().getBaiduAppId());

        // 创建百度API密钥输入框
        JLabel apiKeyLabel = new JLabel("百度API密钥：");
        baiduApiKeyField = new JPasswordField(40);
        baiduApiKeyField.setText(PluginSettings.getInstance().getBaiduApiKey());

        // 添加测试API配置按钮
        JButton testApiButton = new JButton("验证API配置");
        testApiButton.setToolTipText("测试当前输入的API密钥和应用ID是否能正确连接到百度翻译服务");
        testApiButton.addActionListener(e -> {
            // 保存当前面板中的设置值
            String tempAppId = baiduAppIdField.getText();
            String tempApiKey = String.valueOf(baiduApiKeyField.getPassword());

            // 临时保存到设置中
            PluginSettings settings = PluginSettings.getInstance();
            String originalAppId = settings.getBaiduAppId();
            String originalApiKey = settings.getBaiduApiKey();

            try {
                // 临时应用当前设置
                settings.setBaiduAppId(tempAppId);
                settings.setBaiduApiKey(tempApiKey);

                // 清除验证缓存并测试
                com.shuyixiao.util.TranslationUtil.clearValidationCache();

                // 显示测试中对话框
                JLabel progressLabel = new JLabel("正在测试API配置，请稍候...");
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                JPanel progressPanel = new JPanel(new BorderLayout());
                progressPanel.add(progressLabel, BorderLayout.NORTH);
                progressPanel.add(progressBar, BorderLayout.CENTER);
                progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JDialog progressDialog = new JDialog();
                progressDialog.setTitle("API测试中");
                progressDialog.setContentPane(progressPanel);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(panel);
                progressDialog.setModal(false);

                // 在单独线程中测试API，避免UI冻结
                javax.swing.SwingWorker<Boolean, Void> worker = new javax.swing.SwingWorker<Boolean, Void>() {
                    private String errorMessage = "";

                    @Override
                    protected Boolean doInBackground() {
                        try {
                            return com.shuyixiao.BaiduAPI.validateApiConfiguration();
                        } catch (Exception ex) {
                            errorMessage = ex.getMessage();
                            return false;
                        }
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        try {
                            boolean success = get();
                            if (success) {
                                com.intellij.openapi.ui.Messages.showInfoMessage(
                                        "API配置测试成功！\n\n" +
                                        "您的百度翻译API配置有效，已成功连接到百度翻译服务。\n" +
                                        "应用ID: " + tempAppId + "\n" +
                                        "API密钥: " + (tempApiKey.length() > 4 ? tempApiKey.substring(0, 4) + "***" : "***"),
                                        "测试成功");
                            } else {
                                com.intellij.openapi.ui.Messages.showErrorDialog(
                                        "API配置测试失败!\n\n" +
                                        "错误详情: " + errorMessage + "\n\n" +
                                        "请检查以下问题:\n" +
                                        "1. 确保您的应用ID和API密钥完全正确\n" +
                                        "2. 确认您的百度翻译API账户是否有效\n" +
                                        "3. 检查网络连接是否正常\n\n" +
                                        "您可以访问百度翻译开放平台查看正确的API信息:\n" +
                                        "https://fanyi-api.baidu.com/manage/developer",
                                        "测试失败");
                            }
                        } catch (Exception ex) {
                            com.intellij.openapi.ui.Messages.showErrorDialog(
                                    "测试过程中发生错误: " + ex.getMessage(),
                                    "测试错误");
                        } finally {
                            // 恢复原始设置
                            settings.setBaiduAppId(originalAppId);
                            settings.setBaiduApiKey(originalApiKey);
                        }
                    }
                };

                worker.execute();
                progressDialog.setVisible(true);

            } catch (Exception ex) {
                // 恢复原始设置
                settings.setBaiduAppId(originalAppId);
                settings.setBaiduApiKey(originalApiKey);

                com.intellij.openapi.ui.Messages.showErrorDialog(
                        "测试过程中发生错误: " + ex.getMessage(),
                        "测试错误");
            }
        });

        // 添加API配置说明
        JLabel apiHintLabel = new JLabel("<html><font color='red'>配置百度翻译API是必要的，所有翻译功能都需要此配置才能正常工作</font></html>");
        JLabel securityHintLabel = new JLabel("<html><font color='gray'>提示: API密钥以密文显示，确保您的密钥安全</font></html>");

        // 使用GridBagLayout添加组件
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(templateLabel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(appIdLabel, gbc);

        gbc.gridy = 3;
        panel.add(baiduAppIdField, gbc);

        gbc.gridy = 4;
        panel.add(apiKeyLabel, gbc);

        gbc.gridy = 5;
        panel.add(baiduApiKeyField, gbc);

        gbc.gridy = 6;
        panel.add(testApiButton, gbc);

        gbc.gridy = 7;
        panel.add(securityHintLabel, gbc);

        gbc.gridy = 8;
        panel.add(apiHintLabel, gbc);

        return panel;
    }

    @Override
    public boolean isModified() {
        PluginSettings settings = PluginSettings.getInstance();
        return !templateTextArea.getText().equals(settings.getTemplate()) ||
               !String.valueOf(baiduApiKeyField.getPassword()).equals(settings.getBaiduApiKey()) ||
               !baiduAppIdField.getText().equals(settings.getBaiduAppId());
    }

    @Override
    public void apply() {
        PluginSettings settings = PluginSettings.getInstance();
        settings.setTemplate(templateTextArea.getText());
        settings.setBaiduApiKey(String.valueOf(baiduApiKeyField.getPassword()));
        settings.setBaiduAppId(baiduAppIdField.getText());

        // 清除API验证缓存，确保下次使用时重新验证
        com.shuyixiao.util.TranslationUtil.clearValidationCache();
    }

    @Override
    public void reset() {
        PluginSettings settings = PluginSettings.getInstance();
        templateTextArea.setText(settings.getTemplate());
        baiduApiKeyField.setText(settings.getBaiduApiKey());
        baiduAppIdField.setText(settings.getBaiduAppId());
    }
}


