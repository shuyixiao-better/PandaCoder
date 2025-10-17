package com.shuyixiao.esdsl.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.esdsl.model.EsDslRecord;
import com.shuyixiao.esdsl.service.EsDslMonitoringService;
import com.shuyixiao.esdsl.service.EsDslRecordService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * ES DSL 工具窗口
 * 在 IntelliJ IDEA 中提供独立的工具窗口，用于展示、筛选、搜索和管理 ES DSL 查询记录
 */
public class EsDslToolWindow extends JPanel {
    
    private final Project project;
    private final EsDslRecordService recordService;
    private final EsDslMonitoringService monitoringService;
    
    private JBTable dslTable;
    private DslTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> methodFilter;
    private JComboBox<String> timeRangeFilter;
    private JLabel statusLabel;
    private JSplitPane mainSplitter;
    private JTextArea detailArea;
    
    public EsDslToolWindow(@NotNull Project project) {
        this.project = project;
        this.recordService = project.getService(EsDslRecordService.class);
        this.monitoringService = project.getService(EsDslMonitoringService.class);
        
        initializeUI();
        setupEventHandlers();
        refreshData();
        
        // 每10秒自动刷新一次
        Timer refreshTimer = new Timer(10000, e -> refreshData());
        refreshTimer.start();
        
        // 确保 Timer 在窗口关闭时被清理
        Disposer.register(project, () -> refreshTimer.stop());
    }
    
    /**
     * 初始化用户界面
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建工具栏
        add(createToolbar(), BorderLayout.NORTH);
        
        // 创建主要内容区域
        mainSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitter.setTopComponent(createDslListPanel());
        mainSplitter.setBottomComponent(createDetailPanel());
        mainSplitter.setDividerLocation(300);
        
        add(mainSplitter, BorderLayout.CENTER);
        
        // 创建状态栏
        add(createStatusBar(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建工具栏
     */
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBorder(JBUI.Borders.empty(5));
        
        // 监听开关
        JBCheckBox monitoringToggle = new JBCheckBox("启用ES监听", monitoringService.isMonitoringEnabled());
        monitoringToggle.addActionListener(e -> {
            if (monitoringToggle.isSelected()) {
                monitoringService.enableMonitoring();
            } else {
                monitoringService.disableMonitoring();
            }
            updateStatusLabel();
        });
        toolbar.add(monitoringToggle);
        
        toolbar.add(new JBLabel(" | "));
        
        // 搜索框
        searchField = new JBTextField(15);
        searchField.setToolTipText("搜索 DSL 查询...");
        toolbar.add(new JBLabel("搜索: "));
        toolbar.add(searchField);
        
        // 方法过滤
        methodFilter = new JComboBox<>(new String[]{
                "全部方法", "GET", "POST", "PUT", "DELETE"
        });
        toolbar.add(new JBLabel("方法: "));
        toolbar.add(methodFilter);
        
        // 时间范围过滤
        timeRangeFilter = new JComboBox<>(new String[]{
                "最近1小时", "最近6小时", "最近12小时", "最近24小时", "全部"
        });
        toolbar.add(new JBLabel("时间: "));
        toolbar.add(timeRangeFilter);
        
        // 刷新按钮
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        toolbar.add(refreshButton);
        
        // 清空按钮
        JButton clearButton = new JButton("清空所有");
        clearButton.addActionListener(e -> clearAllRecords());
        toolbar.add(clearButton);
        
        // 导出按钮
        JButton exportButton = new JButton("导出选中");
        exportButton.addActionListener(e -> exportSelectedRecord());
        toolbar.add(exportButton);
        
