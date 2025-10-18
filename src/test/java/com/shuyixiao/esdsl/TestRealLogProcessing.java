package com.shuyixiao.esdsl;

import com.shuyixiao.esdsl.parser.EsDslParser;
import com.shuyixiao.esdsl.model.EsDslRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 测试真实日志文件的DSL提取
 * 模拟 EsDslOutputListener 的实际处理流程
 */
public class TestRealLogProcessing {
    
    private static final StringBuilder buffer = new StringBuilder();
    private static final List<EsDslRecord> extractedDsls = new ArrayList<>();
    
    // 时间戳提取模式(简化版,只提取时:分:秒,毫秒)
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+TRACE");
    
    public static void main(String[] args) {
        System.out.println("===== 测试真实日志文件的DSL提取 =====\n");
        
        String logFile = "日志.txt";
        int totalLines = 0;
        int traceLinesDetected = 0;
        int dslExtracted = 0;
        String lastTraceTimestamp = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile, StandardCharsets.UTF_8))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                
                // 模拟 onTextAvailable 的处理逻辑
                if (!line.isEmpty()) {
                    processLine(line);
                }
                
                // 检测TRACE日志
                if (line.contains("TRACE") && line.contains("RequestLogger") && line.contains("curl")) {
                    traceLinesDetected++;
                    String timestamp = extractTimestamp(line);
                    
                    System.out.println("\n【检测到TRACE日志 #" + traceLinesDetected + "】");
                    System.out.println("  时间戳: " + timestamp);
                    System.out.println("  行号: " + totalLines);
                    System.out.println("  前80字符: " + line.substring(0, Math.min(80, line.length())));
                    
                    // 检查是否应该清空缓冲区
                    if (lastTraceTimestamp != null && !lastTraceTimestamp.equals(timestamp)) {
                        System.out.println("  ⚠️  时间戳不同,应该清空缓冲区 (旧: " + lastTraceTimestamp + ", 新: " + timestamp + ")");
                    } else if (lastTraceTimestamp != null && lastTraceTimestamp.equals(timestamp)) {
                        System.out.println("  ⏭️  时间戳相同,忽略重复日志");
                    }
                    
                    lastTraceTimestamp = timestamp;
                }
                
                // 每检测到响应结束(以 ## 结尾或遇到新的INFO/DEBUG日志),尝试解析
                if (shouldTriggerParse(line) && buffer.length() > 500) {
                    String bufferedText = buffer.toString();
                    if (bufferedText.contains("TRACE") && bufferedText.contains("curl")) {
                        EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, "test-project");
                        if (record != null) {
                            dslExtracted++;
                            extractedDsls.add(record);
                            System.out.println("  ✅ DSL #" + dslExtracted + " 提取成功!");
                            System.out.println("     方法: " + record.getMethod());
                            System.out.println("     索引: " + (record.getIndex() != null ? record.getIndex() : "N/A"));
                            System.out.println("     端点: " + (record.getEndpoint() != null ? record.getEndpoint() : "N/A"));
                            System.out.println("     DSL长度: " + (record.getDslQuery() != null ? record.getDslQuery().length() : 0) + " 字符");
                        } else {
                            System.out.println("  ❌ 解析失败: 返回null");
                            System.out.println("     缓冲区大小: " + (bufferedText.length() / 1024) + "KB");
                            System.out.println("     缓冲区前200字符: " + bufferedText.substring(0, Math.min(200, bufferedText.length())));
                        }
                    }
                }
            }
            
            // 处理最后残留的缓冲区
            if (buffer.length() > 500) {
                String bufferedText = buffer.toString();
                if (bufferedText.contains("TRACE") && bufferedText.contains("curl")) {
                    EsDslRecord record = EsDslParser.parseEsDsl(bufferedText, "test-project");
                    if (record != null) {
                        dslExtracted++;
                        extractedDsls.add(record);
                        System.out.println("\n【最后残留缓冲区】");
                        System.out.println("  ✅ DSL #" + dslExtracted + " 提取成功!");
                        System.out.println("     方法: " + record.getMethod());
                        System.out.println("     索引: " + (record.getIndex() != null ? record.getIndex() : "N/A"));
                    }
                }
            }
            
            System.out.println("\n===== 处理完成 =====");
            System.out.println("总行数: " + totalLines);
            System.out.println("检测到TRACE日志: " + traceLinesDetected + " 次");
            System.out.println("成功提取DSL: " + dslExtracted + " 个");
            System.out.println("\n===== 提取的DSL详情 =====");
            
            for (int i = 0; i < extractedDsls.size(); i++) {
                EsDslRecord record = extractedDsls.get(i);
                System.out.println("\nDSL #" + (i + 1) + ":");
                System.out.println("  时间: " + record.getFormattedTimestamp());
                System.out.println("  方法: " + record.getMethod());
                System.out.println("  索引: " + (record.getIndex() != null ? record.getIndex() : "N/A"));
                System.out.println("  端点: " + (record.getEndpoint() != null ? record.getEndpoint() : "N/A"));
                System.out.println("  来源: " + record.getSource());
                if (record.getDslQuery() != null) {
                    String dsl = record.getDslQuery();
                    System.out.println("  DSL (前200字符): " + dsl.substring(0, Math.min(200, dsl.length())));
                }
            }
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理单行日志
     */
    private static void processLine(String line) {
        // 检测新TRACE日志
        if (line.contains("TRACE") && line.contains("RequestLogger") && line.contains("curl")) {
            // 如果缓冲区已有内容,检查时间戳
            if (buffer.length() > 100) {
                String lastTraceTime = extractLastTraceTimestamp(buffer.toString());
                String newTraceTime = extractTimestamp(line);
                
                // 时间戳不同,清空缓冲区
                if (lastTraceTime != null && newTraceTime != null && !lastTraceTime.equals(newTraceTime)) {
                    buffer.setLength(0);
                }
                // 时间戳相同,忽略(不添加)
                else if (lastTraceTime != null && lastTraceTime.equals(newTraceTime)) {
                    return;
                }
            }
            
            // 无条件添加新TRACE日志
            buffer.append(line).append("\n");
            return;
        }
        
        // 其他行通过shouldKeepText检查
        if (shouldKeepText(line)) {
            buffer.append(line).append("\n");
        }
    }
    
    /**
     * 判断是否应该保留该行
     */
    private static boolean shouldKeepText(String text) {
        String lowerText = text.toLowerCase();
        
        // 过滤掉Spring框架日志和数据库日志
        if (lowerText.contains("basejdbclogger") ||
            lowerText.contains("preparing:") ||
            lowerText.contains("parameters:") ||
            lowerText.contains("==>") ||
            lowerText.contains("<==") ||
            lowerText.contains("platformauthserviceimpl") ||
            lowerText.contains("knowledgeelementdetailcontroller") ||
            lowerText.contains("vectordataretrieverelastic") ||
            lowerText.contains("vectorassistant") ||
            lowerText.contains("nettyinternallogger") ||
            lowerText.contains("dingtalk")) {
            return false;
        }
        
        // 保留RequestLogger的TRACE日志
        if (lowerText.contains("requestlogger") && lowerText.contains("trace")) {
            return true;
        }
        
        // 如果缓冲区已有RequestLogger内容,保留后续行
        if (buffer.length() > 0 && buffer.toString().contains("RequestLogger")) {
            // 保留响应行、curl参数、JSON等
            if (text.startsWith("#") ||
                text.contains("'") ||
                text.contains("-d") ||
                text.contains("{") ||
                text.trim().isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否应该触发解析
     */
    private static boolean shouldTriggerParse(String line) {
        // 遇到新的INFO/DEBUG/TRACE日志时触发解析
        return line.matches("^\\d{4}-\\d{2}-\\d{2}.*") && 
               (line.contains(" INFO ") || line.contains(" DEBUG ") || 
                (line.contains(" TRACE ") && !line.contains("RequestLogger")));
    }
    
    /**
     * 从文本中提取时间戳
     */
    private static String extractTimestamp(String text) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * 从缓冲区提取最后一个TRACE时间戳
     */
    private static String extractLastTraceTimestamp(String buffer) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(buffer);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }
}

