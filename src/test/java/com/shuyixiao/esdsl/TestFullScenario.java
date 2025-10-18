package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.model.EsDslRecord;
import com.shuyixiao.esdsl.parser.EsDslParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整场景测试 - 模拟EsDslOutputListener的实际运行
 * 使用日志.txt文件进行自测
 */
public class TestFullScenario {
    
    // 模拟监听器的缓冲区大小
    private static final int MAX_BUFFER_SIZE = 300000;
    private static final int CROSS_LINE_RETAIN_SIZE = 50000;
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  完整场景测试 - API路径提取");
        System.out.println("========================================\n");
        
        StringBuilder buffer = new StringBuilder();
        List<EsDslRecord> records = new ArrayList<>();
        int totalLines = 0;
        int traceCount = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream("日志.txt"), StandardCharsets.UTF_8))) {
            
            String line;
            String lastTraceTime = null;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                String text = line + "\n";
                
                // 检查是否是新的TRACE日志
                if (text.contains("TRACE") && text.contains("RequestLogger") && text.contains("curl")) {
                    traceCount++;
                    
                    // 提取时间戳
                    String newTraceTime = extractTraceTimestamp(text);
                    
                    System.out.println("【TRACE #" + traceCount + "】行号: " + totalLines);
                    System.out.println("  时间戳: " + newTraceTime);
                    System.out.println("  缓冲区大小: " + (buffer.length() / 1024) + "KB");
                    
                    // 如果缓冲区已有内容，检查时间戳
                    if (buffer.length() > 100) {
                        String currentBuffer = buffer.toString();
                        String lastTime = extractLastTraceTimestamp(currentBuffer);
                        
                        System.out.println("  上次时间戳: " + lastTime);
                        
                        // 时间戳不同 - 新请求
                        if (lastTime != null && newTraceTime != null && !lastTime.equals(newTraceTime)) {
                            System.out.println("  ✅ 时间戳不同，先解析旧DSL");
                            
                            // 解析旧的DSL
                            EsDslRecord record = EsDslParser.parseEsDsl(currentBuffer, "TestProject");
                            if (record != null) {
                                records.add(record);
                                System.out.println("  ✅ 成功提取DSL #" + records.size());
                                System.out.println("     - 索引: " + record.getIndex());
                                System.out.println("     - 方法: " + record.getMethod());
                                System.out.println("     - API路径: " + (record.getApiPath() != null ? record.getApiPath() : "❌ NULL"));
                                System.out.println("     - 调用类: " + (record.getCallerClass() != null ? record.getCallerClass() : "❌ NULL"));
                            } else {
                                System.out.println("  ❌ 解析失败");
                            }
                            
                            // 保留上下文
                            if (buffer.length() > CROSS_LINE_RETAIN_SIZE) {
                                String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                                buffer.setLength(0);
                                buffer.append(remaining);
                                System.out.println("  🧹 清理缓冲区，保留 " + (remaining.length() / 1024) + "KB 上下文");
                            }
                        } 
                        // 时间戳相同 - 重复日志
                        else if (lastTime != null && lastTime.equals(newTraceTime)) {
                            System.out.println("  ⏭️  时间戳相同，先解析再忽略重复");
                            
                            // 解析第一条完整的TRACE
                            EsDslRecord record = EsDslParser.parseEsDsl(currentBuffer, "TestProject");
                            if (record != null) {
                                records.add(record);
                                System.out.println("  ✅ 成功提取DSL #" + records.size());
                                System.out.println("     - 索引: " + record.getIndex());
                                System.out.println("     - 方法: " + record.getMethod());
                                System.out.println("     - API路径: " + (record.getApiPath() != null ? record.getApiPath() : "❌ NULL"));
                                System.out.println("     - 调用类: " + (record.getCallerClass() != null ? record.getCallerClass() : "❌ NULL"));
                            } else {
                                System.out.println("  ❌ 解析失败");
                            }
                            
                            // 保留上下文并忽略重复日志
                            if (buffer.length() > CROSS_LINE_RETAIN_SIZE) {
                                String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                                buffer.setLength(0);
                                buffer.append(remaining);
                                System.out.println("  🧹 保留上下文，忽略重复日志");
                            }
                            
                            System.out.println();
                            continue; // 不添加重复日志
                        }
                    }
                    
                    lastTraceTime = newTraceTime;
                    System.out.println();
                }
                
                // 添加到缓冲区
                buffer.append(text);
                
                // 缓冲区太大时清理
                if (buffer.length() > MAX_BUFFER_SIZE) {
                    String remaining = buffer.substring(buffer.length() - CROSS_LINE_RETAIN_SIZE);
                    buffer.setLength(0);
                    buffer.append(remaining);
                }
            }
            
            // 处理最后一条
            if (buffer.length() > 200 && buffer.toString().contains("TRACE")) {
                System.out.println("【处理最后一条】");
                EsDslRecord record = EsDslParser.parseEsDsl(buffer.toString(), "TestProject");
                if (record != null) {
                    records.add(record);
                    System.out.println("  ✅ 成功提取DSL #" + records.size());
                    System.out.println("     - 索引: " + record.getIndex());
                    System.out.println("     - 方法: " + record.getMethod());
                    System.out.println("     - API路径: " + (record.getApiPath() != null ? record.getApiPath() : "❌ NULL"));
                    System.out.println("     - 调用类: " + (record.getCallerClass() != null ? record.getCallerClass() : "❌ NULL"));
                }
                System.out.println();
            }
            
            // 统计结果
            System.out.println("========================================");
            System.out.println("  测试统计结果");
            System.out.println("========================================");
            System.out.println("日志总行数: " + totalLines);
            System.out.println("检测到TRACE日志: " + traceCount);
            System.out.println("成功提取DSL: " + records.size());
            System.out.println();
            
            // 详细显示每条记录
            System.out.println("========================================");
            System.out.println("  提取的DSL记录详情");
            System.out.println("========================================");
            int withApiPath = 0;
            int withCallerClass = 0;
            
            for (int i = 0; i < records.size(); i++) {
                EsDslRecord record = records.get(i);
                System.out.println("【记录 #" + (i + 1) + "】");
                System.out.println("  方法: " + record.getMethod());
                System.out.println("  索引: " + record.getIndex());
                System.out.println("  API路径: " + (record.getApiPath() != null ? "✅ " + record.getApiPath() : "❌ NULL"));
                System.out.println("  调用类: " + (record.getCallerClass() != null ? "✅ " + record.getCallerClass() : "❌ NULL"));
                System.out.println();
                
                if (record.getApiPath() != null) withApiPath++;
                if (record.getCallerClass() != null) withCallerClass++;
            }
            
            System.out.println("========================================");
            System.out.println("有API路径的记录: " + withApiPath + " / " + records.size());
            System.out.println("有调用类的记录: " + withCallerClass + " / " + records.size());
            System.out.println("API路径提取率: " + (records.size() > 0 ? (withApiPath * 100 / records.size()) : 0) + "%");
            System.out.println("========================================");
            
            if (withApiPath == records.size() && records.size() > 0) {
                System.out.println("✅✅✅ 完美！所有记录都成功提取了API路径！✅✅✅");
            } else if (withApiPath > 0) {
                System.out.println("⚠️ 部分记录成功提取了API路径");
            } else {
                System.out.println("❌ 所有记录都没有提取到API路径，需要进一步调试");
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从TRACE日志中提取时间戳
     */
    private static String extractTraceTimestamp(String text) {
        if (text == null || !text.contains("TRACE")) {
            return null;
        }
        
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }
    
    /**
     * 从缓冲区中提取最后一个TRACE日志的时间戳
     */
    private static String extractLastTraceTimestamp(String buffer) {
        if (buffer == null || !buffer.contains("TRACE")) {
            return null;
        }
        
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
            java.util.regex.Matcher matcher = pattern.matcher(buffer);
            String lastTimestamp = null;
            while (matcher.find()) {
                lastTimestamp = matcher.group(1);
            }
            return lastTimestamp;
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }
}

