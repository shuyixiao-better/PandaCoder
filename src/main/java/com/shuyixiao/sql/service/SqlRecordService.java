package com.shuyixiao.sql.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.sql.model.SqlRecord;
import com.shuyixiao.bugrecorder.util.LocalDateTimeAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * SQL 记录服务
 * 负责管理 SQL 查询记录的存储和检索
 */
@Service
public final class SqlRecordService {
    
    private static final Logger LOG = Logger.getInstance(SqlRecordService.class);
    private static final String STORAGE_FILE = "sql-records.json";
    private static final int MAX_RECORDS = 1000; // 最多保存1000条记录
    
    @SuppressWarnings("unused")
    private final Project project;
    private final CopyOnWriteArrayList<SqlRecord> records = new CopyOnWriteArrayList<>();
    private final Gson gson;
    private final File storageFile;
    
    // 记录更新监听器列表（用于实时通知UI）
    private final List<Consumer<SqlRecord>> recordListeners = new CopyOnWriteArrayList<>();
    
    public SqlRecordService(Project project) {
        this.project = project;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
        
        // 存储文件位置
        String projectPath = project.getBasePath();
        if (projectPath != null) {
            File ideaDir = new File(projectPath, ".idea");
            if (!ideaDir.exists()) {
                ideaDir.mkdirs();
            }
            this.storageFile = new File(ideaDir, STORAGE_FILE);
        } else {
            this.storageFile = null;
        }
        
        // 加载历史记录
        loadRecords();
    }
    
    /**
     * 添加新的 SQL 记录
     */
    public void addRecord(SqlRecord record) {
        if (record == null) {
            return;
        }
        
        try {
            records.add(0, record); // 添加到列表开头（最新的在前面）
            
            // 如果超过最大记录数，删除最旧的记录
            if (records.size() > MAX_RECORDS) {
                records.remove(records.size() - 1);
            }
            
            // 异步保存到文件
            saveRecordsAsync();
            
            LOG.debug("Added SQL record: " + record.getId());
            
            // 通知所有监听器（实时更新UI）
            notifyListeners(record);
            
        } catch (Exception e) {
            LOG.error("Failed to add SQL record", e);
        }
    }
    
    /**
     * 添加记录监听器
     */
    public void addRecordListener(Consumer<SqlRecord> listener) {
        if (listener != null && !recordListeners.contains(listener)) {
            recordListeners.add(listener);
            LOG.debug("Added SQL record listener, total listeners: " + recordListeners.size());
        }
    }
    
    /**
     * 移除记录监听器
     */
    public void removeRecordListener(Consumer<SqlRecord> listener) {
        recordListeners.remove(listener);
        LOG.debug("Removed SQL record listener, total listeners: " + recordListeners.size());
    }
    
    /**
     * 通知所有监听器
     */
    private void notifyListeners(SqlRecord record) {
        for (Consumer<SqlRecord> listener : recordListeners) {
            try {
                listener.accept(record);
            } catch (Exception e) {
                LOG.warn("Error notifying SQL record listener", e);
            }
        }
    }
    
    /**
     * 获取所有记录
     */
    public List<SqlRecord> getAllRecords() {
        return new ArrayList<>(records);
    }
    
    /**
     * 获取最近的记录
     */
    public List<SqlRecord> getRecentRecords(int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        return records.stream()
                .filter(record -> record.getTimestamp().isAfter(cutoffTime))
                .collect(Collectors.toList());
    }
    
    /**
     * 按表名筛选
     */
    public List<SqlRecord> getRecordsByTable(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return getAllRecords();
        }
        
