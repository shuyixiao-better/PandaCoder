package com.shuyixiao.sql;

import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.parser.SqlParser;

/**
 * 测试多参数SQL解析
 */
public class TestMultipleParameters {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  多参数SQL解析测试");
        System.out.println("========================================\n");
        
        // 测试1：两个参数
        testCase(
            "Test 1: 两个String参数",
            "2025-10-18 22:21:30,509 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT * FROM saas_knowledge_element WHERE (tenant_id = ? AND code = ?)\n" +
            "2025-10-18 22:21:30,509 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: 1943230203698479104(String), 1950177370535153664(String)\n" +
            "2025-10-18 22:21:30,511 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1\n",
            "SELECT * FROM saas_knowledge_element WHERE (tenant_id = '1943230203698479104' AND code = '1950177370535153664')"
        );
        
        // 测试2：多个混合类型参数
        testCase(
            "Test 2: 混合类型参数",
            "2025-10-18 23:39:50,143 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT count(0) FROM saas_knowledge_element WHERE (tenant_id = ? AND container_id = ? AND parent_code = ? AND (process_status <> ? OR (process_status = ? AND creator = ?)) AND permission_status = ?)\n" +
            "2025-10-18 23:39:50,143 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: 1943230203698479104(String), 1945410884406898688(String), 0(String), -1(Integer), -1(Integer), 1943230204135182336(String), 1(Integer)\n" +
            "2025-10-18 23:39:50,144 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1\n",
            "SELECT count(0) FROM saas_knowledge_element WHERE (tenant_id = '1943230203698479104' AND container_id = '1945410884406898688' AND parent_code = '0' AND (process_status <> -1 OR (process_status = -1 AND creator = '1943230204135182336')) AND permission_status = 1)"
        );
        
        // 测试3：空参数
        testCase(
            "Test 3: 无参数",
            "2025-10-18 22:20:40,809 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT * FROM app_channel_dingtalk\n" +
            "2025-10-18 22:20:40,809 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: \n" +
            "2025-10-18 22:20:40,810 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1\n",
            "SELECT * FROM app_channel_dingtalk"
        );
    }
    
    private static void testCase(String title, String logContent, String expected) {
        System.out.println("【" + title + "】");
        
        SqlRecord record = SqlParser.parseSql(logContent, "TestProject");
        
        if (record == null) {
            System.out.println("❌ 解析失败！");
            return;
        }
        
        String executableSql = record.getExecutableSql();
        
        if (executableSql.equals(expected)) {
            System.out.println("✅ 测试通过！");
            System.out.println("   参数: " + record.getParameters());
            System.out.println("   结果: " + executableSql.substring(0, Math.min(80, executableSql.length())) + "...");
        } else {
            System.out.println("❌ 测试失败！");
            System.out.println("   期望: " + expected);
            System.out.println("   实际: " + executableSql);
        }
        
        System.out.println();
    }
}

