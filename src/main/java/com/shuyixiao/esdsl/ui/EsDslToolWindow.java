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
import com.shuyixiao.ui.EnhancedNotificationUtil;
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
        
        // ✅ 注册实时监听器（当有新记录时立即刷新UI）
        Consumer<EsDslRecord> recordListener = record -> {
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
        searchField.setToolTipText("<html>搜索范围：<br>" +
                "• DSL 查询内容<br>" +
                "• 索引名称<br>" +
                "• 端点路径<br>" +
                "<i>提示：支持模糊搜索，不区分大小写</i></html>");
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
        dslTable.getColumnModel().getColumn(2).setPreferredWidth(200); // API路径
        dslTable.getColumnModel().getColumn(3).setPreferredWidth(250); // DSL摘要
        dslTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // 执行时间
        dslTable.getColumnModel().getColumn(5).setPreferredWidth(100); // 时间戳
        dslTable.getColumnModel().getColumn(6).setPreferredWidth(60);  // 状态
        
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
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(false);  // 不在单词边界换行，保持代码完整性
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
        
        JButton kibanaButton = new JButton("一键Kibana");
        kibanaButton.setToolTipText("生成可在Kibana中直接使用的查询语句");
        kibanaButton.addActionListener(e -> copyKibanaFormat());
        buttonPanel.add(kibanaButton);
        
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
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTableContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTableContextMenu(e);
                }
            }
        });
    }
    
    /**
     * 显示表格右键菜单
     */
    private void showTableContextMenu(MouseEvent e) {
        int row = dslTable.rowAtPoint(e.getPoint());
        if (row >= 0 && row < dslTable.getRowCount()) {
            dslTable.setRowSelectionInterval(row, row);
            
            JPopupMenu popupMenu = new JPopupMenu();
            EsDslRecord record = tableModel.getRecordAt(row);
            
            if (record != null) {
                // 复制方法
                JMenuItem copyMethodItem = new JMenuItem("复制方法");
                copyMethodItem.addActionListener(ev -> {
                    copyToClipboard(record.getMethod());
                    EnhancedNotificationUtil.showCopySuccess(project, "方法已复制");
                });
                popupMenu.add(copyMethodItem);

                // 复制索引
                if (record.getIndex() != null) {
                    JMenuItem copyIndexItem = new JMenuItem("复制索引");
                    copyIndexItem.addActionListener(ev -> {
                        copyToClipboard(record.getIndex());
                        EnhancedNotificationUtil.showCopySuccess(project, "索引已复制");
                    });
                    popupMenu.add(copyIndexItem);
                }

                // 复制API路径
                if (record.getApiPath() != null) {
                    JMenuItem copyApiPathItem = new JMenuItem("复制API路径");
                    copyApiPathItem.addActionListener(ev -> {
                        copyToClipboard(record.getApiPath());
                        EnhancedNotificationUtil.showCopySuccess(project, "API路径已复制");
                    });
                    popupMenu.add(copyApiPathItem);
                }

                // 复制DSL摘要
                JMenuItem copyDslSummaryItem = new JMenuItem("复制DSL摘要");
                copyDslSummaryItem.addActionListener(ev -> {
                    copyToClipboard(record.getShortQuery());
                    EnhancedNotificationUtil.showCopySuccess(project, "DSL摘要已复制");
                });
                popupMenu.add(copyDslSummaryItem);

                popupMenu.addSeparator();

                // 复制完整DSL
                JMenuItem copyFullDslItem = new JMenuItem("复制完整DSL");
                copyFullDslItem.addActionListener(ev -> {
                    copyToClipboard(record.getDslQuery());
                    EnhancedNotificationUtil.showCopySuccess(project, "完整DSL已复制");
                });
                popupMenu.add(copyFullDslItem);

                // 复制端点
                if (record.getEndpoint() != null) {
                    JMenuItem copyEndpointItem = new JMenuItem("复制端点");
                    copyEndpointItem.addActionListener(ev -> {
                        copyToClipboard(record.getEndpoint());
                        EnhancedNotificationUtil.showCopySuccess(project, "端点已复制");
                    });
                    popupMenu.add(copyEndpointItem);
                }

                popupMenu.addSeparator();

                // 复制全部信息
                JMenuItem copyAllInfoItem = new JMenuItem("复制全部信息");
                copyAllInfoItem.addActionListener(ev -> {
                    String allInfo = buildExportText(record);
                    copyToClipboard(allInfo);
                    EnhancedNotificationUtil.showCopySuccess(project, "全部信息已复制");
                });
                popupMenu.add(copyAllInfoItem);
            }
            
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<EsDslRecord> records = getFilteredRecords();
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 保存当前选中的行
                    int selectedRow = dslTable.getSelectedRow();
                    
                    tableModel.updateData(records);
                    updateStatusLabel();
                    
                    // 恢复选中状态
                    if (selectedRow >= 0 && selectedRow < dslTable.getRowCount()) {
                        dslTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }
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
            // ✅ 显示API路径和调用类
            if (record.getApiPath() != null) {
                detail.append("API路径: ").append(record.getApiPath()).append("\n");
            }
            if (record.getCallerClass() != null) {
                detail.append("调用类: ").append(record.getCallerClass()).append("\n");
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
        // 直接清空，不需要确认弹窗
        recordService.clearAllRecords();
        refreshData();
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
            EnhancedNotificationUtil.showCopySuccess(project, "DSL 已复制到剪贴板");
        }
    }

    /**
     * 复制全部到剪贴板
     */
    private void copyAllToClipboard() {
        copyToClipboard(detailArea.getText());
        EnhancedNotificationUtil.showCopySuccess(project, "已复制到剪贴板");
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
     * 生成Kibana格式并复制到剪贴板
     * 格式: METHOD /index/_search
     * {
     *   "query": {...}
     * }
     */
    private void copyKibanaFormat() {
        int selectedRow = dslTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个查询记录", "提示");
            return;
        }
        
        EsDslRecord record = tableModel.getRecordAt(selectedRow);
        if (record == null) {
            Messages.showWarningDialog(project, "无法获取记录信息", "提示");
            return;
        }
        
        try {
            StringBuilder kibanaQuery = new StringBuilder();
            
            // 第一行: 请求方法和路径
            String method = record.getMethod() != null ? record.getMethod() : "POST";
            String endpoint = record.getEndpoint() != null ? record.getEndpoint() : "your_index/_search";
            
            // ✅ endpoint已经包含完整路径(索引名/_search?参数)
            // 不需要再拼接index,直接使用endpoint
            kibanaQuery.append(method).append(" /").append(endpoint);
            kibanaQuery.append("\n");
            
            // 第二行开始: DSL查询体
            String dsl = record.getDslQuery();
            if (dsl != null && !dsl.trim().isEmpty()) {
                kibanaQuery.append(dsl);
            } else {
                kibanaQuery.append("{}");
            }
            
            // 复制到剪贴板
            copyToClipboard(kibanaQuery.toString());

            // 显示成功消息
            EnhancedNotificationUtil.showCopySuccess(project, "Kibana格式已复制到剪贴板");
            
        } catch (Exception e) {
            Messages.showErrorDialog(project, "生成Kibana格式失败: " + e.getMessage(), "错误");
        }
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
        private final String[] columnNames = {"方法", "索引", "API路径", "DSL摘要", "执行时间", "时间戳", "状态"};
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
                case 2: // API路径
                    return record.getApiPath() != null ? record.getApiPath() : "N/A";
                case 3: // DSL摘要
                    return record.getShortQuery();
                case 4: // 执行时间
                    return record.getExecutionTime() != null ? record.getExecutionTime() + " ms" : "N/A";
                case 5: // 时间戳
                    return record.getFormattedTimestamp();
                case 6: // 状态
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
            if (column == 6 && value != null) {
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

