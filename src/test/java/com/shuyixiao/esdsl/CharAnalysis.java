package com.shuyixiao.esdsl;

/**
 * 字符级别分析
 */
public class CharAnalysis {
    
    public static void main(String[] args) {
        // 真实的TRACE日志
        String real = "2025-10-18 21:27:52,063 TRACE (RequestLogger.java:90)- curl -iX GET 'http://10.10.0.210:9222/_cluster/health'";
        
        System.out.println("========================================");
        System.out.println("  字符级别分析");
        System.out.println("========================================\n");
        
        System.out.println("原始文本:");
        System.out.println(real);
        System.out.println("\n每个字符的详情:");
        
        // 找到TRACE的位置
        int traceIndex = real.indexOf("TRACE");
        int curlIndex = real.indexOf("curl");
        
        // 分析TRACE到curl之间的内容
        String between = real.substring(traceIndex, curlIndex + 4);
        System.out.println("\nTRACE到curl之间的内容:");
        System.out.println("\"" + between + "\"");
        System.out.println("\n字符分析:");
        
        for (int i = 0; i < between.length(); i++) {
            char c = between.charAt(i);
            System.out.printf("%3d: '%c' (0x%04X) %s\n", i, 
                Character.isWhitespace(c) ? ' ' : c, 
                (int)c,
                Character.isWhitespace(c) ? "[空白]" : "");
        }
        
        // 测试不同的正则模式
        System.out.println("\n========================================");
        System.out.println("  测试不同正则模式");
        System.out.println("========================================\n");
        
        String[] patterns = {
            "TRACE\\s+\\(RequestLogger",                    // 原始：要求至少一个空格
            "TRACE\\s*\\(RequestLogger",                    // 修改1：允许0个或多个空格
            "TRACE.*?\\(RequestLogger",                      // 修改2：任意字符
            "TRACE[\\s\\S]*?\\(RequestLogger",             // 修改3：任意字符包括换行
            "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-",   // 修改4：到-为止
            "TRACE\\s*\\(RequestLogger\\.java:\\d+\\)-?\\s*curl",  // 修改5：完整到curl
        };
        
        for (int i = 0; i < patterns.length; i++) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(patterns[i]);
            java.util.regex.Matcher m = p.matcher(real);
            System.out.printf("模式 %d: %s\n", i+1, m.find() ? "✅ 匹配" : "❌ 不匹配");
            System.out.println("   " + patterns[i]);
        }
    }
}

