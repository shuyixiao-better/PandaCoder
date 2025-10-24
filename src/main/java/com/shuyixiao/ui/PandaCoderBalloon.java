package com.shuyixiao.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Desktop;

/**
 * PandaCoder 轻量级气泡提示
 * 替代模态对话框，提供更好的用户体验
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class PandaCoderBalloon {
    
    private static final String VERSION = com.shuyixiao.version.VersionInfo.getVersion();
    
    /**
     * 显示欢迎气泡
     * 
     * @param project 当前项目
     * @param editor 编辑器（可为null）
     */
    public static void showWelcome(Project project, Editor editor) {
        String html = createWelcomeHtml();
        
        Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                html,
                MessageType.INFO,
                createHyperlinkListener(project)
            )
            .setFadeoutTime(7000)  // 7秒自动消失
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setAnimationCycle(200)
            .setCloseButtonEnabled(true)
            .createBalloon();
        
        if (editor != null) {
            // 在编辑器中显示
            balloon.show(
                JBPopupFactory.getInstance().guessBestPopupLocation(editor),
                Balloon.Position.below
            );
        } else if (project != null) {
            // 在屏幕中央显示
            try {
                JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame != null) {
                    balloon.show(
                        RelativePoint.getCenterOf(frame.getRootPane()),
                        Balloon.Position.above
                    );
                }
            } catch (Exception e) {
                // 如果获取窗口失败，降级到对话框
                WelcomeDialog.show(project);
            }
        }
    }
    
    /**
     * 创建欢迎消息 HTML
     */
    private static String createWelcomeHtml() {
        return "<html>" +
               "<div style='padding: 15px; width: 380px; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Arial, sans-serif;'>" +
               
               // 标题
               "<h2 style='margin: 0 0 10px 0; color: #2C3E50; font-size: 18px;'>" +
               "🐼 PandaCoder v" + VERSION + "</h2>" +
               
               "<p style='margin: 5px 0 15px 0; color: #5A6C7D; font-size: 13px;'>" +
               "中文开发者的智能编码助手" +
               "</p>" +
               
               "<hr style='border: none; border-top: 1px solid #E1E8ED; margin: 15px 0;'/>" +
               
               // 快速功能介绍
               "<div style='margin: 12px 0;'>" +
               "<p style='margin: 5px 0; font-weight: 600; color: #2C3E50; font-size: 13px;'>⚡ 核心功能</p>" +
               "<p style='margin: 3px 0; font-size: 12px; line-height: 1.6; color: #5A6C7D;'>" +
               "• Git 统计分析 | ES/SQL 监控<br/>" +
               "• Jenkins 增强 | Spring Boot 图标<br/>" +
               "• 中文智能转换 | 多引擎翻译" +
               "</p>" +
               "</div>" +
               
               "<hr style='border: none; border-top: 1px solid #E1E8ED; margin: 15px 0;'/>" +
               
               // 操作链接
               "<div style='margin: 15px 0; text-align: center;'>" +
               "<a href='open_toolwindow' style='color: #1DA1F2; text-decoration: none; font-size: 13px; margin: 0 8px;'>" +
               "📂 打开功能面板</a> | " +
               "<a href='show_features' style='color: #1DA1F2; text-decoration: none; font-size: 13px; margin: 0 8px;'>" +
               "✨ 查看所有功能</a>" +
               "</div>" +
               
               "<div style='margin: 10px 0; text-align: center;'>" +
               "<a href='follow_wechat' style='color: #27AE60; text-decoration: none; font-size: 13px; margin: 0 8px;'>" +
               "📱 关注公众号</a> | " +
               "<a href='github' style='color: #9B59B6; text-decoration: none; font-size: 13px; margin: 0 8px;'>" +
               "⭐ GitHub Star</a>" +
               "</div>" +
               
               // 底部提示
               "<div style='margin-top: 15px; padding-top: 12px; border-top: 1px solid #E1E8ED; " +
               "text-align: center; font-size: 11px; color: #95A5A6;'>" +
               "💡 提示：按 <kbd style='padding: 2px 6px; background: #F5F8FA; border: 1px solid #E1E8ED; " +
               "border-radius: 3px; font-family: monospace;'>Alt+P</kbd> 随时打开助手面板" +
               "</div>" +
               
               "</div>" +
               "</html>";
    }
    
    /**
     * 创建超链接监听器
     */
    private static HyperlinkListener createHyperlinkListener(Project project) {
        return e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                
                switch (desc) {
                    case "open_toolwindow":
                        // 打开 Tool Window（阶段二实现后会激活）
                        openPandaCoderToolWindow(project);
                        break;
                        
                    case "show_features":
                        // 显示完整功能对话框
                        WelcomeDialog.show(project);
                        break;
                        
                    case "follow_wechat":
                        // 显示公众号二维码
                        QRCodeDialog.showWechatQRCode(project);
                        break;
                        
                    case "github":
                        // 打开 GitHub 仓库
                        openUrl("https://github.com/shuyixiao-better/PandaCoder");
                        break;
                }
            }
        };
    }
    
    /**
     * 打开 PandaCoder Tool Window
     */
    private static void openPandaCoderToolWindow(Project project) {
        if (project == null) return;
        
        try {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = manager.getToolWindow("PandaCoder");
            
            if (toolWindow != null) {
                toolWindow.activate(null);
            } else {
                // 如果 Tool Window 还未实现，显示完整对话框
                WelcomeDialog.show(project);
            }
        } catch (Exception e) {
            // 降级到完整对话框
            WelcomeDialog.show(project);
        }
    }
    
    /**
     * 显示里程碑气泡（用于智能推广）
     * 
     * @param project 当前项目
     * @param usageCount 使用次数
     */
    public static void showMilestone(Project project, int usageCount) {
        if (project == null) return;
        
        String message = getMilestoneMessage(usageCount);
        if (message == null) return;
        
        Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                message,
                MessageType.INFO,
                createHyperlinkListener(project)
            )
            .setFadeoutTime(5000)  // 5秒自动消失
            .setHideOnClickOutside(true)
            .setCloseButtonEnabled(true)
            .createBalloon();
        
        // 在状态栏右侧显示
        try {
            JFrame frame = WindowManager.getInstance().getFrame(project);
            if (frame != null && frame.getRootPane() != null) {
                // 在窗口右下角显示
                balloon.show(
                    RelativePoint.getSouthEastOf(frame.getRootPane()),
                    Balloon.Position.atRight
                );
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 获取里程碑消息
     */
    private static String getMilestoneMessage(int count) {
        String content;
        
        switch (count) {
            case 10:
                content = "<h3 style='margin: 0 0 8px 0; color: #2C3E50;'>🎉 您已使用 PandaCoder 10 次！</h3>" +
                         "<p style='margin: 0; font-size: 13px; color: #5A6C7D;'>" +
                         "觉得有用？<a href='github' style='color: #1DA1F2;'>给个 Star</a> 支持作者 😊</p>";
                break;
                
            case 50:
                content = "<h3 style='margin: 0 0 8px 0; color: #2C3E50;'>🚀 您已使用 PandaCoder 50 次！</h3>" +
                         "<p style='margin: 0; font-size: 13px; color: #5A6C7D;'>" +
                         "成为资深用户啦！<a href='follow_wechat' style='color: #27AE60;'>关注公众号</a>获取高级技巧</p>";
                break;
                
            case 100:
                content = "<h3 style='margin: 0 0 8px 0; color: #2C3E50;'>💎 您已使用 PandaCoder 100 次！</h3>" +
                         "<p style='margin: 0; font-size: 13px; color: #5A6C7D;'>" +
                         "感谢一路相伴！<a href='follow_wechat' style='color: #27AE60;'>关注公众号</a>第一时间获取新功能</p>";
                break;
                
            default:
                return null;
        }
        
        return "<html><div style='padding: 12px; width: 300px; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Arial, sans-serif;'>" +
               content +
               "</div></html>";
    }
    
    /**
     * 打开URL
     */
    private static void openUrl(String url) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
}

