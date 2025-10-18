package com.shuyixiao.esdsl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * 快速检查日志文件
 */
public class QuickCheck {
    
    public static void main(String[] args) {
        String logFile = "日志.txt";
        int totalLines = 0;
        int traceLines = 0;
        int debugLines = 0;
        int requestLoggerLines = 0;
        int curlLines = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                
                String lower = line.toLowerCase();
                if (lower.contains("trace")) traceLines++;
                if (lower.contains("debug")) debugLines++;
                if (lower.contains("requestlogger")) requestLoggerLines++;
                if (lower.contains("curl")) curlLines++;
                
                // 输出前5个包含curl的行
                if (curlLines <= 5 && lower.contains("curl")) {
                    System.out.println("\n【Curl行 #" + curlLines + "】");
                    System.out.println("行号: " + totalLines);
                    System.out.println("内容: " + line.substring(0, Math.min(150, line.length())));
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("  日志文件统计");
            System.out.println("========================================");
            System.out.println("总行数: " + totalLines);
            System.out.println("包含TRACE的行: " + traceLines);
            System.out.println("包含DEBUG的行: " + debugLines);
            System.out.println("包含RequestLogger的行: " + requestLoggerLines);
            System.out.println("包含curl的行: " + curlLines);
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

