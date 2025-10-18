package com.shuyixiao.esdsl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 详细检查RequestLogger日志
 */
public class DetailCheck {
    
    public static void main(String[] args) {
        String logFile = "日志.txt";
        int lineNum = 0;
        int requestLoggerCount = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(
                    new java.io.FileInputStream(logFile), 
                    StandardCharsets.UTF_8))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                if (line.toLowerCase().contains("requestlogger")) {
                    requestLoggerCount++;
                    System.out.println("\n【RequestLogger #" + requestLoggerCount + "】");
                    System.out.println("行号: " + lineNum);
                    System.out.println("完整内容: " + line);
                    
                    // 判断级别
                    if (line.contains("TRACE")) {
                        System.out.println("级别: TRACE ✅");
                    } else if (line.contains("DEBUG")) {
                        System.out.println("级别: DEBUG ⚠️");
                    } else if (line.contains("INFO")) {
                        System.out.println("级别: INFO");
                    } else {
                        System.out.println("级别: 未知");
                    }
                    
                    // 判断是否包含curl
                    if (line.contains("curl")) {
                        System.out.println("包含curl: 是 ✅");
                    } else {
                        System.out.println("包含curl: 否 ❌");
                    }
                    
                    // 判断请求方法
                    if (line.contains(" GET ")) {
                        System.out.println("方法: GET");
                    } else if (line.contains(" POST ")) {
                        System.out.println("方法: POST");
                    } else if (line.contains(" PUT ")) {
                        System.out.println("方法: PUT");
                    } else if (line.contains(" DELETE ")) {
                        System.out.println("方法: DELETE");
                    }
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("总共找到 " + requestLoggerCount + " 行RequestLogger日志");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

