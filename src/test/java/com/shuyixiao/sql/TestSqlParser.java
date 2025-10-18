package com.shuyixiao.sql;

import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.parser.SqlParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL解析器测试
 * 使用日志.txt文件进行自测
 */
public class TestSqlParser {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  SQL Monitor 功能测试");
        System.out.println("========================================\n");
        
        StringBuilder buffer = new StringBuilder();
        List<SqlRecord> records = new ArrayList<>();
        int totalLines = 0;
        int sqlCount = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream("日志.txt"), StandardCharsets.UTF_8))) {
            
            String line;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                buffer.append(line).append("\n");
                
                // 当遇到Total行时，说明一条完整的SQL日志结束
                if (line.contains("<==") && line.contains("Total:")) {
                    sqlCount++;
                    
                    String context = buffer.toString();
                    
                    // 解析SQL
                    SqlRecord record = SqlParser.parseSql(context, "TestProject");
                    if (record != null) {
                        records.add(record);
                        
                        System.out.println("【SQL #" + records.size() + "】");
                        System.out.println("  操作: " + record.getOperation());
                        System.out.println("  表名: " + (record.getTableName() != null ? record.getTableName() : "N/A"));
                        System.out.println("  结果数: " + (record.getResultCount() != null ? record.getResultCount() : "N/A"));
                        System.out.println("  API路径: " + (record.getApiPath() != null ? record.getApiPath() : "N/A"));
                        System.out.println("  调用类: " + (record.getCallerClass() != null ? record.getCallerClass() : "N/A"));
                        System.out.println("  SQL: " + record.getShortSql());
                        System.out.println();
                    }
                    
                    // 清空缓冲区，但保留最近的内容用于API路径提取
                    if (buffer.length() > 50000) {
                        String remaining = buffer.substring(buffer.length() - 20000);
                        buffer.setLength(0);
                        buffer.append(remaining);
                    }
                }
                
                // 缓冲区太大时清理
                if (buffer.length() > 100000) {
                    String remaining = buffer.substring(buffer.length() - 50000);
                    buffer.setLength(0);
                    buffer.append(remaining);
                }
            }
            
            // 统计结果
            System.out.println("========================================");
            System.out.println("  测试统计结果");
            System.out.println("========================================");
            System.out.println("日志总行数: " + totalLines);
            System.out.println("检测到SQL日志: " + sqlCount);
            System.out.println("成功解析SQL: " + records.size());
            System.out.println("解析成功率: " + (sqlCount > 0 ? (records.size() * 100 / sqlCount) : 0) + "%");
            System.out.println();
            
            // 按操作类型统计
            int selectCount = 0;
            int insertCount = 0;
            int updateCount = 0;
            int deleteCount = 0;
            int withApiPath = 0;
            int withCallerClass = 0;
            
            for (SqlRecord record : records) {
                String op = record.getOperation();
                if ("SELECT".equals(op)) selectCount++;
                else if ("INSERT".equals(op)) insertCount++;
                else if ("UPDATE".equals(op)) updateCount++;
                else if ("DELETE".equals(op)) deleteCount++;
                
                if (record.getApiPath() != null) withApiPath++;
                if (record.getCallerClass() != null) withCallerClass++;
            }
            
            System.out.println("========================================");
            System.out.println("  操作类型统计");
            System.out.println("========================================");
            System.out.println("SELECT: " + selectCount);
            System.out.println("INSERT: " + insertCount);
            System.out.println("UPDATE: " + updateCount);
            System.out.println("DELETE: " + deleteCount);
            System.out.println();
            System.out.println("有API路径的记录: " + withApiPath + " / " + records.size());
            System.out.println("有调用类的记录: " + withCallerClass + " / " + records.size());
            System.out.println("========================================");
            
            if (records.size() == sqlCount && records.size() > 0) {
                System.out.println("✅✅✅ 完美！所有SQL都成功解析！✅✅✅");
            } else if (records.size() > 0) {
                System.out.println("✅ 部分SQL成功解析");
            } else {
                System.out.println("❌ 没有成功解析任何SQL");
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

