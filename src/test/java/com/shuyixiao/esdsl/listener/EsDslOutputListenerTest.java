package com.shuyixiao.esdsl.listener;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

/**
 * EsDslOutputListener 单元测试
 */
public class EsDslOutputListenerTest {
    
    @Test
    public void testExtractTraceTimestamp() throws Exception {
        EsDslOutputListener listener = new EsDslOutputListener(null);
        Method method = EsDslOutputListener.class.getDeclaredMethod("extractTraceTimestamp", String.class);
        method.setAccessible(true);
        
        // 测试实际日志格式
        String actualLog = "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST";
        String result = (String) method.invoke(listener, actualLog);
        
        System.out.println("测试1 - 实际日志格式:");
        System.out.println("  输入: " + actualLog);
        System.out.println("  输出: " + result);
        System.out.println("  预期: 21:21:00,523");
        assertNotNull("应该能提取时间戳", result);
        assertEquals("21:21:00,523", result);
        
        // 测试DEBUG日志格式
        String debugLog = "2025-10-18 21:21:00,523 DEBUG (RequestLogger.java:58)- request [POST http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch] returned [HTTP/1.1 200 OK]";
        result = (String) method.invoke(listener, debugLog);
        
        System.out.println("\n测试2 - DEBUG日志格式:");
        System.out.println("  输入: " + debugLog.substring(0, 80) + "...");
        System.out.println("  输出: " + result);
        System.out.println("  预期: null (因为不包含TRACE)");
        assertNull("DEBUG日志应该返回null", result);
    }
    
    @Test
    public void testExtractLastTraceTimestamp() throws Exception {
        EsDslOutputListener listener = new EsDslOutputListener(null);
        Method method = EsDslOutputListener.class.getDeclaredMethod("extractLastTraceTimestamp", String.class);
        method.setAccessible(true);
        
        // 测试包含多个TRACE日志的缓冲区
        String buffer = "2025-10-18 21:20:00,123 TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'\n" +
                       "# HTTP/1.1 200 OK\n" +
                       "# {\"status\":\"yellow\"}\n" +
                       "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0}'";
        
        String result = (String) method.invoke(listener, buffer);
        
        System.out.println("测试3 - 提取缓冲区最后TRACE时间戳:");
        System.out.println("  缓冲区包含2个TRACE日志");
        System.out.println("  第一个: 21:20:00,123");
        System.out.println("  第二个: 21:21:00,523");
        System.out.println("  提取结果: " + result);
        System.out.println("  预期: 21:21:00,523");
        assertNotNull("应该能提取最后一个TRACE时间戳", result);
        assertEquals("21:21:00,523", result);
    }
    
    @Test
    public void testShouldKeepText() throws Exception {
        EsDslOutputListener listener = new EsDslOutputListener(null);
        Method method = EsDslOutputListener.class.getDeclaredMethod("shouldKeepText", String.class);
        method.setAccessible(true);
        
        System.out.println("测试4 - shouldKeepText 过滤逻辑:");
        
        // 应该保留的日志
        String[] shouldKeep = {
            "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST",
            "# HTTP/1.1 200 OK",
            "'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0,\"query\":{\"bool\":{\"must\":[{\"term\":{\"tenantId\":{\"value\":\"1943230203698479104\"}}}]}}}",
            "{\"took\":1,\"timed_out\":false}"
        };
        
        for (String text : shouldKeep) {
            Boolean result = (Boolean) method.invoke(listener, text);
            String preview = text.length() > 80 ? text.substring(0, 80) + "..." : text;
            System.out.println("  ✓ 应该保留: " + preview + " -> " + result);
            assertTrue("应该保留: " + preview, result);
        }
        
        // 应该过滤的日志
        String[] shouldFilter = {
            "2025-10-18 21:21:00,479 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT id,creator",
            "2025-10-18 21:21:00,480 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: 1943230203698479104(String)",
            "2025-10-18 21:21:00,481 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1",
            "2025-10-18 21:21:00,466 INFO (PlatformAuthServiceImpl.java:66)- PlatformAuthServiceImpl.check"
        };
        
        for (String text : shouldFilter) {
            Boolean result = (Boolean) method.invoke(listener, text);
            String preview = text.length() > 80 ? text.substring(0, 80) + "..." : text;
            System.out.println("  ✗ 应该过滤: " + preview + " -> " + result);
            assertFalse("应该过滤: " + preview, result);
        }
    }
    
    @Test
    public void testFullWorkflow() throws Exception {
        System.out.println("\n测试5 - 完整工作流:");
        System.out.println("模拟实际日志输入流程...\n");
        
        EsDslOutputListener listener = new EsDslOutputListener(null);
        
        // 获取私有方法
        Method shouldKeepText = EsDslOutputListener.class.getDeclaredMethod("shouldKeepText", String.class);
        shouldKeepText.setAccessible(true);
        
        Method extractTraceTimestamp = EsDslOutputListener.class.getDeclaredMethod("extractTraceTimestamp", String.class);
        extractTraceTimestamp.setAccessible(true);
        
        // 模拟日志流
        String[] logLines = {
            "2025-10-18 21:21:00,523 DEBUG (RequestLogger.java:58)- request [POST http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch] returned [HTTP/1.1 200 OK]",
            "2025-10-18 21:21:00,523 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0,\"query\":{\"bool\":{\"must\":[{\"term\":{\"tenantId\":{\"value\":\"1943230203698479104\"}}}]}},\"size\":12}'",
            "# HTTP/1.1 200 OK",
            "# X-elastic-product: Elasticsearch",
            "#",
            "# {\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1}}"
        };
        
        StringBuilder buffer = new StringBuilder();
        int kept = 0;
        int filtered = 0;
        
        for (String line : logLines) {
            Boolean keep = (Boolean) shouldKeepText.invoke(listener, line);
            String preview = line.length() > 60 ? line.substring(0, 60) + "..." : line;
            
            if (keep) {
                buffer.append(line).append("\n");
                kept++;
                System.out.println("  [KEEP] " + preview);
                
                // 如果是TRACE日志,提取时间戳
                if (line.contains("TRACE")) {
                    String timestamp = (String) extractTraceTimestamp.invoke(listener, line);
                    System.out.println("         -> 提取时间戳: " + timestamp);
                }
            } else {
                filtered++;
                System.out.println("  [SKIP] " + preview);
            }
        }
        
        System.out.println("\n结果统计:");
        System.out.println("  保留: " + kept + " 行");
        System.out.println("  过滤: " + filtered + " 行");
        System.out.println("  缓冲区大小: " + buffer.length() + " 字符");
        
        assertTrue("应该保留TRACE日志", buffer.toString().contains("TRACE"));
        assertTrue("应该保留响应头", buffer.toString().contains("# HTTP/1.1 200 OK"));
        assertTrue("应该保留JSON", buffer.toString().contains("{\"took\":1"));
        assertFalse("不应该包含DEBUG日志", buffer.toString().contains("DEBUG"));
    }
}

