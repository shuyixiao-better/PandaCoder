package com.shuyixiao.sql;

import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.parser.SqlParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * SQL Monitor Bug修复测试
 * 测试两个修复：
 * 1. API路径提取
 * 2. 可执行SQL生成（参数替换）
 */
public class TestSqlBugFix {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  SQL Monitor Bug修复测试");
        System.out.println("========================================\n");
        
        testApiPathExtraction();
        System.out.println("\n========================================\n");
        testExecutableSqlGeneration();
    }
    
    /**
     * 测试1: API路径提取
     */
    private static void testApiPathExtraction() {
        System.out.println("【测试1: API路径提取】\n");
        
        StringBuilder buffer = new StringBuilder();
        int sqlCount = 0;
        int apiPathCount = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream("日志.txt"), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
                
                // 当遇到Total行时，说明一条完整的SQL日志结束
                if (line.contains("<==") && line.contains("Total:")) {
                    sqlCount++;
                    
                    String context = buffer.toString();
                    SqlRecord record = SqlParser.parseSql(context, "TestProject");
                    
                    if (record != null) {
                        String apiPath = record.getApiPath();
                        if (apiPath != null && !apiPath.isEmpty()) {
                            apiPathCount++;
                            System.out.println("✅ SQL #" + sqlCount + " - API路径: " + apiPath);
                        } else {
                            System.out.println("❌ SQL #" + sqlCount + " - API路径: N/A");
                            // 显示缓冲区前500字符用于调试
                            String preview = context.length() > 500 ? 
                                context.substring(0, 500) + "..." : context;
                            System.out.println("   缓冲区预览: " + preview.replace("\n", " "));
                        }
                    }
                    
                    // 保留100KB上下文
                    if (buffer.length() > 100000) {
                        String remaining = buffer.substring(buffer.length() - 100000);
                        buffer.setLength(0);
                        buffer.append(remaining);
                    }
                }
            }
            
            System.out.println("\n统计结果:");
            System.out.println("总SQL数: " + sqlCount);
            System.out.println("有API路径: " + apiPathCount);
            System.out.println("API路径提取率: " + (sqlCount > 0 ? (apiPathCount * 100 / sqlCount) : 0) + "%");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试2: 可执行SQL生成
     */
    private static void testExecutableSqlGeneration() {
        System.out.println("【测试2: 可执行SQL生成】\n");
        
        // 测试用例
        String[][] testCases = {
            // 单个String参数
            {
                "SELECT * FROM user WHERE id = ?",
                "0(String)",
                "SELECT * FROM user WHERE id = '0'"
            },
            // 多个参数（String + String）
            {
                "SELECT * FROM element WHERE tenant_id = ? AND code = ?",
                "1943230203698479104(String), 1950177370535153664(String)",
                "SELECT * FROM element WHERE tenant_id = '1943230203698479104' AND code = '1950177370535153664'"
            },
            // Integer参数
            {
                "SELECT * FROM order WHERE user_id = ? AND status = ?",
                "123(Integer), 1(Integer)",
                "SELECT * FROM order WHERE user_id = 123 AND status = 1"
            },
            // 混合类型参数
            {
                "SELECT * FROM log WHERE user_id = ? AND created_at > ? LIMIT ?",
                "1001(Long), 2025-01-01 00:00:00(Timestamp), 10(Integer)",
                "SELECT * FROM log WHERE user_id = 1001 AND created_at > '2025-01-01 00:00:00' LIMIT 10"
            }
        };
        
        int passCount = 0;
        
        for (int i = 0; i < testCases.length; i++) {
            String sql = testCases[i][0];
            String params = testCases[i][1];
            String expected = testCases[i][2];
            
            // 创建记录
            SqlRecord record = SqlRecord.builder()
                .sqlStatement(sql)
                .parameters(params)
                .build();
            
            // 生成可执行SQL
            String executable = record.getExecutableSql();
            
            // 验证结果
            boolean pass = executable.equals(expected);
            if (pass) {
                passCount++;
                System.out.println("✅ 测试 #" + (i + 1) + " 通过");
            } else {
                System.out.println("❌ 测试 #" + (i + 1) + " 失败");
                System.out.println("   原始SQL: " + sql);
                System.out.println("   参数: " + params);
                System.out.println("   期望: " + expected);
                System.out.println("   实际: " + executable);
            }
        }
        
        System.out.println("\n统计结果:");
        System.out.println("总测试: " + testCases.length);
        System.out.println("通过: " + passCount);
        System.out.println("失败: " + (testCases.length - passCount));
        System.out.println("通过率: " + (passCount * 100 / testCases.length) + "%");
    }
}