        return toolbar;
    }
    
    /**
     * 创建 DSL 列表面板
     */
    private JComponent createDslListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyTop(5));
        
        // 创建表格
        tableModel = new DslTableModel();
        dslTable = new JBTable(tableModel);
        dslTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dslTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPanel();
            }
        });
        
        // 设置表格样式
        dslTable.setShowGrid(false);
        dslTable.setIntercellSpacing(new Dimension(0, 0));
        dslTable.setRowHeight(25);
        
        // 设置列宽
        dslTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // 方法
        dslTable.getColumnModel().getColumn(1).setPreferredWidth(100); // 索引
        dslTable.getColumnModel().getColumn(2).setPreferredWidth(300); // DSL摘要
        dslTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // 执行时间
        dslTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 时间戳
        dslTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // 状态
        
        // 设置自定义渲染器
        dslTable.setDefaultRenderer(Object.class, new DslTableCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(dslTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建详细信息面板
     */
    private JComponent createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        JBLabel titleLabel = new JBLabel("DSL 详情");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(detailArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton copyButton = new JButton("复制 DSL");
        copyButton.addActionListener(e -> copyDslToClipboard());
        buttonPanel.add(copyButton);
        
        JButton copyAllButton = new JButton("复制全部");
        copyAllButton.addActionListener(e -> copyAllToClipboard());
        buttonPanel.add(copyAllButton);
        
        JButton formatButton = new JButton("格式化");
        formatButton.addActionListener(e -> formatDetailArea());
        buttonPanel.add(formatButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建状态栏
     */
    private JComponent createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(JBUI.Borders.emptyTop(2));
        
        statusLabel = new JBLabel();
        statusBar.add(statusLabel);
        
        updateStatusLabel();
        
        return statusBar;
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 搜索框事件
        searchField.addActionListener(e -> refreshData());
        
        // 过滤器事件
        methodFilter.addActionListener(e -> refreshData());
        timeRangeFilter.addActionListener(e -> refreshData());
        
        // 双击查看详情
        dslTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetailDialog();
                }
            }
        });
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<EsDslRecord> records = getFilteredRecords();
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    tableModel.updateData(records);
                    updateStatusLabel();
                });
                
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(project, "刷新数据失败: " + e.getMessage(), "错误");
                });
            }
        });
    }
    
    /**
     * 获取过滤后的记录
     */
    private List<EsDslRecord> getFilteredRecords() {
        // 时间范围
        String timeRange = (String) timeRangeFilter.getSelectedItem();
        List<EsDslRecord> records;
        
        if ("全部".equals(timeRange)) {
            records = recordService.getAllRecords();
        } else {
            int hours = getHoursFromTimeRange(timeRange);
            records = recordService.getRecentRecords(hours);
        }
        
        // 方法过滤
        String method = (String) methodFilter.getSelectedItem();
        if (method != null && !"全部方法".equals(method)) {
            records = recordService.getRecordsByMethod(method);
        }
        
        // 搜索过滤
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            records = recordService.searchRecords(searchText);
        }
        
        return records;
    }
    
    /**
     * 从时间范围获取小时数
     */
    private int getHoursFromTimeRange(String timeRange) {
        switch (timeRange) {
            case "最近1小时": return 1;
            case "最近6小时": return 6;
            case "最近12小时": return 12;
            case "最近24小时": return 24;
            default: return 24;
        }
    }
    
    /**
     * 更新详细信息面板
     */
    private void updateDetailPanel() {
        int selectedRow = dslTable.getSelectedRow();
        if (selectedRow < 0) {
            detailArea.setText("");
            return;
        }
        
        EsDslRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            StringBuilder detail = new StringBuilder();
            detail.append("=== ES DSL 查询详情 ===\n\n");
            detail.append("时间: ").append(record.getFormattedTimestamp()).append("\n");
            detail.append("项目: ").append(record.getProject()).append("\n");
            detail.append("方法: ").append(record.getMethod()).append("\n");
            detail.append("索引: ").append(record.getIndex() != null ? record.getIndex() : "N/A").append("\n");
            detail.append("端点: ").append(record.getEndpoint() != null ? record.getEndpoint() : "N/A").append("\n");
            detail.append("来源: ").append(record.getSource()).append("\n");
            detail.append("状态码: ").append(record.getHttpStatus()).append("\n");
            if (record.getExecutionTime() != null) {
                detail.append("执行时间: ").append(record.getExecutionTime()).append(" ms\n");
            }
            detail.append("\n=== DSL 查询 ===\n");
            detail.append(record.getDslQuery());
            
            if (record.getResponse() != null && !record.getResponse().isEmpty()) {
                detail.append("\n\n=== 响应 ===\n");
                detail.append(record.getResponse());
            }
            
            detailArea.setText(detail.toString());
            detailArea.setCaretPosition(0);
        }
    }
    
    /**
     * 更新状态标签
     */
    private void updateStatusLabel() {
        boolean monitoring = monitoringService.isMonitoringEnabled();
        int activeListeners = monitoringService.getActiveListenerCount();
        
        EsDslRecordService.Statistics stats = recordService.getStatistics();
        
        String status = String.format(
                "监听状态: %s | 活动监听器: %d | 总查询: %d | 成功: %d | 失败: %d | 索引数: %d",
                monitoring ? "启用" : "禁用",
                activeListeners,
                stats.getTotalCount(),
                stats.getSuccessCount(),
                stats.getFailureCount(),
                stats.getDistinctIndexes()
        );
        
        statusLabel.setText(status);
        statusLabel.setForeground(monitoring ? JBColor.BLACK : JBColor.RED);
    }
    
    /**
     * 清空所有记录
     */
    private void clearAllRecords() {
        int result = Messages.showYesNoDialog(
                project,
                "确定要清空所有 ES DSL 查询记录吗？",
                "清空确认",
                Messages.getQuestionIcon()
        );
        if (result == Messages.YES) {
            recordService.clearAllRecords();
            refreshData();
            Messages.showInfoMessage(project, "已清空所有记录", "操作成功");
        }
    }
    
    /**
     * 导出选中的记录
     */
    private void exportSelectedRecord() {
        int selectedRow = dslTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个查询记录", "提示");
            return;
        }
        
        EsDslRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            String exported = buildExportText(record);
            copyToClipboard(exported);
            Messages.showInfoMessage(project, "已导出到剪贴板", "操作成功");
        }
    }
    
    /**
     * 构建导出文本
     */
    private String buildExportText(EsDslRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ES DSL 查询导出\n\n");
        sb.append("## 基本信息\n");
        sb.append("- 时间: ").append(record.getFormattedTimestamp()).append("\n");
        sb.append("- 方法: ").append(record.getMethod()).append("\n");
        sb.append("- 索引: ").append(record.getIndex() != null ? record.getIndex() : "N/A").append("\n");
        sb.append("- 端点: ").append(record.getEndpoint() != null ? record.getEndpoint() : "N/A").append("\n\n");
        sb.append("## DSL 查询\n");
        sb.append("```json\n");
        sb.append(record.getDslQuery());
        sb.append("\n```\n");
        return sb.toString();
    }
    
    /**
     * 复制 DSL 到剪贴板
     */
    private void copyDslToClipboard() {
        int selectedRow = dslTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个查询记录", "提示");
            return;
        }
        
        EsDslRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            copyToClipboard(record.getDslQuery());
            Messages.showInfoMessage(project, "DSL 已复制到剪贴板", "操作成功");
        }
    }
    
    /**
     * 复制全部到剪贴板
     */
    private void copyAllToClipboard() {
        copyToClipboard(detailArea.getText());
        Messages.showInfoMessage(project, "已复制到剪贴板", "操作成功");
    }
    
    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (Exception e) {
            Messages.showErrorDialog(project, "复制失败: " + e.getMessage(), "错误");
        }
    }
    
    /**
     * 格式化详情区域
     */
    private void formatDetailArea() {
        // 这里可以添加更高级的 JSON 格式化逻辑
        Messages.showInfoMessage(project, "格式化功能开发中", "提示");
    }
    
    /**
     * 显示详情对话框
     */
    private void showDetailDialog() {
        int selectedRow = dslTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        EsDslRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            String details = buildExportText(record);
            
            JTextArea textArea = new JTextArea(details);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(800, 600));
            
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "ES DSL 查询详情", true);
            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton closeButton = new JButton("关闭");
            closeButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(closeButton);
            
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }
    
    /**
     * DSL 表格模型
     */
    private static class DslTableModel extends AbstractTableModel {
        private final String[] columnNames = {"方法", "索引", "DSL摘要", "执行时间", "时间戳", "状态"};
        private List<EsDslRecord> records = List.of();
        
        public void updateData(List<EsDslRecord> newRecords) {
            this.records = newRecords;
            fireTableDataChanged();
        }
        
        public EsDslRecord getRecordAt(int row) {
            return row >= 0 && row < records.size() ? records.get(row) : null;
        }
        
        @Override
        public int getRowCount() {
            return records.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= records.size()) return null;
            
            EsDslRecord record = records.get(rowIndex);
            
            switch (columnIndex) {
                case 0: // 方法
                    return record.getMethod();
                case 1: // 索引
                    return record.getIndex() != null ? record.getIndex() : "N/A";
                case 2: // DSL摘要
                    return record.getShortQuery();
                case 3: // 执行时间
                    return record.getExecutionTime() != null ? record.getExecutionTime() + " ms" : "N/A";
                case 4: // 时间戳
                    return record.getFormattedTimestamp();
                case 5: // 状态
                    return record.getHttpStatus();
                default:
                    return null;
            }
        }
    }
    
    /**
     * DSL 表格单元格渲染器
     */
    private static class DslTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // 方法列着色
            if (column == 0 && value != null) {
                String method = value.toString();
                Color color = getMethodColor(method);
                if (!isSelected) {
                    component.setForeground(color);
                }
            }
            
            // 状态列着色
            if (column == 5 && value != null) {
                try {
                    int status = Integer.parseInt(value.toString());
                    if (status >= 200 && status < 300) {
                        if (!isSelected) {
                            component.setForeground(new Color(0, 128, 0)); // 绿色
                        }
                    } else if (status >= 400) {
                        if (!isSelected) {
                            component.setForeground(new Color(220, 20, 60)); // 红色
                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            
            return component;
        }
        
        private Color getMethodColor(String method) {
            switch (method.toUpperCase()) {
                case "GET":
                    return new Color(97, 175, 254);  // 蓝色
                case "POST":
                    return new Color(73, 204, 144);  // 绿色
                case "PUT":
                    return new Color(252, 161, 48);  // 橙色
                case "DELETE":
                    return new Color(249, 62, 62);   // 红色
                default:
                    return Color.GRAY;
            }
        }
    }
}

