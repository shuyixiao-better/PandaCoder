package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.BugStatus;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 增强版Bug记录工具窗口
 * 提供更好的用户界面和交互体验
 */
public class EnhancedBugRecorderToolWindow extends SimpleToolWindowPanel {

    private static final Logger LOG = Logger.getInstance(EnhancedBugRecorderToolWindow.class);

    private final Project project;
    private final BugRecordService bugRecordService;
    
    // UI组件
    private JBTable bugTable;
    private BugTableModel tableModel;
    private ActionToolbar toolbar;
    private JBLabel statusLabel;
    private Timer refreshTimer;
    
    // 数据
    private final List<BugRecord> bugRecords = new CopyOnWriteArrayList<>();
    
    // 配置参数
    private static final long REFRESH_INTERVAL_MS = 5000; // 5秒刷新一次

    public EnhancedBugRecorderToolWindow(@NotNull Project project) {
        super(false, true);
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        
        // 初始化UI
        initializeUI();
        
        // 启动定期刷新
        startRefreshTimer();
        
        LOG.info("Enhanced Bug Recorder Tool Window initialized for project: " + project.getName());
    }

    /**
     * 初始化用户界面
     */
    private void initializeUI() {
        try {
            // 创建表格模型
            tableModel = new BugTableModel();
            
            // 创建表格
            bugTable = new JBTable(tableModel);
            bugTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            bugTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            // 设置自定义渲染器
            bugTable.setDefaultRenderer(Object.class, new BugTableCellRenderer());
            
            // 设置列宽
            setupColumnWidths();
            
            // 创建滚动面板
            JBScrollPane scrollPane = new JBScrollPane(bugTable);
            scrollPane.setBorder(JBUI.Borders.empty());
            
            // 创建状态栏
            statusLabel = new JBLabel("就绪");
            statusLabel.setBorder(JBUI.Borders.empty(2, 5));
            
            // 创建底部面板
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(statusLabel, BorderLayout.WEST);
            
            // 创建主面板
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(bottomPanel, BorderLayout.SOUTH);
            
            // 设置内容
            setContent(mainPanel);
            
            // 加载初始数据
            loadData();
            
            LOG.debug("Enhanced Bug Recorder Tool Window UI initialized successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to initialize Enhanced Bug Recorder Tool Window UI", e);
        }
    }

    /**
     * 设置列宽
     */
    private void setupColumnWidths() {
        if (bugTable == null) return;
        
        // 设置列宽比例
        bugTable.getColumnModel().getColumn(0).setPreferredWidth(30);  // 状态
        bugTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 时间
        bugTable.getColumnModel().getColumn(2).setPreferredWidth(100); // 类型
        bugTable.getColumnModel().getColumn(3).setPreferredWidth(200); // 摘要
        bugTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // 发生次数
        bugTable.getColumnModel().getColumn(5).setPreferredWidth(120); // 项目
    }

