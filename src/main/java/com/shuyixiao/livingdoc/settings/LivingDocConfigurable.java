package com.shuyixiao.livingdoc.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 活文档插件设置面板（Preferences/Settings）
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class LivingDocConfigurable implements Configurable {
    
    private final Project project;
    private JPanel mainPanel;
    
    // AI 模型配置
    private ComboBox<String> aiProviderCombo;
    private JBTextField giteeApiKeyField;
    private JBTextField giteeBaseUrlField;
    private JBTextField giteeModelField;
    private JBTextField giteeEmbeddingModelField;
    
    private JBTextField openaiApiKeyField;
    private JBTextField openaiBaseUrlField;
    private JBTextField openaiModelField;
    private JBTextField openaiEmbeddingModelField;
    
    private JBTextField ollamaBaseUrlField;
    private JBTextField ollamaModelField;
    private JBTextField ollamaEmbeddingModelField;
    
    // 向量数据库配置
    private ComboBox<String> vectorStoreTypeCombo;
    private JBTextField esHostField;
    private JBTextField esPortField;
    private JBTextField esUsernameField;
    private JPasswordField esPasswordField;
    private JBTextField esIndexNameField;
    private JBTextField esDimensionsField;
    private ComboBox<String> esSimilarityCombo;
    
    // RAG 配置
    private JBTextField ragChunkSizeField;
    private JBTextField ragChunkOverlapField;
    private JBTextField ragTopKField;
    private JBTextField ragSimilarityThresholdField;
    
    // 其他配置
    private JBCheckBox autoIndexOnSaveCheck;
    private JBCheckBox showNotificationsCheck;
    private JBCheckBox enableLoggingCheck;
    
    public LivingDocConfigurable(Project project) {
        this.project = project;
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "活文档 (Living Doc)";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());
        
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Tab 1: AI 模型配置
        tabbedPane.addTab("AI 模型", createAiModelPanel());
        
        // Tab 2: 向量数据库配置
        tabbedPane.addTab("向量数据库", createVectorStorePanel());
        
        // Tab 3: RAG 配置
        tabbedPane.addTab("RAG 检索", createRagPanel());
        
        // Tab 4: 其他配置
        tabbedPane.addTab("其他设置", createOtherPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * AI 模型配置面板
     */
    private JPanel createAiModelPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // AI 提供商选择
        panel.add(new JBLabel("AI 提供商:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        aiProviderCombo = new ComboBox<>(new String[]{"gitee-ai", "openai", "ollama"});
        panel.add(aiProviderCombo, gbc);
        
        // Gitee AI 配置
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JBLabel("<html><b>Gitee AI 配置</b></html>"), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JBLabel("API Key:"), gbc);
        gbc.gridx = 1;
        giteeApiKeyField = new JBTextField();
        giteeApiKeyField.setToolTipText("在 https://ai.gitee.com/ 获取");
        panel.add(giteeApiKeyField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Base URL:"), gbc);
        gbc.gridx = 1;
        giteeBaseUrlField = new JBTextField("https://ai.gitee.com/v1");
        panel.add(giteeBaseUrlField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Chat 模型:"), gbc);
        gbc.gridx = 1;
        giteeModelField = new JBTextField("qwen-plus");
        giteeModelField.setToolTipText("推荐: qwen-plus, qwen-max, deepseek-chat");
        panel.add(giteeModelField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Embedding 模型:"), gbc);
        gbc.gridx = 1;
        giteeEmbeddingModelField = new JBTextField("text-embedding-v3");
        giteeEmbeddingModelField.setToolTipText("推荐: text-embedding-v3 (1024维)");
        panel.add(giteeEmbeddingModelField, gbc);
        
        // OpenAI 配置
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JBLabel("<html><b>OpenAI 配置</b></html>"), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JBLabel("API Key:"), gbc);
        gbc.gridx = 1;
        openaiApiKeyField = new JBTextField();
        panel.add(openaiApiKeyField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Base URL:"), gbc);
        gbc.gridx = 1;
        openaiBaseUrlField = new JBTextField("https://api.openai.com/v1");
        panel.add(openaiBaseUrlField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Chat 模型:"), gbc);
        gbc.gridx = 1;
        openaiModelField = new JBTextField("gpt-4o-mini");
        panel.add(openaiModelField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Embedding 模型:"), gbc);
        gbc.gridx = 1;
        openaiEmbeddingModelField = new JBTextField("text-embedding-3-small");
        panel.add(openaiEmbeddingModelField, gbc);
        
        // Ollama 配置
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JBLabel("<html><b>Ollama 配置 (本地模型)</b></html>"), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JBLabel("Base URL:"), gbc);
        gbc.gridx = 1;
        ollamaBaseUrlField = new JBTextField("http://localhost:11434");
        panel.add(ollamaBaseUrlField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Chat 模型:"), gbc);
        gbc.gridx = 1;
        ollamaModelField = new JBTextField("qwen2.5:7b");
        panel.add(ollamaModelField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("Embedding 模型:"), gbc);
        gbc.gridx = 1;
        ollamaEmbeddingModelField = new JBTextField("nomic-embed-text");
        panel.add(ollamaEmbeddingModelField, gbc);
        
        // 填充剩余空间
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(null);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    /**
     * 向量数据库配置面板
     */
    private JPanel createVectorStorePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 数据库类型选择
        panel.add(new JBLabel("向量数据库类型:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        vectorStoreTypeCombo = new ComboBox<>(new String[]{"elasticsearch", "pgvector", "chroma", "simple"});
        panel.add(vectorStoreTypeCombo, gbc);
        
        // Elasticsearch 配置
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JBLabel("<html><b>Elasticsearch 8.15 配置（推荐）</b></html>"), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JBLabel("主机:"), gbc);
        gbc.gridx = 1;
        esHostField = new JBTextField("localhost");
        panel.add(esHostField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("端口:"), gbc);
        gbc.gridx = 1;
        esPortField = new JBTextField("9200");
        panel.add(esPortField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("用户名:"), gbc);
        gbc.gridx = 1;
        esUsernameField = new JBTextField();
        esUsernameField.setToolTipText("可选，如果ES启用了安全认证");
        panel.add(esUsernameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("密码:"), gbc);
        gbc.gridx = 1;
        esPasswordField = new JPasswordField();
        panel.add(esPasswordField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("索引名称:"), gbc);
        gbc.gridx = 1;
        esIndexNameField = new JBTextField("livingdoc_vectors");
        panel.add(esIndexNameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("向量维度:"), gbc);
        gbc.gridx = 1;
        esDimensionsField = new JBTextField("1024");
        esDimensionsField.setToolTipText("需要与 Embedding 模型匹配");
        panel.add(esDimensionsField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("相似度算法:"), gbc);
        gbc.gridx = 1;
        esSimilarityCombo = new ComboBox<>(new String[]{"cosine", "dot_product", "l2_norm"});
        panel.add(esSimilarityCombo, gbc);
        
        // 说明信息
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JBLabel infoLabel = new JBLabel("<html><i>💡 使用 Docker 快速启动 Elasticsearch 8.15:<br>" +
            "docker run -d --name elasticsearch -p 9200:9200 -e \"discovery.type=single-node\" elasticsearch:8.15.0</i></html>");
        panel.add(infoLabel, gbc);
        
        // 填充剩余空间
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(null);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    /**
     * RAG 配置面板
     */
    private JPanel createRagPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        panel.add(new JBLabel("文档分块大小:"), gbc);
        gbc.gridx = 1;
        ragChunkSizeField = new JBTextField("800");
        ragChunkSizeField.setToolTipText("每个文档块的字符数（建议 500-1500）");
        panel.add(ragChunkSizeField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("分块重叠大小:"), gbc);
        gbc.gridx = 1;
        ragChunkOverlapField = new JBTextField("200");
        ragChunkOverlapField.setToolTipText("相邻块的重叠字符数（建议 100-300）");
        panel.add(ragChunkOverlapField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("检索Top-K:"), gbc);
        gbc.gridx = 1;
        ragTopKField = new JBTextField("5");
        ragTopKField.setToolTipText("返回最相似的文档数量（建议 3-10）");
        panel.add(ragTopKField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JBLabel("相似度阈值:"), gbc);
        gbc.gridx = 1;
        ragSimilarityThresholdField = new JBTextField("0.7");
        ragSimilarityThresholdField.setToolTipText("最低相似度阈值（0.0-1.0）");
        panel.add(ragSimilarityThresholdField, gbc);
        
        // 说明
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JBLabel descLabel = new JBLabel("<html><br><b>配置说明：</b><br>" +
            "• 分块大小：影响检索精度和上下文长度<br>" +
            "• 重叠大小：保证上下文连贯性<br>" +
            "• Top-K：影响 RAG 生成质量<br>" +
            "• 相似度阈值：过滤不相关的结果</html>");
        panel.add(descLabel, gbc);
        
        // 填充剩余空间
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    /**
     * 其他配置面板
     */
    private JPanel createOtherPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        autoIndexOnSaveCheck = new JBCheckBox("代码保存时自动索引");
        autoIndexOnSaveCheck.setToolTipText("保存 Java 文件时自动更新文档索引");
        panel.add(autoIndexOnSaveCheck, gbc);
        
        gbc.gridy++;
        showNotificationsCheck = new JBCheckBox("显示通知消息");
        showNotificationsCheck.setToolTipText("显示索引完成、错误等通知");
        panel.add(showNotificationsCheck, gbc);
        
        gbc.gridy++;
        enableLoggingCheck = new JBCheckBox("启用详细日志");
        enableLoggingCheck.setToolTipText("记录详细的调试日志（用于排查问题）");
        panel.add(enableLoggingCheck, gbc);
        
        // 填充剩余空间
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    @Override
    public boolean isModified() {
        LivingDocSettings settings = LivingDocSettings.getInstance(project);
        
        return !aiProviderCombo.getSelectedItem().equals(settings.aiProvider) ||
               !giteeApiKeyField.getText().equals(settings.giteeApiKey) ||
               !giteeBaseUrlField.getText().equals(settings.giteeBaseUrl) ||
               !giteeModelField.getText().equals(settings.giteeModel) ||
               !esHostField.getText().equals(settings.esHost) ||
               !esPortField.getText().equals(String.valueOf(settings.esPort)) ||
               !esIndexNameField.getText().equals(settings.esIndexName);
    }
    
    @Override
    public void apply() {
        LivingDocSettings settings = LivingDocSettings.getInstance(project);
        
        // AI 模型配置
        settings.aiProvider = (String) aiProviderCombo.getSelectedItem();
        settings.giteeApiKey = giteeApiKeyField.getText();
        settings.giteeBaseUrl = giteeBaseUrlField.getText();
        settings.giteeModel = giteeModelField.getText();
        settings.giteeEmbeddingModel = giteeEmbeddingModelField.getText();
        
        settings.openaiApiKey = openaiApiKeyField.getText();
        settings.openaiBaseUrl = openaiBaseUrlField.getText();
        settings.openaiModel = openaiModelField.getText();
        settings.openaiEmbeddingModel = openaiEmbeddingModelField.getText();
        
        settings.ollamaBaseUrl = ollamaBaseUrlField.getText();
        settings.ollamaModel = ollamaModelField.getText();
        settings.ollamaEmbeddingModel = ollamaEmbeddingModelField.getText();
        
        // 向量数据库配置
        settings.vectorStoreType = (String) vectorStoreTypeCombo.getSelectedItem();
        settings.esHost = esHostField.getText();
        settings.esPort = Integer.parseInt(esPortField.getText());
        settings.esUsername = esUsernameField.getText();
        settings.esPassword = new String(esPasswordField.getPassword());
        settings.esIndexName = esIndexNameField.getText();
        settings.esDimensions = Integer.parseInt(esDimensionsField.getText());
        settings.esSimilarityAlgorithm = (String) esSimilarityCombo.getSelectedItem();
        
        // RAG 配置
        settings.ragChunkSize = Integer.parseInt(ragChunkSizeField.getText());
        settings.ragChunkOverlap = Integer.parseInt(ragChunkOverlapField.getText());
        settings.ragTopK = Integer.parseInt(ragTopKField.getText());
        settings.ragSimilarityThreshold = Double.parseDouble(ragSimilarityThresholdField.getText());
        
        // 其他配置
        settings.autoIndexOnSave = autoIndexOnSaveCheck.isSelected();
        settings.showNotifications = showNotificationsCheck.isSelected();
        settings.enableLogging = enableLoggingCheck.isSelected();
    }
    
    @Override
    public void reset() {
        LivingDocSettings settings = LivingDocSettings.getInstance(project);
        
        // AI 模型配置
        aiProviderCombo.setSelectedItem(settings.aiProvider);
        giteeApiKeyField.setText(settings.giteeApiKey);
        giteeBaseUrlField.setText(settings.giteeBaseUrl);
        giteeModelField.setText(settings.giteeModel);
        giteeEmbeddingModelField.setText(settings.giteeEmbeddingModel);
        
        openaiApiKeyField.setText(settings.openaiApiKey);
        openaiBaseUrlField.setText(settings.openaiBaseUrl);
        openaiModelField.setText(settings.openaiModel);
        openaiEmbeddingModelField.setText(settings.openaiEmbeddingModel);
        
        ollamaBaseUrlField.setText(settings.ollamaBaseUrl);
        ollamaModelField.setText(settings.ollamaModel);
        ollamaEmbeddingModelField.setText(settings.ollamaEmbeddingModel);
        
        // 向量数据库配置
        vectorStoreTypeCombo.setSelectedItem(settings.vectorStoreType);
        esHostField.setText(settings.esHost);
        esPortField.setText(String.valueOf(settings.esPort));
        esUsernameField.setText(settings.esUsername);
        esPasswordField.setText(settings.esPassword);
        esIndexNameField.setText(settings.esIndexName);
        esDimensionsField.setText(String.valueOf(settings.esDimensions));
        esSimilarityCombo.setSelectedItem(settings.esSimilarityAlgorithm);
        
        // RAG 配置
        ragChunkSizeField.setText(String.valueOf(settings.ragChunkSize));
        ragChunkOverlapField.setText(String.valueOf(settings.ragChunkOverlap));
        ragTopKField.setText(String.valueOf(settings.ragTopK));
        ragSimilarityThresholdField.setText(String.valueOf(settings.ragSimilarityThreshold));
        
        // 其他配置
        autoIndexOnSaveCheck.setSelected(settings.autoIndexOnSave);
        showNotificationsCheck.setSelected(settings.showNotifications);
        enableLoggingCheck.setSelected(settings.enableLogging);
    }
}

