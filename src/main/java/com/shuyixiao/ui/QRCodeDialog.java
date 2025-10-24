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
        mainPanel.setPreferredSize(JBUI.size(450, 600));
        mainPanel.setBorder(JBUI.Borders.empty(15));

        // 二维码图片
        JBPanel<?> imagePanel = new JBPanel<>(new BorderLayout());
        imagePanel.setBorder(JBUI.Borders.empty(5));

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
                System.out.println("开始加载本地资源，路径: " + qrCodePath);
                java.net.URL imageUrl = null;
                
                // 方式1：使用 QRCodeDialog 类加载器加载（带 / 开头）
                imageUrl = QRCodeDialog.class.getResource(qrCodePath);
                System.out.println("方式1 (QRCodeDialog.class.getResource 带/): " + imageUrl);
                
                // 方式2：如果方式1失败，去掉开头的 / 再用 ClassLoader 试
                if (imageUrl == null && qrCodePath.startsWith("/")) {
                    String pathWithoutSlash = qrCodePath.substring(1);
                    imageUrl = QRCodeDialog.class.getClassLoader().getResource(pathWithoutSlash);
                    System.out.println("方式2 (ClassLoader 去掉/) " + pathWithoutSlash + ": " + imageUrl);
                }
                
                // 方式3：如果还是失败，尝试使用线程上下文类加载器
                if (imageUrl == null) {
                    String pathWithoutSlash = qrCodePath.startsWith("/") ? qrCodePath.substring(1) : qrCodePath;
                    imageUrl = Thread.currentThread().getContextClassLoader().getResource(pathWithoutSlash);
                    System.out.println("方式3 (Thread.currentThread().getContextClassLoader) " + pathWithoutSlash + ": " + imageUrl);
                }
                
                // 方式4：直接尝试完整路径
                if (imageUrl == null) {
                    String fileName = qrCodePath.substring(qrCodePath.lastIndexOf("/") + 1);
                    imageUrl = QRCodeDialog.class.getClassLoader().getResource("images/" + fileName);
                    System.out.println("方式4 (完整路径) images/" + fileName + ": " + imageUrl);
                }
                
                if (imageUrl != null) {
                    System.out.println("✅ 成功找到资源，URL: " + imageUrl);
                    qrCodeIcon = new ImageIcon(imageUrl);
                } else {
                    System.err.println("❌ 所有方式都无法找到本地图片资源: " + qrCodePath);
                    showErrorPlaceholder(imagePanel, "图片文件未找到", qrCodePath);
                    qrCodeIcon = null;
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
                        
                        // 如果图片太大，等比例缩放（最大边长400）
                        int maxWidth = 400;
                        int maxHeight = 400;
                        if (originalWidth > maxWidth || originalHeight > maxHeight) {
                            double scale = Math.min(maxWidth / (double) originalWidth, maxHeight / (double) originalHeight);
                            int newWidth = (int) Math.round(originalWidth * scale);
                            int newHeight = (int) Math.round(originalHeight * scale);
                            
                            // 创建一个新的ImageIcon来显示缩放后的gif
                            Image img = qrCodeIcon.getImage();
                            Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT);
                            qrCodeIcon = new ImageIcon(scaledImg);
                            
                            System.out.println("缩放后尺寸: " + newWidth + "x" + newHeight);
                        }
                    } else {
                        // 非gif文件，等比缩放，保持清晰不变形（最大边长400，不放大小图）
                        int originalWidth = qrCodeIcon.getIconWidth();
                        int originalHeight = qrCodeIcon.getIconHeight();
                        int maxWidth = 400;
                        int maxHeight = 400;
                        if (originalWidth > maxWidth || originalHeight > maxHeight) {
                            double scale = Math.min(maxWidth / (double) originalWidth, maxHeight / (double) originalHeight);
                            int newWidth = (int) Math.round(originalWidth * scale);
                            int newHeight = (int) Math.round(originalHeight * scale);
                            Image img = qrCodeIcon.getImage();
                            Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                            qrCodeIcon = new ImageIcon(scaledImg);
                        }
                    }
 
                     JBLabel qrCodeLabel = new JBLabel(qrCodeIcon);
                     qrCodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                     qrCodeLabel.setBorder(JBUI.Borders.empty(5));
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
     * 显示打赏二维码对话框
     * 显示微信收款和支付宝收款二维码
     */
    public static void showCoffeeQRCode(@Nullable Project project) {
        // 创建自定义对话框显示两个收款码
        JDialog dialog = new JDialog((java.awt.Frame) null, "☕ 支持作者", true);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);
        
        // 主面板 - 使用更紧凑的布局
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(15));
        mainPanel.setPreferredSize(JBUI.size(480, 420));
        
        // 标题
        JBLabel titleLabel = new JBLabel("☕ 支持作者开发");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(JBUI.Borders.emptyBottom(10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 内容面板 - 使用 GridLayout 确保对齐
        JBPanel<?> contentPanel = new JBPanel<>(new GridLayout(1, 2, 15, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(JBUI.Borders.empty(5));
        
        // 微信收款码
        JBPanel<?> wechatPanel = createCompactPaymentPanel(
            "💚 微信收款",
            "/images/微信收款.jpg",
            "微信扫码支持"
        );
        contentPanel.add(wechatPanel);
        
        // 支付宝收款码
        JBPanel<?> alipayPanel = createCompactPaymentPanel(
            "💙 支付宝收款",
            "/images/支付宝收款.jpg",
            "支付宝扫码支持"
        );
        contentPanel.add(alipayPanel);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // 说明文字 - 完美居中对齐
        JBPanel<?> descPanel = new JBPanel<>(new BorderLayout());
        descPanel.setOpaque(false);
        descPanel.setBorder(JBUI.Borders.emptyTop(8));
        
        JBLabel descLabel = new JBLabel(
            "<html>" +
            "<div style='text-align: center;'>" +
            "感谢您对 PandaCoder 插件的支持！<br/>" +
            "<span style='color: #888; font-size: 10px;'>" +
            "您的支持是我持续改进的动力 💪 支持金额不限，心意最重要 ❤️" +
            "</span>" +
            "</div>" +
            "</html>"
        );
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 使用 BorderLayout 确保完美居中
        descPanel.add(descLabel, BorderLayout.CENTER);
        mainPanel.add(descPanel, BorderLayout.SOUTH);
        
        // 按钮面板 - 更紧凑
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 0, 5));
        buttonPanel.setBorder(JBUI.Borders.emptyTop(5));
        
        JButton closeButton = new JButton("关闭");
        closeButton.setPreferredSize(JBUI.size(80, 30));
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        
        // 组装对话框
        JBPanel<?> dialogContent = new JBPanel<>(new BorderLayout());
        dialogContent.add(mainPanel, BorderLayout.CENTER);
        dialogContent.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(dialogContent);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
    
    /**
     * 创建紧凑型收款码面板
     */
    private static JBPanel<?> createCompactPaymentPanel(String title, String imagePath, String description) {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(8));
        panel.setOpaque(false);
        
        // 标题
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(JBUI.Borders.emptyBottom(8));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 图片区域
        JBPanel<?> imagePanel = new JBPanel<>(new BorderLayout());
        imagePanel.setOpaque(false);
        
        try {
            java.net.URL imageUrl = QRCodeDialog.class.getResource(imagePath);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                
                // 缩放图片到合适大小（最大160x160，更紧凑）
                int maxSize = 160;
                int originalWidth = icon.getIconWidth();
                int originalHeight = icon.getIconHeight();
                
                if (originalWidth > maxSize || originalHeight > maxSize) {
                    double scale = Math.min(maxSize / (double) originalWidth, maxSize / (double) originalHeight);
                    int newWidth = (int) Math.round(originalWidth * scale);
                    int newHeight = (int) Math.round(originalHeight * scale);
                    
                    Image img = icon.getImage();
                    Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaledImg);
                }
                
                JBLabel imageLabel = new JBLabel(icon);
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imagePanel.add(imageLabel, BorderLayout.CENTER);
            } else {
                JBLabel errorLabel = new JBLabel("❌ 图片加载失败");
                errorLabel.setForeground(UIUtil.getContextHelpForeground());
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imagePanel.add(errorLabel, BorderLayout.CENTER);
            }
        } catch (Exception e) {
            JBLabel errorLabel = new JBLabel("❌ 图片加载异常");
            errorLabel.setForeground(UIUtil.getContextHelpForeground());
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imagePanel.add(errorLabel, BorderLayout.CENTER);
        }
        
        panel.add(imagePanel, BorderLayout.CENTER);
        
        // 描述 - 更紧凑
        JBLabel descLabel = new JBLabel(description);
        descLabel.setFont(descLabel.getFont().deriveFont(9f));
        descLabel.setForeground(UIUtil.getContextHelpForeground());
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setBorder(JBUI.Borders.emptyTop(5));
        panel.add(descLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建单个收款码面板（保留原方法作为备用）
     */
    private static JBPanel<?> createPaymentPanel(String title, String imagePath, String description) {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 标题
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        // 图片
        try {
            java.net.URL imageUrl = QRCodeDialog.class.getResource(imagePath);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                
                // 缩放图片到合适大小（最大200x200）
                int maxSize = 200;
                int originalWidth = icon.getIconWidth();
                int originalHeight = icon.getIconHeight();
                
                if (originalWidth > maxSize || originalHeight > maxSize) {
                    double scale = Math.min(maxSize / (double) originalWidth, maxSize / (double) originalHeight);
                    int newWidth = (int) Math.round(originalWidth * scale);
                    int newHeight = (int) Math.round(originalHeight * scale);
                    
                    Image img = icon.getImage();
                    Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaledImg);
                }
                
                JBLabel imageLabel = new JBLabel(icon);
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(imageLabel);
            } else {
                JBLabel errorLabel = new JBLabel("❌ 图片加载失败");
                errorLabel.setForeground(UIUtil.getContextHelpForeground());
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(errorLabel);
            }
        } catch (Exception e) {
            JBLabel errorLabel = new JBLabel("❌ 图片加载异常");
            errorLabel.setForeground(UIUtil.getContextHelpForeground());
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(errorLabel);
        }
        
        panel.add(Box.createVerticalStrut(8));
        
        // 描述
        JBLabel descLabel = new JBLabel(description);
        descLabel.setFont(descLabel.getFont().deriveFont(10f));
        descLabel.setForeground(UIUtil.getContextHelpForeground());
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(descLabel);
        
        return panel;
    }

    /**
     * 显示微信公众号二维码对话框
     * 使用本地资源图片，保留网络链接用于复制
     */
    public static void showWechatQRCode(@Nullable Project project) {
        QRCodeDialog dialog = new QRCodeDialog(
                project,
                "公众号",
                "扫描二维码关注「舒一笑的架构笔记」<br>" +
                        "获取最新技术分享、插件更新和问题解答",
                "/images/WechatOfficialAccount.gif",  // 使用本地图片资源
                "复制链接",
                "https://i-blog.csdnimg.cn/direct/68693f613c2a4e2cb0ff042fbadc2a9c.gif#pic_center"  // 保留网络链接用于复制
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
}