package com.shuyixiao.toolwindow.panels;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.shuyixiao.ui.QRCodeDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 推广面板 - 可折叠的作者信息和推广内容
 * 优雅地展示公众号、社交链接等商业化内容
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class PromotionPanel extends JBPanel<PromotionPanel> {
    
    private final Project project;
    private boolean expanded = false;
    private final JBPanel<?> contentPanel;
    private final JBLabel expandIcon;
    private final JButton expandButton;
    
    public PromotionPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1, 0, 0, 0),
            JBUI.Borders.empty(12, 10)
        ));
        setOpaque(false);
        
        // 头部（可点击展开/折叠）
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JBLabel titleLabel = new JBLabel("🌟 跟随作者成长");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        expandIcon = new JBLabel("▼");
        expandIcon.setForeground(UIUtil.getContextHelpForeground());
        expandIcon.setFont(expandIcon.getFont().deriveFont(10f));
        headerPanel.add(expandIcon, BorderLayout.EAST);
        
        // 点击展开/折叠
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
        });
        
        // 添加一个按钮来展开
        expandButton = new JButton("点击展开");
        expandButton.putClientProperty("JButton.buttonType", "borderless");
        expandButton.setFont(expandButton.getFont().deriveFont(10f));
        expandButton.addActionListener(e -> toggleExpanded());
        expandButton.setVisible(false); // 默认隐藏，用图标就够了
        
        add(headerPanel, BorderLayout.NORTH);
        
        // 内容面板（默认折叠）
        contentPanel = createContentPanel();
        contentPanel.setVisible(false);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * 切换展开/折叠状态
     */
    private void toggleExpanded() {
        expanded = !expanded;
        contentPanel.setVisible(expanded);
        expandIcon.setText(expanded ? "▲" : "▼");
        revalidate();
        repaint();
    }
    
    /**
     * 创建内容面板
     */
    private JBPanel<?> createContentPanel() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.emptyTop(12));
        panel.setOpaque(false);
        
        // 作者信息
        JBPanel<?> authorPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        authorPanel.setOpaque(false);
        authorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JBLabel authorLabel = new JBLabel(
            "<html>" +
            "作者：<b>@舒一笑不秃头</b><br/>" +
            "<span style='color: #888; font-size: 10px;'>专注于架构与技术分享</span>" +
            "</html>"
        );
        authorLabel.setFont(authorLabel.getFont().deriveFont(11f));
        authorPanel.add(authorLabel);
        
        panel.add(authorPanel);
        panel.add(Box.createVerticalStrut(12));
        
        // 公众号按钮
        JButton wechatButton = new JButton("📱 关注公众号");
        wechatButton.putClientProperty("JButton.buttonType", "borderless");
        wechatButton.setFont(wechatButton.getFont().deriveFont(12f));
        wechatButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        wechatButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wechatButton.addActionListener(e -> {
            QRCodeDialog.showWechatQRCode(project);
        });
        panel.add(wechatButton);
        
        panel.add(Box.createVerticalStrut(10));
        
        // 社交链接
        JBPanel<?> linksPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linksPanel.setOpaque(false);
        linksPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        linksPanel.add(createLinkLabel("🐙 GitHub", 
            "https://github.com/shuyixiao-better/PandaCoder"));
        linksPanel.add(new JBLabel("|"));
        linksPanel.add(createLinkLabel("📝 博客", 
            "https://www.shuyixiao.cn"));
        linksPanel.add(new JBLabel("|"));
        linksPanel.add(createLinkLabel("🏢 TorchV", 
            "https://torchv.com/"));
        
        panel.add(linksPanel);
        
        panel.add(Box.createVerticalStrut(12));
        
        // 分隔线
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        
        panel.add(Box.createVerticalStrut(12));
        
        // 未来商业化预留区域
        JBPanel<?> futurePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        futurePanel.setOpaque(false);
        futurePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JBLabel futureLabel = new JBLabel(
            "<html>" +
            "<span style='color: #888; font-size: 10px;'>💡 更多高级功能开发中...</span>" +
            "</html>"
        );
        futurePanel.add(futureLabel);
        
        panel.add(futurePanel);
        
        return panel;
    }
    
    /**
     * 创建链接标签
     */
    private JComponent createLinkLabel(String text, String url) {
        JBLabel label = new JBLabel(text);
        label.setForeground(new Color(30, 144, 255)); // 蓝色
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setFont(label.getFont().deriveFont(10f));
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUrl(url);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(new Color(0, 100, 200)); // 深蓝色
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(new Color(30, 144, 255)); // 恢复蓝色
            }
        });
        
        return label;
    }
    
    /**
     * 打开 URL
     */
    private void openUrl(String url) {
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

