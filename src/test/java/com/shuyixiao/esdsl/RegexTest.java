package com.shuyixiao.esdsl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 测试正则表达式匹配
 */
public class RegexTest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  正则表达式匹配测试");
        System.out.println("========================================\n");
        
        // GET请求模式 - 使用宽松的空白符匹配
        Pattern getPattern = Pattern.compile(
            "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-?\\s*curl\\s+(?:-[iI]\\s*)?-[Xx]\\s+GET\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        
        // POST请求模式 - 使用宽松的空白符匹配
        Pattern postPattern = Pattern.compile(
            "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-?\\s*curl\\s+(?:-[iI]\\s*)?-[Xx]\\s+(POST|PUT|DELETE)\\s+'([^']+)'(?:.*?)-d\\s+'(\\{.+?\\})'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        // 测试案例1: 真实的GET请求
        String test1 = "2025-10-18 21:27:52,063 TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'\n" +
                      "# HTTP/1.1 200 OK\n";
        
        System.out.println("【测试1: GET请求】");
        System.out.println("文本: " + test1.substring(0, Math.min(120, test1.length())));
        Matcher m1 = getPattern.matcher(test1);
        if (m1.find()) {
            System.out.println("✅ GET模式匹配成功");
            System.out.println("   URL: " + m1.group(1));
        } else {
            System.out.println("❌ GET模式匹配失败");
            
            // 尝试简化的模式
            Pattern simpleGet = Pattern.compile("curl.*?GET\\s+'([^']+)'");
            Matcher sm1 = simpleGet.matcher(test1);
            if (sm1.find()) {
                System.out.println("   但简化模式能匹配: " + sm1.group(1));
            }
        }
        
        // 测试案例2: 真实的POST请求
        String test2 = "2025-10-18 21:28:02,306 TRACE (RequestLogger.java:90)- curl -iX POST 'http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true' -d '{\"from\":0,\"query\":{\"bool\":{\"must\":[{\"term\":{\"tenantId\":{\"value\":\"123\"}}}]}}}'";
        
        System.out.println("\n【测试2: POST请求】");
        System.out.println("文本长度: " + test2.length());
        Matcher m2 = postPattern.matcher(test2);
        if (m2.find()) {
            System.out.println("✅ POST模式匹配成功");
            System.out.println("   方法: " + m2.group(1));
            System.out.println("   URL: " + m2.group(2));
            System.out.println("   DSL (前50字符): " + m2.group(3).substring(0, Math.min(50, m2.group(3).length())));
        } else {
            System.out.println("❌ POST模式匹配失败");
            
            // 尝试简化的模式
            Pattern simplePost = Pattern.compile("curl.*?POST\\s+'([^']+)'.*?-d\\s+'(\\{.+?\\})'", Pattern.DOTALL);
            Matcher sm2 = simplePost.matcher(test2);
            if (sm2.find()) {
                System.out.println("   但简化模式能匹配");
                System.out.println("   URL: " + sm2.group(1));
                System.out.println("   DSL: " + sm2.group(2).substring(0, Math.min(50, sm2.group(2).length())));
            }
        }
        
        // 测试案例3: 不带日期的GET
        String test3 = "TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'";
        
        System.out.println("\n【测试3: 不带日期的GET】");
        System.out.println("文本: " + test3);
        Matcher m3 = getPattern.matcher(test3);
        if (m3.find()) {
            System.out.println("✅ GET模式匹配成功");
            System.out.println("   URL: " + m3.group(1));
        } else {
            System.out.println("❌ GET模式匹配失败");
        }
        
        // 测试案例4: 测试URL提取
        System.out.println("\n【测试4: URL解析】");
        String[] testUrls = {
            "http://10.10.0.210:9222/_cluster/health",
            "http://10.10.0.210:9222/dataset_chunk_sharding_24_1536/_search?typed_keys=true&search_type=query_then_fetch"
        };
        
        for (String url : testUrls) {
            System.out.println("\nURL: " + url);
            String[] parts = extractUrlParts(url);
            if (parts != null && parts.length >= 2) {
                System.out.println("  索引: " + parts[0]);
                System.out.println("  端点: " + parts[1]);
            } else {
                System.out.println("  解析失败");
            }
        }
    }
    
    /**
     * 从完整 URL 中提取索引和端点
     */
    private static String[] extractUrlParts(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // 去掉协议和主机部分
            String path = url;
            if (path.contains("://")) {
                path = path.substring(path.indexOf("://") + 3);
                if (path.contains("/")) {
                    path = path.substring(path.indexOf("/") + 1);
                }
            }
            
            // 提取索引名称(第一个路径段)
            String index = null;
            String endpoint = path;
            
            String[] parts = path.split("/");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                index = parts[0].split("\\?")[0]; // 去掉查询参数
            }
            
            return new String[]{index, endpoint};
        } catch (Exception e) {
            return null;
        }
    }
}

