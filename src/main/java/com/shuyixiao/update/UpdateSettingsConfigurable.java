package com.shuyixiao.update;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateSettingsConfigurable.java
 * author 舒一笑不秃头
 * version 1.0.0
 * Description 更新设置配置页面 提供用户配置更新检查相关选项
 * createTime 2025年10月24日 16:00:00
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class UpdateSettingsConfigurable implements Configurable {

    private JPanel mainPanel;
    private JBCheckBox enableAutoCheckBox;
    private JBCheckBox enableNotificationsCheckBox;
    private ButtonGroup frequencyGroup;
    private JRadioButton startupRadio;
    private JRadioButton dailyRadio;
    private JRadioButton weeklyRadio;
    private JRadioButton disabledRadio;
    private JButton checkNowButton;
    private JButton clearIgnoredButton;
    private JLabel statusLabel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "更新设置";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();

        // 主面板
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 创建表单
        JPanel formPanel = createFormPanel(settings);
        mainPanel.add(formPanel, BorderLayout.NORTH);

        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // 创建状态面板
        JPanel statusPanel = createStatusPanel(settings);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * 创建表单面板
     */
    private JPanel createFormPanel(UpdateSettingsState settings) {
        // 启用自动检查复选框
        enableAutoCheckBox = new JBCheckBox("启用自动检查更新", settings.isAutoCheckEnabled());
        enableAutoCheckBox.setToolTipText("在IDE启动时自动检查插件更新");

        // 启用更新通知复选框
        enableNotificationsCheckBox = new JBCheckBox("显示更新通知", settings.isUpdateNotificationsEnabled());
        enableNotificationsCheckBox.setToolTipText("发现新版本时显示通知");

        // 提醒频率单选按钮
        JBLabel frequencyLabel = new JBLabel("提醒频率：");
        frequencyGroup = new ButtonGroup();

        startupRadio = new JRadioButton("每次启动");
        startupRadio.setToolTipText("每次启动IDE时检查更新");

        dailyRadio = new JRadioButton("每天一次（推荐）");
        dailyRadio.setToolTipText("每天首次打开项目时检查更新");

        weeklyRadio = new JRadioButton("每周一次");
        weeklyRadio.setToolTipText("每周首次打开项目时检查更新");

        disabledRadio = new JRadioButton("从不");
        disabledRadio.setToolTipText("不自动检查更新");

        frequencyGroup.add(startupRadio);
        frequencyGroup.add(dailyRadio);
        frequencyGroup.add(weeklyRadio);
        frequencyGroup.add(disabledRadio);

        // 根据当前设置选择对应的单选按钮
        UpdateSettingsState.ReminderFrequency frequency = settings.getReminderFrequencyEnum();
        switch (frequency) {
            case STARTUP:
                startupRadio.setSelected(true);
                break;
            case DAILY:
                dailyRadio.setSelected(true);
                break;
            case WEEKLY:
                weeklyRadio.setSelected(true);
                break;
            case DISABLED:
                disabledRadio.setSelected(true);
                break;
        }

        // 创建频率选项面板
        JPanel frequencyPanel = new JPanel();
        frequencyPanel.setLayout(new BoxLayout(frequencyPanel, BoxLayout.Y_AXIS));
        frequencyPanel.add(startupRadio);
        frequencyPanel.add(dailyRadio);
        frequencyPanel.add(weeklyRadio);
        frequencyPanel.add(disabledRadio);

        // 使用FormBuilder构建表单
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("<html><b>自动更新检查</b></html>"))
                .addVerticalGap(5)
                .addComponent(enableAutoCheckBox)
                .addComponent(enableNotificationsCheckBox)
                .addVerticalGap(10)
                .addLabeledComponent(frequencyLabel, frequencyPanel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(JBUI.Borders.empty(10, 0));

        // 立即检查更新按钮
        checkNowButton = new JButton("立即检查更新");
        checkNowButton.setToolTipText("手动检查插件更新");
        checkNowButton.addActionListener(e -> {
            checkNowButton.setEnabled(false);
            checkNowButton.setText("检查中...");

            // 在后台线程中检查更新
            new Thread(() -> {
                try {
                    Project project = ProjectManager.getInstance().getDefaultProject();
                    UpdateCheckService.manualCheckUpdate(project);

                    // 等待一段时间后恢复按钮状态
                    Thread.sleep(2000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        checkNowButton.setEnabled(true);
                        checkNowButton.setText("立即检查更新");
                        statusLabel.setText("已完成检查");
                    });
                }
            }).start();
        });

        // 清除已忽略版本按钮
        clearIgnoredButton = new JButton("清除已忽略版本");
        clearIgnoredButton.setToolTipText("清除所有被忽略的版本，下次将重新提醒");
        clearIgnoredButton.addActionListener(e -> {
            UpdateSettingsState settings = UpdateSettingsState.getInstance();
            int count = settings.getIgnoredVersions().size();

            if (count > 0) {
                int confirm = JOptionPane.showConfirmDialog(
                        mainPanel,
                        "确定要清除 " + count + " 个已忽略的版本吗？\n清除后，这些版本的更新通知将会重新显示。",
                        "确认清除",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    settings.setIgnoredVersions(new ArrayList<>());
                    statusLabel.setText("已清除 " + count + " 个忽略的版本");
                    JOptionPane.showMessageDialog(
                            mainPanel,
                            "已成功清除所有忽略的版本",
                            "清除成功",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(
                        mainPanel,
                        "当前没有被忽略的版本",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        buttonPanel.add(checkNowButton);
        buttonPanel.add(clearIgnoredButton);

        return buttonPanel;
    }

    /**
     * 创建状态面板
     */
    private JPanel createStatusPanel(UpdateSettingsState settings) {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));

        // 状态信息
        StringBuilder statusText = new StringBuilder("<html><body style='color: gray; font-size: 10px;'>");
        statusText.append("当前版本: <b>").append(com.shuyixiao.version.VersionInfo.getVersion()).append("</b><br>");

        if (settings.getLastShownVersion() != null && !settings.getLastShownVersion().isEmpty()) {
            statusText.append("上次提醒版本: ").append(settings.getLastShownVersion()).append("<br>");
        }

        if (settings.getLastCheckDate() != null && !settings.getLastCheckDate().isEmpty()) {
            statusText.append("上次检查日期: ").append(settings.getLastCheckDate()).append("<br>");
        }

        int ignoredCount = settings.getIgnoredVersions() != null ? settings.getIgnoredVersions().size() : 0;
        if (ignoredCount > 0) {
            statusText.append("已忽略版本数: ").append(ignoredCount).append("<br>");
        }

        statusText.append("</body></html>");

        statusLabel = new JLabel(statusText.toString());
        statusPanel.add(statusLabel, BorderLayout.NORTH);

        // 添加说明文本
        JBLabel infoLabel = new JBLabel(
                "<html><body style='color: gray; font-size: 10px;'>" +
                        "<br><b>说明：</b><br>" +
                        "• 启用自动检查后，插件将在IDE启动时检查更新<br>" +
                        "• 检查时机：IDE启动延迟5秒后，避免影响启动速度<br>" +
                        "• 每天首次打开项目时会检查更新（如果启用）<br>" +
                        "• 忽略的版本不会再显示更新提醒<br>" +
                        "• 更新通知会在屏幕右下角显示，不会打断您的工作<br>" +
                        "</body></html>"
        );
        statusPanel.add(infoLabel, BorderLayout.CENTER);

        return statusPanel;
    }

    @Override
    public boolean isModified() {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();

        // 检查是否有任何设置被修改
        if (enableAutoCheckBox.isSelected() != settings.isAutoCheckEnabled()) {
            return true;
        }

        if (enableNotificationsCheckBox.isSelected() != settings.isUpdateNotificationsEnabled()) {
            return true;
        }

        // 检查提醒频率是否修改
        UpdateSettingsState.ReminderFrequency currentFrequency = settings.getReminderFrequencyEnum();
        UpdateSettingsState.ReminderFrequency selectedFrequency = getSelectedFrequency();

        return currentFrequency != selectedFrequency;
    }

    @Override
    public void apply() throws ConfigurationException {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();

        // 保存设置
        settings.setAutoCheckEnabled(enableAutoCheckBox.isSelected());
        settings.setUpdateNotificationsEnabled(enableNotificationsCheckBox.isSelected());
        settings.setReminderFrequency(getSelectedFrequency().name());

        statusLabel.setText("设置已保存");
    }

    @Override
    public void reset() {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();

        // 重置为当前保存的设置
        enableAutoCheckBox.setSelected(settings.isAutoCheckEnabled());
        enableNotificationsCheckBox.setSelected(settings.isUpdateNotificationsEnabled());

        UpdateSettingsState.ReminderFrequency frequency = settings.getReminderFrequencyEnum();
        switch (frequency) {
            case STARTUP:
                startupRadio.setSelected(true);
                break;
            case DAILY:
                dailyRadio.setSelected(true);
                break;
            case WEEKLY:
                weeklyRadio.setSelected(true);
                break;
            case DISABLED:
                disabledRadio.setSelected(true);
                break;
        }
    }

    /**
     * 获取当前选择的提醒频率
     */
    private UpdateSettingsState.ReminderFrequency getSelectedFrequency() {
        if (startupRadio.isSelected()) {
            return UpdateSettingsState.ReminderFrequency.STARTUP;
        } else if (dailyRadio.isSelected()) {
            return UpdateSettingsState.ReminderFrequency.DAILY;
        } else if (weeklyRadio.isSelected()) {
            return UpdateSettingsState.ReminderFrequency.WEEKLY;
        } else {
            return UpdateSettingsState.ReminderFrequency.DISABLED;
        }
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        enableAutoCheckBox = null;
        enableNotificationsCheckBox = null;
        frequencyGroup = null;
        startupRadio = null;
        dailyRadio = null;
        weeklyRadio = null;
        disabledRadio = null;
        checkNowButton = null;
        clearIgnoredButton = null;
        statusLabel = null;
    }
}

