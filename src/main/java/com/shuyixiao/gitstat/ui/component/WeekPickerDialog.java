package com.shuyixiao.gitstat.ui.component;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 周选择器对话框
 * 提供可视化的日历界面，用于选择某一周
 */
public class WeekPickerDialog extends DialogWrapper {
    
    private LocalDate selectedWeekStart;
    private JPanel calendarPanel;
    private JLabel monthLabel;
    private YearMonth currentMonth;
    
    public WeekPickerDialog(Component parent, LocalDate initialDate) {
        super(parent, false);
        this.currentMonth = YearMonth.from(initialDate);
        this.selectedWeekStart = initialDate.with(DayOfWeek.MONDAY);
        setTitle("选择周");
        init();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setPreferredSize(new Dimension(400, 350));
        
        // 顶部：月份导航
        JPanel headerPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("◀");
        prevButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });
        
        JButton nextButton = new JButton("▶");
        nextButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });
        
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 16f));
        
        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // 中间：日历
        calendarPanel = new JPanel(new GridLayout(0, 7, 2, 2));
        mainPanel.add(calendarPanel, BorderLayout.CENTER);
        
        // 底部：选中信息
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel infoLabel = new JLabel("点击任意日期选择该日期所在的周");
        infoLabel.setForeground(JBColor.GRAY);
        infoPanel.add(infoLabel);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        updateCalendar();
        
        return mainPanel;
    }
    
    private void updateCalendar() {
        calendarPanel.removeAll();
        
        // 更新月份标签
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月", Locale.CHINA);
        monthLabel.setText(currentMonth.format(formatter));
        
        // 添加星期标题
        String[] weekDays = {"一", "二", "三", "四", "五", "六", "日"};
        for (String day : weekDays) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setForeground(JBColor.GRAY);
            calendarPanel.add(label);
        }
        
        // 获取当月第一天
        LocalDate firstDay = currentMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        
        // 添加空白占位
        for (int i = 1; i < firstDayOfWeek; i++) {
            calendarPanel.add(new JLabel(""));
        }
        
        // 添加日期按钮
        int daysInMonth = currentMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            JButton dayButton = createDayButton(date);
            calendarPanel.add(dayButton);
        }
        
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }
    
    private JButton createDayButton(LocalDate date) {
        JButton button = new JButton(String.valueOf(date.getDayOfMonth()));
        button.setPreferredSize(new Dimension(50, 40));
        
        // 计算该日期所在周的周一
        LocalDate weekMonday = date.with(DayOfWeek.MONDAY);
        
        // 判断是否是选中的周
        boolean isSelectedWeek = weekMonday.equals(selectedWeekStart);
        
        // 设置样式
        if (isSelectedWeek) {
            button.setBackground(new JBColor(new Color(100, 150, 255), new Color(70, 100, 180)));
            button.setForeground(Color.WHITE);
            button.setOpaque(true);
        }
        
        // 今天的日期高亮
        if (date.equals(LocalDate.now())) {
            button.setBorder(BorderFactory.createLineBorder(new JBColor(Color.BLUE, Color.CYAN), 2));
        }

        // 点击事件
        button.addActionListener(e -> {
            selectedWeekStart = date.with(DayOfWeek.MONDAY);
            updateCalendar();
        });

        // 鼠标悬停提示
        LocalDate weekEnd = weekMonday.with(DayOfWeek.SUNDAY);
        DateTimeFormatter tipFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        button.setToolTipText(String.format("选择这一周 (%s 至 %s)",
            weekMonday.format(tipFormatter),
            weekEnd.format(tipFormatter)));

        return button;
    }

    /**
     * 获取选中的周开始日期（周一）
     */
    public LocalDate getSelectedWeekStart() {
        return selectedWeekStart;
    }

    /**
     * 获取选中的周结束日期（周日）
     */
    public LocalDate getSelectedWeekEnd() {
        return selectedWeekStart.with(DayOfWeek.SUNDAY);
    }
}

