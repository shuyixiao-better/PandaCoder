package com.shuyixiao.livingdoc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.*;
import com.shuyixiao.livingdoc.settings.LivingDocSettings;

import javax.swing.*;
import java.awt.*;

/**
 * 活文档工具窗口主面板
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class LivingDocToolWindowPanel {
    
    private final Project project;
    private JPanel mainPanel;
    private JBTextField searchField;
    private JTextArea resultsArea;
    private JTextArea chatArea;
    private JBTextField chatInputField;
    private JBLabel statusLabel;
    
    public LivingDocToolWindowPanel(Project project) {
        this.project = project;
        initUI();
    }
    
    private void initUI() {
        mainPanel = new JPanel(new BorderLayout());
        
        // 创建Tab面板
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("📄 搜索", createSearchPanel());
        tabbedPane.addTab("💬 问答", createChatPanel());
        tabbedPane.addTab("📊 统计", createStatsPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 底部状态栏
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 搜索面板
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 搜索框区域
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        JBLabel searchLabel = new JBLabel("🔍 语义搜索:");
        searchField = new JBTextField();
        searchField.setToolTipText("输入问题，例如：用户登录接口的参数有哪些？");
        
        JButton searchButton = new JButton("搜索");
        searchButton.addActionListener(e -> performSearch());
        
        // 回车搜索
        searchField.addActionListener(e -> performSearch());
        
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // 结果显示区域
        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setText("💡 提示：\n" +
                "1. 输入您的问题进行语义搜索\n" +
                "2. 系统会找到最相关的文档\n" +
                "3. 双击结果可跳转到源代码\n\n" +
                "示例问题：\n" +
                "• 用户登录接口的参数有哪些？\n" +
                "• 如何创建订单？\n" +
                "• 获取用户信息的API路径是什么？");
        
        JBScrollPane scrollPane = new JBScrollPane(resultsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("清除");
        clearButton.addActionListener(e -> resultsArea.setText(""));
        buttonPanel.add(clearButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 聊天问答面板
     */
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 聊天显示区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setText("🤖 AI助手：您好！我是活文档AI助手。\n\n" +
                "我可以帮您：\n" +
                "✓ 解答项目API使用问题\n" +
                "✓ 查找接口文档\n" +
                "✓ 解释代码功能\n\n" +
                "请在下方输入您的问题...\n\n");
        
        JBScrollPane scrollPane = new JBScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JBLabel inputLabel = new JBLabel("提问:");
        chatInputField = new JBTextField();
        chatInputField.setToolTipText("输入您的问题，例如：用户登录接口需要哪些参数？");
        
        JButton askButton = new JButton("发送");
        askButton.addActionListener(e -> askQuestion());
        
        // 回车发送
        chatInputField.addActionListener(e -> askQuestion());
        
        inputPanel.add(inputLabel, BorderLayout.WEST);
        inputPanel.add(chatInputField, BorderLayout.CENTER);
        inputPanel.add(askButton, BorderLayout.EAST);
        
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 统计信息面板
     */
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 配置信息
        LivingDocSettings settings = LivingDocSettings.getInstance(project);
        
        panel.add(new JBLabel("<html><b>📊 系统状态</b></html>"), gbc);
        
        gbc.gridy++;
        panel.add(new JBLabel("AI 提供商: " + settings.aiProvider), gbc);
        
        gbc.gridy++;
        panel.add(new JBLabel("向量数据库: " + settings.vectorStoreType), gbc);
        
        gbc.gridy++;
        panel.add(new JBLabel("Elasticsearch: " + settings.esHost + ":" + settings.esPort), gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JBLabel("<html><b>📈 索引统计</b></html>"), gbc);
        
        gbc.gridy++;
        JBLabel docCountLabel = new JBLabel("总文档数: 加载中...");
        panel.add(docCountLabel, gbc);
        
        gbc.gridy++;
        JBLabel healthLabel = new JBLabel("健康状态: 检查中...");
        panel.add(healthLabel, gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JBLabel("<html><b>🔧 操作</b></html>"), gbc);
        
        gbc.gridy++;
        JButton refreshButton = new JButton("刷新统计");
        refreshButton.addActionListener(e -> refreshStats(docCountLabel, healthLabel));
        panel.add(refreshButton, gbc);
        
        gbc.gridy++;
        JButton indexButton = new JButton("重新索引项目");
        indexButton.addActionListener(e -> reindexProject());
        panel.add(indexButton, gbc);
        
        gbc.gridy++;
        JButton clearButton = new JButton("清空索引");
        clearButton.addActionListener(e -> clearIndex());
        panel.add(clearButton, gbc);
        
        // 填充剩余空间
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        // 自动刷新统计
        refreshStats(docCountLabel, healthLabel);
        
        return panel;
    }
    
    /**
     * 状态栏
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        
        statusLabel = new JBLabel("✓ 就绪");
        panel.add(statusLabel);
        
        return panel;
    }
    
    /**
     * 执行搜索
     */
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        
        statusLabel.setText("🔄 搜索中...");
        resultsArea.setText("正在搜索: \"" + query + "\"\n\n");
        
        // TODO: 调用 RAG 服务进行搜索
        // 这里需要集成实际的搜索逻辑
        
        resultsArea.append("💡 提示：搜索功能需要先索引项目文档\n");
        resultsArea.append("请使用 Tools -> 活文档 -> 索引项目 进行索引\n");
        
        statusLabel.setText("✓ 搜索完成");
    }
    
    /**
     * 提问
     */
    private void askQuestion() {
        String question = chatInputField.getText().trim();
        if (question.isEmpty()) {
            return;
        }
        
        chatArea.append("\n🧑 您: " + question + "\n\n");
        chatInputField.setText("");
        
        statusLabel.setText("🔄 AI思考中...");
        chatArea.append("🤖 AI助手: ");
        
        // TODO: 调用 RAG 服务进行问答
        // 这里需要集成实际的RAG问答逻辑
        
        chatArea.append("功能开发中，敬请期待...\n\n");
        chatArea.append("💡 提示：请先配置 AI API Key\n");
        chatArea.append("File -> Settings -> Tools -> 活文档\n\n");
        
        statusLabel.setText("✓ 就绪");
        
        // 滚动到底部
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * 刷新统计
     */
    private void refreshStats(JBLabel docCountLabel, JBLabel healthLabel) {
        // TODO: 调用服务获取实际统计信息
        docCountLabel.setText("总文档数: 0 (未索引)");
        healthLabel.setText("健康状态: ⚠️ 未连接");
    }
    
    /**
     * 重新索引项目
     */
    private void reindexProject() {
        int result = JOptionPane.showConfirmDialog(
            mainPanel,
            "确定要重新索引整个项目吗？\n这可能需要几分钟时间。",
            "确认",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            statusLabel.setText("🔄 索引中...");
            // TODO: 调用索引服务
            JOptionPane.showMessageDialog(mainPanel, "索引功能开发中...");
            statusLabel.setText("✓ 就绪");
        }
    }
    
    /**
     * 清空索引
     */
    private void clearIndex() {
        int result = JOptionPane.showConfirmDialog(
            mainPanel,
            "⚠️ 警告：这将删除所有已索引的文档！\n确定要继续吗？",
            "确认删除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            statusLabel.setText("🔄 清空中...");
            // TODO: 调用清空服务
            JOptionPane.showMessageDialog(mainPanel, "清空功能开发中...");
            statusLabel.setText("✓ 就绪");
        }
    }
    
    public JComponent getContent() {
        return mainPanel;
    }
}

