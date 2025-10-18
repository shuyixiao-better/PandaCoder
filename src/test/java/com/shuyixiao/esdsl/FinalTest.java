package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.parser.EsDslParser;
import com.shuyixiao.esdsl.model.EsDslRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * 最终测试 - 使用真实日志文件完整测试
 */
public class FinalTest {
    
    private static int totalTraceFound = 0;
    private static int totalDslExtracted = 0;
    private static int getRequests = 0;
    private static int postRequests = 0;
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ES DSL 完整提取测试");
        System.out.println("========================================\n");
        
        String logFile = "日志.txt";
        StringBuilder buffer = new StringBuilder();
        int lineNum = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                // 检测TRACE日志
                if (line.contains("TRACE") && line.contains("RequestLogger") && line.contains("curl")) {
                    totalTraceFound++;
                    
                    // 判断是GET还是POST
                    if (line.contains(" GET ")) {
                        getRequests++;
                    } else if (line.contains(" POST ")) {
                        postRequests++;
                    }
                    
                    // 清空之前的缓冲区并开始新的收集
                    if (buffer.length() > 0) {
                        tryExtract(buffer.toString(), totalTraceFound - 1);
                    }
                    buffer.setLength(0);
                    buffer.append(line).append("\n");
                    continue;
                }
                
                // 如果缓冲区有TRACE日志，继续收集
                if (buffer.length() > 0 && buffer.toString().contains("TRACE")) {
                    // 检查是否是新的日志行（有时间戳）
                    if (line.matches("^\\d{4}-\\d{2}-\\d{2}.*") && 
                        !line.contains("TRACE") &&
                        !line.contains("DEBUG") &&
                        buffer.length() > 200) {
                        // 新日志开始，触发提取
                        tryExtract(buffer.toString(), totalTraceFound);
                        buffer.setLength(0);
                    } else if (line.startsWith("#") || line.contains("{") || line.trim().isEmpty()) {
                        // 继续收集响应数据
                        buffer.append(line).append("\n");
                    }
                }
            }
            
            // 处理最后的缓冲区
            if (buffer.length() > 100) {
                tryExtract(buffer.toString(), totalTraceFound);
            }
            
            // 输出统计
            System.out.println("\n========================================");
            System.out.println("  测试统计结果");
            System.out.println("========================================");
            System.out.println("日志总行数: " + lineNum);
            System.out.println("检测到TRACE日志: " + totalTraceFound);
            System.out.println("  - GET请求: " + getRequests);
            System.out.println("  - POST请求: " + postRequests);
            System.out.println("成功提取DSL: " + totalDslExtracted);
            System.out.println("提取成功率: " + String.format("%.1f%%", (totalDslExtracted * 100.0 / totalTraceFound)));
            System.out.println("========================================");
            
            if (totalDslExtracted == totalTraceFound) {
                System.out.println("✅✅✅ 完美！所有ES DSL都被成功提取！✅✅✅");
            } else {
                int missing = totalTraceFound - totalDslExtracted;
                System.out.println("⚠️  还有 " + missing + " 个DSL未能提取");
                System.out.println("需要进一步分析和修复");
            }
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void tryExtract(String text, int traceNum) {
        try {
            EsDslRecord record = EsDslParser.parseEsDsl(text, "test-project");
            if (record != null) {
                totalDslExtracted++;
                System.out.println("\n【TRACE #" + traceNum + "】✅ 提取成功");
                System.out.println("  方法: " + record.getMethod());
                System.out.println("  索引: " + (record.getIndex() != null ? record.getIndex() : "N/A"));
                System.out.println("  端点: " + (record.getEndpoint() != null ? record.getEndpoint() : "N/A"));
                System.out.println("  来源: " + record.getSource());
                if (record.getDslQuery() != null) {
                    int dslLen = record.getDslQuery().length();
                    System.out.println("  DSL长度: " + dslLen + " 字符");
                }
            } else {
                System.out.println("\n【TRACE #" + traceNum + "】❌ 提取失败");
                System.out.println("  缓冲区大小: " + text.length());
                System.out.println("  前150字符: " + text.substring(0, Math.min(150, text.length())));
            }
        } catch (Exception e) {
            System.out.println("\n【TRACE #" + traceNum + "】❌ 提取异常: " + e.getMessage());
        }
    }
}

