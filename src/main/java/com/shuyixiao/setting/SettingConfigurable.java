package com.shuyixiao.setting;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.Desktop;
import java.net.URI;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName SettingConfigurable.java
 * author 舒一笑不秃头 yixiaoshu88@163.com
 * version 1.0.0
 * Description 设置面板增加配置项
 * createTime 2024年09月07日 19:39:00
 */
public class SettingConfigurable implements SearchableConfigurable {

    // 控件成员变量提前初始化，避免NPE
    private JCheckBox enableGoogleTranslationCheckBox = new JCheckBox("启用 Google Cloud Translation");
    private JPasswordField googleApiKeyField = new JPasswordField(40);
    private JTextField googleProjectIdField = new JTextField(40);
    private JComboBox<String> googleRegionComboBox = new JComboBox<>(new String[]{"global", "us", "europe", "asia", "australia", "china"});
    private JButton testGoogleApiButton = new JButton("验证 Google API 配置");
    
    private JCheckBox enableDomesticAICheckBox = new JCheckBox("启用国内大模型翻译");
    private JComboBox<String> domesticAIModelComboBox;
    private JPasswordField domesticAIApiKeyField = new JPasswordField(40);
    private JButton testDomesticAPIButton = new JButton("验证国内大模型配置");
    
    private JCheckBox useCustomPromptCheckBox = new JCheckBox("使用自定义翻译提示词");
    private JTextArea translationPromptArea = new JTextArea(6, 40);
    private JButton resetPromptButton = new JButton("重置为默认提示词");
    
    private JTextArea templateTextArea = new JTextArea(10, 40);
    private JPasswordField baiduApiKeyField = new JPasswordField(40);
    private JTextField baiduAppIdField = new JTextField(40);
    private JButton testBaiduApiButton = new JButton("验证百度API配置");

    private JPanel panel;

    // 模型映射：中文名称 -> 英文值
    private final String[][] modelMapping = {
        {"通义千问 (Qianwen)", "qianwen"},
        {"文心一言 (Wenxin)", "wenxin"},
        {"智谱AI (GLM-4)", "zhipu"},
        {"腾讯混元 (Hunyuan)", "hunyuan"}
    };

    // AI助手配置
    private JCheckBox enableAiChatAssistantCheckBox = new JCheckBox("启用 AI 智能助手（聊天/Agent）");
    private JComboBox<String> aiProviderComboBox = new JComboBox<>(new String[]{"OpenAI兼容", "国内模型（通义/文心/智谱/混元）"});
    private JTextField aiBaseUrlField = new JTextField(40);
    private JPasswordField aiApiKeyField = new JPasswordField(40);
    private JTextField aiModelField = new JTextField(40);
    private JButton testAiChatButton = new JButton("验证助手配置并试聊");

    public SettingConfigurable() {
        // 初始化下拉框，只显示中文名称
        String[] displayNames = new String[modelMapping.length];
        for (int i = 0; i < modelMapping.length; i++) {
            displayNames[i] = modelMapping[i][0];
        }
        domesticAIModelComboBox = new JComboBox<>(displayNames);
    }

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
        panel = new JPanel(new BorderLayout());
        
        // 创建标签页面板
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 创建各个标签页
        tabbedPane.addTab("翻译引擎", createTranslationEnginePanel());
        tabbedPane.addTab("提示词配置", createPromptPanel());
        tabbedPane.addTab("模板配置", createTemplatePanel());
        tabbedPane.addTab("百度翻译", createBaiduPanel());
        tabbedPane.addTab("智能助手", createAiAssistantPanel());
//        tabbedPane.addTab("Bug记录", createBugStoragePanel());
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        // 初始化控件值
        initializeControlValues();
        
        // 设置字段状态
        updateAllFieldsState();
        
        // 添加监听器
        addEventListeners();
        
