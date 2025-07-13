package com.shuyixiao.spring.boot.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Spring Boot Helper配置界面
 * 提供用户界面来配置Spring Boot Helper的各种功能
 */
public class SpringBootHelperConfigurable implements Configurable {

    private SpringBootHelperSettings settings;
    private JPanel mainPanel;
    
    // 复选框组件
    private JCheckBox configurationCompletionCheckBox;
    private JCheckBox annotationCompletionCheckBox;
    private JCheckBox configurationValidationCheckBox;
    private JCheckBox annotationDocumentationCheckBox;
    private JCheckBox configurationDocumentationCheckBox;
    private JCheckBox configurationFormattingCheckBox;
    private JCheckBox autoImportCheckBox;
    private JCheckBox deprecationWarningsCheckBox;
    private JCheckBox environmentSpecificCompletionCheckBox;
    private JCheckBox configurationTypeHintsCheckBox;
    private JCheckBox annotationParameterValidationCheckBox;
    private JCheckBox duplicateKeyDetectionCheckBox;
    
    // 按钮组件
    private JButton resetButton;
    private JButton enableAllButton;
    private JButton disableAllButton;

    public SpringBootHelperConfigurable() {
        settings = SpringBootHelperSettings.getInstance();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Spring Boot Helper";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "settings.springboot.helper";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (mainPanel == null) {
            createUI();
        }
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return settings.isConfigurationCompletionEnabled() != configurationCompletionCheckBox.isSelected() ||
               settings.isAnnotationCompletionEnabled() != annotationCompletionCheckBox.isSelected() ||
               settings.isConfigurationValidationEnabled() != configurationValidationCheckBox.isSelected() ||
               settings.isAnnotationDocumentationEnabled() != annotationDocumentationCheckBox.isSelected() ||
               settings.isConfigurationDocumentationEnabled() != configurationDocumentationCheckBox.isSelected() ||
               settings.isConfigurationFormattingEnabled() != configurationFormattingCheckBox.isSelected() ||
               settings.isAutoImportEnabled() != autoImportCheckBox.isSelected() ||
               settings.isDeprecationWarningsEnabled() != deprecationWarningsCheckBox.isSelected() ||
               settings.isEnvironmentSpecificCompletionEnabled() != environmentSpecificCompletionCheckBox.isSelected() ||
               settings.isConfigurationTypeHintsEnabled() != configurationTypeHintsCheckBox.isSelected() ||
               settings.isAnnotationParameterValidationEnabled() != annotationParameterValidationCheckBox.isSelected() ||
               settings.isDuplicateKeyDetectionEnabled() != duplicateKeyDetectionCheckBox.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        settings.setConfigurationCompletionEnabled(configurationCompletionCheckBox.isSelected());
        settings.setAnnotationCompletionEnabled(annotationCompletionCheckBox.isSelected());
        settings.setConfigurationValidationEnabled(configurationValidationCheckBox.isSelected());
        settings.setAnnotationDocumentationEnabled(annotationDocumentationCheckBox.isSelected());
        settings.setConfigurationDocumentationEnabled(configurationDocumentationCheckBox.isSelected());
        settings.setConfigurationFormattingEnabled(configurationFormattingCheckBox.isSelected());
        settings.setAutoImportEnabled(autoImportCheckBox.isSelected());
        settings.setDeprecationWarningsEnabled(deprecationWarningsCheckBox.isSelected());
        settings.setEnvironmentSpecificCompletionEnabled(environmentSpecificCompletionCheckBox.isSelected());
        settings.setConfigurationTypeHintsEnabled(configurationTypeHintsCheckBox.isSelected());
        settings.setAnnotationParameterValidationEnabled(annotationParameterValidationCheckBox.isSelected());
        settings.setDuplicateKeyDetectionEnabled(duplicateKeyDetectionCheckBox.isSelected());
    }

    @Override
    public void reset() {
        configurationCompletionCheckBox.setSelected(settings.isConfigurationCompletionEnabled());
        annotationCompletionCheckBox.setSelected(settings.isAnnotationCompletionEnabled());
        configurationValidationCheckBox.setSelected(settings.isConfigurationValidationEnabled());
        annotationDocumentationCheckBox.setSelected(settings.isAnnotationDocumentationEnabled());
        configurationDocumentationCheckBox.setSelected(settings.isConfigurationDocumentationEnabled());
        configurationFormattingCheckBox.setSelected(settings.isConfigurationFormattingEnabled());
        autoImportCheckBox.setSelected(settings.isAutoImportEnabled());
        deprecationWarningsCheckBox.setSelected(settings.isDeprecationWarningsEnabled());
        environmentSpecificCompletionCheckBox.setSelected(settings.isEnvironmentSpecificCompletionEnabled());
        configurationTypeHintsCheckBox.setSelected(settings.isConfigurationTypeHintsEnabled());
        annotationParameterValidationCheckBox.setSelected(settings.isAnnotationParameterValidationEnabled());
        duplicateKeyDetectionCheckBox.setSelected(settings.isDuplicateKeyDetectionEnabled());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        configurationCompletionCheckBox = null;
        annotationCompletionCheckBox = null;
        configurationValidationCheckBox = null;
        annotationDocumentationCheckBox = null;
        configurationDocumentationCheckBox = null;
        configurationFormattingCheckBox = null;
        autoImportCheckBox = null;
        deprecationWarningsCheckBox = null;
        environmentSpecificCompletionCheckBox = null;
        configurationTypeHintsCheckBox = null;
        annotationParameterValidationCheckBox = null;
        duplicateKeyDetectionCheckBox = null;
        resetButton = null;
        enableAllButton = null;
        disableAllButton = null;
    }

    /**
     * 创建用户界面
     */
    private void createUI() {
        mainPanel = new JPanel(new BorderLayout());
        
        // 创建主要内容面板
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 创建标题
        JLabel titleLabel = new JLabel("Spring Boot Helper 配置");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(titleLabel, gbc);
        
        // 重置gridwidth
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        
        // 智能补全功能组
        int row = 1;
        addSectionTitle(contentPanel, gbc, "智能补全功能", row++);
        
        configurationCompletionCheckBox = new JCheckBox("配置文件智能补全");
        configurationCompletionCheckBox.setToolTipText("为application.properties等配置文件提供智能补全");
        addCheckBox(contentPanel, gbc, configurationCompletionCheckBox, row++);
        
        annotationCompletionCheckBox = new JCheckBox("注解智能补全");
        annotationCompletionCheckBox.setToolTipText("为Spring Boot注解提供智能补全");
        addCheckBox(contentPanel, gbc, annotationCompletionCheckBox, row++);
        
        environmentSpecificCompletionCheckBox = new JCheckBox("环境特定补全");
        environmentSpecificCompletionCheckBox.setToolTipText("根据不同环境提供特定的配置补全");
        addCheckBox(contentPanel, gbc, environmentSpecificCompletionCheckBox, row++);
        
        // 文档和提示功能组
        row++;
        addSectionTitle(contentPanel, gbc, "文档和提示功能", row++);
        
        annotationDocumentationCheckBox = new JCheckBox("注解文档提示");
        annotationDocumentationCheckBox.setToolTipText("鼠标悬停时显示注解文档");
        addCheckBox(contentPanel, gbc, annotationDocumentationCheckBox, row++);
        
        configurationDocumentationCheckBox = new JCheckBox("配置文档提示");
        configurationDocumentationCheckBox.setToolTipText("鼠标悬停时显示配置属性文档");
        addCheckBox(contentPanel, gbc, configurationDocumentationCheckBox, row++);
        
        configurationTypeHintsCheckBox = new JCheckBox("配置类型提示");
        configurationTypeHintsCheckBox.setToolTipText("显示配置属性的类型信息");
        addCheckBox(contentPanel, gbc, configurationTypeHintsCheckBox, row++);
        
        deprecationWarningsCheckBox = new JCheckBox("废弃属性警告");
        deprecationWarningsCheckBox.setToolTipText("为已废弃的配置属性显示警告");
        addCheckBox(contentPanel, gbc, deprecationWarningsCheckBox, row++);
        
        // 验证和检查功能组
        row++;
        addSectionTitle(contentPanel, gbc, "验证和检查功能", row++);
        
        configurationValidationCheckBox = new JCheckBox("配置文件值验证");
        configurationValidationCheckBox.setToolTipText("验证配置文件中的值是否正确");
        addCheckBox(contentPanel, gbc, configurationValidationCheckBox, row++);
        
        annotationParameterValidationCheckBox = new JCheckBox("注解参数验证");
        annotationParameterValidationCheckBox.setToolTipText("验证注解参数是否正确");
        addCheckBox(contentPanel, gbc, annotationParameterValidationCheckBox, row++);
        
        duplicateKeyDetectionCheckBox = new JCheckBox("重复键检测");
        duplicateKeyDetectionCheckBox.setToolTipText("检测配置文件中的重复键");
        addCheckBox(contentPanel, gbc, duplicateKeyDetectionCheckBox, row++);
        
        // 其他功能组
        row++;
        addSectionTitle(contentPanel, gbc, "其他功能", row++);
        
        configurationFormattingCheckBox = new JCheckBox("配置文件格式化");
        configurationFormattingCheckBox.setToolTipText("自动格式化配置文件");
        addCheckBox(contentPanel, gbc, configurationFormattingCheckBox, row++);
        
        autoImportCheckBox = new JCheckBox("自动导入依赖");
        autoImportCheckBox.setToolTipText("自动导入Spring Boot相关依赖");
        addCheckBox(contentPanel, gbc, autoImportCheckBox, row++);
        
        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();
        
        // 将内容面板放入滚动面板
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 添加分组标题
     */
    private void addSectionTitle(JPanel panel, GridBagConstraints gbc, String title, int row) {
        JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 14f));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sectionLabel, gbc);
        
        // 重置设置
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
    }

    /**
     * 添加复选框
     */
    private void addCheckBox(JPanel panel, GridBagConstraints gbc, JCheckBox checkBox, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 20, 2, 5);
        panel.add(checkBox, gbc);
        gbc.insets = new Insets(5, 5, 5, 5);
    }

    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        resetButton = new JButton("重置为默认值");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToDefaults();
            }
        });
        
        enableAllButton = new JButton("启用所有功能");
        enableAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableAll();
            }
        });
        
        disableAllButton = new JButton("禁用所有功能");
        disableAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disableAll();
            }
        });
        
        buttonPanel.add(resetButton);
        buttonPanel.add(enableAllButton);
        buttonPanel.add(disableAllButton);
        
        return buttonPanel;
    }

    /**
     * 重置为默认值
     */
    private void resetToDefaults() {
        configurationCompletionCheckBox.setSelected(true);
        annotationCompletionCheckBox.setSelected(true);
        configurationValidationCheckBox.setSelected(true);
        annotationDocumentationCheckBox.setSelected(true);
        configurationDocumentationCheckBox.setSelected(true);
        configurationFormattingCheckBox.setSelected(true);
        autoImportCheckBox.setSelected(true);
        deprecationWarningsCheckBox.setSelected(true);
        environmentSpecificCompletionCheckBox.setSelected(true);
        configurationTypeHintsCheckBox.setSelected(true);
        annotationParameterValidationCheckBox.setSelected(true);
        duplicateKeyDetectionCheckBox.setSelected(true);
    }

    /**
     * 启用所有功能
     */
    private void enableAll() {
        configurationCompletionCheckBox.setSelected(true);
        annotationCompletionCheckBox.setSelected(true);
        configurationValidationCheckBox.setSelected(true);
        annotationDocumentationCheckBox.setSelected(true);
        configurationDocumentationCheckBox.setSelected(true);
        configurationFormattingCheckBox.setSelected(true);
        autoImportCheckBox.setSelected(true);
        deprecationWarningsCheckBox.setSelected(true);
        environmentSpecificCompletionCheckBox.setSelected(true);
        configurationTypeHintsCheckBox.setSelected(true);
        annotationParameterValidationCheckBox.setSelected(true);
        duplicateKeyDetectionCheckBox.setSelected(true);
    }

    /**
     * 禁用所有功能
     */
    private void disableAll() {
        configurationCompletionCheckBox.setSelected(false);
        annotationCompletionCheckBox.setSelected(false);
        configurationValidationCheckBox.setSelected(false);
        annotationDocumentationCheckBox.setSelected(false);
        configurationDocumentationCheckBox.setSelected(false);
        configurationFormattingCheckBox.setSelected(false);
        autoImportCheckBox.setSelected(false);
        deprecationWarningsCheckBox.setSelected(false);
        environmentSpecificCompletionCheckBox.setSelected(false);
        configurationTypeHintsCheckBox.setSelected(false);
        annotationParameterValidationCheckBox.setSelected(false);
        duplicateKeyDetectionCheckBox.setSelected(false);
    }
} 