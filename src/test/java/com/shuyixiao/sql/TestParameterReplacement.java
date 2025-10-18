package com.shuyixiao.sql;

import com.shuyixiao.sql.model.SqlRecord;

/**
 * 测试参数替换功能
 */
public class TestParameterReplacement {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  参数替换功能测试");
        System.out.println("========================================\n");
        
        // 测试案例1：单个String参数
        testCase(
            "SELECT id,creator,create_time,modifier,modifier_time,sort,tenant_id,license_content FROM saas_knowledge_license WHERE (tenant_id = ?)",
            "0(String)",
            "SELECT id,creator,create_time,modifier,modifier_time,sort,tenant_id,license_content FROM saas_knowledge_license WHERE (tenant_id = '0')"
        );
        
        // 测试案例2：两个String参数
        testCase(
            "SELECT * FROM saas_knowledge_element WHERE (tenant_id = ? AND code = ?)",
            "1943230203698479104(String), 1950177370535153664(String)",
            "SELECT * FROM saas_knowledge_element WHERE (tenant_id = '1943230203698479104' AND code = '1950177370535153664')"
        );
        
        // 测试案例3：空参数
        testCase(
            "SELECT * FROM app_channel_dingtalk",
            "",
            "SELECT * FROM app_channel_dingtalk"
        );
        
        // 测试案例4：复杂参数（4个参数）
        testCase(
            "SELECT * FROM kb_page_event WHERE (tenant_id = ? AND page_code = ? AND event = ? AND user_code = ?)",
            "1943230203698479104(String), 1950373381136228352(String), EVENT_PAGE_BROWSER(String), 1943230204135182336(String)",
            "SELECT * FROM kb_page_event WHERE (tenant_id = '1943230203698479104' AND page_code = '1950373381136228352' AND event = 'EVENT_PAGE_BROWSER' AND user_code = '1943230204135182336')"
        );
    }
    
    private static void testCase(String sql, String params, String expected) {
        System.out.println("【测试用例】");
        System.out.println("原始SQL: " + sql);
        System.out.println("参数: " + params);
        System.out.println();
        
        SqlRecord record = SqlRecord.builder()
            .sqlStatement(sql)
            .parameters(params)
            .build();
        
        String executable = record.getExecutableSql();
        
        System.out.println("期望结果:");
        System.out.println(expected);
        System.out.println();
        System.out.println("实际结果:");
        System.out.println(executable);
        System.out.println();
        
        if (executable.equals(expected)) {
            System.out.println("✅ 测试通过！");
        } else {
            System.out.println("❌ 测试失败！");
            System.out.println("差异:");
            System.out.println("  期望: " + expected.length() + " 字符");
            System.out.println("  实际: " + executable.length() + " 字符");
        }
        
        System.out.println("\n========================================\n");
    }
}

