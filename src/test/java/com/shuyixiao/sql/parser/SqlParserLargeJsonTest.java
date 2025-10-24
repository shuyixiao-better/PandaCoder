package com.shuyixiao.sql.parser;

import com.shuyixiao.sql.model.SqlRecord;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * SQL Parser Large JSON Test
 */
public class SqlParserLargeJsonTest {
    
    @Test
    public void testParseMultilineParameters() {
        // Test multiline parameter extraction
        String testLog = "2025-10-23 17:34:58,098 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: UPDATE test SET data=?, name=? WHERE id=?\n" +
                "2025-10-23 17:34:58,099 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: this is a long text\n" +
                "with multiple lines\n" +
                "and even JSON\n" +
                "{\"key\": \"value\"}(String), John Doe(String), 123(Integer)\n" +
                "2025-10-23 17:34:58,101 DEBUG (BaseJdbcLogger.java:135)- <==    Updates: 1";
        
        SqlRecord record = SqlParser.parseSql(testLog, "TestProject");
        
        assertNotNull("Should parse successfully", record);
        assertNotNull("Should extract parameters", record.getParameters());
        
        String params = record.getParameters();
        System.out.println("Extracted parameters: " + params);
        
        // Verify multiline parameters are extracted
        assertTrue("Should contain multiline content", params.contains("with multiple lines"));
        assertTrue("Should contain JSON", params.contains("{\"key\": \"value\"}"));
        
        // Test executable SQL
        String executableSql = record.getExecutableSql();
        System.out.println("Executable SQL: " + executableSql);
        
        assertFalse("Should not contain ? placeholders", executableSql.contains("?"));
        assertTrue("Should contain the JSON value", executableSql.contains("{\"key\": \"value\"}"));
    }
    
    @Test
    public void testReplaceParametersWithSimpleValues() {
        String sql = "SELECT * FROM users WHERE id=? AND name=? AND age=?";
        String params = "123(Integer), Alice(String), 25(Integer)";
        
        String result = SqlParser.replaceParameters(sql, params);
        
        System.out.println("Original: " + sql);
        System.out.println("Parameters: " + params);
        System.out.println("Result: " + result);
        
        assertFalse("Should not contain ?", result.contains("?"));
        assertTrue("Should contain id value", result.contains("123"));
        assertTrue("Should contain name value", result.contains("Alice"));
        assertTrue("Should contain age value", result.contains("25"));
    }
    
    @Test
    public void testReplaceParametersWithComplexJson() {
        String sql = "UPDATE table SET data=?, name=? WHERE id=?";
        String params = "{\"key\":\"value\",\"nested\":{\"a\":1}}(String), John Doe(String), 456(Integer)";
        
        String result = SqlParser.replaceParameters(sql, params);
        
        System.out.println("Original: " + sql);
        System.out.println("Parameters: " + params);
        System.out.println("Result: " + result);
        
        assertFalse("Should not contain ?", result.contains("?"));
        assertTrue("Should contain JSON", result.contains("{\"key\":\"value\""));
        assertTrue("Should contain name", result.contains("John Doe"));
        assertTrue("Should contain id", result.contains("456"));
    }
    
    @Test
    public void testParseLargeJsonFromLogFile() throws Exception {
        File logFile = new File("日志.txt");
        if (!logFile.exists()) {
            System.out.println("Log file not found, skipping test");
            return;
        }
        
        String logContent = new String(Files.readAllBytes(logFile.toPath()), "UTF-8");
        
        System.out.println("========================================");
        System.out.println("Testing large JSON SQL parsing");
        System.out.println("========================================");
        System.out.println("Log file size: " + (logContent.length() / 1024) + " KB");
        System.out.println();
        
        SqlRecord record = SqlParser.parseSql(logContent, "TestProject");
        
        assertNotNull("Should parse successfully", record);
        
        System.out.println("=== SQL Record Details ===");
        System.out.println("Project: " + record.getProject());
        System.out.println("Time: " + record.getFormattedTimestamp());
        System.out.println("Source: " + record.getSource());
        System.out.println("Operation: " + record.getOperation());
        System.out.println("Table: " + record.getTableName());
        System.out.println("Result Count: " + record.getResultCount());
        System.out.println("API Path: " + (record.getApiPath() != null ? record.getApiPath() : "N/A"));
        System.out.println();
        
        assertEquals("Operation should be UPDATE", "UPDATE", record.getOperation());
        assertEquals("Table should be saas_prompt_template", "saas_prompt_template", record.getTableName());
        
        String parameters = record.getParameters();
        assertNotNull("Parameters should not be null", parameters);
        System.out.println("Parameters length: " + parameters.length() + " chars");
        assertTrue("Parameters should be large (contain JSON)", parameters.length() > 1000);
        
        String executableSql = record.getExecutableSql();
        assertNotNull("Executable SQL should not be null", executableSql);
        System.out.println("Executable SQL length: " + executableSql.length() + " chars");
        
        assertFalse("Executable SQL should not contain ? placeholders", executableSql.contains("?"));
        assertTrue("Executable SQL should be longer than original", 
            executableSql.length() > record.getSqlStatement().length());
        
        System.out.println();
        System.out.println("========================================");
        System.out.println("All tests passed!");
        System.out.println("========================================");
        
        // Save executable SQL for verification
        File outputFile = new File("test-executable-sql.txt");
        Files.write(outputFile.toPath(), executableSql.getBytes("UTF-8"));
        System.out.println("Executable SQL saved to: test-executable-sql.txt");
    }
}

