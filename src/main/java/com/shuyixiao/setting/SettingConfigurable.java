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
import java.awt.event.ItemListener;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName SettingConfigurable.java
 * author 舒一笑 yixiaoshu88@163.com
 * version 1.0.0
 * Description 设置面板增加配置项
 * createTime 2024年09月07日 19:39:00
 */
public class SettingConfigurable implements SearchableConfigurable {

    // 控件成员变量提前初始化，避免NPE
    private JCheckBox enableGoogleTranslationCheckBox = new JCheckBox("Enable Google Cloud Translation");
    private JPasswordField googleApiKeyField = new JPasswordField(40);
    private JTextField googleProjectIdField = new JTextField(40);
    private JComboBox<String> googleRegionComboBox = new JComboBox<>(new String[]{"global", "us", "europe", "asia", "australia", "china"});
    private JTextArea templateTextArea = new JTextArea(10, 40);
    private JPasswordField baiduApiKeyField = new JPasswordField(40);
    private JTextField baiduAppIdField = new JTextField(40);
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
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 只做赋值，不再new控件
        templateTextArea.setLineWrap(true);
        String template = PluginSettings.getInstance().getTemplate();
        if (template != null) {
            templateTextArea.setText(template);
        }
        baiduAppIdField.setText(PluginSettings.getInstance().getBaiduAppId());
        baiduApiKeyField.setText(PluginSettings.getInstance().getBaiduApiKey());
        enableGoogleTranslationCheckBox.setSelected(PluginSettings.getInstance().isEnableGoogleTranslation());
        googleApiKeyField.setText(PluginSettings.getInstance().getGoogleApiKey());
        googleProjectIdField.setText(PluginSettings.getInstance().getGoogleProjectId());
        googleRegionComboBox.setSelectedItem(PluginSettings.getInstance().getGoogleRegion());

        // 先声明和初始化所有控件
        JLabel templateLabel = new JLabel("文件注释模板：");
        JScrollPane scrollPane = new JScrollPane(templateTextArea);
        JLabel appIdLabel = new JLabel("百度应用ID：");
        JLabel apiKeyLabel = new JLabel("百度API密钥：");
        JButton testApiButton = new JButton("验证API配置");
        testApiButton.setToolTipText("测试当前输入的API密钥和应用ID是否能正确连接到百度翻译服务");
        JLabel apiHintLabel = new JLabel("<html><font color='red'>配置百度翻译API是必要的，所有翻译功能都需要此配置才能正常工作</font></html>");
        JLabel securityHintLabel = new JLabel("<html><font color='gray'>提示: API密钥以密文显示，确保您的密钥安全</font></html>");
        JLabel googleApiKeyLabel = new JLabel("Google API Key:");
        JLabel googleProjectIdLabel = new JLabel("Google Project ID:");
        JLabel googleRegionLabel = new JLabel("Google Region:");

        int row = 0;
        // Google Cloud Translation 配置
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(enableGoogleTranslationCheckBox, gbc);
        gbc.gridy = row++;
        panel.add(googleApiKeyLabel, gbc);
        gbc.gridy = row++;
        panel.add(googleApiKeyField, gbc);
        gbc.gridy = row++;
        panel.add(googleProjectIdLabel, gbc);
        gbc.gridy = row++;
        panel.add(googleProjectIdField, gbc);
        gbc.gridy = row++;
        panel.add(googleRegionLabel, gbc);
        gbc.gridy = row++;
        panel.add(googleRegionComboBox, gbc);

        // 模板区域
        gbc.gridy = row++;
        panel.add(templateLabel, gbc);
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 百度API配置
        gbc.gridy = row++;
        panel.add(appIdLabel, gbc);
        gbc.gridy = row++;
        panel.add(baiduAppIdField, gbc);
        gbc.gridy = row++;
        panel.add(apiKeyLabel, gbc);
        gbc.gridy = row++;
        panel.add(baiduApiKeyField, gbc);
        gbc.gridy = row++;
        panel.add(testApiButton, gbc);
        gbc.gridy = row++;
        panel.add(securityHintLabel, gbc);
        gbc.gridy = row++;
        panel.add(apiHintLabel, gbc);

        return panel;
    }

    @Override
    public boolean isModified() {
        PluginSettings settings = PluginSettings.getInstance();
        return enableGoogleTranslationCheckBox.isSelected() != settings.isEnableGoogleTranslation()
            || !String.valueOf(googleApiKeyField.getPassword()).equals(settings.getGoogleApiKey())
            || !googleProjectIdField.getText().equals(settings.getGoogleProjectId())
            || !googleRegionComboBox.getSelectedItem().toString().equals(settings.getGoogleRegion())
            || !templateTextArea.getText().equals(settings.getTemplate())
            || !String.valueOf(baiduApiKeyField.getPassword()).equals(settings.getBaiduApiKey())
            || !baiduAppIdField.getText().equals(settings.getBaiduAppId());
    }

    @Override
    public void apply() {
        PluginSettings settings = PluginSettings.getInstance();
        settings.setEnableGoogleTranslation(enableGoogleTranslationCheckBox.isSelected());
        settings.setGoogleApiKey(String.valueOf(googleApiKeyField.getPassword()));
        settings.setGoogleProjectId(googleProjectIdField.getText());
        settings.setGoogleRegion(googleRegionComboBox.getSelectedItem().toString());
        settings.setTemplate(templateTextArea.getText());
        settings.setBaiduApiKey(String.valueOf(baiduApiKeyField.getPassword()));
        settings.setBaiduAppId(baiduAppIdField.getText());
        com.shuyixiao.util.TranslationUtil.clearValidationCache();
    }

    @Override
    public void reset() {
        PluginSettings settings = PluginSettings.getInstance();
        enableGoogleTranslationCheckBox.setSelected(settings.isEnableGoogleTranslation());
        googleApiKeyField.setText(settings.getGoogleApiKey());
        googleProjectIdField.setText(settings.getGoogleProjectId());
        googleRegionComboBox.setSelectedItem(settings.getGoogleRegion());
        templateTextArea.setText(settings.getTemplate());
        baiduApiKeyField.setText(settings.getBaiduApiKey());
        baiduAppIdField.setText(settings.getBaiduAppId());
    }
}


