package com.shuyixiao.livingdoc.ui;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.livingdoc.analyzer.model.ApiEndpoint;
import com.shuyixiao.livingdoc.search.SimpleSearchService;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.List;

/**
 * 活文档工具窗口面板
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class LivingDocToolWindowPanel extends JBPanel {
    private final Project project;
    private final JEditorPane resultsPane;

    public LivingDocToolWindowPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));

        // 结果显示区
        resultsPane = new JEditorPane();
        resultsPane.setContentType("text/html");
        resultsPane.setEditable(false);
        resultsPane.setText("<html><body><h2>活文档搜索</h2><p>使用 <b>Tools -> 活文档 -> 搜索文档</b> 或按 <b>Ctrl+Alt+Shift+S</b> 进行搜索。</p></body></html>");
        
        // 添加超链接监听器（用于跳转到源代码）
        resultsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if (desc.startsWith("file://")) {
                    String[] parts = desc.substring(7).split(":");
                    if (parts.length == 2) {
                        String filePath = parts[0];
                        int lineNumber = Integer.parseInt(parts[1]) - 1; // 转换为 0-based
                        openFileAtLine(filePath, lineNumber);
                    }
                }
            }
        });
        
        add(new JBScrollPane(resultsPane), BorderLayout.CENTER);
        
        // 说明面板
        JPanel infoPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JBLabel("💡 提示: 使用 Tools -> 活文档 -> 索引项目 生成文档索引"));
        add(infoPanel, BorderLayout.SOUTH);
    }

    public JComponent getContent() {
        return this;
    }
    
    /**
     * 显示搜索结果
     */
    public void displaySearchResults(String query, List<SimpleSearchService.SearchResultItem> results) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 10px; }")
            .append("h2 { color: #2196F3; }")
            .append(".result { border: 1px solid #ddd; padding: 10px; margin: 10px 0; border-radius: 5px; }")
            .append(".method { display: inline-block; padding: 2px 8px; border-radius: 3px; font-weight: bold; color: white; }")
            .append(".get { background-color: #4CAF50; }")
            .append(".post { background-color: #2196F3; }")
            .append(".put { background-color: #FF9800; }")
            .append(".delete { background-color: #F44336; }")
            .append(".url { font-size: 14px; color: #333; font-weight: bold; }")
            .append(".desc { color: #666; margin-top: 5px; }")
            .append(".info { color: #999; font-size: 12px; margin-top: 5px; }")
            .append(".link { color: #2196F3; text-decoration: none; }")
            .append(".score { float: right; color: #4CAF50; font-weight: bold; }")
            .append("</style></head><body>");
        
        html.append("<h2>搜索结果: \"").append(escapeHtml(query)).append("\"</h2>");
        html.append("<p>找到 ").append(results.size()).append(" 个匹配的接口</p>");
        
        for (int i = 0; i < results.size(); i++) {
            SimpleSearchService.SearchResultItem item = results.get(i);
            ApiEndpoint endpoint = item.getEndpoint();
            
            html.append("<div class='result'>");
            html.append("<span class='score'>分数: ").append(String.format("%.1f", item.getScore())).append("</span>");
            html.append("<span class='method ").append(endpoint.getMethod().toLowerCase()).append("'>")
                .append(endpoint.getMethod()).append("</span> ");
            html.append("<span class='url'>").append(escapeHtml(endpoint.getUrl())).append("</span>");
            
            if (endpoint.getDescription() != null && !endpoint.getDescription().isEmpty()) {
                html.append("<div class='desc'>📝 ").append(escapeHtml(endpoint.getDescription())).append("</div>");
            }
            
            html.append("<div class='info'>");
            html.append("📦 ").append(escapeHtml(endpoint.getClassName())).append(".").append(endpoint.getMethodName()).append("() | ");
            html.append("匹配字段: ").append(item.getMatchedField()).append(" | ");
            html.append("<a class='link' href='file://").append(endpoint.getFilePath()).append(":")
                .append(endpoint.getLineNumber()).append("'>跳转到源代码</a>");
            html.append("</div>");
            
            html.append("</div>");
        }
        
        html.append("</body></html>");
        resultsPane.setText(html.toString());
        resultsPane.setCaretPosition(0); // 滚动到顶部
    }
    
    /**
     * 打开文件并跳转到指定行
     */
    private void openFileAtLine(String filePath, int lineNumber) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null) {
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, lineNumber, 0);
            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
        }
    }
    
    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
