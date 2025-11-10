package com.shuyixiao.gitstat.weekly.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.gitstat.weekly.config.UserIdentityConfigState;
import com.shuyixiao.gitstat.weekly.util.DeviceIdentifierUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 用户身份配置界面
 * 允许用户配置自己的用户名、编码等信息
 * 
 * @author PandaCoder Team
 * @since 2.2.0
 */
public class UserIdentityConfigurable implements Configurable {
    
    private JBTextField userNameField;
    private JBLabel userCodeLabel;
    private JBTextField userEmailField;
    private JBLabel deviceIdLabel;
    private JPanel mainPanel;
    private String generatedUserCode;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Git统计 - 用户身份配置";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        userNameField = new JBTextField(30);
        userEmailField = new JBTextField(30);

        // 获取并显示设备ID
        String deviceId = DeviceIdentifierUtil.getDeviceId();
        String displayDeviceId = deviceId.length() > 16 ?
            deviceId.substring(0, 16) + "..." : deviceId;
        deviceIdLabel = new JBLabel(displayDeviceId);
        deviceIdLabel.setToolTipText("完整设备ID: " + deviceId);

        // 根据设备ID生成用户编码（取前12位）
        generatedUserCode = deviceId.length() >= 12 ?
            deviceId.substring(0, 12).toUpperCase() : deviceId.toUpperCase();
        userCodeLabel = new JBLabel(generatedUserCode);
        userCodeLabel.setToolTipText("用户编码由设备ID自动生成: " + generatedUserCode);
        
        // 创建说明面板
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        JTextArea infoArea = new JTextArea();
        infoArea.setText(
            "📋 用户身份配置说明\n\n" +
            "此配置用于在周报归档时标识用户身份，确保数据的可追溯性。\n\n" +
            "• 设备ID：自动获取，基于您的MAC地址生成，用于唯一标识您的设备\n" +
            "• 用户编码：根据设备ID自动生成，用于唯一标识您的账户\n" +
            "• 用户名：您的真实姓名或昵称（必填）\n" +
            "• 邮箱：您的工作邮箱（可选）\n\n" +
            "⚠️ 注意：用户名为必填项，归档周报前请先配置。"
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(mainPanel != null ? mainPanel.getBackground() : null);
        infoArea.setFont(new JBLabel().getFont());
        infoArea.setBorder(JBUI.Borders.empty(5));
        
        infoPanel.add(infoArea, BorderLayout.CENTER);
        
        // 创建表单
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(infoPanel)
            .addSeparator()
            .addLabeledComponent(new JBLabel("设备ID (自动获取):"), deviceIdLabel, 5, false)
            .addLabeledComponent(new JBLabel("用户编码 (自动生成):"), userCodeLabel, 5, false)
            .addSeparator()
            .addLabeledComponent(new JBLabel("用户名 *:"), userNameField, 5, false)
            .addLabeledComponent(new JBLabel("邮箱:"), userEmailField, 5, false)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        UserIdentityConfigState config = UserIdentityConfigState.getInstance();

        return !userNameField.getText().equals(config.getUserName())
            || !userEmailField.getText().equals(config.getUserEmail());
    }
    
    @Override
    public void apply() throws ConfigurationException {
        // 验证必填字段
        String userName = userNameField.getText().trim();

        if (userName.isEmpty()) {
            throw new ConfigurationException("用户名不能为空");
        }

        // 保存配置
        UserIdentityConfigState config = UserIdentityConfigState.getInstance();
        config.setUserName(userName);
        config.setUserCode(generatedUserCode);  // 使用自动生成的用户编码
        config.setUserEmail(userEmailField.getText().trim());
        config.setUserDepartment("");  // 部门字段设为空
    }
    
    @Override
    public void reset() {
        UserIdentityConfigState config = UserIdentityConfigState.getInstance();

        userNameField.setText(config.getUserName());
        userEmailField.setText(config.getUserEmail());
        // 用户编码和部门不需要重置，因为它们是自动生成/固定的
    }
}