        return records.stream()
                .filter(record -> tableName.equalsIgnoreCase(record.getTableName()))
                .collect(Collectors.toList());
    }
    
    /**
     * 按操作类型筛选
     */
    public List<SqlRecord> getRecordsByOperation(String operation) {
        if (operation == null || operation.isEmpty()) {
            return getAllRecords();
        }
        
        return records.stream()
                .filter(record -> operation.equalsIgnoreCase(record.getOperation()))
                .collect(Collectors.toList());
    }
    
    /**
     * 搜索记录
     */
    public List<SqlRecord> searchRecords(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return getAllRecords();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return records.stream()
                .filter(record -> 
                    (record.getSqlStatement() != null && record.getSqlStatement().toLowerCase().contains(lowerKeyword)) ||
                    (record.getTableName() != null && record.getTableName().toLowerCase().contains(lowerKeyword)) ||
                    (record.getApiPath() != null && record.getApiPath().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }
    
    /**
     * 清除所有记录
     */
    public void clearAllRecords() {
        records.clear();
        saveRecordsAsync();
        LOG.info("Cleared all SQL records");
    }
    
    /**
     * 清除指定时间之前的记录
     */
    public void clearOldRecords(int days) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        records.removeIf(record -> record.getTimestamp().isBefore(cutoffTime));
        saveRecordsAsync();
        LOG.info("Cleared SQL records older than " + days + " days");
    }
    
    /**
     * 获取统计信息
     */
    public Statistics getStatistics() {
        int totalCount = records.size();
        int selectCount = (int) records.stream().filter(r -> "SELECT".equals(r.getOperation())).count();
        int insertCount = (int) records.stream().filter(r -> "INSERT".equals(r.getOperation())).count();
        int updateCount = (int) records.stream().filter(r -> "UPDATE".equals(r.getOperation())).count();
        int deleteCount = (int) records.stream().filter(r -> "DELETE".equals(r.getOperation())).count();
        
        // 统计各个表的查询次数
        long distinctTables = records.stream()
                .map(SqlRecord::getTableName)
                .filter(table -> table != null && !table.isEmpty())
                .distinct()
                .count();
        
        return new Statistics(totalCount, selectCount, insertCount, updateCount, deleteCount, (int) distinctTables);
    }
    
    /**
     * 从文件加载记录
     */
    private void loadRecords() {
        if (storageFile == null || !storageFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(storageFile)) {
            Type listType = new TypeToken<ArrayList<SqlRecord>>() {}.getType();
            List<SqlRecord> loadedRecords = gson.fromJson(reader, listType);
            
            if (loadedRecords != null) {
                records.addAll(loadedRecords);
                LOG.info("Loaded " + loadedRecords.size() + " SQL records from file");
            }
        } catch (IOException e) {
            LOG.warn("Failed to load SQL records from file", e);
        }
    }
    
    /**
     * 保存记录到文件（异步）
     */
    private void saveRecordsAsync() {
        if (storageFile == null) {
            return;
        }
        
        // 在后台线程中保存
        new Thread(() -> {
            try (FileWriter writer = new FileWriter(storageFile)) {
                gson.toJson(records, writer);
                LOG.debug("Saved " + records.size() + " SQL records to file");
            } catch (IOException e) {
                LOG.error("Failed to save SQL records to file", e);
            }
        }, "SQL-Record-Saver").start();
    }
    
    /**
     * 统计信息类
     */
    public static class Statistics {
        private final int totalCount;
        private final int selectCount;
        private final int insertCount;
        private final int updateCount;
        private final int deleteCount;
        private final int distinctTables;
        
        public Statistics(int totalCount, int selectCount, int insertCount, 
                         int updateCount, int deleteCount, int distinctTables) {
            this.totalCount = totalCount;
            this.selectCount = selectCount;
            this.insertCount = insertCount;
            this.updateCount = updateCount;
            this.deleteCount = deleteCount;
            this.distinctTables = distinctTables;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public int getSelectCount() {
            return selectCount;
        }
        
        public int getInsertCount() {
            return insertCount;
        }
        
        public int getUpdateCount() {
            return updateCount;
        }
        
        public int getDeleteCount() {
            return deleteCount;
        }
        
        public int getDistinctTables() {
            return distinctTables;
        }
    }
}

