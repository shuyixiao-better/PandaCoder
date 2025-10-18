package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.parser.EsDslParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 测试API路径和调用类提取功能
 */
public class TestApiPathExtraction {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  API路径提取功能测试");
        System.out.println("========================================\n");
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream("日志.txt"), StandardCharsets.UTF_8))) {
            
            StringBuilder buffer = new StringBuilder();
            String line;
            int lineNumber = 0;
            int testCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                buffer.append(line).append("\n");
                
                // 当遇到TRACE日志时，提取API路径和调用类
                if (line.contains("TRACE") && line.contains("RequestLogger") && line.contains("curl")) {
                    testCount++;
                    
                    // 获取缓冲区内容（包含之前的日志）
                    String context = buffer.toString();
                    
                    // 提取API路径
                    String apiPath = EsDslParser.extractApiPath(context);
                    
                    // 提取调用类
                    String callerClass = EsDslParser.extractCallerClass(context);
                    
                    System.out.println("【测试 #" + testCount + "】行号: " + lineNumber);
                    System.out.println("  API路径: " + (apiPath != null ? apiPath : "❌ 未提取到"));
                    System.out.println("  调用类: " + (callerClass != null ? callerClass : "❌ 未提取到"));
                    System.out.println();
                    
                    // 清空缓冲区，准备下一次提取
                    buffer.setLength(0);
                }
                
                // 保留最近20KB的内容用于API路径提取
                if (buffer.length() > 20000) {
                    String remaining = buffer.substring(buffer.length() - 10000);
                    buffer.setLength(0);
                    buffer.append(remaining);
                }
            }
            
            System.out.println("========================================");
            System.out.println("  测试完成");
            System.out.println("========================================");
            System.out.println("总测试数: " + testCount);
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

