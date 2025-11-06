package com.shuyixiao.gitstat.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.gitstat.email.config.GitStatEmailConfigState;
import com.shuyixiao.gitstat.email.model.GitStatEmailConfig;
import com.shuyixiao.gitstat.email.model.GitStatEmailRecord;
import com.shuyixiao.gitstat.email.model.SmtpPreset;
import com.shuyixiao.gitstat.email.service.GitStatEmailService;
import com.shuyixiao.gitstat.email.util.PasswordEncryptor;
import com.shuyixiao.gitstat.model.GitAuthorDailyStat;
import com.shuyixiao.gitstat.model.GitAuthorStat;
import com.shuyixiao.gitstat.model.GitDailyStat;
import com.shuyixiao.gitstat.model.GitProjectStat;
import com.shuyixiao.gitstat.service.GitStatService;
import com.shuyixiao.gitstat.weekly.config.WeeklyReportConfigState;
import com.shuyixiao.gitstat.weekly.model.WeeklyReportConfig;
import com.shuyixiao.gitstat.weekly.service.GitWeeklyReportService;
import com.shuyixiao.gitstat.ui.component.WeekPickerDialog;
import com.shuyixiao.ui.EnhancedNotificationUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Git 统计工具窗口
 * 在 IntelliJ IDEA 中提供独立的工具窗口，用于展示 Git 统计信息
 */
public class GitStatToolWindow extends JPanel {
    
    private final Project project;
    private final GitStatService gitStatService;
    private final GitStatEmailService emailService;
    private final GitWeeklyReportService weeklyReportService;

    private JTabbedPane tabbedPane;
    
    // 作者统计标签页
    private JBTable authorTable;
    private AuthorTableModel authorTableModel;
    private JComboBox<String> authorSortComboBox;
    
    // 每日统计标签页
    private JBTable dailyTable;
    private DailyTableModel dailyTableModel;
    private JComboBox<String> dayRangeComboBox;
    
    // 作者每日统计标签页
    private JBTable authorDailyTable;
    private AuthorDailyTableModel authorDailyTableModel;
    private JComboBox<String> authorDailyRangeComboBox;
    private JComboBox<String> authorSelectionComboBox;
    
    // 项目代码统计标签页
    private JTextArea projectStatsArea;
    
    // 总览标签页
    private JTextArea overviewArea;
    
    // 邮件报告标签页
    private JComboBox<String> scheduleFilterAuthorComboBox;
    private JComboBox<String> manualFilterAuthorComboBox;
    
    // AI 代码统计标签页
    private JBTable aiStatsTable;
    private AiStatsTableModel aiStatsTableModel;
    private JTextArea aiOverviewArea;

    // 周报标签页
    private JTextArea weeklyCommitsArea;
    private JTextArea weeklyReportArea;
    private JTextField apiUrlField;
    private JPasswordField apiKeyField;
    private JTextField modelField;
    private JTextArea promptTemplateArea;
    private JButton generateReportButton;
    private JComboBox<String> weeklyAuthorComboBox;
    private JTextField weekStartDateField;  // 周开始日期选择
    private JLabel commitsLabel;  // 提交日志标签（动态更新）

    // 状态标签
    private JLabel statusLabel;
    
