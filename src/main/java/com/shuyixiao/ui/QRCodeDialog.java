package com.shuyixiao.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;

/**
 * 二维码显示对话框
 * 用于展示微信公众号二维码等信息
 */
public class QRCodeDialog extends DialogWrapper {

    private final String qrCodePath;
    private final String title;
    private final String description;
    private final String actionText;
    private final String actionUrl;

    public QRCodeDialog(@Nullable Project project, String title, String description,
                        String qrCodePath, String actionText, String actionUrl) {
        super(project);
        this.title = title;
        this.description = description;
        this.qrCodePath = qrCodePath;
        this.actionText = actionText;
        this.actionUrl = actionUrl;
        setTitle(title);
        setResizable(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(400, 500));
        mainPanel.setBorder(JBUI.Borders.empty(20));

        // 标题
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(JBUI.Borders.emptyBottom(15));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 二维码图片
        JBPanel<?> imagePanel = new JBPanel<>(new BorderLayout());
        imagePanel.setBorder(JBUI.Borders.empty(10));

        try {
            // 加载二维码图片
            ImageIcon qrCodeIcon = new ImageIcon(getClass().getResource(qrCodePath));
            if (qrCodeIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                // 调整图片大小
                Image img = qrCodeIcon.getImage();
                Image scaledImg = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                qrCodeIcon = new ImageIcon(scaledImg);

                JBLabel qrCodeLabel = new JBLabel(qrCodeIcon);
                qrCodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                qrCodeLabel.setBorder(JBUI.Borders.empty(10));
                imagePanel.add(qrCodeLabel, BorderLayout.CENTER);
            } else {
                // 如果图片加载失败，显示占位符
                JBLabel placeholderLabel = new JBLabel("📱 https://i-blog.csdnimg.cn/direct/68693f613c2a4e2cb0ff042fbadc2a9c.gif#pic_center");
                placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(Font.BOLD, 48f));
                placeholderLabel.setForeground(UIUtil.getContextHelpForeground());
                placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
                placeholderLabel.setBorder(JBUI.Borders.empty(50));
                imagePanel.add(placeholderLabel, BorderLayout.CENTER);
            }
        } catch (Exception e) {
            // 异常处理，显示占位符
            JBLabel placeholderLabel = new JBLabel("📱 https://i-blog.csdnimg.cn/direct/68693f613c2a4e2cb0ff042fbadc2a9c.gif#pic_center");
            placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(Font.BOLD, 48f));
            placeholderLabel.setForeground(UIUtil.getContextHelpForeground());
            placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
            placeholderLabel.setBorder(JBUI.Borders.empty(50));
            imagePanel.add(placeholderLabel, BorderLayout.CENTER);
        }

        mainPanel.add(imagePanel, BorderLayout.CENTER);

        // 描述文本
        JBLabel descLabel = new JBLabel("<html><body style='width: 350px; text-align: center'>" +
                description + "</body></html>");
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setBorder(JBUI.Borders.emptyTop(15));
        mainPanel.add(descLabel, BorderLayout.SOUTH);

        return mainPanel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{
                new AbstractAction("复制链接") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        copyToClipboard();
                    }
                },
                new AbstractAction("打开链接") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        openUrl();
                    }
                },
                new AbstractAction("关闭") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doCancelAction();
                    }
                }
        };
    }

    /**
     * 复制链接到剪贴板
     */
    private void copyToClipboard() {
        if (actionUrl != null && !actionUrl.isEmpty()) {
            java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(actionUrl), null);

            // 显示成功提示
            JOptionPane.showMessageDialog(
                    getContentPanel(),
                    "链接已复制到剪贴板！",
                    "复制成功",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    /**
     * 打开链接
     */
    private void openUrl() {
        if (actionUrl != null && !actionUrl.isEmpty()) {
            try {
                Desktop.getDesktop().browse(URI.create(actionUrl));
            } catch (IOException | UnsupportedOperationException e) {
                // 如果无法打开浏览器，显示链接
                JOptionPane.showMessageDialog(
                        getContentPanel(),
                        "无法自动打开链接，请手动访问：\n" + actionUrl,
                        "打开链接",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }

    /**
     * 显示微信公众号二维码对话框
     */
    public static void showWechatQRCode(@Nullable Project project) {
        QRCodeDialog dialog = new QRCodeDialog(
                project,
                "📱 关注微信公众号",
                "扫描二维码关注「舒一笑的架构笔记」<br>" +
                        "获取最新技术分享、插件更新和问题解答",
                "/images/WechatOfficialAccount.gif",
                "复制链接",
                "https://i-blog.csdnimg.cn/direct/68693f613c2a4e2cb0ff042fbadc2a9c.gif#pic_center" // 替换为实际的公众号链接
        );
        dialog.show();
    }

    /**
     * 显示GitHub二维码对话框
     */
    public static void showGitHubQRCode(@Nullable Project project) {
        QRCodeDialog dialog = new QRCodeDialog(
                project,
                "🐙 GitHub 项目",
                "扫描二维码访问 PandaCoder 项目<br>" +
                        "获取最新版本、提交问题和贡献代码",
                "/images/GitHubQRCode.png",
                "复制链接",
                "https://github.com/shuyixiao-better/PandaCoder"
        );
        dialog.show();
    }

    /**
     * 显示博客二维码对话框
     */
    public static void showBlogQRCode(@Nullable Project project) {
        QRCodeDialog dialog = new QRCodeDialog(
                project,
                "🌐 个人博客",
                "扫描二维码访问「舒一笑的架构笔记」<br>" +
                        "阅读技术文章和开发心得",
                "/images/BlogQRCode.png",
                "复制链接",
                "https://www.shuyixiao.cloud"
        );
        dialog.show();
    }
}