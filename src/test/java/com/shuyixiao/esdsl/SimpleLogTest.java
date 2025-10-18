package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.parser.EsDslParser;
import com.shuyixiao.esdsl.model.EsDslRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 简化的DSL提取测试
 */
public class SimpleLogTest {
    
    private static final Pattern TRACE_PATTERN = Pattern.compile("(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
    private static int totalTraceFound = 0;
    private static int totalDslExtracted = 0;
    private static List<String> failedExtractions = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ES DSL 提取测试");
        System.out.println("========================================\n");
        
        String logFile = "日志.txt";
        StringBuilder buffer = new StringBuilder();
        String lastTimestamp = null;
        int lineNum = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                // 检测到新的TRACE日志
                if (line.contains("TRACE") && line.contains("RequestLogger") && line.contains("curl")) {
                    totalTraceFound++;
                    String timestamp = extractTimestamp(line);
                    
                    System.out.println("\n【TRACE #" + totalTraceFound + "】行号: " + lineNum + ", 时间: " + timestamp);
                    
                    // 如果缓冲区有内容且时间戳不同，先尝试提取之前的
                    if (buffer.length() > 100) {
                        String prevTimestamp = extractLastTimestamp(buffer.toString());
                        if (prevTimestamp != null && !prevTimestamp.equals(timestamp)) {
                            System.out.println("  → 时间戳变化，先处理之前的缓冲区");
                            tryExtractDsl(buffer.toString(), prevTimestamp);
                            buffer.setLength(0);
                        }
                    }
                    
                    buffer.append(line).append("\n");
                    lastTimestamp = timestamp;
                    continue;
                }
                
                // 如果缓冲区有TRACE日志，继续收集相关内容
                if (buffer.length() > 0 && buffer.toString().contains("TRACE")) {
                    // 过滤掉无关日志
                    if (!isNoiseLog(line)) {
                        // 检查是否是响应行或JSON
                        if (line.startsWith("#") || line.contains("{") || line.trim().isEmpty()) {
                            buffer.append(line).append("\n");
                        }
                        // 检测到新的日志行（不是响应），触发提取
                        else if (line.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                            tryExtractDsl(buffer.toString(), lastTimestamp);
                            buffer.setLength(0);
                            lastTimestamp = null;
                        }
                    }
                }
            }
            
            // 处理最后的缓冲区
            if (buffer.length() > 100 && buffer.toString().contains("TRACE")) {
                tryExtractDsl(buffer.toString(), lastTimestamp);
            }
            
            // 输出统计结果
            System.out.println("\n========================================");
            System.out.println("  测试结果统计");
            System.out.println("========================================");
            System.out.println("总行数: " + lineNum);
            System.out.println("检测到TRACE日志: " + totalTraceFound);
            System.out.println("成功提取DSL: " + totalDslExtracted);
            System.out.println("提取成功率: " + (totalTraceFound > 0 ? 
                String.format("%.1f%%", (totalDslExtracted * 100.0 / totalTraceFound)) : "N/A"));
            
            if (!failedExtractions.isEmpty()) {
                System.out.println("\n失败的提取:");
                for (String failed : failedExtractions) {
                    System.out.println("  - " + failed);
                }
            }
            
            System.out.println("\n========================================");
            if (totalDslExtracted == totalTraceFound) {
                System.out.println("✅ 完美！所有ES DSL都被成功提取！");
            } else {
                System.out.println("⚠️  有 " + (totalTraceFound - totalDslExtracted) + " 个DSL未能提取");
            }
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void tryExtractDsl(String bufferedText, String timestamp) {
        try {
            EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, "test-project");
            if (record != null) {
                totalDslExtracted++;
                System.out.println("  ✅ 提取成功");
                System.out.println("     方法: " + record.getMethod());
                System.out.println("     索引: " + (record.getIndex() != null ? record.getIndex() : "N/A"));
                System.out.println("     端点: " + (record.getEndpoint() != null ? record.getEndpoint() : "N/A"));
                System.out.println("     DSL长度: " + (record.getDslQuery() != null ? record.getDslQuery().length() : 0) + " 字符");
            } else {
                System.out.println("  ❌ 提取失败 - Parser返回null");
                System.out.println("     缓冲区大小: " + bufferedText.length());
                System.out.println("     前150字符: " + bufferedText.substring(0, Math.min(150, bufferedText.length())));
                failedExtractions.add("时间:" + timestamp + ", 大小:" + bufferedText.length());
            }
        } catch (Exception e) {
            System.out.println("  ❌ 提取异常: " + e.getMessage());
            failedExtractions.add("时间:" + timestamp + ", 异常:" + e.getMessage());
        }
    }
    
    private static boolean isNoiseLog(String line) {
        String lower = line.toLowerCase();
        return lower.contains("basejdbclogger") ||
               lower.contains("preparing:") ||
               lower.contains("parameters:") ||
               lower.contains("==>") ||
               lower.contains("<==") ||
               lower.contains("platformauthserviceimpl") ||
               lower.contains("knowledgeelementdetailcontroller") ||
               lower.contains("vectordataretrieverelastic") ||
               lower.contains("vectorassistant") ||
               lower.contains("nettyinternallogger") ||
               lower.contains("dingtalk");
    }
    
    private static String extractTimestamp(String text) {
        Matcher matcher = TRACE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private static String extractLastTimestamp(String buffer) {
        Matcher matcher = TRACE_PATTERN.matcher(buffer);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }
}

