package com.shuyixiao;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.shuyixiao.ui.PandaCoderBalloon;

/**
 * PandaCoder 助手面板入口
 * 优化后使用轻量级气泡提示，替代模态对话框
 * 提供更好的用户体验，不打断工作流
 * 
 * @author 舒一笑不秃头 yixiaoshu88@163.com
 * @version 2.2.0
 * @since 2024年08月21日
 */
public class ReportMessage extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        // 优先使用轻量级气泡提示
        if (editor != null) {
            // 在编辑器中显示气泡，7秒自动消失
            PandaCoderBalloon.showWelcome(e.getProject(), editor);
        } else {
            // 降级方案：如果没有编辑器（如在项目视图右键），显示气泡或对话框
            PandaCoderBalloon.showWelcome(e.getProject(), null);
        }
    }
}
