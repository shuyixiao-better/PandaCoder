package com.shuyixiao.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 现代化的输入对话框
 * 提供美观的用户输入界面
 */
public class ModernInputDialog extends DialogWrapper {
    
    private final String prompt;
    private final String defaultValue;
    private final InputValidator validator;
    private JBTextField inputField;
    private JBLabel hintLabel;

    /**
     * 输入验证器接口
     */
    public interface InputValidator {
        /**
         * 验证输入内容
         * @param input 用户输入
         * @return 验证结果，null表示验证通过，否则返回错误信息
         */
        String validate(String input);
    }

    public ModernInputDialog(@Nullable Project project, String title, String prompt, 
                            String defaultValue, InputValidator validator) {
        super(project);
        this.prompt = prompt;
        this.defaultValue = defaultValue;
        this.validator = validator;
        setTitle(title);
        setResizable(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(400, 120));
        mainPanel.setBorder(JBUI.Borders.empty(20));

        // 提示信息
        JBLabel promptLabel = new JBLabel(prompt);
        promptLabel.setFont(promptLabel.getFont().deriveFont(Font.PLAIN, 13f));
        promptLabel.setBorder(JBUI.Borders.emptyBottom(8));
        mainPanel.add(promptLabel, BorderLayout.NORTH);

        // 输入框
        inputField = new JBTextField(defaultValue != null ? defaultValue : "");
        inputField.setPreferredSize(JBUI.size(350, 28));
        inputField.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1),
            JBUI.Borders.empty(5, 8)
        ));
        
        // 添加实时验证
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                validateInput();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                validateInput();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                validateInput();
            }
        });

        JBPanel<?> inputPanel = new JBPanel<>(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // 提示信息标签
        hintLabel = new JBLabel(" ");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 11f));
        hintLabel.setForeground(UIUtil.getErrorForeground());
        hintLabel.setBorder(JBUI.Borders.emptyTop(5));
        mainPanel.add(hintLabel, BorderLayout.SOUTH);

        // 设置输入框获得焦点
        SwingUtilities.invokeLater(() -> {
            inputField.requestFocusInWindow();
            if (defaultValue != null && !defaultValue.isEmpty()) {
                inputField.selectAll();
            }
        });

        return mainPanel;
    }

    private void validateInput() {
        if (validator != null) {
            String error = validator.validate(inputField.getText());
            if (error != null) {
                hintLabel.setText("⚠️ " + error);
                hintLabel.setVisible(true);
                setOKActionEnabled(false);
            } else {
                hintLabel.setText(" ");
                hintLabel.setVisible(true);
                setOKActionEnabled(true);
            }
        } else {
            setOKActionEnabled(!inputField.getText().trim().isEmpty());
        }
        
        // 重新布局以适应提示文本的变化
        getContentPanel().revalidate();
    }

    @Override
    protected ValidationInfo doValidate() {
        String input = inputField.getText().trim();
        
        if (input.isEmpty()) {
            return new ValidationInfo("输入不能为空", inputField);
        }
        
        if (validator != null) {
            String error = validator.validate(input);
            if (error != null) {
                return new ValidationInfo(error, inputField);
            }
        }
        
        return null;
    }

    /**
     * 获取用户输入的内容
     */
    public String getInputText() {
        return inputField != null ? inputField.getText().trim() : "";
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{
            new AbstractAction("确定") {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    doOKAction();
                }
            },
            new AbstractAction("取消") {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    doCancelAction();
                }
            }
        };
    }

    /**
     * 显示输入对话框的便捷方法
     */
    public static String showDialog(@Nullable Project project, String title, String prompt) {
        return showDialog(project, title, prompt, "", null);
    }

    /**
     * 显示输入对话框的便捷方法（带默认值）
     */
    public static String showDialog(@Nullable Project project, String title, String prompt, 
                                   String defaultValue) {
        return showDialog(project, title, prompt, defaultValue, null);
    }

    /**
     * 显示输入对话框的便捷方法（带验证器）
     */
    public static String showDialog(@Nullable Project project, String title, String prompt, 
                                   String defaultValue, InputValidator validator) {
        ModernInputDialog dialog = new ModernInputDialog(project, title, prompt, defaultValue, validator);
        if (dialog.showAndGet()) {
            return dialog.getInputText();
        }
        return null;
    }

    /**
     * 创建中文类名验证器
     */
    public static InputValidator createChineseClassNameValidator() {
        return input -> {
            if (input == null || input.trim().isEmpty()) {
                return "类名不能为空";
            }
            
            input = input.trim();
            
            // 检查是否包含中文字符
            if (!input.matches(".*[\\u4e00-\\u9fa5].*")) {
                return "请输入包含中文的类名";
            }
            
            // 检查长度
            if (input.length() > 50) {
                return "类名过长，请控制在50个字符以内";
            }
            
            // 检查特殊字符
            if (input.matches(".*[<>:\"/\\\\|?*].*")) {
                return "类名不能包含特殊字符：< > : \" / \\ | ? *";
            }
            
            return null;
        };
    }

    /**
     * 创建翻译文本验证器
     */
    public static InputValidator createTranslationValidator() {
        return input -> {
            if (input == null || input.trim().isEmpty()) {
                return "翻译文本不能为空";
            }
            
            input = input.trim();
            
            // 检查长度
            if (input.length() > 200) {
                return "文本过长，请控制在200个字符以内";
            }
            
            return null;
        };
    }
} 