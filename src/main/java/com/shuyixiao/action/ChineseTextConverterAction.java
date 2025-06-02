package com.shuyixiao.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;

/**
 * 中文文本转换基础类
 * 为各种中文转换功能提供基础支持
 */
public abstract class ChineseTextConverterAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // 只在编辑器中有选中文本时启用该动作
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            e.getPresentation().setEnabledAndVisible(selectionModel.hasSelection());
        } else {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    /**
     * 获取选中的文本
     *
     * @param e 动作事件
     * @return 选中的文本，如果没有选中文本返回null
     */
    protected String getSelectedText(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            return null;
        }

        return selectedText;
    }

    /**
     * 在状态栏显示消息
     *
     * @param project 当前项目
     * @param message 要显示的消息
     */
    protected void showStatusBarMessage(Project project, String message) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.setInfo(message);
        }
    }

    /**
     * 检查是否可以执行转换
     *
     * @param e 动作事件
     * @return 如果可以执行转换返回true，否则返回false
     */
    protected boolean canPerformConversion(AnActionEvent e) {
        if (e.getProject() == null) {
            return false;
        }

        if (!com.shuyixiao.util.TranslationUtil.checkApiConfiguration()) {
            return false; // API未配置，无法继续
        }

        return getSelectedText(e) != null;
    }
}
