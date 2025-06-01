package com.shuyixiao;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;

import java.io.UnsupportedEncodingException;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName ConvertToCamelCaseAction.java
 * author 舒一笑 yixiaoshu88@163.com
 * version 1.0.0
 * Description 中文转小驼峰
 * createTime 2024年08月21日 23:31:00
 */
public class ConvertToCamelCaseAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取当前编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            return;
        }

        // 将选中的中文文本翻译为英文
        String translatedText = null;
        try {
            translatedText = BaiduAPI.translate(selectedText);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        // 将英文文本转换为驼峰命名
        String camelCaseText = toCamelCase(translatedText);

        // 替换选中文本为转换后的文本
        Document document = editor.getDocument();
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        // 开始写入前加入操作队列，这样可以撤销（Ctrl+Z）
        Runnable runnable = () -> document.replaceString(start, end, camelCaseText);
        WriteCommandAction.runWriteCommandAction(e.getProject(), runnable);

        // 显示一个消息框提示转换成功
        Messages.showMessageDialog("中文：" + selectedText + " 翻译为: " + camelCaseText,
                "Conversion Successful", Messages.getInformationIcon());
    }

    private String toCamelCase(String text) {
        String[] words = text.split(" ");
        StringBuilder camelCaseText = new StringBuilder(words[0].toLowerCase());

        for (int i = 1; i < words.length; i++) {
            camelCaseText.append(capitalize(words[i]));
        }

        return camelCaseText.toString();
    }

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
}