    public GitStatToolWindow(@NotNull Project project) {
        this.project = project;
        this.gitStatService = project.getService(GitStatService.class);
        this.emailService = project.getService(GitStatEmailService.class);
        this.weeklyReportService = project.getService(GitWeeklyReportService.class);

        // 先加载邮件配置，再初始化 UI（这样 UI 创建时就能获取到正确的配置）
        loadEmailConfig();
        initializeUI();
        setupEventHandlers();

        // 等待 IDEA 退出 dumb mode 后再刷新数据
        com.intellij.openapi.project.DumbService.getInstance(project).runWhenSmart(() -> {
            System.out.println("GitStatToolWindow: IDEA 已退出 dumb mode，准备刷新数据");
            // 延迟 2 秒，确保 Git 仓库和其他服务完全初始化，避免影响 IDEA 启动性能
            // 使用后台线程延迟，不阻塞 EDT
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    Thread.sleep(2000);  // 延迟 2 秒
                    System.out.println("GitStatToolWindow: 延迟完成，开始后台加载数据");
                    // 首次自动加载不显示通知，静默加载
                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshData();  // 使用不显示通知的版本
                    });
                } catch (InterruptedException e) {
                    System.out.println("GitStatToolWindow: 延迟加载被中断");
                    Thread.currentThread().interrupt();
                }
            });
        });
    }
    
    /**
     * 初始化用户界面
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建工具栏
        add(createToolbar(), BorderLayout.NORTH);
        
        // 创建标签页
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("作者统计", createAuthorStatsPanel());
        tabbedPane.addTab("每日统计", createDailyStatsPanel());
        tabbedPane.addTab("作者每日统计", createAuthorDailyStatsPanel());
        tabbedPane.addTab("项目代码统计", createProjectStatsPanel());
        tabbedPane.addTab("总览", createOverviewPanel());
        tabbedPane.addTab("🤖 AI 代码统计", createAiStatsPanel());
        tabbedPane.addTab("📧 邮件报告", createEmailReportPanel());
        tabbedPane.addTab("📝 工作周报", createWeeklyReportPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // 创建状态栏
        add(createStatusBar(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建工具栏
     */
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBorder(JBUI.Borders.empty(5));
        
        JButton refreshButton = new JButton("刷新数据");
        refreshButton.addActionListener(e -> refreshData(false));  // 手动点击也使用右下角通知
        toolbar.add(refreshButton);
        
        toolbar.add(new JBLabel(" | "));
        
        JButton exportButton = new JButton("导出统计");
        exportButton.addActionListener(e -> exportStatistics());
        toolbar.add(exportButton);
        
        return toolbar;
    }
    
    /**
     * 创建作者统计面板
     */
    private JComponent createAuthorStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        // 过滤工具栏
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JBLabel("排序方式: "));
        authorSortComboBox = new JComboBox<>(new String[]{
                "按提交次数", "按新增代码", "按删除代码", "按净变化"
        });
        authorSortComboBox.addActionListener(e -> updateAuthorTable());
        filterPanel.add(authorSortComboBox);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        // 创建表格
        authorTableModel = new AuthorTableModel();
        authorTable = new JBTable(authorTableModel);
        authorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置表格样式
        authorTable.setShowGrid(false);
        authorTable.setIntercellSpacing(new Dimension(0, 0));
        authorTable.setRowHeight(25);
        
        // 设置列宽
        authorTable.getColumnModel().getColumn(0).setPreferredWidth(150); // 作者
        authorTable.getColumnModel().getColumn(1).setPreferredWidth(200); // 邮箱
        authorTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 提交次数
        authorTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 新增行数
        authorTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 删除行数
        authorTable.getColumnModel().getColumn(5).setPreferredWidth(100); // 净变化
        authorTable.getColumnModel().getColumn(6).setPreferredWidth(120); // 首次提交
        authorTable.getColumnModel().getColumn(7).setPreferredWidth(120); // 最后提交
        
        // 设置自定义渲染器
        authorTable.setDefaultRenderer(Object.class, new NumberTableCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(authorTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建每日统计面板
     */
    private JComponent createDailyStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        // 过滤工具栏
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JBLabel("时间范围: "));
        dayRangeComboBox = new JComboBox<>(new String[]{
                "最近7天", "最近30天", "最近90天", "全部"
        });
        dayRangeComboBox.addActionListener(e -> updateDailyTable());
        filterPanel.add(dayRangeComboBox);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        // 创建表格
        dailyTableModel = new DailyTableModel();
        dailyTable = new JBTable(dailyTableModel);
        dailyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置表格样式
        dailyTable.setShowGrid(false);
        dailyTable.setIntercellSpacing(new Dimension(0, 0));
        dailyTable.setRowHeight(25);
        
        // 设置列宽
        dailyTable.getColumnModel().getColumn(0).setPreferredWidth(120); // 日期
        dailyTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 提交次数
        dailyTable.getColumnModel().getColumn(2).setPreferredWidth(100); // 新增行数
        dailyTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 删除行数
        dailyTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 净变化
        dailyTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // 活跃作者
        
        // 设置自定义渲染器
        dailyTable.setDefaultRenderer(Object.class, new NumberTableCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(dailyTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建作者每日统计面板
     */
    private JComponent createAuthorDailyStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        // 过滤工具栏
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // 作者选择下拉框
        filterPanel.add(new JBLabel("选择作者: "));
        authorSelectionComboBox = new JComboBox<>();
        authorSelectionComboBox.addItem("全部作者");
        authorSelectionComboBox.addActionListener(e -> updateAuthorDailyTable());
        filterPanel.add(authorSelectionComboBox);
        
        filterPanel.add(new JBLabel("  时间范围: "));
        authorDailyRangeComboBox = new JComboBox<>(new String[]{
                "最近7天", "最近30天", "最近90天", "全部"
        });
        authorDailyRangeComboBox.addActionListener(e -> updateAuthorDailyTable());
        filterPanel.add(authorDailyRangeComboBox);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        // 创建表格
        authorDailyTableModel = new AuthorDailyTableModel();
        authorDailyTable = new JBTable(authorDailyTableModel);
        authorDailyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置表格样式
        authorDailyTable.setShowGrid(false);
        authorDailyTable.setIntercellSpacing(new Dimension(0, 0));
        authorDailyTable.setRowHeight(25);
        
        // 设置列宽
        authorDailyTable.getColumnModel().getColumn(0).setPreferredWidth(120); // 日期
        authorDailyTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 作者
        authorDailyTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 提交次数
        authorDailyTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 新增行数
        authorDailyTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 删除行数
        authorDailyTable.getColumnModel().getColumn(5).setPreferredWidth(100); // 净变化
        
        // 设置自定义渲染器
        authorDailyTable.setDefaultRenderer(Object.class, new NumberTableCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(authorDailyTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建项目代码统计面板
     */
    private JComponent createProjectStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        JBLabel titleLabel = new JBLabel("项目代码统计");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        projectStatsArea = new JTextArea();
        projectStatsArea.setEditable(false);
        projectStatsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        
        JBScrollPane scrollPane = new JBScrollPane(projectStatsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建总览面板
     */
    private JComponent createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));
        
        JBLabel titleLabel = new JBLabel("Git 统计总览");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        overviewArea = new JTextArea();
        overviewArea.setEditable(false);
        overviewArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        
        JBScrollPane scrollPane = new JBScrollPane(overviewArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
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
        // 已在组件创建时添加
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        refreshData(false, false);  // 默认不显示弹窗，不显示通知
    }

    /**
     * 刷新数据
     * @param showDialog 是否显示弹窗提示（true=弹窗，false=右下角通知）
     */
    private void refreshData(boolean showDialog) {
        refreshData(showDialog, true);  // 手动刷新时显示通知
    }

    /**
     * 刷新数据
     * @param showDialog 是否显示弹窗提示（true=弹窗，false=右下角通知）
     * @param showNotification 是否显示通知（首次自动加载时不显示，手动刷新时显示）
     */
    private void refreshData(boolean showDialog, boolean showNotification) {
        System.out.println("GitStatToolWindow.refreshData: 开始刷新数据 (showDialog=" + showDialog + ", showNotification=" + showNotification + ")");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 显示加载提示
                ApplicationManager.getApplication().invokeLater(() -> {
                    statusLabel.setText("正在加载 Git 统计数据...");
                    statusLabel.setForeground(JBColor.BLUE);
                });

                System.out.println("  调用 gitStatService.refreshStatistics()");
                long startTime = System.currentTimeMillis();

                // 刷新统计数据（这是阻塞操作，会等待完成）
                gitStatService.refreshStatistics();

                long endTime = System.currentTimeMillis();
                System.out.println("  gitStatService.refreshStatistics() 完成，耗时: " + (endTime - startTime) + "ms");

                // 验证数据是否加载成功
                int authorCount = gitStatService.getAllAuthorStats().size();
                int dailyCount = gitStatService.getAllDailyStats().size();
                System.out.println("  数据验证 - 作者统计: " + authorCount + ", 每日统计: " + dailyCount);

                // 判断数据是否有效
                boolean hasValidData = authorCount > 0 || dailyCount > 0;

                if (!hasValidData) {
                    System.out.println("  警告：刷新完成但没有找到有效的统计数据");
                }

                // 更新 UI（在EDT线程中）
                ApplicationManager.getApplication().invokeLater(() -> {
                    System.out.println("  开始更新 UI (hasValidData=" + hasValidData + ")");

                    // 无论是否有数据都更新 UI，确保界面状态正确
                    updateAuthorSelectionComboBox();
                    updateEmailAuthorComboBoxes();
                    updateAuthorTable();
                    updateDailyTable();
                    updateAuthorDailyTable();
                    updateProjectStatsArea();
                    updateOverviewArea();
                    updateAiStats();  // 更新 AI 统计
                    updateStatusLabel();
                    System.out.println("  UI 更新完成");

                    // 只有在需要显示通知且数据加载成功时才显示
                    if (showNotification) {
                        if (hasValidData) {
                            // 根据参数决定显示方式
                            if (showDialog) {
                                Messages.showInfoMessage(project,
                                    "Git 统计数据已刷新\n作者数量: " + authorCount,
                                    "刷新成功");
                            } else {
                                // 使用右下角通知（会自动消失）
                                com.intellij.notification.Notifications.Bus.notify(
                                    new com.intellij.notification.Notification(
                                        "GitStat",
                                        "Git 统计",
                                        "Git 统计数据已刷新 (作者数量: " + authorCount + ")",
                                        com.intellij.notification.NotificationType.INFORMATION
                                    ),
                                    project
                                );
                            }
                        } else {
                            // 如果是手动刷新但没有数据，给出警告提示
                            com.intellij.notification.Notifications.Bus.notify(
                                new com.intellij.notification.Notification(
                                    "GitStat",
                                    "Git 统计",
                                    "未找到 Git 统计数据，请确认项目包含 Git 仓库",
                                    com.intellij.notification.NotificationType.WARNING
                                ),
                                project
                            );
                        }
                    }
                });

            } catch (Exception e) {
                System.out.println("  刷新数据异常: " + e.getMessage());
                e.printStackTrace();
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 只有在手动刷新时才显示错误对话框
                    if (showNotification) {
                        Messages.showErrorDialog(project, "刷新数据失败: " + e.getMessage(), "错误");
                    }
                    statusLabel.setText("刷新失败");
                    statusLabel.setForeground(JBColor.RED);
                });
            }
        });
    }
    
    /**
     * 更新作者选择下拉框
     */
    private void updateAuthorSelectionComboBox() {
        // 保存当前选择
        String currentSelection = (String) authorSelectionComboBox.getSelectedItem();
        
        // 清空并重新填充
        authorSelectionComboBox.removeAllItems();
        authorSelectionComboBox.addItem("全部作者");
        
        // 添加所有作者
        List<String> authorNames = gitStatService.getAllAuthorNames();
        for (String authorName : authorNames) {
            authorSelectionComboBox.addItem(authorName);
        }
        
        // 尝试恢复之前的选择
        if (currentSelection != null && !currentSelection.isEmpty()) {
            for (int i = 0; i < authorSelectionComboBox.getItemCount(); i++) {
                if (currentSelection.equals(authorSelectionComboBox.getItemAt(i))) {
                    authorSelectionComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    /**
     * 更新邮件面板的作者选择下拉框
     */
    private void updateEmailAuthorComboBoxes() {
        if (scheduleFilterAuthorComboBox == null || manualFilterAuthorComboBox == null) {
            return;
        }

        // 保存当前选择
        String scheduleSelection = (String) scheduleFilterAuthorComboBox.getSelectedItem();
        String manualSelection = (String) manualFilterAuthorComboBox.getSelectedItem();

        // 如果是第一次调用（下拉框只有默认项），从配置中加载
        boolean isFirstTime = scheduleFilterAuthorComboBox.getItemCount() <= 1;
        System.out.println("GitStatToolWindow.updateEmailAuthorComboBoxes: isFirstTime=" + isFirstTime +
                          ", scheduleFilterAuthorComboBox.getItemCount()=" + scheduleFilterAuthorComboBox.getItemCount());
        if (isFirstTime) {
            GitStatEmailConfig config = emailService.getConfig();
            scheduleSelection = config.getFilterAuthor();
            System.out.println("  从配置加载定时发送作者筛选: " + scheduleSelection);
            manualSelection = null;  // 手动发送不需要从配置加载
        }

        // 清空并重新填充定时发送下拉框
        scheduleFilterAuthorComboBox.removeAllItems();
        scheduleFilterAuthorComboBox.addItem("(所有作者)");

        // 清空并重新填充手动发送下拉框
        manualFilterAuthorComboBox.removeAllItems();
        manualFilterAuthorComboBox.addItem("(所有作者)");

        // 添加所有作者
        List<String> authorNames = gitStatService.getAllAuthorNames();
        for (String authorName : authorNames) {
            scheduleFilterAuthorComboBox.addItem(authorName);
            manualFilterAuthorComboBox.addItem(authorName);
        }

        // 尝试恢复之前的选择（定时发送）
        if (scheduleSelection != null && !scheduleSelection.isEmpty()) {
            boolean found = false;
            for (int i = 0; i < scheduleFilterAuthorComboBox.getItemCount(); i++) {
                if (scheduleSelection.equals(scheduleFilterAuthorComboBox.getItemAt(i))) {
                    scheduleFilterAuthorComboBox.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            // 如果没找到匹配的作者，默认选择"(所有作者)"
            if (!found) {
                scheduleFilterAuthorComboBox.setSelectedIndex(0);
            }
        } else {
            // 如果配置中是 null，选择"(所有作者)"
            scheduleFilterAuthorComboBox.setSelectedIndex(0);
        }

        // 尝试恢复之前的选择（手动发送）
        if (manualSelection != null && !manualSelection.isEmpty()) {
            for (int i = 0; i < manualFilterAuthorComboBox.getItemCount(); i++) {
                if (manualSelection.equals(manualFilterAuthorComboBox.getItemAt(i))) {
                    manualFilterAuthorComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    /**
     * 更新作者统计表格
     */
    private void updateAuthorTable() {
        String sortType = (String) authorSortComboBox.getSelectedItem();
        List<GitAuthorStat> stats;

        if ("按新增代码".equals(sortType)) {
            stats = gitStatService.getAuthorStatsSortedByLines();
        } else {
            stats = gitStatService.getAuthorStatsSortedByCommits();
        }

        // 添加日志
        System.out.println("GitStatToolWindow.updateAuthorTable: 获取到 " + stats.size() + " 条作者统计数据");

        authorTableModel.updateData(stats);
    }
    
    /**
     * 更新每日统计表格
     */
    private void updateDailyTable() {
        String range = (String) dayRangeComboBox.getSelectedItem();
        List<GitDailyStat> stats;
        
        switch (range) {
            case "最近7天":
                stats = gitStatService.getRecentDailyStats(7);
                break;
            case "最近30天":
                stats = gitStatService.getRecentDailyStats(30);
                break;
            case "最近90天":
                stats = gitStatService.getRecentDailyStats(90);
                break;
            default:
                stats = gitStatService.getAllDailyStats();
                break;
        }
        
        dailyTableModel.updateData(stats);
    }
    
    /**
     * 更新作者每日统计表格
     */
    private void updateAuthorDailyTable() {
        String range = (String) authorDailyRangeComboBox.getSelectedItem();
        String selectedAuthor = (String) authorSelectionComboBox.getSelectedItem();
        List<GitAuthorDailyStat> stats;
        
        // 判断是否选择了特定作者
        boolean isAllAuthors = selectedAuthor == null || "全部作者".equals(selectedAuthor);
        
        if (isAllAuthors) {
            // 显示所有作者的统计
            switch (range) {
                case "最近7天":
                    stats = gitStatService.getRecentAuthorDailyStats(7);
                    break;
                case "最近30天":
                    stats = gitStatService.getRecentAuthorDailyStats(30);
                    break;
                case "最近90天":
                    stats = gitStatService.getRecentAuthorDailyStats(90);
                    break;
                default:
                    stats = gitStatService.getAllAuthorDailyStats();
                    break;
            }
        } else {
            // 显示特定作者的统计
            if ("全部".equals(range)) {
                stats = gitStatService.getAuthorDailyStatsByAuthorName(selectedAuthor);
            } else {
                int days = switch (range) {
                    case "最近7天" -> 7;
                    case "最近30天" -> 30;
                    case "最近90天" -> 90;
                    default -> 365;
                };
                stats = gitStatService.getAuthorDailyStatsByAuthorAndDays(selectedAuthor, days);
            }
        }
        
        authorDailyTableModel.updateData(stats);
    }
    
    /**
     * 更新项目代码统计区域
     */
    private void updateProjectStatsArea() {
        GitProjectStat projectStat = gitStatService.getProjectStat();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== 项目代码统计 ===\n\n");
        sb.append("📁 项目概览\n");
        sb.append("  • 总文件数: ").append(formatNumber(projectStat.getTotalFiles())).append("\n");
        sb.append("  • 总行数: ").append(formatNumber(projectStat.getTotalLines())).append("\n");
        sb.append("  • 代码行数: ").append(formatNumber(projectStat.getTotalCodeLines())).append("\n");
        sb.append("  • 空行数: ").append(formatNumber(projectStat.getTotalBlankLines())).append("\n");
        sb.append("  • 注释行数: ").append(formatNumber(projectStat.getTotalCommentLines())).append("\n\n");
        
        // 按文件类型统计
        if (!projectStat.getFilesByExtension().isEmpty()) {
            sb.append("📊 按文件类型统计\n");
            sb.append(String.format("  %-15s %10s %15s\n", "文件类型", "文件数", "代码行数"));
            sb.append("  ").append("-".repeat(42)).append("\n");
            
            projectStat.getFilesByExtension().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(
                            projectStat.getLinesByExtension().getOrDefault(b.getKey(), 0),
                            projectStat.getLinesByExtension().getOrDefault(a.getKey(), 0)))
                    .forEach(entry -> {
                        String ext = entry.getKey();
                        int files = entry.getValue();
                        int lines = projectStat.getLinesByExtension().getOrDefault(ext, 0);
                        sb.append(String.format("  %-15s %10s %15s\n", 
                                ext, 
                                formatNumber(files), 
                                formatNumber(lines)));
                    });
        }
        
        projectStatsArea.setText(sb.toString());
        projectStatsArea.setCaretPosition(0);
    }
    
    /**
     * 更新总览区域
     */
    private void updateOverviewArea() {
        Map<String, Object> stats = gitStatService.getOverallStatistics();
        GitProjectStat projectStat = gitStatService.getProjectStat();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Git 统计总览 ===\n\n");
        
        sb.append("📁 项目代码量\n");
        sb.append("  • 总文件数: ").append(formatNumber(projectStat.getTotalFiles())).append("\n");
        sb.append("  • 总代码行数: ").append(formatNumber(projectStat.getTotalCodeLines())).append("\n");
        sb.append("  • 总行数: ").append(formatNumber(projectStat.getTotalLines())).append("\n\n");
        
        sb.append("📊 Git 历史统计\n");
        sb.append("  • 总提交次数: ").append(formatNumber((Integer) stats.get("totalCommits"))).append("\n");
        sb.append("  • 总作者数: ").append(stats.get("totalAuthors")).append("\n");
        sb.append("  • 历史新增行数: ").append(formatNumber((Integer) stats.get("totalAdditions"))).append("\n");
        sb.append("  • 历史删除行数: ").append(formatNumber((Integer) stats.get("totalDeletions"))).append("\n");
        sb.append("  • 净变化: ").append(formatNumber((Integer) stats.get("netChanges"))).append("\n");
        sb.append("  • 最后刷新: ").append(stats.get("lastRefreshDate")).append("\n\n");
        
        // 添加 AI 统计概览
        try {
            Map<String, Object> aiStats = gitStatService.getAiStatService().getOverallAiStatistics();
            if (aiStats != null && (Integer) aiStats.get("totalCommits") > 0) {
                sb.append("🤖 AI 辅助开发统计\n");
                sb.append("  • AI 辅助提交: ").append(formatNumber((Integer) aiStats.get("totalAiCommits")))
                  .append(" / ").append(formatNumber((Integer) aiStats.get("totalCommits")))
                  .append(" (").append(String.format("%.1f%%", aiStats.get("aiCommitPercentage"))).append(")\n");
                sb.append("  • AI 生成代码: ").append(formatNumber((Integer) aiStats.get("totalAiAdditions")))
                  .append(" 行 (").append(String.format("%.1f%%", aiStats.get("aiCodePercentage"))).append(")\n");
                sb.append("  • 使用 AI 的开发者: ").append(aiStats.get("aiUserCount")).append(" 人\n");
                
                // 显示 AI 工具使用情况
                @SuppressWarnings("unchecked")
                Map<String, Integer> toolUsage = (Map<String, Integer>) aiStats.get("aiToolUsage");
                if (toolUsage != null && !toolUsage.isEmpty()) {
                    sb.append("  • 常用 AI 工具: ");
                    toolUsage.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .limit(3)
                        .forEach(entry -> sb.append(entry.getKey()).append(" (")
                            .append(entry.getValue()).append("次), "));
                    sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            // AI 统计可能未启用，忽略错误
        }
        
        sb.append("🏆 Top 5 贡献者（按提交次数）\n");
        List<GitAuthorStat> topAuthors = gitStatService.getAuthorStatsSortedByCommits();
        int count = Math.min(5, topAuthors.size());
        for (int i = 0; i < count; i++) {
            GitAuthorStat author = topAuthors.get(i);
            sb.append(String.format("  %d. %s - %s 次提交\n", 
                    i + 1, 
                    author.getAuthorName(), 
                    formatNumber(author.getTotalCommits())));
        }
        
        sb.append("\n💻 Top 5 代码贡献者（按新增代码）\n");
        List<GitAuthorStat> topCoders = gitStatService.getAuthorStatsSortedByLines();
        count = Math.min(5, topCoders.size());
        for (int i = 0; i < count; i++) {
            GitAuthorStat author = topCoders.get(i);
            sb.append(String.format("  %d. %s - %s 行代码\n", 
                    i + 1, 
                    author.getAuthorName(), 
                    formatNumber(author.getTotalAdditions())));
        }
        
        overviewArea.setText(sb.toString());
        overviewArea.setCaretPosition(0);
    }
    
    /**
     * 更新状态标签
     */
    private void updateStatusLabel() {
        Map<String, Object> stats = gitStatService.getOverallStatistics();
        
        String status = String.format(
                "作者数: %d | 提交次数: %s | 新增: %s 行 | 删除: %s 行 | 净变化: %s 行",
                stats.get("totalAuthors"),
                formatNumber((Integer) stats.get("totalCommits")),
                formatNumber((Integer) stats.get("totalAdditions")),
                formatNumber((Integer) stats.get("totalDeletions")),
                formatNumber((Integer) stats.get("netChanges"))
        );
        
        statusLabel.setText(status);
        statusLabel.setForeground(JBColor.BLACK);
    }
    
    /**
     * 导出统计信息
     */
    private void exportStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Git 统计报告\n");
        sb.append("=".repeat(80)).append("\n\n");
        sb.append(overviewArea.getText());

        // 复制到剪贴板
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
            EnhancedNotificationUtil.showCopySuccess(project, "统计信息已复制到剪贴板");
        } catch (Exception e) {
            Messages.showErrorDialog(project, "导出失败: " + e.getMessage(), "错误");
        }
    }
    
    /**
     * 格式化数字（添加千分位分隔符）
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    /**
     * 作者统计表格模型
     */
    private static class AuthorTableModel extends AbstractTableModel {
        private final String[] columnNames = {"作者", "邮箱", "提交次数", "新增行数", "删除行数", "净变化", "首次提交", "最后提交"};
        private List<GitAuthorStat> stats = List.of();
        
        public void updateData(List<GitAuthorStat> newStats) {
            this.stats = newStats;
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return stats.size();
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
            if (rowIndex >= stats.size()) return null;
            
            GitAuthorStat stat = stats.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return stat.getAuthorName();
                case 1: return stat.getAuthorEmail();
                case 2: return stat.getTotalCommits();
                case 3: return stat.getTotalAdditions();
                case 4: return stat.getTotalDeletions();
                case 5: return stat.getNetChanges();
                case 6: return stat.getFirstCommit() != null ? 
                        stat.getFirstCommit().format(DateTimeFormatter.ISO_DATE) : "N/A";
                case 7: return stat.getLastCommit() != null ? 
                        stat.getLastCommit().format(DateTimeFormatter.ISO_DATE) : "N/A";
                default: return null;
            }
        }
    }
    
    /**
     * 每日统计表格模型
     */
    private static class DailyTableModel extends AbstractTableModel {
        private final String[] columnNames = {"日期", "提交次数", "新增行数", "删除行数", "净变化", "活跃作者"};
        private List<GitDailyStat> stats = List.of();
        
        public void updateData(List<GitDailyStat> newStats) {
            this.stats = newStats;
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return stats.size();
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
            if (rowIndex >= stats.size()) return null;
            
            GitDailyStat stat = stats.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return stat.getDate().format(DateTimeFormatter.ISO_DATE);
                case 1: return stat.getCommits();
                case 2: return stat.getAdditions();
                case 3: return stat.getDeletions();
                case 4: return stat.getNetChanges();
                case 5: return stat.getActiveAuthors();
                default: return null;
            }
        }
    }
    
    /**
     * 作者每日统计表格模型
     */
    private static class AuthorDailyTableModel extends AbstractTableModel {
        private final String[] columnNames = {"日期", "作者", "提交次数", "新增行数", "删除行数", "净变化"};
        private List<GitAuthorDailyStat> stats = List.of();
        
        public void updateData(List<GitAuthorDailyStat> newStats) {
            this.stats = newStats;
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return stats.size();
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
            if (rowIndex >= stats.size()) return null;
            
            GitAuthorDailyStat stat = stats.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return stat.getDate().format(DateTimeFormatter.ISO_DATE);
                case 1: return stat.getAuthorName();
                case 2: return stat.getCommits();
                case 3: return stat.getAdditions();
                case 4: return stat.getDeletions();
                case 5: return stat.getNetChanges();
                default: return null;
            }
        }
    }
    
    /**
     * 数字表格单元格渲染器
     */
    /**
     * 创建邮件报告面板
     */
    private JComponent createEmailReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 创建滚动面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // SMTP 配置区域
        contentPanel.add(createSmtpConfigPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 定时发送配置区域
        contentPanel.add(createScheduleConfigPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 手动发送区域
        contentPanel.add(createManualSendPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 发送历史区域
        contentPanel.add(createEmailHistoryPanel());
        
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建 SMTP 配置面板
     */
    private JPanel createSmtpConfigPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("SMTP 配置"));
        
        // 邮箱服务选择下拉框
        JComboBox<SmtpPreset> emailServiceComboBox = new JComboBox<>(SmtpPreset.getPresets());
        JLabel serviceDescLabel = new JLabel(" ");
        
        // SMTP 字段
        JTextField smtpHostField = new JTextField(20);
        JTextField smtpPortField = new JTextField(5);
        JTextField senderEmailField = new JTextField(20);
        JPasswordField senderPasswordField = new JPasswordField(20);
        JTextField recipientEmailField = new JTextField(20);
        
        JCheckBox tlsCheckBox = new JCheckBox("启用 TLS", true);
        JCheckBox sslCheckBox = new JCheckBox("启用 SSL", false);
        
        // 添加邮箱服务选择
        panel.add(createLabeledField("邮箱服务:", emailServiceComboBox));
        
        // 添加说明标签
        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serviceDescLabel.setForeground(new Color(128, 128, 128));
        descPanel.add(new JLabel("   "));
        descPanel.add(serviceDescLabel);
        panel.add(descPanel);
        
        panel.add(Box.createVerticalStrut(5));
        
        panel.add(createLabeledField("SMTP服务器:", smtpHostField));
        panel.add(createLabeledField("端口:", smtpPortField));
        panel.add(createLabeledField("发送者邮箱:", senderEmailField));
        panel.add(createLabeledField("SMTP密码:", senderPasswordField));
        panel.add(createLabeledField("接收者邮箱:", recipientEmailField));
        
        JPanel checksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checksPanel.add(tlsCheckBox);
        checksPanel.add(sslCheckBox);
        panel.add(checksPanel);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton testButton = new JButton("测试连接");
        JButton saveButton = new JButton("保存配置");

        testButton.addActionListener(e -> {
            if (saveSmtpConfig(smtpHostField, smtpPortField, senderEmailField,
                              senderPasswordField, recipientEmailField, tlsCheckBox, sslCheckBox)) {
                // 在后台线程中执行网络连接测试
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    boolean success = emailService.testConnection();

                    // 在EDT线程中显示结果
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (success) {
                            Messages.showInfoMessage(project, "SMTP 连接测试成功！", "测试成功");
                        } else {
                            Messages.showErrorDialog(project, "SMTP 连接测试失败，请检查配置！", "测试失败");
                        }
                    });
                });
            }
        });

        saveButton.addActionListener(e -> {
            if (saveSmtpConfig(smtpHostField, smtpPortField, senderEmailField,
                              senderPasswordField, recipientEmailField, tlsCheckBox, sslCheckBox)) {
                Messages.showInfoMessage(project, "配置已保存！", "保存成功");
            }
        });

        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        panel.add(buttonPanel);

        // 先从配置加载初始值（在添加监听器之前）
        GitStatEmailConfig config = emailService.getConfig();
        smtpHostField.setText(config.getSmtpHost());
        smtpPortField.setText(String.valueOf(config.getSmtpPort()));
        senderEmailField.setText(config.getSenderEmail());
        senderPasswordField.setText(config.getSenderPassword());  // 反显密码
        recipientEmailField.setText(config.getRecipientEmail());
        tlsCheckBox.setSelected(config.isEnableTLS());
        sslCheckBox.setSelected(config.isEnableSSL());

        // 根据配置智能匹配邮箱服务（在添加监听器之前设置）
        SmtpPreset matchedPreset = findMatchingPreset(config);
        System.out.println("GitStatToolWindow: 匹配到的预设 = " + (matchedPreset != null ? matchedPreset.getName() : "null"));

        if (matchedPreset != null) {
            // 使用索引来选择，而不是对象匹配
            for (int i = 0; i < emailServiceComboBox.getItemCount(); i++) {
                SmtpPreset preset = emailServiceComboBox.getItemAt(i);
                if (preset.getName().equals(matchedPreset.getName())) {
                    System.out.println("  设置下拉框索引为: " + i + " (" + preset.getName() + ")");
                    emailServiceComboBox.setSelectedIndex(i);
                    serviceDescLabel.setText("💡 " + preset.getDescription());
                    break;
                }
            }
        } else {
            // 如果没有匹配的预设，选择"自定义"
            for (int i = 0; i < emailServiceComboBox.getItemCount(); i++) {
                SmtpPreset preset = emailServiceComboBox.getItemAt(i);
                if ("自定义".equals(preset.getName())) {
                    System.out.println("  没有匹配的预设，设置为自定义，索引: " + i);
                    emailServiceComboBox.setSelectedIndex(i);
                    serviceDescLabel.setText("💡 " + preset.getDescription());
                    break;
                }
            }
        }

        // 最后添加邮箱服务选择事件监听器（这样初始化时不会触发）
        emailServiceComboBox.addActionListener(e -> {
            SmtpPreset preset = (SmtpPreset) emailServiceComboBox.getSelectedItem();
            if (preset != null) {
                // 如果不是"自定义"，则自动填充配置
                if (!"自定义".equals(preset.getName())) {
                    smtpHostField.setText(preset.getSmtpHost());
                    smtpPortField.setText(String.valueOf(preset.getSmtpPort()));
                    tlsCheckBox.setSelected(preset.isEnableTLS());
                    sslCheckBox.setSelected(preset.isEnableSSL());
                }
                // 显示说明
                serviceDescLabel.setText("💡 " + preset.getDescription());
            }
        });

        return panel;
    }
    
    /**
     * 创建定时发送配置面板
     */
    private JPanel createScheduleConfigPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("定时发送"));
        
        JCheckBox enableScheduledCheckBox = new JCheckBox("启用每日定时发送", false);
        JTextField scheduledTimeField = new JTextField("18:00", 5);
        scheduleFilterAuthorComboBox = new JComboBox<>();
        JCheckBox includeTrendsCheckBox = new JCheckBox("包含趋势分析", true);
        
        // 填充作者列表
        scheduleFilterAuthorComboBox.addItem("(所有作者)");
        gitStatService.getAllAuthorStats().forEach(author -> 
            scheduleFilterAuthorComboBox.addItem(author.getAuthorName())
        );
        
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel1.add(enableScheduledCheckBox);
        panel.add(panel1);
        
        panel.add(createLabeledField("发送时间:", scheduledTimeField));
        panel.add(createLabeledField("筛选作者:", scheduleFilterAuthorComboBox));
        
        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel2.add(includeTrendsCheckBox);
        panel.add(panel2);
        
        JButton applyScheduleButton = new JButton("应用定时设置");
        applyScheduleButton.addActionListener(e -> {
            GitStatEmailConfig config = emailService.getConfig();
            config.setEnableScheduled(enableScheduledCheckBox.isSelected());
            config.setScheduledTime(scheduledTimeField.getText());
            
            String selectedAuthor = (String) scheduleFilterAuthorComboBox.getSelectedItem();
            config.setFilterAuthor("(所有作者)".equals(selectedAuthor) ? null : selectedAuthor);
            config.setIncludeTrends(includeTrendsCheckBox.isSelected());
            
            emailService.setConfig(config);
            saveEmailConfigState(config);
            
            Messages.showInfoMessage(project, 
                enableScheduledCheckBox.isSelected() ? 
                "定时任务已启动，将在每天 " + scheduledTimeField.getText() + " 发送邮件" :
                "定时任务已停止", 
                "设置成功");
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(applyScheduleButton);
        panel.add(buttonPanel);
        
        // 从配置加载初始值
        GitStatEmailConfig config = emailService.getConfig();
        enableScheduledCheckBox.setSelected(config.isEnableScheduled());
        scheduledTimeField.setText(config.getScheduledTime());
        includeTrendsCheckBox.setSelected(config.isIncludeTrends());
        // 注意：作者筛选的反显在 updateEmailAuthorComboBoxes() 中处理

        return panel;
    }
    
    /**
     * 创建手动发送面板
     */
    private JPanel createManualSendPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("手动发送"));
        
        // 作者选择下拉框
        manualFilterAuthorComboBox = new JComboBox<>();
        manualFilterAuthorComboBox.addItem("(所有作者)");
        gitStatService.getAllAuthorStats().forEach(author -> 
            manualFilterAuthorComboBox.addItem(author.getAuthorName())
        );
        
        panel.add(createLabeledField("选择作者:", manualFilterAuthorComboBox));
        panel.add(Box.createVerticalStrut(10));
        
        JButton sendTodayButton = new JButton("📧 发送今日统计");
        JButton sendYesterdayButton = new JButton("📧 发送昨日统计");
        
        sendTodayButton.addActionListener(e -> {
            // 临时设置筛选作者
            String selectedAuthor = (String) manualFilterAuthorComboBox.getSelectedItem();
            String originalFilter = emailService.getConfig().getFilterAuthor();

            // 显示进度提示
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showInfoMessage(project,
                    "正在后台发送邮件，请稍候...\n作者: " + selectedAuthor,
                    "发送中");
            });

            // 在后台线程中执行耗时操作
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    GitStatEmailConfig tempConfig = emailService.getConfig();
                    tempConfig.setFilterAuthor("(所有作者)".equals(selectedAuthor) ? null : selectedAuthor);
                    emailService.setConfig(tempConfig);

                    boolean success = emailService.sendTodayEmail();

                    // 在EDT线程中显示结果
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (success) {
                            Messages.showInfoMessage(project,
                                "今日统计邮件已发送！\n作者: " + selectedAuthor,
                                "发送成功");
                        } else {
                            Messages.showErrorDialog(project, "邮件发送失败，请检查配置或网络！", "发送失败");
                        }
                    });
                } finally {
                    // 恢复原始配置
                    GitStatEmailConfig restoreConfig = emailService.getConfig();
                    restoreConfig.setFilterAuthor(originalFilter);
                    emailService.setConfig(restoreConfig);
                }
            });
        });
        
        sendYesterdayButton.addActionListener(e -> {
            // 临时设置筛选作者
            String selectedAuthor = (String) manualFilterAuthorComboBox.getSelectedItem();
            String originalFilter = emailService.getConfig().getFilterAuthor();

            // 显示进度提示
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showInfoMessage(project,
                    "正在后台发送邮件，请稍候...\n作者: " + selectedAuthor,
                    "发送中");
            });

            // 在后台线程中执行耗时操作
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    GitStatEmailConfig tempConfig = emailService.getConfig();
                    tempConfig.setFilterAuthor("(所有作者)".equals(selectedAuthor) ? null : selectedAuthor);
                    emailService.setConfig(tempConfig);

                    boolean success = emailService.sendEmail(java.time.LocalDate.now().minusDays(1));

                    // 在EDT线程中显示结果
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (success) {
                            Messages.showInfoMessage(project,
                                "昨日统计邮件已发送！\n作者: " + selectedAuthor,
                                "发送成功");
                        } else {
                            Messages.showErrorDialog(project, "邮件发送失败，请检查配置或网络！", "发送失败");
                        }
                    });
                } finally {
                    // 恢复原始配置
                    GitStatEmailConfig restoreConfig = emailService.getConfig();
                    restoreConfig.setFilterAuthor(originalFilter);
                    emailService.setConfig(restoreConfig);
                }
            });
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(sendTodayButton);
        buttonPanel.add(sendYesterdayButton);
        panel.add(buttonPanel);
        
        return panel;
    }
    
    /**
     * 创建邮件历史面板
     */
    private JPanel createEmailHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("发送历史"));
        
        JTextArea historyArea = new JTextArea(10, 50);
        historyArea.setEditable(false);
        
        JButton refreshButton = new JButton("刷新历史");
        refreshButton.addActionListener(e -> {
            List<GitStatEmailRecord> history = emailService.getEmailHistory();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-20s %-15s %-8s %-6s %-6s\n", 
                     "发送时间", "接收者", "状态", "提交", "代码"));
            sb.append("─".repeat(70)).append("\n");
            
            for (GitStatEmailRecord record : history) {
                sb.append(String.format("%-20s %-15s %-8s %-6d +%-6d\n",
                    record.getSendTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    record.getRecipient(),
                    record.isSuccess() ? "✅成功" : "❌失败",
                    record.getCommits(),
                    record.getAdditions()
                ));
            }
            
            historyArea.setText(sb.toString());
        });
        
        panel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        panel.add(refreshButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建带标签的字段
     */
    private JPanel createLabeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(label));
        panel.add(field);
        return panel;
    }
    
    /**
     * 保存 SMTP 配置
     */
    private boolean saveSmtpConfig(JTextField smtpHostField, JTextField smtpPortField,
                                   JTextField senderEmailField, JPasswordField senderPasswordField,
                                   JTextField recipientEmailField, JCheckBox tlsCheckBox, 
                                   JCheckBox sslCheckBox) {
        try {
            GitStatEmailConfig config = emailService.getConfig();
            config.setSmtpHost(smtpHostField.getText().trim());
            config.setSmtpPort(Integer.parseInt(smtpPortField.getText().trim()));
            config.setSenderEmail(senderEmailField.getText().trim());
            config.setRecipientEmail(recipientEmailField.getText().trim());
            config.setEnableTLS(tlsCheckBox.isSelected());
            config.setEnableSSL(sslCheckBox.isSelected());
            
            // 加密密码
            String password = new String(senderPasswordField.getPassword());
            if (!password.isEmpty()) {
                config.setSenderPassword(PasswordEncryptor.encrypt(password, project));
            }
            
            emailService.setConfig(config);
            saveEmailConfigState(config);
            
            return true;
        } catch (NumberFormatException ex) {
            Messages.showErrorDialog(project, "端口号必须是数字！", "配置错误");
            return false;
        }
    }
    
    /**
     * 根据配置查找匹配的邮箱服务预设
     */
    private SmtpPreset findMatchingPreset(GitStatEmailConfig config) {
        SmtpPreset[] presets = SmtpPreset.getPresets();

        // 添加日志
        System.out.println("GitStatToolWindow.findMatchingPreset: 配置信息 - Host=" + config.getSmtpHost() +
                          ", Port=" + config.getSmtpPort() +
                          ", TLS=" + config.isEnableTLS() +
                          ", SSL=" + config.isEnableSSL());

        // 遍历所有预设，查找完全匹配的
        for (SmtpPreset preset : presets) {
            // 跳过"自定义"选项
            if ("自定义".equals(preset.getName())) {
                continue;
            }

            System.out.println("  检查预设: " + preset.getName() +
                              " - Host=" + preset.getSmtpHost() +
                              ", Port=" + preset.getSmtpPort() +
                              ", TLS=" + preset.isEnableTLS() +
                              ", SSL=" + preset.isEnableSSL());

            // 检查是否完全匹配
            if (preset.getSmtpHost().equals(config.getSmtpHost()) &&
                preset.getSmtpPort() == config.getSmtpPort() &&
                preset.isEnableTLS() == config.isEnableTLS() &&
                preset.isEnableSSL() == config.isEnableSSL()) {
                System.out.println("  找到匹配的预设: " + preset.getName());
                return preset;
            }
        }

        // 没有找到匹配的预设
        System.out.println("  没有找到匹配的预设");
        return null;
    }

    /**
     * 加载邮件配置
     */
    private void loadEmailConfig() {
        GitStatEmailConfigState state = GitStatEmailConfigState.getInstance(project);
        GitStatEmailConfig config = new GitStatEmailConfig();

        System.out.println("GitStatToolWindow.loadEmailConfig: 从持久化状态加载配置");
        System.out.println("  smtpHost=" + state.smtpHost);
        System.out.println("  smtpPort=" + state.smtpPort);
        System.out.println("  enableTLS=" + state.enableTLS);
        System.out.println("  enableSSL=" + state.enableSSL);
        System.out.println("  filterAuthor=" + state.filterAuthor);

        config.setSmtpHost(state.smtpHost);
        config.setSmtpPort(state.smtpPort);
        config.setEnableTLS(state.enableTLS);
        config.setEnableSSL(state.enableSSL);
        config.setSenderEmail(state.senderEmail);

        // 解密密码（state 中存储的是加密后的密码）
        // 新版本的 PasswordEncryptor 会自动处理加密/明文密码，不会抛出异常
        if (state.senderPassword != null && !state.senderPassword.isEmpty()) {
            String decryptedPassword = PasswordEncryptor.decrypt(state.senderPassword, project);
            config.setSenderPassword(decryptedPassword);

            // 检查密码是否已加密，如果是明文则提示用户重新保存
            if (!PasswordEncryptor.isEncrypted(state.senderPassword)) {
                System.out.println("  警告：密码未加密，建议重新保存配置以加密密码");
            } else {
                System.out.println("  密码解密成功");
            }
        } else {
            config.setSenderPassword("");
            System.out.println("  密码为空");
        }

        config.setSenderName(state.senderName);
        config.setRecipientEmail(state.recipientEmail);
        config.setEnableScheduled(state.enableScheduled);
        config.setScheduledTime(state.scheduledTime);
        config.setFilterAuthor(state.filterAuthor.isEmpty() ? null : state.filterAuthor);
        config.setIncludeTrends(state.includeTrends);
        config.setSendHtml(state.sendHtml);
        config.setEmailSubject(state.emailSubject);

        System.out.println("  加载后 config.getFilterAuthor()=" + config.getFilterAuthor());

        emailService.setConfig(config);
    }
    
    /**
     * 保存邮件配置状态
     */
    private void saveEmailConfigState(GitStatEmailConfig config) {
        GitStatEmailConfigState state = GitStatEmailConfigState.getInstance(project);
        
        state.smtpHost = config.getSmtpHost();
        state.smtpPort = config.getSmtpPort();
        state.enableTLS = config.isEnableTLS();
        state.enableSSL = config.isEnableSSL();
        state.senderEmail = config.getSenderEmail();
        state.senderPassword = config.getSenderPassword();
        state.senderName = config.getSenderName();
        state.recipientEmail = config.getRecipientEmail();
        state.enableScheduled = config.isEnableScheduled();
        state.scheduledTime = config.getScheduledTime();
        state.filterAuthor = config.getFilterAuthor() == null ? "" : config.getFilterAuthor();
        state.includeTrends = config.isIncludeTrends();
        state.sendHtml = config.isSendHtml();
        state.emailSubject = config.getEmailSubject();
    }
    
    private static class NumberTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // 数字列右对齐
            if (value instanceof Integer) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                // 格式化数字（添加千分位分隔符）
                setText(String.format("%,d", (Integer) value));
                
                // 负数显示为红色
                if ((Integer) value < 0 && !isSelected) {
                    component.setForeground(JBColor.RED);
                } else if ((Integer) value > 0 && !isSelected) {
                    component.setForeground(new Color(73, 204, 144)); // 绿色
                }
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            
            return component;
        }
    }
    
    /**
     * 创建 AI 代码统计面板
     */
    private JComponent createAiStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.3);
        
        // 上半部分：整体统计和AI工具排行
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        
        // 整体AI统计
        aiOverviewArea = new JTextArea();
        aiOverviewArea.setEditable(false);
        aiOverviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JBScrollPane overviewScrollPane = new JBScrollPane(aiOverviewArea);
        overviewScrollPane.setBorder(BorderFactory.createTitledBorder("AI 代码统计概览"));
        
        topPanel.add(overviewScrollPane, BorderLayout.CENTER);
        
        // 下半部分：作者AI使用统计表格
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("作者 AI 使用统计"));
        
        // 创建表格
        aiStatsTableModel = new AiStatsTableModel();
        aiStatsTable = new JBTable(aiStatsTableModel);
        aiStatsTable.setAutoCreateRowSorter(true);
        
        // 设置列宽
        aiStatsTable.getColumnModel().getColumn(0).setPreferredWidth(120); // 作者姓名
        aiStatsTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 总提交
        aiStatsTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // AI提交
        aiStatsTable.getColumnModel().getColumn(3).setPreferredWidth(100); // AI提交占比
        aiStatsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // AI代码行数
        aiStatsTable.getColumnModel().getColumn(5).setPreferredWidth(100); // AI代码占比
        aiStatsTable.getColumnModel().getColumn(6).setPreferredWidth(120); // 主要AI工具
        
        // 自定义渲染器：高亮高AI使用率
        aiStatsTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (value instanceof String) {
                    String percentStr = (String) value;
                    try {
                        double percent = Double.parseDouble(percentStr.replace("%", ""));
                        if (!isSelected) {
                            if (percent >= 70) {
                                c.setBackground(new JBColor(new Color(200, 230, 255), new Color(40, 60, 90)));
                            } else if (percent >= 50) {
                                c.setBackground(new JBColor(new Color(230, 240, 255), new Color(50, 60, 80)));
                            } else {
                                c.setBackground(table.getBackground());
                            }
                        }
                    } catch (Exception e) {
                        c.setBackground(table.getBackground());
                    }
                }
                
                return c;
            }
        });
        
        JBScrollPane scrollPane = new JBScrollPane(aiStatsTable);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加到分割面板
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 更新 AI 统计数据
     */
    private void updateAiStats() {
        try {
            // 更新整体AI统计
            Map<String, Object> aiStats = gitStatService.getAiStatService().getOverallAiStatistics();
            StringBuilder sb = new StringBuilder();
            
            sb.append("=== AI 代码统计概览 ===\n\n");
            
            int totalCommits = (Integer) aiStats.get("totalCommits");
            if (totalCommits == 0) {
                sb.append("暂无 AI 统计数据。\n\n");
                sb.append("提示：\n");
                sb.append("1. 使用 AI 工具（Copilot、Cursor 等）编写代码\n");
                sb.append("2. 在 commit message 中添加 [AI] 或 [Copilot] 等标记\n");
                sb.append("3. 刷新统计数据\n");
            } else {
                sb.append("📊 提交统计\n");
                sb.append("  • 总提交次数: ").append(formatNumber(totalCommits)).append("\n");
                sb.append("  • AI 辅助提交: ").append(formatNumber((Integer) aiStats.get("totalAiCommits")))
                  .append(" (").append(String.format("%.1f%%", aiStats.get("aiCommitPercentage"))).append(")\n");
                sb.append("  • 纯人工提交: ").append(formatNumber((Integer) aiStats.get("totalManualCommits")))
                  .append(" (").append(String.format("%.1f%%", 
                      100 - (Double) aiStats.get("aiCommitPercentage"))).append(")\n\n");
                
                sb.append("📝 代码统计\n");
                sb.append("  • 总代码行数: ").append(formatNumber((Integer) aiStats.get("totalAdditions"))).append("\n");
                sb.append("  • AI 生成代码: ").append(formatNumber((Integer) aiStats.get("totalAiAdditions")))
                  .append(" (").append(String.format("%.1f%%", aiStats.get("aiCodePercentage"))).append(")\n");
                sb.append("  • 人工编写代码: ").append(formatNumber((Integer) aiStats.get("totalManualAdditions")))
                  .append(" (").append(String.format("%.1f%%", 
                      100 - (Double) aiStats.get("aiCodePercentage"))).append(")\n\n");
                
                sb.append("👥 团队统计\n");
                sb.append("  • 使用 AI 的开发者: ").append(aiStats.get("aiUserCount")).append(" 人\n\n");
                
                // AI 工具使用排行
                @SuppressWarnings("unchecked")
                Map<String, Integer> toolUsage = (Map<String, Integer>) aiStats.get("aiToolUsage");
                if (toolUsage != null && !toolUsage.isEmpty()) {
                    sb.append("🔧 AI 工具使用排行\n");
                    toolUsage.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .forEach(entry -> {
                            int usageCount = entry.getValue();
                            double percentage = totalCommits > 0 ? (double) usageCount / totalCommits * 100 : 0;
                            sb.append(String.format("  • %s: %d 次 (%.1f%%)  %s\n", 
                                entry.getKey(), 
                                usageCount,
                                percentage,
                                createProgressBar(percentage, 30)));
                        });
                }
            }
            
            aiOverviewArea.setText(sb.toString());
            aiOverviewArea.setCaretPosition(0);
            
            // 更新作者AI统计表格
            aiStatsTableModel.setData(gitStatService.getAiStatService().getAllAuthorAiStats());
            
        } catch (Exception e) {
            aiOverviewArea.setText("AI 统计功能未启用或数据加载失败。\n\n" + 
                "错误信息: " + e.getMessage());
        }
    }
    
    /**
     * 创建进度条字符串
     */
    private String createProgressBar(double percentage, int length) {
        int filled = (int) (percentage / 100.0 * length);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        return bar.toString();
    }
    
    /**
     * AI 统计表格模型
     */
    private class AiStatsTableModel extends AbstractTableModel {
        private final String[] columnNames = {
            "作者姓名", "总提交", "AI提交", "AI提交占比", 
            "AI代码行数", "AI代码占比", "主要AI工具"
        };
        
        private List<com.shuyixiao.gitstat.model.GitAuthorAiStat> data = new ArrayList<>();
        
        public void setData(List<com.shuyixiao.gitstat.model.GitAuthorAiStat> data) {
            this.data = data;
            // 按 AI 代码占比降序排序
            this.data.sort((a, b) -> Double.compare(b.getAiCodePercentage(), a.getAiCodePercentage()));
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return data.size();
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
        public Object getValueAt(int row, int column) {
            com.shuyixiao.gitstat.model.GitAuthorAiStat stat = data.get(row);
            
            switch (column) {
                case 0: return stat.getAuthorName();
                case 1: return stat.getTotalCommits();
                case 2: return stat.getAiCommits();
                case 3: return String.format("%.1f%%", stat.getAiCommitPercentage());
                case 4: return formatNumber(stat.getTotalAiAdditions());
                case 5: return String.format("%.1f%%", stat.getAiCodePercentage());
                case 6: return getMostUsedAiTool(stat);
                default: return "";
            }
        }
        
        private String getMostUsedAiTool(com.shuyixiao.gitstat.model.GitAuthorAiStat stat) {
            String tool = stat.getMostUsedAiTool();
            return tool != null ? tool : "-";
        }
    }

    /**
     * 创建工作周报面板
     */
    private JComponent createWeeklyReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        // 创建主分割面板（上下分割）
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.3);

        // 上半部分：配置区域
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.setBorder(JBUI.Borders.empty(5));

        // 配置表单
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // 周选择区域
        JPanel weekPanel = new JPanel(new BorderLayout(5, 0));
        JBLabel weekLabel = new JBLabel("选择周:");
        weekLabel.setPreferredSize(new Dimension(80, 25));
        weekPanel.add(weekLabel, BorderLayout.WEST);

        JPanel weekInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // 日期显示标签
        weekStartDateField = new JTextField(12);
        // 默认设置为本周一
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.with(DayOfWeek.SUNDAY);
        weekStartDateField.setText(weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        weekStartDateField.setEditable(false);
        weekStartDateField.setToolTipText("当前选中周的开始日期");
        weekInputPanel.add(weekStartDateField);

        // 日历选择按钮
        JButton calendarButton = new JButton("📅 选择");
        calendarButton.setToolTipText("打开日历选择周");
        calendarButton.addActionListener(e -> {
            try {
                LocalDate currentDate = LocalDate.parse(weekStartDateField.getText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                WeekPickerDialog dialog = new WeekPickerDialog(this, currentDate);
                if (dialog.showAndGet()) {
                    LocalDate selectedStart = dialog.getSelectedWeekStart();
                    LocalDate selectedEnd = dialog.getSelectedWeekEnd();
                    weekStartDateField.setText(selectedStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }
            } catch (Exception ex) {
                LocalDate now = LocalDate.now();
                LocalDate monday = now.with(DayOfWeek.MONDAY);
                weekStartDateField.setText(monday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        });
        weekInputPanel.add(calendarButton);

        JButton thisWeekButton = new JButton("本周");
        thisWeekButton.setToolTipText("快速选择本周");
        thisWeekButton.addActionListener(e -> {
            LocalDate now = LocalDate.now();
            LocalDate monday = now.with(DayOfWeek.MONDAY);
            weekStartDateField.setText(monday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        });
        weekInputPanel.add(thisWeekButton);

        JButton lastWeekButton = new JButton("上周");
        lastWeekButton.setToolTipText("快速选择上周");
        lastWeekButton.addActionListener(e -> {
            LocalDate now = LocalDate.now();
            LocalDate lastMonday = now.with(DayOfWeek.MONDAY).minusWeeks(1);
            weekStartDateField.setText(lastMonday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        });
        weekInputPanel.add(lastWeekButton);

        weekPanel.add(weekInputPanel, BorderLayout.CENTER);
        formPanel.add(weekPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // 作者筛选
        JPanel authorPanel = new JPanel(new BorderLayout(5, 0));
        JBLabel authorLabel = new JBLabel("选择作者:");
        authorLabel.setPreferredSize(new Dimension(80, 25));
        authorPanel.add(authorLabel, BorderLayout.WEST);
        weeklyAuthorComboBox = new JComboBox<>();
        weeklyAuthorComboBox.addItem("全部作者");
        authorPanel.add(weeklyAuthorComboBox, BorderLayout.CENTER);
        JButton refreshAuthorsButton = new JButton("刷新作者列表");
        refreshAuthorsButton.addActionListener(e -> refreshAuthorList());
        authorPanel.add(refreshAuthorsButton, BorderLayout.EAST);
        formPanel.add(authorPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // 分隔线
        formPanel.add(new JSeparator());
        formPanel.add(Box.createVerticalStrut(10));

        // API URL
        JPanel apiUrlPanel = new JPanel(new BorderLayout(5, 0));
        JBLabel apiUrlLabel = new JBLabel("API 地址:");
        apiUrlLabel.setPreferredSize(new Dimension(80, 25));
        apiUrlPanel.add(apiUrlLabel, BorderLayout.WEST);
        apiUrlField = new JTextField();
        apiUrlPanel.add(apiUrlField, BorderLayout.CENTER);
        formPanel.add(apiUrlPanel);
        formPanel.add(Box.createVerticalStrut(5));

        // API Key
        JPanel apiKeyPanel = new JPanel(new BorderLayout(5, 0));
        JBLabel apiKeyLabel = new JBLabel("API 密钥:");
        apiKeyLabel.setPreferredSize(new Dimension(80, 25));
        apiKeyPanel.add(apiKeyLabel, BorderLayout.WEST);
        apiKeyField = new JPasswordField();
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);
        formPanel.add(apiKeyPanel);
        formPanel.add(Box.createVerticalStrut(5));

        // Model
        JPanel modelPanel = new JPanel(new BorderLayout(5, 0));
        JBLabel modelLabel = new JBLabel("模型名称:");
        modelLabel.setPreferredSize(new Dimension(80, 25));
        modelPanel.add(modelLabel, BorderLayout.WEST);
        modelField = new JTextField();
        modelPanel.add(modelField, BorderLayout.CENTER);
        formPanel.add(modelPanel);
        formPanel.add(Box.createVerticalStrut(5));

        // Prompt Template
        JPanel promptPanel = new JPanel(new BorderLayout(5, 0));
        promptPanel.add(new JBLabel("提示词模板 (使用 {commits} 作为占位符):"), BorderLayout.NORTH);
        promptTemplateArea = new JTextArea(10, 40);
        promptTemplateArea.setLineWrap(true);
        promptTemplateArea.setWrapStyleWord(true);
        JBScrollPane promptScrollPane = new JBScrollPane(promptTemplateArea);
        promptPanel.add(promptScrollPane, BorderLayout.CENTER);
        formPanel.add(promptPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton saveConfigButton = new JButton("保存配置");
        saveConfigButton.addActionListener(e -> saveWeeklyReportConfig());
        buttonPanel.add(saveConfigButton);

        JButton loadCommitsButton = new JButton("加载提交");
        loadCommitsButton.setToolTipText("加载选中周的提交记录");
        loadCommitsButton.addActionListener(e -> loadWeeklyCommits());
        buttonPanel.add(loadCommitsButton);

        generateReportButton = new JButton("生成周报");
        generateReportButton.addActionListener(e -> generateWeeklyReport());
        buttonPanel.add(generateReportButton);

        JButton copyReportButton = new JButton("复制周报");
        copyReportButton.addActionListener(e -> copyWeeklyReport());
        buttonPanel.add(copyReportButton);

        formPanel.add(buttonPanel);

        configPanel.add(formPanel, BorderLayout.NORTH);

        // 下半部分：显示区域（左右分割）
        JSplitPane displaySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        displaySplitPane.setResizeWeight(0.4);

        // 左侧：提交日志
        JPanel commitsPanel = new JPanel(new BorderLayout());
        commitsPanel.setBorder(JBUI.Borders.empty(5));

        // 创建标题面板，包含标签和复制按钮
        JPanel commitsTitlePanel = new JPanel(new BorderLayout());
        commitsLabel = new JLabel("提交日志:");
        commitsTitlePanel.add(commitsLabel, BorderLayout.WEST);

        JButton copyCommitsButton = new JButton("复制提交日志");
        copyCommitsButton.setToolTipText("复制提交日志到剪贴板");
        copyCommitsButton.addActionListener(e -> copyCommitsLog());
        commitsTitlePanel.add(copyCommitsButton, BorderLayout.EAST);

        commitsPanel.add(commitsTitlePanel, BorderLayout.NORTH);

        weeklyCommitsArea = new JTextArea();
        weeklyCommitsArea.setEditable(false);
        weeklyCommitsArea.setLineWrap(true);
        weeklyCommitsArea.setWrapStyleWord(true);
        JBScrollPane commitsScrollPane = new JBScrollPane(weeklyCommitsArea);
        commitsPanel.add(commitsScrollPane, BorderLayout.CENTER);

        // 右侧：生成的周报
        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBorder(JBUI.Borders.empty(5));
        reportPanel.add(new JBLabel("生成的周报:"), BorderLayout.NORTH);
        weeklyReportArea = new JTextArea();
        weeklyReportArea.setEditable(false);
        weeklyReportArea.setLineWrap(true);
        weeklyReportArea.setWrapStyleWord(true);
        JBScrollPane reportScrollPane = new JBScrollPane(weeklyReportArea);
        reportPanel.add(reportScrollPane, BorderLayout.CENTER);

        displaySplitPane.setLeftComponent(commitsPanel);
        displaySplitPane.setRightComponent(reportPanel);

        mainSplitPane.setTopComponent(configPanel);
        mainSplitPane.setBottomComponent(displaySplitPane);

        panel.add(mainSplitPane, BorderLayout.CENTER);

        // 加载配置
        loadWeeklyReportConfig();

        // 初始化作者列表
        refreshAuthorList();

        return panel;
    }

    /**
     * 刷新作者列表
     */
    private void refreshAuthorList() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<String> authors = weeklyReportService.getAllAuthors();

            ApplicationManager.getApplication().invokeLater(() -> {
                weeklyAuthorComboBox.removeAllItems();
                weeklyAuthorComboBox.addItem("全部作者");
                for (String author : authors) {
                    weeklyAuthorComboBox.addItem(author);
                }
            });
        });
    }

    /**
     * 加载周报配置
     */
    private void loadWeeklyReportConfig() {
        WeeklyReportConfigState configState = WeeklyReportConfigState.getInstance(project);
        WeeklyReportConfig config = configState.toConfig();

        apiUrlField.setText(config.getApiUrl());
        apiKeyField.setText(config.getApiKey());
        modelField.setText(config.getModel());
        promptTemplateArea.setText(config.getPromptTemplate());
    }

    /**
     * 保存周报配置
     */
    private void saveWeeklyReportConfig() {
        WeeklyReportConfig config = new WeeklyReportConfig();
        config.setApiUrl(apiUrlField.getText());
        config.setApiKey(new String(apiKeyField.getPassword()));
        config.setModel(modelField.getText());
        config.setPromptTemplate(promptTemplateArea.getText());

        WeeklyReportConfigState configState = WeeklyReportConfigState.getInstance(project);
        configState.fromConfig(config);

        EnhancedNotificationUtil.showSimpleInfo(project, "✅ 保存成功", "周报配置已保存");
    }

    /**
     * 加载提交日志（支持自定义周）
     */
    private void loadWeeklyCommits() {
        // 解析日期
        String dateText = weekStartDateField.getText().trim();
        LocalDate startDate;
        LocalDate endDate;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            startDate = LocalDate.parse(dateText, formatter);

            // 确保是周一
            if (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
                EnhancedNotificationUtil.showWarning(project, "⚠️ 提示",
                    "请输入周一的日期（当前输入的是" + startDate.getDayOfWeek().toString() + "）", null);
                return;
            }

            // 计算周日
            endDate = startDate.with(DayOfWeek.SUNDAY);

        } catch (Exception e) {
            EnhancedNotificationUtil.showWarning(project, "⚠️ 提示",
                "日期格式错误，请使用 yyyy-MM-dd 格式（例如：2024-01-01）", null);
            return;
        }

        // 更新标签
        String dateRange = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                          " 至 " +
                          endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        commitsLabel.setText("提交日志 (" + dateRange + "):");

        weeklyCommitsArea.setText("正在加载提交日志...");

        // 获取选中的作者
        String selectedAuthor = (String) weeklyAuthorComboBox.getSelectedItem();
        String authorFilter = null;
        if (selectedAuthor != null && !"全部作者".equals(selectedAuthor)) {
            authorFilter = selectedAuthor;
        }

        final String finalAuthorFilter = authorFilter;
        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String commits = weeklyReportService.getCommitsByDateRange(finalStartDate, finalEndDate, finalAuthorFilter);

            ApplicationManager.getApplication().invokeLater(() -> {
                weeklyCommitsArea.setText(commits);
                weeklyCommitsArea.setCaretPosition(0);
            });
        });
    }

    /**
     * 生成周报
     */
    private void generateWeeklyReport() {
        String commits = weeklyCommitsArea.getText();
        if (commits == null || commits.trim().isEmpty()) {
            EnhancedNotificationUtil.showWarning(project, "⚠️ 提示", "请先加载本周提交日志", null);
            return;
        }

        WeeklyReportConfigState configState = WeeklyReportConfigState.getInstance(project);
        WeeklyReportConfig config = configState.toConfig();

        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            EnhancedNotificationUtil.showWarning(project, "⚠️ 提示", "请先配置 API 密钥", null);
            return;
        }

        weeklyReportArea.setText("正在生成周报，请稍候...\n");
        generateReportButton.setEnabled(false);

        weeklyReportService.generateWeeklyReport(
            config,
            commits,
            // onChunk: 接收流式数据
            chunk -> ApplicationManager.getApplication().invokeLater(() -> {
                weeklyReportArea.append(chunk);
            }),
            // onComplete: 完成
            () -> ApplicationManager.getApplication().invokeLater(() -> {
                generateReportButton.setEnabled(true);
                EnhancedNotificationUtil.showSimpleInfo(project, "✅ 生成成功", "周报生成完成");
            }),
            // onError: 错误
            error -> ApplicationManager.getApplication().invokeLater(() -> {
                generateReportButton.setEnabled(true);
                weeklyReportArea.setText("生成失败: " + error);
                EnhancedNotificationUtil.showEnhancedError(project, "❌ 生成失败", "周报生成失败", error, null);
            })
        );
    }

    /**
     * 复制周报到剪贴板
     */
    private void copyWeeklyReport() {
        String report = weeklyReportArea.getText();
        if (report == null || report.trim().isEmpty()) {
            EnhancedNotificationUtil.showWarning(project, "⚠️ 提示", "周报内容为空", null);
            return;
        }

        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(report);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

        EnhancedNotificationUtil.showCopySuccess(project, "周报已复制到剪贴板");
    }

    /**
     * 复制提交日志到剪贴板
     */
    private void copyCommitsLog() {
        String commits = weeklyCommitsArea.getText();
        if (commits == null || commits.trim().isEmpty()) {
            EnhancedNotificationUtil.showWarning(project, "⚠️ 提示", "提交日志内容为空", null);
            return;
        }

        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(commits);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

        EnhancedNotificationUtil.showCopySuccess(project, "提交日志已复制到剪贴板");
    }
}

