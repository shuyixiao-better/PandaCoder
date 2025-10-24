package com.shuyixiao.gitstat.email.service;

import com.shuyixiao.gitstat.email.model.GitStatEmailConfig;
import com.shuyixiao.gitstat.email.model.GitStatEmailContent;
import com.shuyixiao.gitstat.model.GitDailyStat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
        html.append("            <p>此邮件由 PandaCoder Git 统计工具自动生成</p>\n");
        html.append("            <p style=\"margin: 8px 0; color: #666; font-size: 12px;\">").append(java.time.LocalDateTime.now().format(TIME_FORMATTER)).append("</p>\n");
        html.append("            <div style=\"margin-top: 15px; padding-top: 15px; border-top: 1px solid #e0e0e0;\">\n");
        html.append("                <p style=\"margin: 5px 0; color: #888; font-size: 11px; line-height: 1.6;\">\n");
        html.append("                    💡 技术分享 · 关注公众号：<strong style=\"color: #667eea;\">舒一笑的架构笔记</strong>\n");
        html.append("                </p>\n");
        html.append("                <p style=\"margin: 5px 0; color: #888; font-size: 11px;\">\n");
        html.append("                    🌐 个人官网：<a href=\"https://www.shuyixiao.cn\" style=\"color: #667eea; text-decoration: none;\">www.shuyixiao.cn</a>\n");
        html.append("                </p>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 生成 7 天趋势 HTML
     */
    private String generate7DayTrendHtml(Map<LocalDate, GitDailyStat> last7Days) {
        StringBuilder html = new StringBuilder();
        
        html.append("            <div class=\"stat-card\">\n");
        html.append("                <h2 style=\"margin-top: 0;\">📈 近7天趋势</h2>\n");
        html.append("                <div class=\"chart-container\">\n");
        html.append("                    <div class=\"bar-chart\">\n");
        
        // 找到最大值用于归一化
        int maxCommits = last7Days.values().stream()
            .mapToInt(GitDailyStat::getCommits)
            .max()
            .orElse(1);
        
        for (Map.Entry<LocalDate, GitDailyStat> entry : last7Days.entrySet()) {
            LocalDate date = entry.getKey();
            GitDailyStat stat = entry.getValue();
            int commits = stat.getCommits();
            
            // 计算高度百分比
            double heightPercent = maxCommits > 0 ? (commits * 100.0 / maxCommits) : 0;
            
            html.append("                        <div class=\"bar\" style=\"height: ").append((int)heightPercent).append("%\">\n");
            html.append("                            <div class=\"bar-value\">").append(commits).append("</div>\n");
            html.append("                            <div class=\"bar-label\">").append(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA)).append("</div>\n");
            html.append("                        </div>\n");
        }
        
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        return html.toString();
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
        text.append("作者: ").append(content.getAuthorName()).append("\n\n");
        
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
        text.append("此邮件由 PandaCoder Git 统计工具自动生成\n");
        text.append(java.time.LocalDateTime.now().format(TIME_FORMATTER)).append("\n");
        text.append("----------------------------------------\n");
        text.append("💡 技术分享 · 关注公众号：舒一笑的架构笔记\n");
        text.append("🌐 个人官网：www.shuyixiao.cn\n");
        text.append("========================================\n");
        
        return text.toString();
    }
    
    /**
     * HTML 样式
     */
    private String getHtmlStyles() {
        return "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }\n" +
               "        .container { max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }\n" +
               "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 30px; text-align: center; }\n" +
               "        .header h1 { margin: 0; font-size: 28px; }\n" +
               "        .header p { margin: 10px 0 0 0; opacity: 0.9; }\n" +
               "        .content { padding: 30px; }\n" +
               "        .stat-card { background: #f8f9fa; border-radius: 6px; padding: 20px; margin-bottom: 20px; }\n" +
               "        .stat-row { display: flex; justify-content: space-around; margin: 15px 0; }\n" +
               "        .stat-item { text-align: center; }\n" +
               "        .stat-label { color: #666; font-size: 14px; }\n" +
               "        .stat-value { font-size: 28px; font-weight: bold; color: #333; margin-top: 8px; }\n" +
               "        .stat-value.positive { color: #28a745; }\n" +
               "        .stat-value.negative { color: #dc3545; }\n" +
               "        .chart-container { margin: 20px 0; }\n" +
               "        .bar-chart { display: flex; align-items: flex-end; height: 150px; gap: 8px; border-bottom: 2px solid #ddd; }\n" +
               "        .bar { flex: 1; background: linear-gradient(to top, #667eea, #764ba2); border-radius: 4px 4px 0 0; position: relative; min-height: 10px; }\n" +
               "        .bar-value { position: absolute; top: -25px; width: 100%; text-align: center; font-size: 12px; font-weight: bold; color: #333; }\n" +
               "        .bar-label { position: absolute; bottom: -25px; width: 100%; text-align: center; font-size: 11px; color: #666; }\n" +
               "        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }\n" +
               "        .badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; margin: 0 4px; }\n" +
               "        .badge-info { background-color: #d1ecf1; color: #0c5460; }\n";
    }
}

