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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
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
    
    // 状态标签
    private JLabel statusLabel;
    
    public GitStatToolWindow(@NotNull Project project) {
        this.project = project;
        this.gitStatService = project.getService(GitStatService.class);
        this.emailService = project.getService(GitStatEmailService.class);
        
        initializeUI();
        setupEventHandlers();
        refreshData();
        loadEmailConfig();
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
        tabbedPane.addTab("📧 邮件报告", createEmailReportPanel());
        
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
        refreshButton.addActionListener(e -> refreshData());
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
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 显示加载提示
                ApplicationManager.getApplication().invokeLater(() -> {
                    statusLabel.setText("正在加载 Git 统计数据...");
                    statusLabel.setForeground(JBColor.BLUE);
                });
                
                // 刷新统计数据
                gitStatService.refreshStatistics();
                
                // 更新 UI
                ApplicationManager.getApplication().invokeLater(() -> {
                    updateAuthorSelectionComboBox();
                    updateAuthorTable();
                    updateDailyTable();
                    updateAuthorDailyTable();
                    updateProjectStatsArea();
                    updateOverviewArea();
                    updateStatusLabel();
                    
                    Messages.showInfoMessage(project, "Git 统计数据已刷新", "刷新成功");
                });
                
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(project, "刷新数据失败: " + e.getMessage(), "错误");
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
            Messages.showInfoMessage(project, "统计信息已复制到剪贴板", "导出成功");
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
        
        // 邮箱服务选择事件
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
        
        // 初始化时触发一次
        if (emailServiceComboBox.getSelectedItem() != null) {
            SmtpPreset initialPreset = (SmtpPreset) emailServiceComboBox.getSelectedItem();
            serviceDescLabel.setText("💡 " + initialPreset.getDescription());
        }
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton testButton = new JButton("测试连接");
        JButton saveButton = new JButton("保存配置");
        
        testButton.addActionListener(e -> {
            if (saveSmtpConfig(smtpHostField, smtpPortField, senderEmailField, 
                              senderPasswordField, recipientEmailField, tlsCheckBox, sslCheckBox)) {
                if (emailService.testConnection()) {
                    Messages.showInfoMessage(project, "SMTP 连接测试成功！", "测试成功");
                } else {
                    Messages.showErrorDialog(project, "SMTP 连接测试失败，请检查配置！", "测试失败");
                }
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
        
        // 从配置加载初始值
        GitStatEmailConfig config = emailService.getConfig();
        smtpHostField.setText(config.getSmtpHost());
        smtpPortField.setText(String.valueOf(config.getSmtpPort()));
        senderEmailField.setText(config.getSenderEmail());
        recipientEmailField.setText(config.getRecipientEmail());
        tlsCheckBox.setSelected(config.isEnableTLS());
        sslCheckBox.setSelected(config.isEnableSSL());
        
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
        JComboBox<String> filterAuthorComboBox = new JComboBox<>();
        JCheckBox includeTrendsCheckBox = new JCheckBox("包含趋势分析", true);
        
        // 填充作者列表
        filterAuthorComboBox.addItem("(所有作者)");
        gitStatService.getAllAuthorStats().forEach(author -> 
            filterAuthorComboBox.addItem(author.getAuthorName())
        );
        
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel1.add(enableScheduledCheckBox);
        panel.add(panel1);
        
        panel.add(createLabeledField("发送时间:", scheduledTimeField));
        panel.add(createLabeledField("筛选作者:", filterAuthorComboBox));
        
        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel2.add(includeTrendsCheckBox);
        panel.add(panel2);
        
        JButton applyScheduleButton = new JButton("应用定时设置");
        applyScheduleButton.addActionListener(e -> {
            GitStatEmailConfig config = emailService.getConfig();
            config.setEnableScheduled(enableScheduledCheckBox.isSelected());
            config.setScheduledTime(scheduledTimeField.getText());
            
            String selectedAuthor = (String) filterAuthorComboBox.getSelectedItem();
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
        if (config.getFilterAuthor() != null) {
            filterAuthorComboBox.setSelectedItem(config.getFilterAuthor());
        }
        
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
        JComboBox<String> manualAuthorComboBox = new JComboBox<>();
        manualAuthorComboBox.addItem("(所有作者)");
        gitStatService.getAllAuthorStats().forEach(author -> 
            manualAuthorComboBox.addItem(author.getAuthorName())
        );
        
        panel.add(createLabeledField("选择作者:", manualAuthorComboBox));
        panel.add(Box.createVerticalStrut(10));
        
        JButton sendTodayButton = new JButton("📧 发送今日统计");
        JButton sendYesterdayButton = new JButton("📧 发送昨日统计");
        
        sendTodayButton.addActionListener(e -> {
            // 临时设置筛选作者
            String selectedAuthor = (String) manualAuthorComboBox.getSelectedItem();
            String originalFilter = emailService.getConfig().getFilterAuthor();
            
            try {
                GitStatEmailConfig tempConfig = emailService.getConfig();
                tempConfig.setFilterAuthor("(所有作者)".equals(selectedAuthor) ? null : selectedAuthor);
                emailService.setConfig(tempConfig);
                
                if (emailService.sendTodayEmail()) {
                    Messages.showInfoMessage(project, 
                        "今日统计邮件已发送！\n作者: " + selectedAuthor, 
                        "发送成功");
                } else {
                    Messages.showErrorDialog(project, "邮件发送失败，请检查配置或网络！", "发送失败");
                }
            } finally {
                // 恢复原始配置
                GitStatEmailConfig restoreConfig = emailService.getConfig();
                restoreConfig.setFilterAuthor(originalFilter);
                emailService.setConfig(restoreConfig);
            }
        });
        
        sendYesterdayButton.addActionListener(e -> {
            // 临时设置筛选作者
            String selectedAuthor = (String) manualAuthorComboBox.getSelectedItem();
            String originalFilter = emailService.getConfig().getFilterAuthor();
            
            try {
                GitStatEmailConfig tempConfig = emailService.getConfig();
                tempConfig.setFilterAuthor("(所有作者)".equals(selectedAuthor) ? null : selectedAuthor);
                emailService.setConfig(tempConfig);
                
                if (emailService.sendEmail(java.time.LocalDate.now().minusDays(1))) {
                    Messages.showInfoMessage(project, 
                        "昨日统计邮件已发送！\n作者: " + selectedAuthor, 
                        "发送成功");
                } else {
                    Messages.showErrorDialog(project, "邮件发送失败，请检查配置或网络！", "发送失败");
                }
            } finally {
                // 恢复原始配置
                GitStatEmailConfig restoreConfig = emailService.getConfig();
                restoreConfig.setFilterAuthor(originalFilter);
                emailService.setConfig(restoreConfig);
            }
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
     * 加载邮件配置
     */
    private void loadEmailConfig() {
        GitStatEmailConfigState state = GitStatEmailConfigState.getInstance(project);
        GitStatEmailConfig config = new GitStatEmailConfig();
        
        config.setSmtpHost(state.smtpHost);
        config.setSmtpPort(state.smtpPort);
        config.setEnableTLS(state.enableTLS);
        config.setEnableSSL(state.enableSSL);
        config.setSenderEmail(state.senderEmail);
        config.setSenderPassword(state.senderPassword);
        config.setSenderName(state.senderName);
        config.setRecipientEmail(state.recipientEmail);
        config.setEnableScheduled(state.enableScheduled);
        config.setScheduledTime(state.scheduledTime);
        config.setFilterAuthor(state.filterAuthor.isEmpty() ? null : state.filterAuthor);
        config.setIncludeTrends(state.includeTrends);
        config.setSendHtml(state.sendHtml);
        config.setEmailSubject(state.emailSubject);
        
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
}

