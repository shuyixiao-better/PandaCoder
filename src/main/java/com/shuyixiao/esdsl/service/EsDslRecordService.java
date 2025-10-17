package com.shuyixiao.esdsl.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.esdsl.model.EsDslRecord;
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
import java.util.stream.Collectors;

/**
 * ES DSL 记录服务
 * 负责管理 ES DSL 查询记录的存储和检索
 */
@Service
public final class EsDslRecordService {
    
    private static final Logger LOG = Logger.getInstance(EsDslRecordService.class);
    private static final String STORAGE_FILE = "es-dsl-records.json";
    private static final int MAX_RECORDS = 1000; // 最多保存1000条记录
    
    @SuppressWarnings("unused")
    private final Project project;
    private final CopyOnWriteArrayList<EsDslRecord> records = new CopyOnWriteArrayList<>();
    private final Gson gson;
    private final File storageFile;
    
    public EsDslRecordService(Project project) {
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
     * 添加新的 ES DSL 记录
     */
    public void addRecord(EsDslRecord record) {
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
            
            LOG.debug("Added ES DSL record: " + record.getId());
        } catch (Exception e) {
            LOG.error("Failed to add ES DSL record", e);
        }
    }
    
    /**
     * 获取所有记录
     */
    public List<EsDslRecord> getAllRecords() {
        return new ArrayList<>(records);
    }
    
    /**
     * 获取最近的记录
     */
    public List<EsDslRecord> getRecentRecords(int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        return records.stream()
                .filter(record -> record.getTimestamp().isAfter(cutoffTime))
                .collect(Collectors.toList());
    }
    
    /**
     * 按索引名称筛选
     */
    public List<EsDslRecord> getRecordsByIndex(String index) {
        if (index == null || index.isEmpty()) {
            return getAllRecords();
        }
        
        return records.stream()
                .filter(record -> index.equalsIgnoreCase(record.getIndex()))
                .collect(Collectors.toList());
    }
    
    /**
     * 按方法筛选
     */
    public List<EsDslRecord> getRecordsByMethod(String method) {
        if (method == null || method.isEmpty()) {
            return getAllRecords();
        }
        
        return records.stream()
                .filter(record -> method.equalsIgnoreCase(record.getMethod()))
                .collect(Collectors.toList());
    }
    
    /**
     * 搜索记录
     */
    public List<EsDslRecord> searchRecords(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return getAllRecords();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return records.stream()
                .filter(record -> 
                    (record.getDslQuery() != null && record.getDslQuery().toLowerCase().contains(lowerKeyword)) ||
                    (record.getIndex() != null && record.getIndex().toLowerCase().contains(lowerKeyword)) ||
                    (record.getEndpoint() != null && record.getEndpoint().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }
    
    /**
     * 清除所有记录
     */
    public void clearAllRecords() {
        records.clear();
        saveRecordsAsync();
        LOG.info("Cleared all ES DSL records");
    }
    
    /**
     * 清除指定时间之前的记录
     */
    public void clearOldRecords(int days) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        records.removeIf(record -> record.getTimestamp().isBefore(cutoffTime));
        saveRecordsAsync();
        LOG.info("Cleared ES DSL records older than " + days + " days");
    }
    
    /**
     * 获取统计信息
     */
    public Statistics getStatistics() {
        int totalCount = records.size();
        int successCount = (int) records.stream().filter(EsDslRecord::isSuccess).count();
        int failureCount = totalCount - successCount;
        
        // 统计各个索引的查询次数
        long distinctIndexes = records.stream()
                .map(EsDslRecord::getIndex)
                .filter(index -> index != null && !index.isEmpty())
                .distinct()
                .count();
        
        return new Statistics(totalCount, successCount, failureCount, (int) distinctIndexes);
    }
    
    /**
     * 从文件加载记录
     */
    private void loadRecords() {
        if (storageFile == null || !storageFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(storageFile)) {
            Type listType = new TypeToken<ArrayList<EsDslRecord>>() {}.getType();
            List<EsDslRecord> loadedRecords = gson.fromJson(reader, listType);
            
            if (loadedRecords != null) {
                records.addAll(loadedRecords);
                LOG.info("Loaded " + loadedRecords.size() + " ES DSL records from file");
            }
        } catch (IOException e) {
            LOG.warn("Failed to load ES DSL records from file", e);
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
                LOG.debug("Saved " + records.size() + " ES DSL records to file");
            } catch (IOException e) {
                LOG.error("Failed to save ES DSL records to file", e);
            }
        }, "ES-DSL-Record-Saver").start();
    }
    
    /**
     * 统计信息类
     */
    public static class Statistics {
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final int distinctIndexes;
        
        public Statistics(int totalCount, int successCount, int failureCount, int distinctIndexes) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.distinctIndexes = distinctIndexes;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        public int getDistinctIndexes() {
            return distinctIndexes;
        }
    }
}

