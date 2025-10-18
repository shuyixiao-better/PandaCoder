package com.shuyixiao.sql.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.service.SqlMonitoringService;
import com.shuyixiao.sql.service.SqlRecordService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * SQL Monitor 工具窗口
 * 在 IntelliJ IDEA 中提供独立的工具窗口，用于展示、筛选、搜索和管理 SQL 查询记录
 */
public class SqlToolWindow extends JPanel {
    
    private final Project project;
    private final SqlRecordService recordService;
    private final SqlMonitoringService monitoringService;
    
    private JBTable sqlTable;
    private SqlTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> operationFilter;
    private JComboBox<String> timeRangeFilter;
    private JLabel statusLabel;
    private JSplitPane mainSplitter;
    private JTextArea detailArea;
    
    public SqlToolWindow(@NotNull Project project) {
        this.project = project;
        this.recordService = project.getService(SqlRecordService.class);
        this.monitoringService = project.getService(SqlMonitoringService.class);
        
        initializeUI();
        setupEventHandlers();
        refreshData();
        
        // 注册实时监听器（当有新记录时立即刷新UI）
        Consumer<SqlRecord> recordListener = record -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                refreshData();
            });
        };
        recordService.addRecordListener(recordListener);
        
        // 每10秒自动刷新一次（作为备用机制）
        Timer refreshTimer = new Timer(10000, e -> refreshData());
        refreshTimer.start();
        
        // 确保 Timer 和监听器在窗口关闭时被清理
        Disposer.register(project, () -> {
            refreshTimer.stop();
            recordService.removeRecordListener(recordListener);
        });
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
        mainSplitter.setTopComponent(createSqlListPanel());
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
        JBCheckBox monitoringToggle = new JBCheckBox("启用SQL监听", monitoringService.isMonitoringEnabled());
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
        searchField.setToolTipText("搜索 SQL 查询...");
        toolbar.add(new JBLabel("搜索: "));
        toolbar.add(searchField);
        
        // 操作类型过滤
        operationFilter = new JComboBox<>(new String[]{
                "全部操作", "SELECT", "INSERT", "UPDATE", "DELETE"
        });
        toolbar.add(new JBLabel("操作: "));
        toolbar.add(operationFilter);
        
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
     * 创建 SQL 列表面板
     */
    private JComponent createSqlListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyTop(5));
        
        // 创建表格
        tableModel = new SqlTableModel();
        sqlTable = new JBTable(tableModel);
        sqlTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sqlTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPanel();
            }
        });
        
        // 设置表格样式
        sqlTable.setShowGrid(false);
        sqlTable.setIntercellSpacing(new Dimension(0, 0));
        sqlTable.setRowHeight(25);
        
        // 设置列宽
        sqlTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // 操作类型
        sqlTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 表名
        sqlTable.getColumnModel().getColumn(2).setPreferredWidth(200); // API路径
        sqlTable.getColumnModel().getColumn(3).setPreferredWidth(300); // SQL摘要
        sqlTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // 结果数
        sqlTable.getColumnModel().getColumn(5).setPreferredWidth(120); // 时间戳
        
        // 设置自定义渲染器
        sqlTable.setDefaultRenderer(Object.class, new SqlTableCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(sqlTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建详细信息面板
     */
    private JComponent createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        JBLabel titleLabel = new JBLabel("SQL 详情");
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
        
        JButton copySqlButton = new JButton("复制 SQL");
        copySqlButton.addActionListener(e -> copySqlToClipboard());
        buttonPanel.add(copySqlButton);
        
        JButton copyAllButton = new JButton("复制全部");
        copyAllButton.addActionListener(e -> copyAllToClipboard());
        buttonPanel.add(copyAllButton);
        
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
        operationFilter.addActionListener(e -> refreshData());
        timeRangeFilter.addActionListener(e -> refreshData());
        
        // 双击查看详情
        sqlTable.addMouseListener(new MouseAdapter() {
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
                List<SqlRecord> records = getFilteredRecords();
                
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
    private List<SqlRecord> getFilteredRecords() {
        // 时间范围
        String timeRange = (String) timeRangeFilter.getSelectedItem();
        List<SqlRecord> records;
        
        if ("全部".equals(timeRange)) {
            records = recordService.getAllRecords();
        } else {
            int hours = getHoursFromTimeRange(timeRange);
            records = recordService.getRecentRecords(hours);
        }
        
        // 操作类型过滤
        String operation = (String) operationFilter.getSelectedItem();
        if (operation != null && !"全部操作".equals(operation)) {
            records = recordService.getRecordsByOperation(operation);
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
        int selectedRow = sqlTable.getSelectedRow();
        if (selectedRow < 0) {
            detailArea.setText("");
            return;
        }
        
        SqlRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            StringBuilder detail = new StringBuilder();
            detail.append("=== SQL 查询详情 ===\n\n");
            detail.append("时间: ").append(record.getFormattedTimestamp()).append("\n");
            detail.append("项目: ").append(record.getProject()).append("\n");
            detail.append("操作: ").append(record.getOperation()).append("\n");
            detail.append("表名: ").append(record.getTableName() != null ? record.getTableName() : "N/A").append("\n");
            detail.append("来源: ").append(record.getSource()).append("\n");
            detail.append("结果数: ").append(record.getResultCount() != null ? record.getResultCount() : "N/A").append("\n");
            if (record.getExecutionTime() != null) {
                detail.append("执行时间: ").append(record.getExecutionTime()).append(" ms\n");
            }
            if (record.getApiPath() != null) {
                detail.append("API路径: ").append(record.getApiPath()).append("\n");
            }
            if (record.getCallerClass() != null) {
                detail.append("调用类: ").append(record.getCallerClass()).append("\n");
            }
            detail.append("\n=== 可执行 SQL ===\n");
            detail.append(record.getExecutableSql());
            
            detail.append("\n\n=== 原始 SQL ===\n");
            detail.append(record.getSqlStatement());
            
            if (record.getParameters() != null && !record.getParameters().isEmpty()) {
                detail.append("\n\n=== 参数 ===\n");
                detail.append(record.getParameters());
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
        
        SqlRecordService.Statistics stats = recordService.getStatistics();
        
        String status = String.format(
                "监听状态: %s | 活动监听器: %d | 总查询: %d | SELECT: %d | INSERT: %d | UPDATE: %d | DELETE: %d | 表数: %d",
                monitoring ? "启用" : "禁用",
                activeListeners,
                stats.getTotalCount(),
                stats.getSelectCount(),
                stats.getInsertCount(),
                stats.getUpdateCount(),
                stats.getDeleteCount(),
                stats.getDistinctTables()
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
                "确定要清空所有 SQL 查询记录吗？",
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
        int selectedRow = sqlTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个查询记录", "提示");
            return;
        }
        
        SqlRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            String exported = buildExportText(record);
            copyToClipboard(exported);
            Messages.showInfoMessage(project, "已导出到剪贴板", "操作成功");
        }
    }
    
    /**
     * 构建导出文本
     */
    private String buildExportText(SqlRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SQL 查询导出\n\n");
        sb.append("## 基本信息\n");
        sb.append("- 时间: ").append(record.getFormattedTimestamp()).append("\n");
        sb.append("- 操作: ").append(record.getOperation()).append("\n");
        sb.append("- 表名: ").append(record.getTableName() != null ? record.getTableName() : "N/A").append("\n");
        if (record.getApiPath() != null) {
            sb.append("- API路径: ").append(record.getApiPath()).append("\n");
        }
        sb.append("\n## SQL 语句\n");
        sb.append("```sql\n");
        sb.append(record.getSqlStatement());
        sb.append("\n```\n");
        if (record.getParameters() != null && !record.getParameters().isEmpty()) {
            sb.append("\n## 参数\n");
            sb.append(record.getParameters()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 复制 SQL 到剪贴板（可执行版本）
     */
    private void copySqlToClipboard() {
        int selectedRow = sqlTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个查询记录", "提示");
            return;
        }
        
        SqlRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            // 复制可执行的SQL（参数已替换）
            String executableSql = record.getExecutableSql();
            copyToClipboard(executableSql);
            Messages.showInfoMessage(project, "可执行 SQL 已复制到剪贴板", "操作成功");
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
     * 显示详情对话框
     */
    private void showDetailDialog() {
        int selectedRow = sqlTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        SqlRecord record = tableModel.getRecordAt(selectedRow);
        if (record != null) {
            String details = buildExportText(record);
            
            JTextArea textArea = new JTextArea(details);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(800, 600));
            
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "SQL 查询详情", true);
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
     * SQL 表格模型
     */
    private static class SqlTableModel extends AbstractTableModel {
        private final String[] columnNames = {"操作", "表名", "API路径", "SQL摘要", "结果数", "时间戳"};
        private List<SqlRecord> records = List.of();
        
        public void updateData(List<SqlRecord> newRecords) {
            this.records = newRecords;
            fireTableDataChanged();
        }
        
        public SqlRecord getRecordAt(int row) {
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
            
            SqlRecord record = records.get(rowIndex);
            
            switch (columnIndex) {
                case 0: // 操作
                    return record.getOperation();
                case 1: // 表名
                    return record.getTableName() != null ? record.getTableName() : "N/A";
                case 2: // API路径
                    return record.getApiPath() != null ? record.getApiPath() : "N/A";
                case 3: // SQL摘要（可执行）
                    String executableSql = record.getExecutableSql();
                    if (executableSql.length() > 100) {
                        return executableSql.substring(0, 100) + "...";
                    }
                    return executableSql;
                case 4: // 结果数
                    return record.getResultCount() != null ? record.getResultCount().toString() : "N/A";
                case 5: // 时间戳
                    return record.getFormattedTimestamp();
                default:
                    return null;
            }
        }
    }
    
    /**
     * SQL 表格单元格渲染器
     */
    private static class SqlTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // 操作列着色
            if (column == 0 && value != null) {
                String operation = value.toString();
                Color color = getOperationColor(operation);
                if (!isSelected) {
                    component.setForeground(color);
                }
            }
            
            return component;
        }
        
        private Color getOperationColor(String operation) {
            switch (operation.toUpperCase()) {
                case "SELECT":
                    return new Color(97, 175, 254);  // 蓝色
                case "INSERT":
                    return new Color(73, 204, 144);  // 绿色
                case "UPDATE":
                    return new Color(252, 161, 48);  // 橙色
                case "DELETE":
                    return new Color(249, 62, 62);   // 红色
                default:
                    return Color.GRAY;
            }
        }
    }
}

