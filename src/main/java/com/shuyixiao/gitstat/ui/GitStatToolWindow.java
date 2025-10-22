package com.shuyixiao.gitstat.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Git 统计工具窗口
 * 在 IntelliJ IDEA 中提供独立的工具窗口，用于展示 Git 统计信息
 */
public class GitStatToolWindow extends JPanel {
    
    private final Project project;
    private final GitStatService gitStatService;
    
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
        
        initializeUI();
        setupEventHandlers();
        refreshData();
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
                stats = gitStatService.getAuthorDailyStatsByAuthorName(selectedAuthor)
                        .stream()
                        .sorted(Comparator.comparing(GitAuthorDailyStat::getDate))
                        .collect(java.util.stream.Collectors.toList());
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