    /**
     * 启动刷新定时器
     */
    private void startRefreshTimer() {
        refreshTimer = new Timer("BugRecorder-Refresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        refreshData();
                    } catch (Exception e) {
                        LOG.warn("Error during data refresh", e);
                    }
                });
            }
        }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            // 获取最近的Bug记录（最近7天）
            List<BugRecord> records = bugRecordService.getRecentRecords(7);
            
            // 更新数据模型
            bugRecords.clear();
            bugRecords.addAll(records);
            tableModel.fireTableDataChanged();
            
            // 更新状态
            updateStatus("已加载 " + records.size() + " 条记录");
            
            LOG.debug("Loaded " + records.size() + " bug records for display");
            
        } catch (Exception e) {
            LOG.error("Failed to load bug records", e);
            updateStatus("加载数据失败: " + e.getMessage());
        }
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        try {
            // 获取最新的Bug记录
            List<BugRecord> records = bugRecordService.getRecentRecords(7);
            
            // 检查是否有变化
            if (hasDataChanged(records)) {
                bugRecords.clear();
                bugRecords.addAll(records);
                tableModel.fireTableDataChanged();
                
                updateStatus("已刷新 " + records.size() + " 条记录");
                LOG.debug("Refreshed bug records display (" + records.size() + " records)");
            }
            
        } catch (Exception e) {
            LOG.warn("Error refreshing bug records", e);
            updateStatus("刷新数据失败: " + e.getMessage());
        }
    }

    /**
     * 检查数据是否发生变化
     */
    private boolean hasDataChanged(List<BugRecord> newRecords) {
        if (bugRecords.size() != newRecords.size()) {
            return true;
        }
        
        // 检查记录ID和时间戳
        for (int i = 0; i < bugRecords.size(); i++) {
            BugRecord oldRecord = bugRecords.get(i);
            BugRecord newRecord = newRecords.get(i);
            
            if (!oldRecord.getId().equals(newRecord.getId()) ||
                !oldRecord.getTimestamp().equals(newRecord.getTimestamp()) ||
                oldRecord.getOccurrenceCount() != newRecord.getOccurrenceCount() ||
                oldRecord.getStatus() != newRecord.getStatus()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 更新状态
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * 初始化工具窗口
     */
    public void initToolWindow(@NotNull ToolWindow toolWindow) {
        try {
            // 创建动作组
            DefaultActionGroup actionGroup = new DefaultActionGroup();
            
            // 添加动作（示例）
            // actionGroup.add(new RefreshAction(this));
            // actionGroup.add(new ClearAction(this));
            // actionGroup.addSeparator();
            // actionGroup.add(new ExportAction(this));
            
            // 创建工具栏
            ActionManager actionManager = ActionManager.getInstance();
            toolbar = actionManager.createActionToolbar("BugRecorderToolbar", actionGroup, true);
            toolbar.setTargetComponent(this);
            
            // 设置工具栏
            setToolbar(toolbar.getComponent());
            
            LOG.debug("Enhanced Bug Recorder Tool Window initialized for tool window: " + toolWindow.getId());
            
        } catch (Exception e) {
            LOG.error("Failed to initialize Enhanced Bug Recorder Tool Window", e);
        }
    }

    /**
     * 获取内容面板
     */
    @NotNull
    public JComponent getContent() {
        return this;
    }

    /**
     * 关闭工具窗口
     */
    public void dispose() {
        LOG.info("Disposing Enhanced Bug Recorder Tool Window for project: " + project.getName());
        
        // 停止刷新定时器
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        
        // 清理数据
        bugRecords.clear();
        
        LOG.info("Enhanced Bug Recorder Tool Window disposed for project: " + project.getName());
    }

    /**
     * 获取项目
     */
    @NotNull
    public Project getProject() {
        return project;
    }

    /**
     * 获取Bug记录服务
     */
    @NotNull
    public BugRecordService getBugRecordService() {
        return bugRecordService;
    }

    /**
     * Bug表格模型
     */
    private static class BugTableModel extends AbstractTableModel {
        
        private static final String[] COLUMN_NAMES = {
            "状态", "时间", "类型", "摘要", "发生次数", "项目"
        };
        
        private final List<BugRecord> bugRecords;

        public BugTableModel() {
            this.bugRecords = new CopyOnWriteArrayList<>();
        }

        @Override
        public int getRowCount() {
            return bugRecords.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= bugRecords.size()) {
                return null;
            }
            
            BugRecord record = bugRecords.get(rowIndex);
            
            switch (columnIndex) {
                case 0: // 状态
                    return record.getStatus().getDisplayName();
                case 1: // 时间
                    return record.getFormattedTimestamp();
                case 2: // 类型
                    return record.getErrorType().getDisplayName();
                case 3: // 摘要
                    return record.getSummary();
                case 4: // 发生次数
                    return record.getOccurrenceCount();
                case 5: // 项目
                    return record.getProject();
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: // 状态
                    return String.class;
                case 1: // 时间
                    return String.class;
                case 2: // 类型
                    return String.class;
                case 3: // 摘要
                    return String.class;
                case 4: // 发生次数
                    return Integer.class;
                case 5: // 项目
                    return String.class;
                default:
                    return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // 默认不可编辑
        }
    }

    /**
     * Bug表格单元格渲染器
     */
    private static class BugTableCellRenderer implements TableCellRenderer {
        
        private final JLabel label = new JLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                                                       boolean isSelected, boolean hasFocus, 
                                                       int row, int column) {
            label.setOpaque(true);
            
            // 设置文本
            if (value != null) {
                label.setText(value.toString());
            } else {
                label.setText("");
            }
            
            // 设置选中状态
            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
                
                // 根据列设置不同的背景色
                switch (column) {
                    case 0: // 状态列
                        if (value instanceof String) {
                            String status = (String) value;
                            if (status.contains("已解决")) {
                                label.setBackground(new JBColor(new Color(200, 255, 200), new Color(50, 100, 50)));
                            } else if (status.contains("处理中")) {
                                label.setBackground(new JBColor(new Color(255, 255, 200), new Color(100, 100, 50)));
                            } else if (status.contains("新建")) {
                                label.setBackground(new JBColor(new Color(255, 200, 200), new Color(100, 50, 50)));
                            }
                        }
                        break;
                    case 2: // 类型列
                        if (value instanceof String) {
                            String type = (String) value;
                            if (type.contains("数据库")) {
                                label.setBackground(new JBColor(new Color(200, 230, 255), new Color(50, 70, 100)));
                            } else if (type.contains("网络")) {
                                label.setBackground(new JBColor(new Color(255, 230, 200), new Color(100, 70, 50)));
                            } else if (type.contains("Spring")) {
                                label.setBackground(new JBColor(new Color(230, 255, 200), new Color(70, 100, 50)));
                            }
                        }
                        break;
                }
            }
            
            // 设置对齐方式
            switch (column) {
                case 0: // 状态
                case 2: // 类型
                case 4: // 发生次数
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    break;
                case 1: // 时间
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case 3: // 摘要
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case 5: // 项目
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                default:
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    break;
            }
            
            // 设置边距
            label.setBorder(JBUI.Borders.empty(2, 5));
            
            return label;
        }
    }
}