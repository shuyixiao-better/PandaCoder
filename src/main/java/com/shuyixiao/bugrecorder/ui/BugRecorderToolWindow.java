package com.shuyixiao.bugrecorder.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.service.BugAnalysisService;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import com.shuyixiao.bugrecorder.service.ConsoleMonitoringService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Bug记录器工具窗口
 * 在IntelliJ IDEA中提供独立的工具窗口，用于展示、筛选、搜索和管理Bug记录
 */
public class BugRecorderToolWindow extends JPanel {

    private final Project project;
    private final BugRecordService bugRecordService;
    private final BugAnalysisService analysisService;
    private final ConsoleMonitoringService monitoringService;

    private JBTable bugTable;
    private BugTableModel tableModel;
    private JTextField searchField;
    private JComboBox<ErrorType> errorTypeFilter;
    private JComboBox<String> dateRangeFilter;
    private JLabel statusLabel;
    private JPanel detailPanel;
    private JTextArea analysisArea;
    private JTextArea solutionArea;

    public BugRecorderToolWindow(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        this.analysisService = project.getService(BugAnalysisService.class);
        this.monitoringService = project.getService(ConsoleMonitoringService.class);

        initializeUI();
        setupEventHandlers();
        refreshData();

        // 每30秒自动刷新一次
        Timer refreshTimer = new Timer(30000, e -> refreshData());
        refreshTimer.start();

        // 确保Timer在窗口关闭时被清理
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
        JSplitPane mainSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitter.setLeftComponent(createBugListPanel());
        mainSplitter.setRightComponent(createDetailPanel());
        mainSplitter.setDividerLocation(400);

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
        JBCheckBox monitoringToggle = new JBCheckBox("启用Bug监听", monitoringService.isMonitoringEnabled());
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
        searchField.getEmptyText().setText("搜索Bug记录...");
        toolbar.add(new JBLabel("搜索: "));
        toolbar.add(searchField);

        // 错误类型过滤
        errorTypeFilter = new JComboBox<>();
        errorTypeFilter.addItem(null); // 全部类型
        for (ErrorType type : ErrorType.values()) {
            errorTypeFilter.addItem(type);
        }
        errorTypeFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("全部类型");
                } else if (value instanceof ErrorType) {
                    setText(((ErrorType) value).getDisplayNameWithIcon());
                }
                return this;
            }
        });
        toolbar.add(new JBLabel("类型: "));
        toolbar.add(errorTypeFilter);

        // 日期范围过滤
        dateRangeFilter = new JComboBox<>(new String[]{
                "今天", "最近3天", "最近7天", "最近30天"
        });
        toolbar.add(new JBLabel("时间: "));
        toolbar.add(dateRangeFilter);

        // 刷新按钮
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        toolbar.add(refreshButton);

        // 清理按钮
        JButton cleanupButton = new JButton("清理旧记录");
        cleanupButton.addActionListener(e -> {
            int result = Messages.showYesNoDialog(
                    project,
                    "确定要清理30天前的Bug记录吗？",
                    "清理确认",
                    Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                bugRecordService.cleanupOldRecords(30);
                refreshData();
                Messages.showInfoMessage(project, "清理完成！", "操作成功");
            }
        });
        toolbar.add(cleanupButton);

        return toolbar;
    }

    /**
     * 创建Bug列表面板
     */
    private JComponent createBugListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyTop(5));

        // 创建表格
        tableModel = new BugTableModel();
        bugTable = new JBTable(tableModel);
        bugTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bugTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPanel();
            }
        });

        // 设置表格样式
        bugTable.setShowGrid(false);
        bugTable.setIntercellSpacing(new Dimension(0, 0));
        bugTable.setRowHeight(25);

        // 设置列宽
        bugTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // 类型
        bugTable.getColumnModel().getColumn(1).setPreferredWidth(200); // 摘要
        bugTable.getColumnModel().getColumn(2).setPreferredWidth(100); // 时间
        bugTable.getColumnModel().getColumn(3).setPreferredWidth(50);  // 状态

        // 设置自定义渲染器
        bugTable.setDefaultRenderer(Object.class, new BugTableCellRenderer());

        JBScrollPane scrollPane = new JBScrollPane(bugTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建详细信息面板
     */
    private JComponent createDetailPanel() {
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(JBUI.Borders.empty(5));

        // 创建标签页
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // 错误详情标签页
        JPanel errorDetailPanel = new JPanel(new BorderLayout());
        JTextArea errorTextArea = new JTextArea();
        errorTextArea.setEditable(false);
        errorTextArea.setBackground(getBackground());
        errorDetailPanel.add(new JBScrollPane(errorTextArea), BorderLayout.CENTER);
        tabbedPane.addTab("错误详情", errorDetailPanel);

        // AI分析标签页
        JPanel analysisPanel = new JPanel(new BorderLayout());
        analysisArea = new JTextArea();
        analysisArea.setEditable(false);
        analysisArea.setLineWrap(true);
        analysisArea.setWrapStyleWord(true);
        analysisArea.setBackground(getBackground());
        analysisPanel.add(new JBScrollPane(analysisArea), BorderLayout.CENTER);

        JButton analyzeButton = new JButton("开始AI分析");
        analyzeButton.addActionListener(e -> performAIAnalysis());
        JPanel analysisButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        analysisButtonPanel.add(analyzeButton);
        analysisPanel.add(analysisButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("AI分析", analysisPanel);

        // 解决方案标签页
        JPanel solutionPanel = new JPanel(new BorderLayout());
        solutionArea = new JTextArea();
        solutionArea.setEditable(false);
        solutionArea.setLineWrap(true);
        solutionArea.setWrapStyleWord(true);
        solutionArea.setBackground(getBackground());
        solutionPanel.add(new JBScrollPane(solutionArea), BorderLayout.CENTER);

        JPanel solutionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton markResolvedButton = new JButton("标记已解决");
        markResolvedButton.addActionListener(e -> markAsResolved());
        solutionButtonPanel.add(markResolvedButton);
        solutionPanel.add(solutionButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("解决方案", solutionPanel);

        detailPanel.add(tabbedPane, BorderLayout.CENTER);

        return detailPanel;
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
        errorTypeFilter.addActionListener(e -> refreshData());
        dateRangeFilter.addActionListener(e -> refreshData());

        // 双击事件
        bugTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    performAIAnalysis();
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
                List<BugRecord> records = getFilteredRecords();

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
    private List<BugRecord> getFilteredRecords() {
        int days = getDaysFromFilter();

        // 搜索
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            return bugRecordService.searchRecords(searchText, days);
        }

        // 按错误类型过滤
        ErrorType selectedType = (ErrorType) errorTypeFilter.getSelectedItem();
        if (selectedType != null) {
            return bugRecordService.getRecordsByErrorType(selectedType, days);
        }

        // 获取全部记录
        return bugRecordService.getRecentRecords(days);
    }

    /**
     * 从过滤器获取天数
     */
    private int getDaysFromFilter() {
        String selected = (String) dateRangeFilter.getSelectedItem();
        if (selected == null) return 7;

        switch (selected) {
            case "今天": return 1;
            case "最近3天": return 3;
            case "最近7天": return 7;
            case "最近30天": return 30;
            default: return 7;
        }
    }

    /**
     * 更新详细信息面板
     */
    private void updateDetailPanel() {
        int selectedRow = bugTable.getSelectedRow();
        if (selectedRow < 0) {
            detailPanel.setVisible(false);
            return;
        }

        BugRecord selectedRecord = tableModel.getRecordAt(selectedRow);
        if (selectedRecord != null) {
            updateDetailContent(selectedRecord);
            detailPanel.setVisible(true);
        }
    }

    /**
     * 更新详细内容
     */
    private void updateDetailContent(BugRecord record) {
        // 更新错误详情
        JTextArea errorTextArea = findErrorTextArea();
        if (errorTextArea != null) {
            StringBuilder errorDetail = new StringBuilder();
            errorDetail.append("错误ID: ").append(record.getId()).append("\n");
            errorDetail.append("项目: ").append(record.getProject()).append("\n");
            errorDetail.append("时间: ").append(record.getFormattedTimestamp()).append("\n");
            errorDetail.append("类型: ").append(record.getErrorType().getDisplayNameWithIcon()).append("\n");
            if (record.getExceptionClass() != null) {
                errorDetail.append("异常类: ").append(record.getExceptionClass()).append("\n");
            }
            if (record.getErrorMessage() != null) {
                errorDetail.append("错误消息: ").append(record.getErrorMessage()).append("\n");
            }
            errorDetail.append("\n完整错误信息:\n").append(record.getRawText());

            errorTextArea.setText(errorDetail.toString());
            errorTextArea.setCaretPosition(0);
        }

        // 更新AI分析内容
        if (record.getAiAnalysis() != null) {
            analysisArea.setText(record.getAiAnalysis());
        } else {
            analysisArea.setText("暂无AI分析结果，点击'开始AI分析'按钮进行分析。");
        }

        // 更新解决方案内容
        if (record.getSolution() != null) {
            solutionArea.setText(record.getSolution());
        } else {
            solutionArea.setText("暂无解决方案，请先进行AI分析。");
        }
    }

    /**
     * 查找错误详情文本区域
     */
    private JTextArea findErrorTextArea() {
        // 简化实现，实际中需要遍历组件树
        return null;
    }

    /**
     * 执行AI分析
     */
    private void performAIAnalysis() {
        int selectedRow = bugTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个Bug记录", "提示");
            return;
        }

        BugRecord selectedRecord = tableModel.getRecordAt(selectedRow);
        if (selectedRecord == null) return;

        analysisArea.setText("正在进行AI分析，请稍候...");
        solutionArea.setText("等待分析完成...");

        CompletableFuture<BugAnalysisService.AnalysisResult> future =
                analysisService.analyzeBugAsync(selectedRecord);

        future.thenAccept(result -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                analysisArea.setText(result.getAnalysis());
                solutionArea.setText(result.getSolution());

                // 更新Bug记录
                analysisService.updateBugWithAnalysis(selectedRecord, result);

                // 显示置信度
                String confidenceMsg = String.format("AI分析完成！置信度: %s (%.1f%%)",
                        result.getConfidenceLevel(), result.getConfidence() * 100);
                Messages.showInfoMessage(project, confidenceMsg, "分析完成");

                // 刷新表格
                refreshData();
            });
        }).exceptionally(throwable -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                analysisArea.setText("AI分析失败: " + throwable.getMessage());
                solutionArea.setText("无法提供解决方案建议。");
                Messages.showErrorDialog(project, "AI分析失败: " + throwable.getMessage(), "错误");
            });
            return null;
        });
    }

    /**
     * 标记为已解决
     */
    private void markAsResolved() {
        int selectedRow = bugTable.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "请先选择一个Bug记录", "提示");
            return;
        }

        BugRecord selectedRecord = tableModel.getRecordAt(selectedRow);
        if (selectedRecord == null) return;

        BugRecord resolvedRecord = selectedRecord.withResolved(true);
        bugRecordService.updateBugRecord(resolvedRecord);

        Messages.showInfoMessage(project, "Bug已标记为已解决", "操作成功");
        refreshData();
    }

    /**
     * 更新状态标签
     */
    private void updateStatusLabel() {
        boolean monitoring = monitoringService.isMonitoringEnabled();
        int activeListeners = monitoringService.getActiveListenerCount();

        BugRecordService.BugStatistics stats = bugRecordService.getStatistics(7);

        String status = String.format(
                "监听状态: %s | 活动监听器: %d | 最近7天Bug: %d个 (已解决: %d个, 待处理: %d个)",
                monitoring ? "启用" : "禁用",
                activeListeners,
                stats.getTotalCount(),
                stats.getResolvedCount(),
                stats.getPendingCount()
        );

        statusLabel.setText(status);
        statusLabel.setForeground(monitoring ? JBColor.BLACK : JBColor.RED);
    }

    /**
     * Bug表格模型
     */
    private static class BugTableModel extends AbstractTableModel {
        private final String[] columnNames = {"类型", "摘要", "时间", "状态"};
        private List<BugRecord> records = List.of();

        public void updateData(List<BugRecord> newRecords) {
            this.records = newRecords;
            fireTableDataChanged();
        }

        public BugRecord getRecordAt(int row) {
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

            BugRecord record = records.get(rowIndex);
            switch (columnIndex) {
                case 0: return record.getErrorType();
                case 1: return record.getShortDescription();
                case 2: return record.getFormattedTimestamp();
                case 3: return record.isResolved() ? "已解决" : "待处理";
                default: return null;
            }
        }
    }

    /**
     * Bug表格单元格渲染器
     */
    private static class BugTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof ErrorType) {
                ErrorType errorType = (ErrorType) value;
                setText(errorType.getDisplayNameWithIcon());
                if (!isSelected) {
                    setBackground(Color.decode(errorType.getColor()));
                    setForeground(Color.WHITE);
                }
            } else if (column == 3) { // 状态列
                if ("已解决".equals(value)) {
                    if (!isSelected) {
                        setBackground(new Color(200, 255, 200));
                        setForeground(Color.BLACK);
                    }
                } else {
                    if (!isSelected) {
                        setBackground(new Color(255, 200, 200));
                        setForeground(Color.BLACK);
                    }
                }
            }

            return this;
        }
    }
}