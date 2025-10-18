package com.shuyixiao.sql;

import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.parser.SqlParser;

/**
 * 测试真实日志解析
 */
public class TestRealLogParsing {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  真实日志解析测试");
        System.out.println("========================================\n");
        
        // 模拟真实的缓冲区内容
        String logContent = 
            "2025-10-18 22:20:40,723 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT id,creator,create_time,modifier,modifier_time,sort,tenant_id,license_content FROM saas_knowledge_license WHERE (tenant_id = ?)\n" +
            "2025-10-18 22:20:40,733 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: 0(String)\n" +
            "2025-10-18 22:20:40,744 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1\n";
        
        System.out.println("【日志内容】");
        System.out.println(logContent);
        System.out.println();
        
        // 解析
        SqlRecord record = SqlParser.parseSql(logContent, "TestProject");
        
        if (record == null) {
            System.out.println("❌ 解析失败！返回 null");
            return;
        }
        
        System.out.println("【解析结果】");
        System.out.println("SQL语句: " + record.getSqlStatement());
        System.out.println("参数: " + record.getParameters());
        System.out.println("表名: " + record.getTableName());
        System.out.println("操作: " + record.getOperation());
        System.out.println("结果数: " + record.getResultCount());
        System.out.println();
        
        System.out.println("【可执行SQL】");
        String executableSql = record.getExecutableSql();
        System.out.println(executableSql);
        System.out.println();
        
        // 验证
        String expected = "SELECT id,creator,create_time,modifier,modifier_time,sort,tenant_id,license_content FROM saas_knowledge_license WHERE (tenant_id = '0')";
        
        if (executableSql.equals(expected)) {
            System.out.println("✅ 测试通过！参数替换成功！");
        } else {
            System.out.println("❌ 测试失败！参数没有被替换！");
            System.out.println("期望: " + expected);
            System.out.println("实际: " + executableSql);
        }
    }
}

