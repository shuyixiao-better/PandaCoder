package com.shuyixiao;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.shuyixiao.converter.TranslationConverter;
import com.shuyixiao.notification.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName ConvertToCamelCaseAction.java
 * author 舒一笑不秃头 yixiaoshu88@163.com
 * version 1.0.0
 * Description 中文转小驼峰
 * createTime 2024年08月21日 23:31:00
 */
public class ConvertToCamelCaseAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 检查API配置是否已设置
        if (!com.shuyixiao.util.TranslationUtil.checkApiConfiguration()) {
            return; // 未配置API，无法继续
        }

        // 获取当前编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        // 获取当前项目
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            // 如果没有选中文本，提示用户
            showStatusBarMessage(project, "请先选择要转换的中文文本");
            return;
        }

        // 显示转换选项弹窗
        showConversionOptionsPopup(e, editor, selectedText);
    }

    /**
     * 显示转换选项弹窗
     */
    private void showConversionOptionsPopup(AnActionEvent e, Editor editor, String selectedText) {
        Project project = e.getProject();
        if (project == null) return;

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(
                new BaseListPopupStep<ConversionOption>("选择转换方式", 
                        ConversionOption.values()) {
                    @Override
                    public @NotNull String getTextFor(ConversionOption value) {
                        return value.getDisplayName();
                    }

                    @Override
                    public Icon getIconFor(ConversionOption value) {
                        return value.getIcon();
                    }

                    @Override
                    public PopupStep<?> onChosen(ConversionOption selectedValue, boolean finalChoice) {
                        if (finalChoice) {
                            processConversion(e, editor, selectedText, selectedValue);
                        }
                        return FINAL_CHOICE;
                    }
                }
        );

        popup.showInBestPositionFor(editor);
    }

    /**
     * 处理转换操作
     */
    private void processConversion(AnActionEvent e, Editor editor, String selectedText, ConversionOption option) {
        Project project = e.getProject();
        if (project == null) return;

        // 获取转换后的文本
        String convertedText;
        if (option == ConversionOption.SMART_CONVERT) {
            // 智能转换 - 先进行文本预处理，再转换
            // 使用更严格的精简算法生成短小精悍的名称
            convertedText = smartConvert(selectedText, project);
        } else {
            // 普通转换
            convertedText = TranslationConverter.convertToLowerCamelCase(selectedText, project);
        }

        if (convertedText == null) {
            // 转换失败，已经显示了错误信息
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        Document document = editor.getDocument();
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        // 根据选择的选项处理
        switch (option) {
            case REPLACE_DIRECTLY:
            case SMART_CONVERT: // 智能转换也是直接替换
                // 直接替换选中文本
                WriteCommandAction.runWriteCommandAction(project, () -> 
                        document.replaceString(start, end, convertedText));
                showStatusBarMessage(project, "已将 '" + selectedText + "' 转换为 '" + convertedText + "'");
                break;

            case SHOW_PREVIEW:
                // 显示预览对话框
                int choice = Messages.showYesNoDialog(
                        project,
                        "将 '" + selectedText + "' 转换为: '" + convertedText + "'\n\n是否应用此转换?",
                        "转换预览",
                        "应用",
                        "取消",
                        Messages.getQuestionIcon()
                );

                if (choice == Messages.YES) {
                    WriteCommandAction.runWriteCommandAction(project, () -> 
                            document.replaceString(start, end, convertedText));
                }
                break;

            case COPY_TO_CLIPBOARD:
                // 复制到剪贴板
                java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(convertedText), null);
                NotificationUtil.showInfoNotification(project, "复制成功", 
                        "已将 '" + convertedText + "' 复制到剪贴板");
                break;
        }
    }

    /**
     * 智能转换功能
     * 针对较长的中文内容进行更激进的精简，提取核心关键词
     */
    private String smartConvert(String chineseText, Project project) {
        // 特别长的文本可能是一段描述，需要先提取关键词
        if (chineseText.length() > 15) {
            // 按标点分割文本
            String[] segments = chineseText.split("[,，.。;；!！?？\\s]+");
            if (segments.length > 1) {
                // 如果有多个分段，只取第一个或最短的有意义分段
                String shortestSegment = segments[0];
                for (String segment : segments) {
                    if (segment.length() >= 2 && segment.length() < shortestSegment.length()) {
                        shortestSegment = segment;
                    }
                }
                // 使用提取的关键片段
                chineseText = shortestSegment;
            }

            // 移除常见修饰词
            String[] modifiers = {"该", "这个", "那个", "我们", "他们", "您的", "我的", "进行", "实现", "完成", "处理"};
            for (String modifier : modifiers) {
                chineseText = chineseText.replace(modifier, "");
            }
        }

        // 调用标准转换处理
        return TranslationConverter.convertToLowerCamelCase(chineseText, project);
    }

    /**
     * 显示通知消息
     */
    private void showStatusBarMessage(Project project, @NlsContexts.StatusBarText String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("PandaCoder.Notifications")
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project);
    }

    /**
     * 转换选项枚举
     */
    private enum ConversionOption {
        REPLACE_DIRECTLY("直接替换", null),
        SHOW_PREVIEW("预览并确认", null),
        COPY_TO_CLIPBOARD("复制到剪贴板", null),
        SMART_CONVERT("智能精简转换", null); // 新增智能转换选项

        private final String displayName;
        private final Icon icon;

        ConversionOption(String displayName, Icon icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Icon getIcon() {
            return icon;
        }
    }
}