        return panel;
    }

    /**
     * 创建智能助手（聊天/Agent）配置面板
     */
    private JPanel createAiAssistantPanel() {
        JPanel aiPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        int row = 0;

        JLabel title = new JLabel("<html><b>AI 智能助手</b></html>");
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        aiPanel.add(title, gbc);

        gbc.gridy = row++;
        aiPanel.add(enableAiChatAssistantCheckBox, gbc);

        JLabel providerLabel = new JLabel("接入方式：");
        gbc.gridy = row++;
        aiPanel.add(providerLabel, gbc);

        gbc.gridy = row++;
        aiPanel.add(aiProviderComboBox, gbc);

        JLabel baseUrlLabel = new JLabel("模型地址（Base URL）：");
        gbc.gridy = row++;
        aiPanel.add(baseUrlLabel, gbc);

        gbc.gridy = row++;
        aiPanel.add(aiBaseUrlField, gbc);

        JLabel apiKeyLabel = new JLabel("API 密钥：");
        gbc.gridy = row++;
        aiPanel.add(apiKeyLabel, gbc);

        gbc.gridy = row++;
        aiPanel.add(aiApiKeyField, gbc);

        JLabel modelLabel = new JLabel("模型名称：");
        gbc.gridy = row++;
        aiPanel.add(modelLabel, gbc);

        gbc.gridy = row++;
        aiPanel.add(aiModelField, gbc);

        gbc.gridy = row++;
        aiPanel.add(testAiChatButton, gbc);

        JLabel tips = new JLabel("<html><font color='gray'>OpenAI兼容：如 `https://api.openai.com`、`http://localhost:11434/v1`；国内模型选择“国内模型”并在翻译引擎页配置密钥</font></html>");
        gbc.gridy = row++;
        aiPanel.add(tips, gbc);

        return aiPanel;
    }
    
    /**
     * 创建翻译引擎配置面板
     */
    private JPanel createTranslationEnginePanel() {
        JPanel enginePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        int row = 0;
        
        // Google Cloud Translation 配置
        JLabel googleSectionLabel = new JLabel("<html><b>Google Cloud Translation</b></html>");
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        enginePanel.add(googleSectionLabel, gbc);
        
        gbc.gridy = row++; gbc.gridwidth = 2;
        enginePanel.add(enableGoogleTranslationCheckBox, gbc);
        
        JLabel googleHintLabel = new JLabel("<html><font color='gray'>启用后将优先使用 Google Cloud Translation</font></html>");
        gbc.gridy = row++;
        enginePanel.add(googleHintLabel, gbc);
        
        JLabel googleApiKeyLabel = new JLabel("Google API Key:");
        gbc.gridy = row++;
        enginePanel.add(googleApiKeyLabel, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(googleApiKeyField, gbc);
        
        JLabel googleProjectIdLabel = new JLabel("Google Project ID:");
        gbc.gridy = row++;
        enginePanel.add(googleProjectIdLabel, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(googleProjectIdField, gbc);
        
        JLabel googleRegionLabel = new JLabel("Google Region:");
        gbc.gridy = row++;
        enginePanel.add(googleRegionLabel, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(googleRegionComboBox, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(testGoogleApiButton, gbc);
        
        // 分隔线
        JSeparator separator1 = new JSeparator();
        gbc.gridy = row++; gbc.insets = JBUI.insets(15, 5);
        enginePanel.add(separator1, gbc);
        gbc.insets = JBUI.insets(5);
        
        // 国内大模型配置
        JLabel domesticAISectionLabel = new JLabel("<html><b>国内大模型翻译</b></html>");
        gbc.gridy = row++; gbc.gridwidth = 2;
        enginePanel.add(domesticAISectionLabel, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(enableDomesticAICheckBox, gbc);
        
        JLabel domesticAIModelLabel = new JLabel("选择模型：");
        gbc.gridy = row++;
        enginePanel.add(domesticAIModelLabel, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(domesticAIModelComboBox, gbc);
        
        JLabel domesticAIApiKeyLabel = new JLabel("API密钥：");
        gbc.gridy = row++;
        enginePanel.add(domesticAIApiKeyLabel, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(domesticAIApiKeyField, gbc);
        
        gbc.gridy = row++;
        enginePanel.add(testDomesticAPIButton, gbc);
        
        return enginePanel;
    }
    
    /**
     * 创建提示词配置面板
     */
    private JPanel createPromptPanel() {
        JPanel promptPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        int row = 0;
        
        JLabel promptSectionLabel = new JLabel("<html><b>翻译提示词配置</b></html>");
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        promptPanel.add(promptSectionLabel, gbc);
        
        gbc.gridy = row++;
        promptPanel.add(useCustomPromptCheckBox, gbc);
        
        JLabel promptHintLabel = new JLabel("<html><font color='gray'>自定义提示词将应用于所有支持的AI模型翻译</font></html>");
        gbc.gridy = row++;
        promptPanel.add(promptHintLabel, gbc);
        
        JLabel translationPromptLabel = new JLabel("翻译提示词：");
        gbc.gridy = row++;
        promptPanel.add(translationPromptLabel, gbc);
        
        translationPromptArea.setLineWrap(true);
        translationPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(translationPromptArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        gbc.gridy = row++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        promptPanel.add(scrollPane, gbc);
        gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridy = row++;
        promptPanel.add(resetPromptButton, gbc);
        
        return promptPanel;
    }
    
    /**
     * 创建模板配置面板
     */
    private JPanel createTemplatePanel() {
        JPanel templatePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        int row = 0;
        
        JLabel templateSectionLabel = new JLabel("<html><b>文件注释模板</b></html>");
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        templatePanel.add(templateSectionLabel, gbc);
        
        JLabel templateLabel = new JLabel("文件注释模板：");
        gbc.gridy = row++;
        templatePanel.add(templateLabel, gbc);
        
        JLabel templateHintLabel = new JLabel("<html><font color='gray'>支持变量：${NAME} ${YEAR} ${TIME}</font></html>");
        gbc.gridy = row++;
        templatePanel.add(templateHintLabel, gbc);
        
        templateTextArea.setLineWrap(true);
        JScrollPane scrollPaneTemplate = new JScrollPane(templateTextArea);
        scrollPaneTemplate.setPreferredSize(new Dimension(500, 300));
        gbc.gridy = row++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        templatePanel.add(scrollPaneTemplate, gbc);
        gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        
        return templatePanel;
    }
    
    /**
     * 创建百度翻译配置面板
     */
    private JPanel createBaiduPanel() {
        JPanel baiduPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        int row = 0;
        
        JLabel baiduSectionLabel = new JLabel("<html><b>百度翻译 API 配置</b></html>");
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        baiduPanel.add(baiduSectionLabel, gbc);
        
        JLabel baiduDescLabel = new JLabel("<html><font color='gray'>百度翻译作为备用翻译引擎，建议配置以确保翻译功能的可靠性</font></html>");
        gbc.gridy = row++;
        baiduPanel.add(baiduDescLabel, gbc);
        
        JLabel appIdLabel = new JLabel("百度应用ID：");
        gbc.gridy = row++;
        baiduPanel.add(appIdLabel, gbc);
        
        gbc.gridy = row++;
        baiduPanel.add(baiduAppIdField, gbc);
        
        JLabel apiKeyLabel = new JLabel("百度API密钥：");
        gbc.gridy = row++;
        baiduPanel.add(apiKeyLabel, gbc);
        
        gbc.gridy = row++;
        baiduPanel.add(baiduApiKeyField, gbc);
        
        gbc.gridy = row++;
        baiduPanel.add(testBaiduApiButton, gbc);
        
        JLabel securityHintLabel = new JLabel("<html><font color='gray'>提示: API密钥以密文显示，确保您的密钥安全</font></html>");
        gbc.gridy = row++;
        baiduPanel.add(securityHintLabel, gbc);
        
        // 配置说明
        JLabel configHelpLabel = new JLabel("<html>" +
                "<b>获取百度翻译API：</b><br>" +
                "1. 访问：<a href='https://fanyi-api.baidu.com/'>https://fanyi-api.baidu.com/</a><br>" +
                "2. 注册账号并创建应用<br>" +
                "3. 获取APP ID和密钥<br>" +
                "</html>");
        gbc.gridy = row++;
        baiduPanel.add(configHelpLabel, gbc);
        
        return baiduPanel;
    }
    
    /**
     * 初始化控件值
     */
    private void initializeControlValues() {
        // 模板设置
        templateTextArea.setLineWrap(true);
        String template = PluginSettings.getInstance().getTemplate();
        if (template != null) {
            templateTextArea.setText(template);
        }
        
        // 百度API设置
        baiduAppIdField.setText(PluginSettings.getInstance().getBaiduAppId());
        baiduApiKeyField.setText(PluginSettings.getInstance().getBaiduApiKey());
        
        // Google翻译设置
        enableGoogleTranslationCheckBox.setSelected(PluginSettings.getInstance().isEnableGoogleTranslation());
        googleApiKeyField.setText(PluginSettings.getInstance().getGoogleApiKey());
        googleProjectIdField.setText(PluginSettings.getInstance().getGoogleProjectId());
        googleRegionComboBox.setSelectedItem(PluginSettings.getInstance().getGoogleRegion());
        
        // 国内大模型设置
        enableDomesticAICheckBox.setSelected(PluginSettings.getInstance().isEnableDomesticAI());
        domesticAIModelComboBox.setSelectedItem(getDisplayNameByValue(PluginSettings.getInstance().getDomesticAIModel()));
        domesticAIApiKeyField.setText(PluginSettings.getInstance().getDomesticAIApiKey());

        // AI 助手设置
        enableAiChatAssistantCheckBox.setSelected(PluginSettings.getInstance().isEnableAiChatAssistant());
        aiProviderComboBox.setSelectedItem("openai".equals(PluginSettings.getInstance().getAiProviderType()) ? "OpenAI兼容" : "国内模型（通义/文心/智谱/混元）");
        aiBaseUrlField.setText(PluginSettings.getInstance().getAiBaseUrl());
        aiApiKeyField.setText(PluginSettings.getInstance().getAiApiKey());
        aiModelField.setText(PluginSettings.getInstance().getAiModel());
        
        // 提示词设置
        useCustomPromptCheckBox.setSelected(PluginSettings.getInstance().isUseCustomPrompt());
        translationPromptArea.setText(PluginSettings.getInstance().getTranslationPrompt());
        translationPromptArea.setLineWrap(true);
        translationPromptArea.setWrapStyleWord(true);
    }
    
    /**
     * 更新所有字段状态
     */
    private void updateAllFieldsState() {
        updateGoogleFieldsState();
        updateDomesticAIFieldsState();
        updateTranslationPromptState();
        updateAiAssistantFieldsState();
    }
    
    /**
     * 添加事件监听器
     */
    private void addEventListeners() {
        enableGoogleTranslationCheckBox.addActionListener(e -> updateGoogleFieldsState());
        testGoogleApiButton.addActionListener(this::testGoogleApiConfiguration);
        testBaiduApiButton.addActionListener(this::testBaiduApiConfiguration);
        enableDomesticAICheckBox.addActionListener(e -> updateDomesticAIFieldsState());
        testDomesticAPIButton.addActionListener(this::testDomesticAPIConfiguration);
        useCustomPromptCheckBox.addActionListener(e -> updateTranslationPromptState());
        resetPromptButton.addActionListener(this::resetTranslationPrompt);

        enableAiChatAssistantCheckBox.addActionListener(e -> updateAiAssistantFieldsState());
        testAiChatButton.addActionListener(this::testAiAssistantConfiguration);
    }
    
    /**
     * 更新Google API相关字段的启用状态
     */
    private void updateGoogleFieldsState() {
        boolean enabled = enableGoogleTranslationCheckBox.isSelected();
        googleApiKeyField.setEnabled(enabled);
        googleProjectIdField.setEnabled(enabled);
        googleRegionComboBox.setEnabled(enabled);
        testGoogleApiButton.setEnabled(enabled);
    }
    
    /**
     * 测试Google API配置
     */
    private void testGoogleApiConfiguration(ActionEvent e) {
        String apiKey = String.valueOf(googleApiKeyField.getPassword()).trim();
        String projectId = googleProjectIdField.getText().trim();
        
        if (apiKey.isEmpty() || projectId.isEmpty()) {
            JOptionPane.showMessageDialog(panel, 
                "请先输入 Google API Key 和 Project ID", 
                "配置不完整", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 这里可以添加实际的API测试逻辑
        try {
            // 临时设置配置进行测试
            PluginSettings tempSettings = PluginSettings.getInstance();
            String oldApiKey = tempSettings.getGoogleApiKey();
            String oldProjectId = tempSettings.getGoogleProjectId();
            String oldRegion = tempSettings.getGoogleRegion();
            
            tempSettings.setGoogleApiKey(apiKey);
            tempSettings.setGoogleProjectId(projectId);
            tempSettings.setGoogleRegion(googleRegionComboBox.getSelectedItem().toString());
            
            // 测试翻译
            String testResult = com.shuyixiao.GoogleCloudTranslationAPI.translate("测试");
            
            // 恢复原配置
            tempSettings.setGoogleApiKey(oldApiKey);
            tempSettings.setGoogleProjectId(oldProjectId);
            tempSettings.setGoogleRegion(oldRegion);
            
            if (testResult != null && !testResult.trim().isEmpty()) {
                JOptionPane.showMessageDialog(panel, 
                    "Google Cloud Translation API 配置验证成功！\n测试翻译：测试 → " + testResult, 
                    "验证成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(panel, 
                    "Google Cloud Translation API 返回结果为空，请检查配置", 
                    "验证失败", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel, 
                "Google Cloud Translation API 验证失败：\n" + ex.getMessage(), 
                "验证失败", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 测试百度API配置
     */
    private void testBaiduApiConfiguration(ActionEvent e) {
        String apiKey = String.valueOf(baiduApiKeyField.getPassword()).trim();
        String appId = baiduAppIdField.getText().trim();
        
        System.out.println("[testBaiduApiConfiguration] 验证按钮被点击");
        System.out.println("[testBaiduApiConfiguration] 从界面读取 - 应用ID: '" + appId + "'");
        System.out.println("[testBaiduApiConfiguration] 从界面读取 - API密钥: '" + apiKey + "' (长度: " + apiKey.length() + ")");
        
        if (apiKey.isEmpty() || appId.isEmpty()) {
            JOptionPane.showMessageDialog(panel, 
                "请先输入百度 API 密钥和应用ID", 
                "配置不完整", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // 直接使用界面上的值进行验证，不需要先保存到PluginSettings
            System.out.println("[testBaiduApiConfiguration] 调用验证方法，使用界面上的值...");
            if (com.shuyixiao.BaiduAPI.validateApiConfiguration(appId, apiKey)) {
                JOptionPane.showMessageDialog(panel, 
                    "百度翻译 API 配置验证成功！", 
                    "验证成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(panel, 
                    "百度翻译 API 配置验证失败，请检查密钥和应用ID是否正确", 
                    "验证失败", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            System.err.println("[testBaiduApiConfiguration] 验证异常: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel, 
                "百度翻译 API 验证失败：\n" + ex.getMessage(), 
                "验证失败", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 更新国内大模型相关字段的启用状态
     */
    private void updateDomesticAIFieldsState() {
        boolean enabled = enableDomesticAICheckBox.isSelected();
        domesticAIModelComboBox.setEnabled(enabled);
        domesticAIApiKeyField.setEnabled(enabled);
        testDomesticAPIButton.setEnabled(enabled);
    }

    /**
     * 更新 AI 助手字段启用状态
     */
    private void updateAiAssistantFieldsState() {
        boolean enabled = enableAiChatAssistantCheckBox.isSelected();
        aiProviderComboBox.setEnabled(enabled);
        aiBaseUrlField.setEnabled(enabled);
        aiApiKeyField.setEnabled(enabled);
        aiModelField.setEnabled(enabled);
        testAiChatButton.setEnabled(enabled);
    }
    
    /**
     * 测试国内大模型配置
     */
    private void testDomesticAPIConfiguration(ActionEvent e) {
        String apiKey = String.valueOf(domesticAIApiKeyField.getPassword()).trim();
        String modelDisplayName = domesticAIModelComboBox.getSelectedItem().toString();
        String modelValue = getValueByDisplayName(modelDisplayName);
        
        System.out.println("[testDomesticAPIConfiguration] 验证按钮被点击");
        System.out.println("[testDomesticAPIConfiguration] 从界面读取 - 模型: '" + modelValue + "' (" + modelDisplayName + ")");
        System.out.println("[testDomesticAPIConfiguration] 从界面读取 - API密钥: '" + apiKey + "' (长度: " + apiKey.length() + ")");
        
        if (apiKey.isEmpty() || modelValue.isEmpty()) {
            JOptionPane.showMessageDialog(panel, 
                "请先输入国内大模型API密钥和模型", 
                "配置不完整", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // 直接使用界面上的值进行验证，不需要修改PluginSettings
            System.out.println("[testDomesticAPIConfiguration] 调用验证方法，使用界面上的值...");
            String testResult = com.shuyixiao.DomesticAITranslationAPI.translate("测试", modelValue, apiKey);
            
            if (testResult != null && !testResult.trim().isEmpty()) {
                System.out.println("[testDomesticAPIConfiguration] 验证成功! 翻译结果: " + testResult);
                JOptionPane.showMessageDialog(panel, 
                    "国内大模型配置验证成功！\n模型：" + modelDisplayName + "\n测试翻译：测试 → " + testResult, 
                    "验证成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.err.println("[testDomesticAPIConfiguration] API返回结果为空");
                JOptionPane.showMessageDialog(panel, 
                    "国内大模型API返回结果为空，请检查配置", 
                    "验证失败", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            System.err.println("[testDomesticAPIConfiguration] 验证异常: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel, 
                "国内大模型验证失败：\n" + ex.getMessage(), 
                "验证失败", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 更新翻译提示词相关字段的启用状态
     */
    private void updateTranslationPromptState() {
        boolean enabled = useCustomPromptCheckBox.isSelected();
        translationPromptArea.setEnabled(enabled);
        resetPromptButton.setEnabled(enabled);
    }
    
    /**
     * 重置翻译提示词
     */
    private void resetTranslationPrompt(ActionEvent e) {
        String defaultPrompt =
                "你是一位专业软件工程师，负责将技术文档中文本翻译为规范的英文编程术语。请遵循：\n" +
                        "1. 【翻译规范】用编程术语表达技术概念，非逐字翻译（例：'配置文件路径'→configPath）\n" +
                        "2. 【命名规则】输出直接可用作代码标识符的形式（类名用大驼峰，方法/变量用小驼峰）\n" +
                        "3. 【术语处理】专业术语保持行业标准（例：'缓存'→cache而非buffer）\n" +
                        "4. 【长文本优化】超过3个技术概念时：\n" +
                        "   a) 优先提取核心术语\n" +
                        "   b) 保持技术逻辑连贯性\n" +
                        "   c) 省略非技术性描述词（'这个'、'一种'等）\n" +
                        "5. 【输出要求】只返回最终翻译结果\n\n" +
                        "待翻译中文：";
        translationPromptArea.setText(defaultPrompt);
    }

    @Override
    public boolean isModified() {
        PluginSettings settings = PluginSettings.getInstance();
        String currentModelValue = getValueByDisplayName(domesticAIModelComboBox.getSelectedItem().toString());
        return enableGoogleTranslationCheckBox.isSelected() != settings.isEnableGoogleTranslation()
            || !String.valueOf(googleApiKeyField.getPassword()).equals(settings.getGoogleApiKey())
            || !googleProjectIdField.getText().equals(settings.getGoogleProjectId())
            || !googleRegionComboBox.getSelectedItem().toString().equals(settings.getGoogleRegion())
            || !templateTextArea.getText().equals(settings.getTemplate())
            || !String.valueOf(baiduApiKeyField.getPassword()).equals(settings.getBaiduApiKey())
            || !baiduAppIdField.getText().equals(settings.getBaiduAppId())
            || enableDomesticAICheckBox.isSelected() != settings.isEnableDomesticAI()
            || !currentModelValue.equals(settings.getDomesticAIModel())
            || !String.valueOf(domesticAIApiKeyField.getPassword()).equals(settings.getDomesticAIApiKey())
            || useCustomPromptCheckBox.isSelected() != settings.isUseCustomPrompt()
            || !translationPromptArea.getText().equals(settings.getTranslationPrompt())
            || enableAiChatAssistantCheckBox.isSelected() != settings.isEnableAiChatAssistant()
            || !("openai".equals(settings.getAiProviderType()) ? "OpenAI兼容" : "国内模型（通义/文心/智谱/混元）").equals(aiProviderComboBox.getSelectedItem().toString())
            || !aiBaseUrlField.getText().equals(settings.getAiBaseUrl())
            || !String.valueOf(aiApiKeyField.getPassword()).equals(settings.getAiApiKey())
            || !aiModelField.getText().equals(settings.getAiModel());
    }

    @Override
    public void apply() {
        PluginSettings settings = PluginSettings.getInstance();
        
        // 获取界面输入值
        String baiduApiKey = String.valueOf(baiduApiKeyField.getPassword());
        String baiduAppId = baiduAppIdField.getText();
        
        // 调试日志
        System.out.println("[SettingConfigurable] 保存配置...");
        System.out.println("[SettingConfigurable] 百度应用ID (从界面): '" + baiduAppId + "'");
        System.out.println("[SettingConfigurable] 百度API密钥 (从界面): '" + baiduApiKey + "' (长度: " + baiduApiKey.length() + ")");
        
        // 保存配置
        settings.setEnableGoogleTranslation(enableGoogleTranslationCheckBox.isSelected());
        settings.setGoogleApiKey(String.valueOf(googleApiKeyField.getPassword()));
        settings.setGoogleProjectId(googleProjectIdField.getText());
        settings.setGoogleRegion(googleRegionComboBox.getSelectedItem().toString());
        settings.setTemplate(templateTextArea.getText());
        settings.setBaiduApiKey(baiduApiKey);
        settings.setBaiduAppId(baiduAppId);
        settings.setEnableDomesticAI(enableDomesticAICheckBox.isSelected());
        settings.setDomesticAIModel(getValueByDisplayName(domesticAIModelComboBox.getSelectedItem().toString()));
        settings.setDomesticAIApiKey(String.valueOf(domesticAIApiKeyField.getPassword()));
        settings.setUseCustomPrompt(useCustomPromptCheckBox.isSelected());
        settings.setTranslationPrompt(translationPromptArea.getText());

        // AI 助手
        settings.setEnableAiChatAssistant(enableAiChatAssistantCheckBox.isSelected());
        settings.setAiProviderType("OpenAI兼容".equals(aiProviderComboBox.getSelectedItem().toString()) ? "openai" : "domestic");
        settings.setAiBaseUrl(aiBaseUrlField.getText());
        settings.setAiApiKey(String.valueOf(aiApiKeyField.getPassword()));
        settings.setAiModel(aiModelField.getText());

        // 验证保存结果
        System.out.println("[SettingConfigurable] 保存后验证...");
        System.out.println("[SettingConfigurable] 百度应用ID (从settings): '" + settings.getBaiduAppId() + "'");
        System.out.println("[SettingConfigurable] 百度API密钥 (从settings): '" + settings.getBaiduApiKey() + "' (长度: " + settings.getBaiduApiKey().length() + ")");
        
        // 清除翻译验证缓存
        com.shuyixiao.util.TranslationUtil.clearValidationCache();
        
        System.out.println("[SettingConfigurable] 配置保存完成!");
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
        enableDomesticAICheckBox.setSelected(settings.isEnableDomesticAI());
        domesticAIModelComboBox.setSelectedItem(getDisplayNameByValue(settings.getDomesticAIModel()));
        domesticAIApiKeyField.setText(settings.getDomesticAIApiKey());
        useCustomPromptCheckBox.setSelected(settings.isUseCustomPrompt());
        translationPromptArea.setText(settings.getTranslationPrompt());
        enableAiChatAssistantCheckBox.setSelected(settings.isEnableAiChatAssistant());
        aiProviderComboBox.setSelectedItem("openai".equals(settings.getAiProviderType()) ? "OpenAI兼容" : "国内模型（通义/文心/智谱/混元）");
        aiBaseUrlField.setText(settings.getAiBaseUrl());
        aiApiKeyField.setText(settings.getAiApiKey());
        aiModelField.setText(settings.getAiModel());
        updateGoogleFieldsState();
        updateDomesticAIFieldsState();
        updateTranslationPromptState();
        updateAiAssistantFieldsState();
    }

    /**
     * 验证 AI 助手配置并试聊
     */
    private void testAiAssistantConfiguration(ActionEvent e) {
        boolean enabled = enableAiChatAssistantCheckBox.isSelected();
        if (!enabled) {
            JOptionPane.showMessageDialog(panel,
                    "请先启用 AI 智能助手",
                    "未启用",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String provider = aiProviderComboBox.getSelectedItem().toString();
        String baseUrl = aiBaseUrlField.getText().trim();
        String apiKey = String.valueOf(aiApiKeyField.getPassword()).trim();
        String model = aiModelField.getText().trim();

        if ("OpenAI兼容".equals(provider)) {
            if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
                JOptionPane.showMessageDialog(panel,
                        "请填写 Base URL、API 密钥和模型名称",
                        "配置不完整",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            if (!enableDomesticAICheckBox.isSelected()) {
                JOptionPane.showMessageDialog(panel,
                        "请在“翻译引擎”页启用国内大模型并配置密钥",
                        "配置不完整",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            String reply;
            if ("OpenAI兼容".equals(provider)) {
                reply = com.shuyixiao.ai.chat.OpenAICompatibleChatClient.quickTest(baseUrl, apiKey, model, "你好，PandaCoder!");
            } else {
                reply = com.shuyixiao.DomesticAITranslationAPI.translate("你好，PandaCoder!", getValueByDisplayName(domesticAIModelComboBox.getSelectedItem().toString()), String.valueOf(domesticAIApiKeyField.getPassword()));
            }
            JOptionPane.showMessageDialog(panel,
                    "助手回复：\n" + (reply == null ? "<空>" : reply),
                    "验证成功",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel,
                    "助手验证失败：\n" + ex.getMessage(),
                    "验证失败",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 根据英文值获取中文显示名称
     */
    private String getDisplayNameByValue(String value) {
        for (String[] mapping : modelMapping) {
            if (mapping[1].equals(value)) {
                return mapping[0];
            }
        }
        return modelMapping[0][0]; // 默认返回第一个
    }
    
    /**
     * 根据中文显示名称获取英文值
     */
    private String getValueByDisplayName(String displayName) {
        for (String[] mapping : modelMapping) {
            if (mapping[0].equals(displayName)) {
                return mapping[1];
            }
        }
        return modelMapping[0][1]; // 默认返回第一个
    }
    
}
