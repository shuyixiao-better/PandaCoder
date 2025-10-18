package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.parser.EsDslParser;
import com.shuyixiao.esdsl.model.EsDslRecord;

/**
 * 调试DSL提取 - 测试具体的日志片段
 */
public class DebugLogTest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  调试 DSL Parser");
        System.out.println("========================================\n");
        
        // 测试案例1: GET请求（第一个失败的）
        String test1 = "2025-10-18 21:27:52,063 TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'\n" +
                      "# HTTP/1.1 200 OK\n" +
                      "# X-elastic-product: Elasticsearch\n" +
                      "# content-type: application/vnd.elasticsearch+json;compatible-with=8\n" +
                      "# content-length: 437\n" +
                      "#\n" +
                      "# {\"cluster_name\":\"elasticsearch\",\"status\":\"yellow\",\"timed_out\":false,\"number_of_nodes\":1}";
        
        System.out.println("【测试1: GET请求】");
        testParse(test1);
        
        // 测试案例2: POST请求（成功的）
        String test2 = "2025-10-18 21:28:02,306 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch' -d '{\"from\":0,\"query\":{\"bool\":{\"must\":[{\"term\":{\"tenantId\":{\"value\":\"1943230203698479104\"}}}]}},\"size\":12}'\n" +
                      "# HTTP/1.1 200 OK\n" +
                      "# X-elastic-product: Elasticsearch\n";
        
        System.out.println("\n【测试2: POST请求】");
        testParse(test2);
        
        // 测试案例3: 简化的GET请求
        String test3 = "TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'\n" +
                      "# HTTP/1.1 200 OK\n" +
                      "# {\"cluster_name\":\"elasticsearch\"}";
        
        System.out.println("\n【测试3: 简化GET请求】");
        testParse(test3);
        
        // 测试案例4: 测试Parser的各个模式
        System.out.println("\n【测试4: 检查Parser识别能力】");
        System.out.println("containsEsDsl(test1): " + EsDslParser.containsEsDsl(test1));
        System.out.println("containsEsDsl(test2): " + EsDslParser.containsEsDsl(test2));
        System.out.println("containsEsDsl(test3): " + EsDslParser.containsEsDsl(test3));
        
        // 测试URL解析
        System.out.println("\n【测试5: 单独测试GET请求模式匹配】");
        java.util.regex.Pattern getPattern = java.util.regex.Pattern.compile(
            "TRACE\\s+\\(RequestLogger\\.java:\\d+\\)-\\s+curl\\s+(?:-[iI]\\s*)?-[Xx]\\s+GET\\s+'([^']+)'",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher getMatcher = getPattern.matcher(test1);
        if (getMatcher.find()) {
            System.out.println("✅ GET模式匹配成功");
            System.out.println("   URL: " + getMatcher.group(1));
        } else {
            System.out.println("❌ GET模式匹配失败");
        }
        
        // 测试POST请求模式匹配
        System.out.println("\n【测试6: 单独测试POST请求模式匹配】");
        java.util.regex.Pattern postPattern = java.util.regex.Pattern.compile(
            "TRACE\\s+\\(RequestLogger\\.java:\\d+\\)-\\s+curl\\s+(?:-[iI]\\s*)?-[Xx]\\s+(POST|PUT|DELETE)\\s+'([^']+)'(?:.*?)-d\\s+'(\\{.+?\\})'",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher postMatcher = postPattern.matcher(test2);
        if (postMatcher.find()) {
            System.out.println("✅ POST模式匹配成功");
            System.out.println("   方法: " + postMatcher.group(1));
            System.out.println("   URL: " + postMatcher.group(2));
            System.out.println("   DSL (前100字符): " + postMatcher.group(3).substring(0, Math.min(100, postMatcher.group(3).length())));
        } else {
            System.out.println("❌ POST模式匹配失败");
        }
    }
    
    private static void testParse(String text) {
        System.out.println("输入长度: " + text.length() + " 字符");
        System.out.println("前120字符: " + text.substring(0, Math.min(120, text.length())));
        
        EsDslRecord record = EsDslParser.parseEsDsl(text, "test-project");
        
        if (record != null) {
            System.out.println("✅ 解析成功!");
            System.out.println("   方法: " + record.getMethod());
            System.out.println("   索引: " + (record.getIndex() != null ? record.getIndex() : "NULL"));
            System.out.println("   端点: " + (record.getEndpoint() != null ? record.getEndpoint() : "NULL"));
            System.out.println("   来源: " + record.getSource());
            System.out.println("   HTTP状态: " + record.getHttpStatus());
            if (record.getDslQuery() != null) {
                System.out.println("   DSL长度: " + record.getDslQuery().length());
                System.out.println("   DSL前100字符: " + record.getDslQuery().substring(0, Math.min(100, record.getDslQuery().length())));
            } else {
                System.out.println("   DSL: NULL");
            }
        } else {
            System.out.println("❌ 解析失败 - 返回null");
        }
    }
}

