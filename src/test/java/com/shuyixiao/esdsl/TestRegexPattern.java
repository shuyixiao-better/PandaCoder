package com.shuyixiao.esdsl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 测试时间戳提取的正则表达式
 */
public class TestRegexPattern {
    
    public static void main(String[] args) {
        System.out.println("===== 测试时间戳提取正则表达式 =====\n");
        
        // 实际日志格式
        String actualLog = "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0}'";
        
        // 当前使用的正则(错误的)
        System.out.println("【测试1】当前正则表达式 (错误的):");
        Pattern oldPattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
        Matcher oldMatcher = oldPattern.matcher(actualLog);
        System.out.println("  日志: " + actualLog.substring(0, 80) + "...");
        System.out.println("  正则: \\d{4}-\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
        if (oldMatcher.find()) {
            System.out.println("  ✓ 匹配成功: " + oldMatcher.group(1));
        } else {
            System.out.println("  ✗ 匹配失败!");
        }
        
        // 修复后的正则
        System.out.println("\n【测试2】修复后的正则表达式:");
        Pattern newPattern = Pattern.compile("(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
        Matcher newMatcher = newPattern.matcher(actualLog);
        System.out.println("  日志: " + actualLog.substring(0, 80) + "...");
        System.out.println("  正则: (\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
        if (newMatcher.find()) {
            System.out.println("  ✓ 匹配成功: " + newMatcher.group(1));
        } else {
            System.out.println("  ✗ 匹配失败!");
        }
        
        // 测试提取缓冲区最后一个TRACE时间戳
        System.out.println("\n【测试3】提取缓冲区最后一个TRACE时间戳:");
        String buffer = "2025-10-18 21:20:00,123 TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'\n" +
                       "# HTTP/1.1 200 OK\n" +
                       "# {\"status\":\"yellow\"}\n" +
                       "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0}'";
        
        System.out.println("  缓冲区包含:");
        System.out.println("    第1个TRACE: 21:20:00,123");
        System.out.println("    第2个TRACE: 21:21:00,523");
        
        // 使用修复后的正则
        Matcher matcher = newPattern.matcher(buffer);
        String lastTimestamp = null;
        int count = 0;
        while (matcher.find()) {
            count++;
            lastTimestamp = matcher.group(1);
            System.out.println("    找到第" + count + "个: " + lastTimestamp);
        }
        System.out.println("  最后一个时间戳: " + lastTimestamp);
        System.out.println("  预期: 21:21:00,523");
        System.out.println("  结果: " + (lastTimestamp != null && lastTimestamp.equals("21:21:00,523") ? "✓ 正确" : "✗ 错误"));
        
        // 测试包含curl的完整TRACE日志
        System.out.println("\n【测试4】检查TRACE日志是否包含curl:");
        boolean hasCurl = actualLog.contains("TRACE") && actualLog.contains("curl");
        System.out.println("  日志包含TRACE: " + actualLog.contains("TRACE"));
        System.out.println("  日志包含curl: " + actualLog.contains("curl"));
        System.out.println("  结果: " + (hasCurl ? "✓ 应该触发时间戳检查" : "✗ 不会触发时间戳检查"));
        
        System.out.println("\n===== 测试完成 =====");
        
        // 测试结论
        System.out.println("\n【问题分析】:");
        System.out.println("1. 当前正则要求 '日期 时间 TRACE',但实际是 '日期 时间 TRACE ('");
        System.out.println("2. 修复方案:简化正则,只匹配 '时间 TRACE',不需要日期部分");
        System.out.println("3. 修复后的正则: (\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
    }
}

