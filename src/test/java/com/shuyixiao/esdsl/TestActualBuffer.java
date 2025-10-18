package com.shuyixiao.esdsl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 测试实际缓冲区内容的时间戳提取
 */
public class TestActualBuffer {
    
    public static void main(String[] args) {
        System.out.println("===== 测试实际缓冲区场景 =====\n");
        
        // 模拟用户实际的缓冲区内容(从日志可以看出,缓冲区大小是10KB)
        // 缓冲区包含多行响应JSON,但TRACE日志可能在前面
        String actualBuffer = 
            "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0,\"query\":{\"bool\":{\"must\":[{\"term\":{\"tenantId\":{\"value\":\"1943230203698479104\"}}},{\"term\":{\"containerId\":{\"value\":\"1978435131131686912\"}}},{\"term\":{\"dataId\":{\"value\":\"1978435256176472064\"}}}]}},\"size\":12,\"sort\":[{\"page\":{\"mode\":\"min\",\"order\":\"asc\"}}],\"track_scores\":false,\"version\":true}'\n" +
            "# HTTP/1.1 200 OK\n" +
            "# X-elastic-product: Elasticsearch\n" +
            "# content-type: application/vnd.elasticsearch+json;compatible-with=8\n" +
            "# Transfer-Encoding: chunked\n" +
            "#\n" +
            "# {\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":{\"value\":1,\"relation\":\"eq\"},\"max_score\":null,\"hits\":[{\"_index\":\"dataset_chunk_sharding_24_1536\",\"_id\":\"1978777608099213312\",\"_version\":1,\"_score\":null}]}}";
        
        System.out.println("【场景1】缓冲区包含完整的TRACE日志和响应:");
        System.out.println("  缓冲区大小: " + actualBuffer.length() + " 字符 (约" + (actualBuffer.length() / 1024) + "KB)");
        System.out.println("  前150字符: " + actualBuffer.substring(0, Math.min(150, actualBuffer.length())));
        
        // 测试当前的正则
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
        Matcher matcher = pattern.matcher(actualBuffer);
        
        System.out.println("\n  使用正则: \\d{4}-\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
        String lastTimestamp = null;
        int count = 0;
        while (matcher.find()) {
            count++;
            lastTimestamp = matcher.group(1);
            System.out.println("    找到第" + count + "个TRACE时间戳: " + lastTimestamp);
        }
        
        if (lastTimestamp != null) {
            System.out.println("  ✓ 提取成功: " + lastTimestamp);
        } else {
            System.out.println("  ✗ 提取失败: null");
        }
        
        // 测试缓冲区是否包含TRACE
        System.out.println("\n【场景2】检查缓冲区内容:");
        System.out.println("  包含'TRACE': " + actualBuffer.contains("TRACE"));
        System.out.println("  包含'curl': " + actualBuffer.contains("curl"));
        System.out.println("  包含'21:21:00,523': " + actualBuffer.contains("21:21:00,523"));
        
        // 查找TRACE出现的位置
        int traceIndex = actualBuffer.indexOf("TRACE");
        if (traceIndex != -1) {
            System.out.println("  TRACE位置: 第" + traceIndex + "个字符");
            System.out.println("  TRACE前100字符: " + actualBuffer.substring(Math.max(0, traceIndex - 100), traceIndex));
            System.out.println("  TRACE后100字符: " + actualBuffer.substring(traceIndex, Math.min(actualBuffer.length(), traceIndex + 100)));
        }
        
        // 测试空缓冲区场景(用户日志显示"缓冲区最后TRACE: null")
        System.out.println("\n【场景3】模拟用户的问题场景 - 缓冲区为空或没有TRACE:");
        String emptyBuffer = "# HTTP/1.1 200 OK\n# {\"status\":\"yellow\"}";
        System.out.println("  缓冲区内容: " + emptyBuffer);
        System.out.println("  包含'TRACE': " + emptyBuffer.contains("TRACE"));
        
        Matcher emptyMatcher = pattern.matcher(emptyBuffer);
        String emptyResult = null;
        while (emptyMatcher.find()) {
            emptyResult = emptyMatcher.group(1);
        }
        System.out.println("  提取结果: " + emptyResult);
        System.out.println("  ✓ 这就是用户看到的'缓冲区最后TRACE: null'");
        
        // 分析问题
        System.out.println("\n【问题分析】:");
        System.out.println("1. 如果缓冲区包含完整TRACE日志,正则可以正常提取时间戳");
        System.out.println("2. 如果缓冲区只有响应内容(# 开头),没有TRACE日志,返回null");
        System.out.println("3. 用户的问题:'缓冲区最后TRACE: null' 说明缓冲区中没有TRACE日志!");
        System.out.println("4. 可能原因:");
        System.out.println("   - shouldKeepText过滤掉了TRACE日志");
        System.out.println("   - 或者TRACE日志还没来得及添加到缓冲区就被检查了");
        System.out.println("   - 或者lastClearedTimestamp匹配导致TRACE被拒绝");
        
        // 测试lastClearedTimestamp场景
        System.out.println("\n【场景4】测试lastClearedTimestamp拒绝机制:");
        String newTraceLog = "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST";
        String lastClearedTimestamp = "21:21:00,523";
        
        System.out.println("  新TRACE日志: " + newTraceLog.substring(0, 80));
        System.out.println("  lastClearedTimestamp: " + lastClearedTimestamp);
        System.out.println("  日志包含lastClearedTimestamp: " + newTraceLog.contains(lastClearedTimestamp));
        System.out.println("  ✗ 问题找到了!");
        System.out.println("     如果新TRACE日志的时间戳等于lastClearedTimestamp,");
        System.out.println("     shouldKeepText会拒绝这条日志,导致TRACE日志无法进入缓冲区!");
        
        System.out.println("\n===== 根本原因 =====");
        System.out.println("当检测到新TRACE时:");
        System.out.println("1. 提取新TRACE时间戳: 21:21:00,523");
        System.out.println("2. 清空缓冲区,记录lastClearedTimestamp = 旧时间戳");
        System.out.println("3. 但是如果旧时间戳为null,lastClearedTimestamp = null");
        System.out.println("4. 然后shouldKeepText检查新TRACE日志,但此时:");
        System.out.println("   - 如果lastClearedTimestamp == 新TRACE时间戳,拒绝!");
        System.out.println("   - 或者buffer.length() = 0,不执行后续行保留逻辑");
        System.out.println("5. 结果:新TRACE日志被拒绝,缓冲区保持为空!");
        System.out.println("\n修复方案:");
        System.out.println("1. 在清空缓冲区后,立即添加新TRACE日志,不要通过shouldKeepText检查");
        System.out.println("2. 或者在检测到新TRACE时,不要立即清空,而是标记需要清空,");
        System.out.println("   添加完新TRACE后再清空旧内容");
    }
}

