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
            ImageIcon qrCodeIcon = null;
            System.out.println("尝试加载图片路径: " + qrCodePath);
            
            // 判断是网络URL还是本地资源
            if (qrCodePath.startsWith("http://") || qrCodePath.startsWith("https://")) {
                // 网络图片加载
                System.out.println("检测到网络URL，开始下载图片...");
                try {
                    java.net.URL imageUrl = new java.net.URL(qrCodePath);
                    qrCodeIcon = new ImageIcon(imageUrl);
                    
                    // 等待网络图片加载完成
                    System.out.println("等待网络图片加载...");
                    Thread.sleep(1000); // 给一点时间让图片加载
                    
                } catch (Exception e) {
                    System.out.println("网络图片加载失败: " + e.getMessage());
                    showErrorPlaceholder(imagePanel, "网络图片加载失败", qrCodePath);
                    qrCodeIcon = null; // 设置为null，后面会处理
                }
            } else {
                // 本地资源文件加载
                java.net.URL imageUrl = null;
                
                // 方式1：使用当前类加载器
                imageUrl = getClass().getResource(qrCodePath);
                
                // 方式2：如果方式1失败，尝试使用ClassLoader
                if (imageUrl == null) {
                    imageUrl = getClass().getClassLoader().getResource(qrCodePath.substring(1)); // 去掉开头的 /
                }
                
                // 方式3：如果还是失败，尝试完整路径
                if (imageUrl == null) {
                    imageUrl = getClass().getClassLoader().getResource("images/WechatOfficialAccount.gif");
                }
                
                System.out.println("本地资源URL: " + imageUrl);
                
                if (imageUrl != null) {
                    qrCodeIcon = new ImageIcon(imageUrl);
                } else {
                    System.out.println("无法找到本地图片资源: " + qrCodePath);
                    showErrorPlaceholder(imagePanel, "图片文件未找到", qrCodePath);
                    qrCodeIcon = null; // 设置为null，后面会处理
                }
            }
            
            // 处理加载好的图片
            if (qrCodeIcon != null) {
                System.out.println("图片加载状态: " + qrCodeIcon.getImageLoadStatus());
                System.out.println("原始图片尺寸: " + qrCodeIcon.getIconWidth() + "x" + qrCodeIcon.getIconHeight());
                
                // 检查图片是否有效
                if (qrCodeIcon.getIconWidth() > 0 && qrCodeIcon.getIconHeight() > 0) {
                    
                    // 检查是否为gif文件
                    if (qrCodePath.toLowerCase().endsWith(".gif")) {
                        // 对于gif文件，设置合适的显示尺寸但保持动画
                        int originalWidth = qrCodeIcon.getIconWidth();
                        int originalHeight = qrCodeIcon.getIconHeight();
                        
                        // 如果图片太大，等比例缩放
                        if (originalWidth > 250 || originalHeight > 250) {
                            double scale = Math.min(250.0 / originalWidth, 250.0 / originalHeight);
                            int newWidth = (int) (originalWidth * scale);
                            int newHeight = (int) (originalHeight * scale);
                            
                            // 创建一个新的ImageIcon来显示缩放后的gif
                            Image img = qrCodeIcon.getImage();
                            Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT);
                            qrCodeIcon = new ImageIcon(scaledImg);
                            
                            System.out.println("缩放后尺寸: " + newWidth + "x" + newHeight);
                        }
                    } else {
                        // 非gif文件，按原来方式处理
                        Image img = qrCodeIcon.getImage();
                        Image scaledImg = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                        qrCodeIcon = new ImageIcon(scaledImg);
                    }

                    JBLabel qrCodeLabel = new JBLabel(qrCodeIcon);
                    qrCodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    qrCodeLabel.setBorder(JBUI.Borders.empty(10));
                    imagePanel.add(qrCodeLabel, BorderLayout.CENTER);
                    
                    System.out.println("图片显示成功!");
                } else {
                    System.out.println("图片尺寸无效: " + qrCodeIcon.getIconWidth() + "x" + qrCodeIcon.getIconHeight());
                    showErrorPlaceholder(imagePanel, "图片尺寸无效", qrCodePath);
                }
            } else {
                System.out.println("qrCodeIcon为null");
                showErrorPlaceholder(imagePanel, "图片对象创建失败", qrCodePath);
            }
            
        } catch (Exception e) {
            System.out.println("图片加载异常: " + e.getMessage());
            e.printStackTrace();
            showErrorPlaceholder(imagePanel, "图片加载失败: " + e.getMessage(), qrCodePath);
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
     * 显示错误占位符
     */
    private void showErrorPlaceholder(JBPanel<?> imagePanel, String errorMessage, String path) {
        JBLabel placeholderLabel = new JBLabel("<html><div style='text-align: center'>" +
                "📱<br><br>" +
                "<small>" + errorMessage + "<br>" + path + "</small>" +
                "</div></html>");
        placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(Font.BOLD, 24f));
        placeholderLabel.setForeground(UIUtil.getContextHelpForeground());
        placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        placeholderLabel.setBorder(JBUI.Borders.empty(50));
        imagePanel.add(placeholderLabel, BorderLayout.CENTER);
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
                "https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/CSDN%E6%8E%A8%E5%B9%BF.gif",
                "复制链接",
                "https://i-blog.csdnimg.cn/direct/68693f613c2a4e2cb0ff042fbadc2a9c.gif#pic_center" // 替换为实际的公众号链接
        );
        dialog.show();
    }

    public static void showNotification(@Nullable Project project) {
        QRCodeDialog dialog = new QRCodeDialog(
                project,
                "📱 如有问题或建议，添加微信：Tobeabetterman1001",
                "或者扫描二维码<br>" +
                        "备注来意-PandaCoder问题交流",
                "https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/%E4%B8%80%E7%AC%91%E6%8A%80%E6%9C%AF%E4%BA%A4%E6%B5%81%E7%BE%A4/%E5%BE%AE%E4%BF%A1%E4%BA%8C%E7%BB%B4%E7%A0%81%E5%8A%A0%E6%88%91.jpg",
                "复制链接",
                "https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/%E4%B8%80%E7%AC%91%E6%8A%80%E6%9C%AF%E4%BA%A4%E6%B5%81%E7%BE%A4/%E5%BE%AE%E4%BF%A1%E4%BA%8C%E7%BB%B4%E7%A0%81%E5%8A%A0%E6%88%91.jpg" // 替换为实际的公众号链接
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