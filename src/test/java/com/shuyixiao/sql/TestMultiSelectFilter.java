package com.shuyixiao.sql;

import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.sql.service.SqlRecordService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 测试多选操作类型筛选功能
 */
public class TestMultiSelectFilter {
    
    private SqlRecordService recordService;
    
    @Before
    public void setUp() {
        // 注意：这里需要一个 mock 的 Project 对象
        // 在实际测试中，你可能需要使用 IntelliJ 的测试框架
        // 这里只是演示逻辑
    }
    
    @Test
    public void testMultipleOperationFilter() {
        // 创建测试数据
        List<SqlRecord> testRecords = Arrays.asList(
            createRecord("SELECT", "user", "SELECT * FROM user"),
            createRecord("INSERT", "user", "INSERT INTO user VALUES (1, 'test')"),
            createRecord("UPDATE", "user", "UPDATE user SET name='test'"),
            createRecord("DELETE", "user", "DELETE FROM user WHERE id=1"),
            createRecord("SELECT", "order", "SELECT * FROM order")
        );
        
        // 测试筛选 SELECT 和 INSERT
        List<String> operations = Arrays.asList("SELECT", "INSERT");
        List<SqlRecord> filtered = filterByOperations(testRecords, operations);
        
        assertEquals(3, filtered.size());
        assertTrue(filtered.stream().anyMatch(r -> r.getOperation().equals("SELECT")));
        assertTrue(filtered.stream().anyMatch(r -> r.getOperation().equals("INSERT")));
        assertFalse(filtered.stream().anyMatch(r -> r.getOperation().equals("UPDATE")));
        assertFalse(filtered.stream().anyMatch(r -> r.getOperation().equals("DELETE")));
    }
    
    @Test
    public void testSingleOperationFilter() {
        List<SqlRecord> testRecords = Arrays.asList(
            createRecord("SELECT", "user", "SELECT * FROM user"),
            createRecord("INSERT", "user", "INSERT INTO user VALUES (1, 'test')"),
            createRecord("UPDATE", "user", "UPDATE user SET name='test'")
        );
        
        // 测试只筛选 UPDATE
        List<String> operations = Arrays.asList("UPDATE");
        List<SqlRecord> filtered = filterByOperations(testRecords, operations);
        
        assertEquals(1, filtered.size());
        assertEquals("UPDATE", filtered.get(0).getOperation());
    }
    
    @Test
    public void testAllOperationsFilter() {
        List<SqlRecord> testRecords = Arrays.asList(
            createRecord("SELECT", "user", "SELECT * FROM user"),
            createRecord("INSERT", "user", "INSERT INTO user VALUES (1, 'test')"),
            createRecord("UPDATE", "user", "UPDATE user SET name='test'"),
            createRecord("DELETE", "user", "DELETE FROM user WHERE id=1")
        );
        
        // 测试全选
        List<String> operations = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE");
        List<SqlRecord> filtered = filterByOperations(testRecords, operations);
        
        assertEquals(4, filtered.size());
    }
    
    @Test
    public void testEmptyOperationsFilter() {
        List<SqlRecord> testRecords = Arrays.asList(
            createRecord("SELECT", "user", "SELECT * FROM user"),
            createRecord("INSERT", "user", "INSERT INTO user VALUES (1, 'test')")
        );
        
        // 测试空选择（应该返回所有记录）
        List<String> operations = Arrays.asList();
        List<SqlRecord> filtered = filterByOperations(testRecords, operations);
        
        assertEquals(2, filtered.size());
    }
    
    // 辅助方法：创建测试记录
    private SqlRecord createRecord(String operation, String tableName, String sql) {
        SqlRecord record = new SqlRecord();
        record.setOperation(operation);
        record.setTableName(tableName);
        record.setSqlStatement(sql);
        record.setTimestamp(LocalDateTime.now());
        record.setProject("TestProject");
        record.setSource("Console");
        return record;
    }
    
    // 辅助方法：模拟筛选逻辑
    private List<SqlRecord> filterByOperations(List<SqlRecord> records, List<String> operations) {
        if (operations == null || operations.isEmpty()) {
            return records;
        }
        
        return records.stream()
            .filter(record -> operations.stream()
                .anyMatch(op -> op.equalsIgnoreCase(record.getOperation())))
            .collect(java.util.stream.Collectors.toList());
    }
}

