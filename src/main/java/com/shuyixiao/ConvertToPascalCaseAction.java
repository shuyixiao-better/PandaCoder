package com.shuyixiao;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
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
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.shuyixiao.converter.TranslationConverter;
import com.shuyixiao.notification.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Copyright © 2025年 integration-projects-maven. All rights reserved.
 * ClassName ConvertToPascalCaseAction.java
 * author 舒一笑不秃头 yixiaoshu88@163.com
 * version 1.0.0
 * Description 中文转大驼峰
 * createTime 2025年06月02日 16:00:00
 */
public class ConvertToPascalCaseAction extends AnAction {

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
            // 普通转换 - 使用大驼峰转换
            convertedText = convertToUpperCamelCase(selectedText, project);
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

        // 调用大驼峰转换处理
        return convertToUpperCamelCase(chineseText, project);
    }

    /**
     * 将中文文本转换为大驼峰命名
     */
    private String convertToUpperCamelCase(String chineseText, Project project) {
        // 智能处理中文文本
        String processedText = preprocessChineseText(chineseText);

        // 进行翻译 - 使用统一翻译引擎
        String translatedText;
        try {
            translatedText = com.shuyixiao.converter.TranslationConverter.translateText(processedText);
            if (translatedText == null || translatedText.trim().isEmpty()) {
                Messages.showErrorDialog(project, "翻译结果为空，无法进行转换。请检查您的API配置是否正确。", "翻译失败");
                return null;
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "翻译过程中发生错误: " + ex.getMessage(), "翻译错误");
            return null;
        }

        // 转换为大驼峰命名 (PascalCase)
        return toPascalCase(translatedText);
    }

    /**
     * 预处理中文文本，去除无用词
     */
    private String preprocessChineseText(String chineseText) {
        // 移除标点符号和特殊字符
        String cleanText = chineseText.replaceAll("[\\pP\\p{Punct}]+", " ").trim();

        // 移除常见的无意义词
        String[] stopWords = {"的", "了", "和", "与", "或", "及", "等", "对于", "一个", "这个", "那个"};
        for (String stopWord : stopWords) {
            cleanText = cleanText.replace(stopWord, " ");
        }

        return cleanText;
    }

    /**
     * 将英文文本转换为大驼峰命名格式 (PascalCase)
     */
    private String toPascalCase(String text) {
        // 清理文本，去除多余的空格和标点
        text = text.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        // 分割单词
        String[] words = text.split("\\s+");
        StringBuilder pascalCaseText = new StringBuilder();

        // 过滤常见的无用英文单词
        String[] stopWords = {"the", "a", "an", "of", "for", "to", "in", "on", "at", "with", "by", "as"};

        // 从所有单词中筛选出重要单词
        java.util.List<String> importantWords = new java.util.ArrayList<>();
        for (String word : words) {
            if (word.isEmpty()) continue;
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (word.equalsIgnoreCase(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            if (!isStopWord) {
                importantWords.add(word);
            }
        }

        // 如果提取出的重要单词为空，使用原始单词
        if (importantWords.isEmpty() && words.length > 0) {
            importantWords.add(words[0]);
        }

        // 生成大驼峰命名 (PascalCase)
        for (String word : importantWords) {
            if (!word.isEmpty()) {
                pascalCaseText.append(word.substring(0, 1).toUpperCase())
                               .append(word.substring(1).toLowerCase());
            }
        }

        return pascalCaseText.toString();
    }

    /**
     * 在状态栏显示消息
     */
    private void showStatusBarMessage(Project project, @NlsContexts.StatusBarText String message) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.setInfo(message);
        }
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
