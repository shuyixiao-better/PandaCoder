package com.shuyixiao.gitstat.email.service;

import com.shuyixiao.gitstat.email.model.GitStatEmailConfig;
import com.shuyixiao.gitstat.email.model.GitStatEmailContent;
import com.shuyixiao.gitstat.model.GitDailyStat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName EmailTemplateService.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description 邮件模板服务类，负责生成精美的HTML邮件和纯文本邮件内容，支持渲染统计数据、趋势图表、排名信息等，提供模板变量替换功能
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class EmailTemplateService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 生成邮件内容
     */
    public String generateEmailBody(GitStatEmailContent content, GitStatEmailConfig config) {
        if (config.isSendHtml()) {
            return generateHtmlEmail(content);
        } else {
            return generatePlainTextEmail(content);
        }
    }
    
    /**
     * 生成 HTML 邮件
     */
    private String generateHtmlEmail(GitStatEmailContent content) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <style>\n");
        html.append(getHtmlStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        
        // Header
        html.append("        <div class=\"header\">\n");
        html.append("            <h1>📊 Git 统计日报</h1>\n");
        html.append("            <p>").append(content.getStatisticsDate().format(DATE_FORMATTER));
        html.append(" | ").append(content.getAuthorName()).append("</p>\n");
        html.append("        </div>\n");
        
        // Content
        html.append("        <div class=\"content\">\n");
        
        // 项目信息卡片
        if (content.getProjectName() != null && !content.getProjectName().isEmpty()) {
            html.append("            <div class=\"project-info-card\">\n");
            html.append("                <div style=\"display: flex; align-items: center; gap: 10px;\">\n");
            html.append("                    <span style=\"font-size: 18px;\">📁</span>\n");
            html.append("                    <div style=\"flex: 1;\">\n");
            html.append("                        <div style=\"font-weight: 600; color: #333; font-size: 15px; margin-bottom: 4px;\">").append(escapeHtml(content.getProjectName())).append("</div>\n");
            if (content.getProjectPath() != null && !content.getProjectPath().isEmpty()) {
                html.append("                        <div style=\"color: #666; font-size: 12px; font-family: 'Consolas', 'Monaco', monospace;\">").append(escapeHtml(simplifyPath(content.getProjectPath()))).append("</div>\n");
            }
            html.append("                    </div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
        }
        
        // Today's Summary
        html.append("            <div class=\"stat-card\">\n");
        html.append("                <h2 style=\"margin-top: 0;\">🎯 今日概览</h2>\n");
        html.append("                <div class=\"stat-row\">\n");
        html.append("                    <div class=\"stat-item\">\n");
        html.append("                        <div class=\"stat-label\">提交次数</div>\n");
        html.append("                        <div class=\"stat-value\">").append(content.getTodayCommits()).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"stat-item\">\n");
        html.append("                        <div class=\"stat-label\">新增代码</div>\n");
        html.append("                        <div class=\"stat-value positive\">+").append(content.getTodayAdditions()).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"stat-item\">\n");
        html.append("                        <div class=\"stat-label\">删除代码</div>\n");
        html.append("                        <div class=\"stat-value negative\">-").append(content.getTodayDeletions()).append("</div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("                <div style=\"text-align: center; margin-top: 20px;\">\n");
        html.append("                    <strong>净变化: ");
        int netChanges = content.getTodayNetChanges();
        if (netChanges >= 0) {
            html.append("+");
        }
        html.append(netChanges).append(" 行</strong>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        // 7-Day Trend
        if (content.getLast7Days() != null && !content.getLast7Days().isEmpty()) {
            html.append(generate7DayTrendHtml(content.getLast7Days()));
        }
        
        // Ranking
        if (content.getRankByCommits() > 0 && content.getTotalAuthors() > 1) {
            html.append("            <div class=\"stat-card\">\n");
            html.append("                <h2 style=\"margin-top: 0;\">🏆 统计排名</h2>\n");
            html.append("                <p>\n");
            html.append("                    <span class=\"badge badge-info\">提交排名: 第 ").append(content.getRankByCommits()).append(" 名</span>\n");
            html.append("                    <span class=\"badge badge-info\">代码量排名: 第 ").append(content.getRankByAdditions()).append(" 名</span>\n");
            html.append("                </p>\n");
            html.append("                <p style=\"color: #666; font-size: 14px; margin-top: 10px;\">\n");
            html.append("                    团队共 ").append(content.getTotalAuthors()).append(" 位开发者\n");
            html.append("                </p>\n");
            html.append("            </div>\n");
        }
        
        html.append("        </div>\n");
        
        // Footer
        html.append("        <div class=\"footer\">\n");
        
        // 使用说明
        html.append("            <div style=\"margin: 20px 0; padding: 20px; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); border-radius: 8px; border-left: 4px solid #667eea;\">\n");
        html.append("                <h3 style=\"margin: 0 0 12px 0; color: #333; font-size: 15px; font-weight: 600;\">✨ 关于本工具</h3>\n");
        html.append("                <p style=\"margin: 8px 0; color: #555; font-size: 13px; line-height: 1.8; text-align: justify;\">\n");
        html.append("                    这份统计报告的初衷，是帮助每一位程序员更清晰地了解自己的代码状态。\n");
        html.append("                    通过观察代码的增删趋势，我们可以反思：是否每次都在朝着正确的方向前进？\n");
        html.append("                    是否能够一次性写出高质量的代码，减少返工和修改？\n");
        html.append("                </p>\n");
        html.append("                <p style=\"margin: 8px 0; color: #555; font-size: 13px; line-height: 1.8; text-align: justify;\">\n");
        html.append("                    <strong style=\"color: #667eea;\">这不是绩效考核工具</strong>，而是自我提升的镜子。\n");
        html.append("                    愿每一次提交都是深思熟虑的结晶，愿每一行代码都经得起时间的考验。\n");
        html.append("                </p>\n");
        html.append("                <p style=\"margin: 12px 0 0 0; color: #888; font-size: 12px; font-style: italic; text-align: right;\">\n");
        html.append("                    —— 让代码更优雅，让技术更精进\n");
        html.append("                </p>\n");
        html.append("            </div>\n");
        
        html.append("            <p style=\"margin: 15px 0 8px 0; color: #666; font-size: 12px;\">此邮件由 PandaCoder Git 统计工具自动生成</p>\n");
        html.append("            <p style=\"margin: 8px 0; color: #666; font-size: 12px;\">").append(java.time.LocalDateTime.now().format(TIME_FORMATTER)).append("</p>\n");
        html.append("            <div style=\"margin-top: 15px; padding-top: 15px; border-top: 1px solid #e0e0e0;\">\n");
        html.append("                <p style=\"margin: 5px 0; color: #888; font-size: 11px; line-height: 1.6;\">\n");
        html.append("                    💡 技术分享 · 关注公众号：<strong style=\"color: #667eea;\">舒一笑的架构笔记</strong>\n");
        html.append("                </p>\n");
        html.append("                <p style=\"margin: 5px 0; color: #888; font-size: 11px;\">\n");
        html.append("                    🌐 个人官网：<a href=\"https://www.poeticcoder.com\" style=\"color: #667eea; text-decoration: none;\">www.poeticcoder.com</a>\n");
        html.append("                </p>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 生成 7 天趋势 HTML（4个折线图）
     */
    private String generate7DayTrendHtml(Map<LocalDate, GitDailyStat> last7Days) {
        StringBuilder html = new StringBuilder();
        
        html.append("            <div class=\"stat-card\">\n");
        html.append("                <h2 style=\"margin-top: 0; color: #333; font-size: 20px;\">📈 近7天代码趋势分析</h2>\n");
        
        // 验证数据
        if (last7Days == null || last7Days.isEmpty()) {
            html.append("                <p style=\"color: #999; text-align: center; padding: 20px;\">暂无趋势数据</p>\n");
            html.append("            </div>\n");
            return html.toString();
        }
        
        // 准备数据 - 确保按日期排序
        List<LocalDate> dates = new ArrayList<>(last7Days.keySet());
        dates.sort(LocalDate::compareTo); // 按日期升序排列
        
        System.out.println("========== 开始生成7天趋势图 ==========");
        System.out.println("日期数量: " + dates.size());
        
        List<Integer> commits = new ArrayList<>();
        List<Integer> additions = new ArrayList<>();
        List<Integer> deletions = new ArrayList<>();
        List<Integer> netChanges = new ArrayList<>();
        
        for (LocalDate date : dates) {
            GitDailyStat stat = last7Days.get(date);
            if (stat != null) {
                commits.add(stat.getCommits());
                additions.add(stat.getAdditions());
                deletions.add(stat.getDeletions());
                netChanges.add(stat.getNetChanges());
                System.out.println(date + ": 提交=" + stat.getCommits() + 
                                   ", 新增=" + stat.getAdditions() + 
                                   ", 删除=" + stat.getDeletions() + 
                                   ", 净=" + stat.getNetChanges());
            } else {
                // 如果某天没有数据，填充0
                commits.add(0);
                additions.add(0);
                deletions.add(0);
                netChanges.add(0);
                System.out.println(date + ": 无数据，使用0");
            }
        }
        
        System.out.println("提交数据: " + commits);
        System.out.println("新增数据: " + additions);
        System.out.println("删除数据: " + deletions);
        System.out.println("净变化数据: " + netChanges);
        
        // 生成4个折线图
        html.append(generateLineChart("📊 提交次数趋势", dates, commits, "#667eea", "commits"));
        html.append(generateLineChart("📈 新增代码行数", dates, additions, "#28a745", "additions"));
        html.append(generateLineChart("📉 删除代码行数", dates, deletions, "#dc3545", "deletions"));
        html.append(generateLineChart("⚖️ 净变化趋势", dates, netChanges, "#17a2b8", "net"));
        
        html.append("            </div>\n");
        
        return html.toString();
    }
    
    /**
     * 生成单个折线图（使用HTML+CSS代替SVG以提高邮件兼容性）
     */
    private String generateLineChart(String title, List<LocalDate> dates, List<Integer> values, String color, String chartId) {
        StringBuilder chart = new StringBuilder();
        
        System.out.println("生成图表: " + title + ", chartId=" + chartId);
        System.out.println("  dates数量: " + (dates != null ? dates.size() : "null"));
        System.out.println("  values数量: " + (values != null ? values.size() : "null"));
        if (values != null) {
            System.out.println("  values内容: " + values);
        }
        
        // 数据验证
        if (dates == null || dates.isEmpty() || values == null || values.isEmpty()) {
            System.out.println("  数据为空，返回'暂无数据'");
            chart.append("                <div style=\"margin: 20px 0; padding: 15px; background: #fafafa; border-radius: 8px;\">\n");
            chart.append("                    <h3 style=\"margin: 0; color: #999; font-size: 14px;\">").append(title).append(" - 暂无数据</h3>\n");
            chart.append("                </div>\n");
            return chart.toString();
        }
        
        // 找到最大值和最小值用于归一化
        int maxValue = values.stream().mapToInt(Integer::intValue).max().orElse(1);
        int minValue = values.stream().mapToInt(Integer::intValue).min().orElse(0);
        if (minValue > 0) minValue = 0; // 确保Y轴从0开始（除非有负值）
        
        int range = maxValue - minValue;
        if (range == 0) range = 1; // 避免除以0
        
        System.out.println("  maxValue=" + maxValue + ", minValue=" + minValue + ", range=" + range);
        
        chart.append("                <div style=\"margin: 20px 0; padding: 15px; background: #fafafa; border-radius: 8px; border-left: 4px solid ").append(color).append(";\">\n");
        chart.append("                    <h3 style=\"margin: 0 0 15px 0; color: #555; font-size: 14px; font-weight: 600;\">").append(title).append("</h3>\n");
        
        // 显示最大值参考
        chart.append("                    <div style=\"background: white; padding: 10px; border-radius: 4px;\">\n");
        chart.append("                        <div style=\"text-align: right; font-size: 11px; color: #999; margin-bottom: 5px;\">最大值: ").append(maxValue).append("</div>\n");
        
        // 使用表格布局绘制柱状图
        chart.append("                        <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse: collapse;\">\n");
        chart.append("                            <tr style=\"height: 100px; vertical-align: bottom;\">\n");
        
        // 绘制每一天的柱子
        for (int i = 0; i < dates.size(); i++) {
            int value = values.get(i);
            double normalizedValue = range > 0 ? ((double)(value - minValue) / range) : 0;
            int height = (int)(normalizedValue * 90); // 最大90px高度
            
            chart.append("                                <td style=\"width: ").append((int)(100.0 / dates.size())).append("%; padding: 0 2px; text-align: center; vertical-align: bottom;\">\n");
            
            // 数值标签（在柱子上方）
            if (value > 0) {
                chart.append("                                    <div style=\"font-size: 10px; color: #666; margin-bottom: 2px; min-height: 14px;\">").append(value).append("</div>\n");
            } else {
                chart.append("                                    <div style=\"font-size: 10px; color: #ccc; margin-bottom: 2px; min-height: 14px;\">0</div>\n");
            }
            
            // 柱子
            if (height > 0) {
                chart.append("                                    <div style=\"height: ").append(height).append("px; background: ").append(color).append("; border-radius: 3px 3px 0 0; margin: 0 auto; max-width: 40px;\"></div>\n");
            } else {
                chart.append("                                    <div style=\"height: 2px; background: #e0e0e0; border-radius: 3px; margin: 0 auto; max-width: 40px;\"></div>\n");
            }
            
            chart.append("                                </td>\n");
        }
        
        chart.append("                            </tr>\n");
        
        // 日期标签行
        chart.append("                            <tr>\n");
        for (int i = 0; i < dates.size(); i++) {
            String dayLabel = dates.get(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA);
            chart.append("                                <td style=\"text-align: center; padding-top: 5px;\">\n");
            chart.append("                                    <div style=\"font-size: 10px; color: #999;\">").append(dayLabel).append("</div>\n");
            chart.append("                                </td>\n");
        }
        chart.append("                            </tr>\n");
        chart.append("                        </table>\n");
        chart.append("                    </div>\n");
        chart.append("                </div>\n");
        
        return chart.toString();
    }
    
    /**
     * 生成纯文本邮件
     */
    private String generatePlainTextEmail(GitStatEmailContent content) {
        StringBuilder text = new StringBuilder();
        
        text.append("========================================\n");
        text.append("📊 Git 统计日报\n");
        text.append("========================================\n");
        text.append("日期: ").append(content.getStatisticsDate().format(DATE_FORMATTER)).append("\n");
        text.append("作者: ").append(content.getAuthorName()).append("\n");
        
        // 项目信息
        if (content.getProjectName() != null && !content.getProjectName().isEmpty()) {
            text.append("项目: ").append(content.getProjectName()).append("\n");
            if (content.getProjectPath() != null && !content.getProjectPath().isEmpty()) {
                text.append("路径: ").append(simplifyPath(content.getProjectPath())).append("\n");
            }
        }
        text.append("\n");
        
        text.append("🎯 今日概览\n");
        text.append("----------------------------------------\n");
        text.append("提交次数: ").append(content.getTodayCommits()).append("\n");
        text.append("新增代码: +").append(content.getTodayAdditions()).append(" 行\n");
        text.append("删除代码: -").append(content.getTodayDeletions()).append(" 行\n");
        text.append("净变化:   ");
        int netChanges = content.getTodayNetChanges();
        if (netChanges >= 0) {
            text.append("+");
        }
        text.append(netChanges).append(" 行\n\n");
        
        // 7-Day Trend
        if (content.getLast7Days() != null && !content.getLast7Days().isEmpty()) {
            text.append("📈 近7天趋势\n");
            text.append("----------------------------------------\n");
            for (Map.Entry<LocalDate, GitDailyStat> entry : content.getLast7Days().entrySet()) {
                LocalDate date = entry.getKey();
                GitDailyStat stat = entry.getValue();
                text.append(date.format(DATE_FORMATTER)).append(" ");
                text.append(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA));
                text.append(": ").append(stat.getCommits()).append(" 次提交");
                text.append(", +").append(stat.getAdditions()).append(" 行\n");
            }
            text.append("\n");
        }
        
        // Ranking
        if (content.getRankByCommits() > 0 && content.getTotalAuthors() > 1) {
            text.append("🏆 统计排名\n");
            text.append("----------------------------------------\n");
            text.append("提交次数排名: 第 ").append(content.getRankByCommits()).append(" 名\n");
            text.append("代码量排名:   第 ").append(content.getRankByAdditions()).append(" 名\n");
            text.append("团队共 ").append(content.getTotalAuthors()).append(" 位开发者\n\n");
        }
        
        text.append("========================================\n");
        text.append("✨ 关于本工具\n");
        text.append("========================================\n");
        text.append("这份统计报告的初衷，是帮助每一位程序员更清晰地\n");
        text.append("了解自己的代码状态。通过观察代码的增删趋势，我\n");
        text.append("们可以反思：是否每次都在朝着正确的方向前进？是\n");
        text.append("否能够一次性写出高质量的代码，减少返工和修改？\n\n");
        text.append("【这不是绩效考核工具】，而是自我提升的镜子。\n");
        text.append("愿每一次提交都是深思熟虑的结晶，\n");
        text.append("愿每一行代码都经得起时间的考验。\n\n");
        text.append("—— 让代码更优雅，让技术更精进\n");
        text.append("========================================\n");
        text.append("此邮件由 PandaCoder Git 统计工具自动生成\n");
        text.append(java.time.LocalDateTime.now().format(TIME_FORMATTER)).append("\n");
        text.append("----------------------------------------\n");
        text.append("💡 技术分享 · 关注公众号：舒一笑的架构笔记\n");
        text.append("🌐 个人官网：www.poeticcoder.com\n");
        text.append("========================================\n");
        
        return text.toString();
    }
    
    /**
     * HTML 样式
     */
    private String getHtmlStyles() {
        return "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }\n" +
               "        .container { max-width: 650px; margin: 0 auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }\n" +
               "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 30px; text-align: center; }\n" +
               "        .header h1 { margin: 0; font-size: 28px; font-weight: 600; }\n" +
               "        .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }\n" +
               "        .content { padding: 30px; }\n" +
               "        .project-info-card { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-left: 4px solid #667eea; border-radius: 6px; padding: 16px; margin-bottom: 20px; }\n" +
               "        .stat-card { background: #f8f9fa; border-radius: 6px; padding: 20px; margin-bottom: 20px; }\n" +
               "        .stat-row { display: flex; justify-content: space-around; margin: 15px 0; }\n" +
               "        .stat-item { text-align: center; flex: 1; }\n" +
               "        .stat-label { color: #666; font-size: 14px; margin-bottom: 8px; }\n" +
               "        .stat-value { font-size: 28px; font-weight: bold; color: #333; margin-top: 8px; }\n" +
               "        .stat-value.positive { color: #28a745; }\n" +
               "        .stat-value.negative { color: #dc3545; }\n" +
               "        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }\n" +
               "        .badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; margin: 0 4px; }\n" +
               "        .badge-info { background-color: #d1ecf1; color: #0c5460; }\n";
    }
    
    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * 简化路径显示，只显示最后两级目录
     */
    private String simplifyPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        try {
            // 处理Windows和Unix路径
            String separator = path.contains("\\") ? "\\" : "/";
            String[] parts = path.split(separator.replace("\\", "\\\\"));
            
            if (parts.length <= 2) {
                return path;
            }
            
            // 返回最后两级目录
            return "... " + separator + parts[parts.length - 2] + separator + parts[parts.length - 1];
        } catch (Exception e) {
            // 如果处理失败，返回原路径
            return path;
        }
    }
}

