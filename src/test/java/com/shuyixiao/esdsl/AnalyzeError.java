package com.shuyixiao.esdsl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 分析错误日志
 */
public class AnalyzeError {
    
    public static void main(String[] args) {
        String logFile = "日志.txt";
        int lineNum = 0;
        int totalLines = 0;
        int errorLines = 0;
        int traceLines = 0;
        List<String> errorMessages = new ArrayList<>();
        List<Integer> traceLineNumbers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(
                    new java.io.FileInputStream(logFile), 
                    StandardCharsets.UTF_8))) {
            String line;
            
            // 先统计总行数
            while ((line = reader.readLine()) != null) {
                totalLines++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(
                    new java.io.FileInputStream(logFile), 
                    StandardCharsets.UTF_8))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                // 查找错误信息
                if (line.contains("Error buffer exceeded maximum size")) {
                    errorLines++;
                    System.out.println("\n【错误 #" + errorLines + "】行号: " + lineNum);
                    System.out.println(line);
                    errorMessages.add(line);
                }
                
                // 查找TRACE日志
                if (line.contains("TRACE") && line.contains("RequestLogger")) {
                    traceLineNumbers.add(lineNum);
                }
                
                // 输出最后50行（可能包含错误上下文）
                if (lineNum > totalLines - 50) {
                    if (lineNum == totalLines - 49) {
                        System.out.println("\n========================================");
                        System.out.println("  最后50行日志");
                        System.out.println("========================================");
                    }
                    System.out.println("行" + lineNum + ": " + line.substring(0, Math.min(150, line.length())));
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("  错误分析");
            System.out.println("========================================");
            System.out.println("总行数: " + totalLines);
            System.out.println("错误行数: " + errorLines);
            System.out.println("TRACE日志数: " + traceLineNumbers.size());
            
            if (traceLineNumbers.size() > 0) {
                System.out.println("\nTRACE日志位置:");
                for (int i = 0; i < traceLineNumbers.size(); i++) {
                    System.out.println("  TRACE #" + (i+1) + ": 行 " + traceLineNumbers.get(i));
                }
            }
            
            if (errorLines > 0) {
                System.out.println("\n⚠️  发现错误: Error buffer exceeded maximum size");
                System.out.println("这个错误来自 ConsoleOutputListener (BugRecorder功能)");
                System.out.println("不是ES DSL监听器的问题！");
            }
            
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

