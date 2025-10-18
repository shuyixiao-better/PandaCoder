package com.shuyixiao.esdsl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 直接测试GET请求的正则匹配
 */
public class DirectGetTest {
    
    public static void main(String[] args) {
        // 从日志文件中提取的实际GET请求文本
        String actualGetRequest = "2025-10-18 21:27:52,063 TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'\n" +
                                 "# HTTP/1.1 200 OK\n" +
                                 "# X-elastic-product: Elasticsearch\n" +
                                 "# content-type: application/vnd.elasticsearch+json;compatible-with=8\n" +
                                 "# content-length: 437\n" +
                                 "#\n" +
                                 "# {\"cluster_name\":\"elasticsearch\",\"status\":\"yellow\"}";
        
        System.out.println("========================================");
        System.out.println("  直接测试GET请求正则匹配");
        System.out.println("========================================\n");
        
        System.out.println("文本内容（前200字符）:");
        System.out.println(actualGetRequest.substring(0, Math.min(200, actualGetRequest.length())));
        System.out.println("\n文本总长度: " + actualGetRequest.length() + " 字符\n");
        
        // 测试原始Parser中的GET模式（修改前）
        Pattern oldGetPattern = Pattern.compile(
            "TRACE\\s+\\(RequestLogger\\.java:\\d+\\)-\\s+curl\\s+(?:-[iI]\\s*)?-[Xx]\\s+GET\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        
        // 测试修改后的GET模式 - 使用 -[iIxX]+ 匹配连在一起的参数
        Pattern newGetPattern = Pattern.compile(
            "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-?\\s*curl\\s+-[iIxX]+\\s+GET\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        
        // 测试超级宽松的模式
        Pattern relaxedPattern = Pattern.compile(
            "TRACE.*?curl.*?GET\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        System.out.println("【测试1: 原始GET模式】");
        testPattern(oldGetPattern, actualGetRequest);
        
        System.out.println("\n【测试2: 修改后GET模式】");
        testPattern(newGetPattern, actualGetRequest);
        
        System.out.println("\n【测试3: 超级宽松模式】");
        testPattern(relaxedPattern, actualGetRequest);
        
        // 测试containsEsDsl
        System.out.println("\n【测试4: 包含ES DSL关键词检查】");
        boolean contains = actualGetRequest.toLowerCase().contains("elasticsearch") || 
                          actualGetRequest.toLowerCase().contains("_search") || 
                          actualGetRequest.toLowerCase().contains("requestlogger") ||
                          actualGetRequest.toLowerCase().contains("curl");
        System.out.println("包含ES关键词: " + (contains ? "✅ 是" : "❌ 否"));
        
        // 提取JSON响应
        System.out.println("\n【测试5: 提取响应JSON】");
        int jsonStart = actualGetRequest.indexOf("# {");
        if (jsonStart >= 0) {
            String json = actualGetRequest.substring(jsonStart + 2).trim();
            System.out.println("✅ 找到JSON响应");
            System.out.println("   开始位置: " + jsonStart);
            System.out.println("   JSON: " + json);
        } else {
            System.out.println("❌ 未找到JSON响应");
        }
    }
    
    private static void testPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            System.out.println("✅ 匹配成功!");
            System.out.println("   URL: " + matcher.group(1));
        } else {
            System.out.println("❌ 匹配失败");
            System.out.println("   正则: " + pattern.pattern());
        }
    }
}

