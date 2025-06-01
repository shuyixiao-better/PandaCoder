package com.shuyixiao.setting;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

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
    private JPanel panel;

    @NotNull
    @Override
    public String getId() {
        return "FileAnnotationInformationConfiguration";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Yixiao Plugin";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // 创建面板和输入区域
        panel = new JPanel(new BorderLayout());
        templateTextArea = new JTextArea(10, 20);

        // 确保从设置中加载模板，加载不到就使用默认值
        String template = PluginSettings.getInstance().getTemplate();
        if (template != null) {
            templateTextArea.setText(template);
        }

        // 添加标签和输入框到面板
        panel.add(new JLabel("文件注释模板："), BorderLayout.NORTH);
        panel.add(new JScrollPane(templateTextArea), BorderLayout.CENTER);

        return panel;
    }

    @Override
    public boolean isModified() {
        return !templateTextArea.getText().equals(PluginSettings.getInstance().getTemplate());
    }

    @Override
    public void apply() {
        PluginSettings.getInstance().setTemplate(templateTextArea.getText());
    }

    @Override
    public void reset() {
        templateTextArea.setText(PluginSettings.getInstance().getTemplate());
    }
}


