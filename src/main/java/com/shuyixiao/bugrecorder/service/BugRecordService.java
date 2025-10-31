package com.shuyixiao.bugrecorder.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.BugStatus;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.parser.ErrorParser;
import com.shuyixiao.bugrecorder.util.LocalDateTimeAdapter;
import com.shuyixiao.setting.PluginSettings;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Bug记录存储与管理服务
 * 负责持久化存储识别到的Bug记录，并提供日志文件的管理
 */
@Service
public final class BugRecordService {

    private static final Logger LOG = Logger.getInstance(BugRecordService.class);

    private static final String BUG_RECORDS_DIR = ".pandacoder/bug-records";
    private static final String DAILY_LOG_PREFIX = "bugs-";
    private static final String DAILY_LOG_SUFFIX = ".json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Project project;
    private final Gson gson;
    private final Map<String, CopyOnWriteArrayList<BugRecord>> cache = new ConcurrentHashMap<>();

    public BugRecordService(@NotNull Project project) {
        this.project = project;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        // 确保存储目录存在
        ensureStorageDirectory();

        // 加载今日的Bug记录到缓存
        loadTodayRecords();
        
        // 迁移现有数据，确保所有记录都有状态
        migrateExistingRecords();
    }

    /**
     * 迁移现有记录，确保所有记录都有正确的状态
     */
    private void migrateExistingRecords() {
        try {
            boolean hasChanges = false;
            
            for (Map.Entry<String, CopyOnWriteArrayList<BugRecord>> entry : cache.entrySet()) {
                String dateKey = entry.getKey();
                List<BugRecord> records = entry.getValue();
                List<BugRecord> updatedRecords = new ArrayList<>();
                
                for (BugRecord record : records) {
                    // 检查记录是否需要迁移
                    if (record.getStatus() == null || record.getStatus() == BugStatus.PENDING) {
                        // 根据resolved字段确定状态
                        BugStatus newStatus = record.isResolved() ? BugStatus.RESOLVED : BugStatus.PENDING;
                        BugRecord updatedRecord = record.withStatus(newStatus);
                        updatedRecords.add(updatedRecord);
                        hasChanges = true;
                        LOG.info("Migrated bug record: " + record.getId() + " -> " + newStatus.getDisplayName());
                    } else {
                        updatedRecords.add(record);
                    }
                }
                
                if (hasChanges) {
                    cache.put(dateKey, new CopyOnWriteArrayList<>(updatedRecords));
                    saveDailyRecords(dateKey, updatedRecords);
                }
            }
            
            if (hasChanges) {
                LOG.info("Data migration completed successfully");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to migrate existing records", e);
        }
    }

    /**
     * 保存Bug记录（支持去重）
     */
    public void saveBugRecord(@NotNull BugRecord bugRecord) {
        try {
            // 检查是否启用本地存储
            if (!PluginSettings.getInstance().isEnableLocalBugStorage()) {
                LOG.info("Local bug storage disabled, skipping file save for bug record: " + bugRecord.getId());
                return;
            }

            String dateKey = bugRecord.getTimestamp().format(DATE_FORMATTER);

            // 检查是否已存在相同指纹的记录
            CopyOnWriteArrayList<BugRecord> records = cache.computeIfAbsent(dateKey, k -> new CopyOnWriteArrayList<>());
            
            // 如果有指纹，尝试去重
            if (bugRecord.getFingerprint() != null) {
                BugRecord existingRecord = records.stream()
                        .filter(record -> bugRecord.getFingerprint().equals(record.getFingerprint()))
                        .findFirst()
                        .orElse(null);
                
                if (existingRecord != null) {
                    // 更新现有记录的发生次数和时间戳
                    BugRecord updatedRecord = existingRecord
                            .withOccurrenceCount(existingRecord.getOccurrenceCount() + 1)
                            .withTimestamp(bugRecord.getTimestamp());
                    
                    // 从列表中移除旧记录
                    records.remove(existingRecord);
                    // 添加更新后的记录
                    records.add(updatedRecord);
                    
                    // 持久化到文件
                    saveDailyRecords(dateKey, records);
                    LOG.info("Updated existing bug record: " + bugRecord.getId() + " (fingerprint: " + bugRecord.getFingerprint() + ")");
                    return;
                }
            }
            
            // 如果没有重复，添加新记录
            records.add(bugRecord);

            // 持久化到文件
            saveDailyRecords(dateKey, records);

            LOG.info("Saved new bug record: " + bugRecord.getId() + " (fingerprint: " + bugRecord.getFingerprint() + ")");

        } catch (Exception e) {
            LOG.error("Failed to save bug record", e);
        }
    }

    /**
     * 获取今日的Bug记录
     */
    @NotNull
    public List<BugRecord> getTodayRecords() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return new ArrayList<>(cache.getOrDefault(today, new CopyOnWriteArrayList<>()));
    }

    /**
     * 获取指定日期的Bug记录
     */
    @NotNull
    public List<BugRecord> getRecordsByDate(@NotNull LocalDate date) {
        String dateKey = date.format(DATE_FORMATTER);

        // 如果缓存中没有，尝试从文件加载
        if (!cache.containsKey(dateKey)) {
            loadRecordsByDate(dateKey);
        }

        return new ArrayList<>(cache.getOrDefault(dateKey, new CopyOnWriteArrayList<>()));
    }

    /**
     * 获取最近N天的Bug记录
     */
    @NotNull
    public List<BugRecord> getRecentRecords(int days) {
        List<BugRecord> allRecords = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        for (int i = 0; i < days; i++) {
            LocalDate date = currentDate.minusDays(i);
            allRecords.addAll(getRecordsByDate(date));
        }

        // 按时间倒序排列
        allRecords.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return allRecords;
    }

    /**
     * 根据错误类型筛选记录
     */
    @NotNull
    public List<BugRecord> getRecordsByErrorType(@NotNull ErrorType errorType, int days) {
        return getRecentRecords(days).stream()
                .filter(record -> record.getErrorType() == errorType)
                .collect(Collectors.toList());
    }

    /**
     * 搜索Bug记录
     */
    @NotNull
    public List<BugRecord> searchRecords(@NotNull String keyword, int days) {
        String lowerKeyword = keyword.toLowerCase();

        return getRecentRecords(days).stream()
                .filter(record ->
                        (record.getSummary() != null && record.getSummary().toLowerCase().contains(lowerKeyword)) ||
                                (record.getErrorMessage() != null && record.getErrorMessage().toLowerCase().contains(lowerKeyword)) ||
                                (record.getExceptionClass() != null && record.getExceptionClass().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }

    /**
     * 更新Bug记录
     */
    public void updateBugRecord(@NotNull BugRecord updatedRecord) {
        try {
            String dateKey = updatedRecord.getTimestamp().format(DATE_FORMATTER);
            List<BugRecord> records = cache.get(dateKey);

            if (records != null) {
                // 找到并更新记录
                for (int i = 0; i < records.size(); i++) {
                    if (records.get(i).getId().equals(updatedRecord.getId())) {
                        records.set(i, updatedRecord);
                        saveDailyRecords(dateKey, records);
                        LOG.info("Updated bug record: " + updatedRecord.getId());
                        return;
                    }
                }
            }

            LOG.warn("Bug record not found for update: " + updatedRecord.getId());

        } catch (Exception e) {
            LOG.error("Failed to update bug record", e);
        }
    }

    /**
     * 更新Bug状态
     */
    public void updateBugStatus(@NotNull String recordId, @NotNull BugStatus newStatus) {
        try {
            // 在所有缓存中查找记录
            for (Map.Entry<String, CopyOnWriteArrayList<BugRecord>> entry : cache.entrySet()) {
                List<BugRecord> records = entry.getValue();
                for (int i = 0; i < records.size(); i++) {
                    BugRecord record = records.get(i);
                    if (record.getId().equals(recordId)) {
                        BugRecord updatedRecord = record.withStatus(newStatus);
                        records.set(i, updatedRecord);
                        saveDailyRecords(entry.getKey(), records);
                        LOG.info("Updated bug status: " + recordId + " -> " + newStatus.getDisplayName());
                        return;
                    }
                }
            }

            LOG.warn("Bug record not found for status update: " + recordId);

        } catch (Exception e) {
            LOG.error("Failed to update bug status", e);
        }
    }

    /**
     * 批量更新Bug状态
     */
    public void updateBugStatusBatch(@NotNull List<String> recordIds, @NotNull BugStatus newStatus) {
        for (String recordId : recordIds) {
            updateBugStatus(recordId, newStatus);
        }
    }

    /**
     * 删除Bug记录
     */
    public void deleteBugRecord(@NotNull String recordId) {
        try {
            // 在所有缓存中查找并删除记录
            for (Map.Entry<String, CopyOnWriteArrayList<BugRecord>> entry : cache.entrySet()) {
                List<BugRecord> records = entry.getValue();
                int beforeSize = records.size();
                records.removeIf(record -> record.getId().equals(recordId));
                if (records.size() != beforeSize) {
                    saveDailyRecords(entry.getKey(), records);
                    LOG.info("Deleted bug record: " + recordId);
                    return;
                }
            }

            LOG.warn("Bug record not found for deletion: " + recordId);

        } catch (Exception e) {
            LOG.error("Failed to delete bug record", e);
        }
    }

    /**
     * 批量删除Bug记录
     */
    public void deleteBugRecords(@NotNull List<String> recordIds) {
        for (String recordId : recordIds) {
            deleteBugRecord(recordId);
        }
    }

    /**
     * 清理旧记录
     */
    public void cleanupOldRecords(int keepDays) {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);
            List<String> datesToRemove = new ArrayList<>();

            // 找出需要删除的日期
            for (String dateKey : cache.keySet()) {
                try {
                    LocalDate recordDate = LocalDate.parse(dateKey, DATE_FORMATTER);
                    if (recordDate.isBefore(cutoffDate)) {
                        datesToRemove.add(dateKey);
                    }
                } catch (Exception e) {
                    LOG.warn("Invalid date key: " + dateKey);
                }
            }

            // 删除旧记录
            for (String dateKey : datesToRemove) {
                cache.remove(dateKey);
                Path filePath = getStorageDirectory().resolve(dateKey + DAILY_LOG_SUFFIX);
                Files.deleteIfExists(filePath);
                LOG.info("Cleaned up old records for date: " + dateKey);
            }

            LOG.info("Cleanup completed. Removed " + datesToRemove.size() + " old date files.");

        } catch (Exception e) {
            LOG.error("Failed to cleanup old records", e);
        }
    }

    /**
     * 清理指定状态的记录
     */
    public void cleanupRecordsByStatus(@NotNull BugStatus status, int days) {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(days);
            int removedCount = 0;

            for (Map.Entry<String, CopyOnWriteArrayList<BugRecord>> entry : cache.entrySet()) {
                String dateKey = entry.getKey();
                List<BugRecord> records = entry.getValue();
                
                try {
                    LocalDate recordDate = LocalDate.parse(dateKey, DATE_FORMATTER);
                    if (recordDate.isAfter(cutoffDate)) {
                        // 只处理指定天数内的记录
                        List<BugRecord> filteredRecords = records.stream()
                                .filter(record -> record.getStatus() != status)
                                .collect(Collectors.toList());
                        
                        if (filteredRecords.size() != records.size()) {
                            removedCount += records.size() - filteredRecords.size();
                            cache.put(dateKey, new CopyOnWriteArrayList<>(filteredRecords));
                            saveDailyRecords(dateKey, filteredRecords);
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Invalid date key: " + dateKey);
                }
            }

            LOG.info("Cleaned up " + removedCount + " records with status: " + status.getDisplayName());

        } catch (Exception e) {
            LOG.error("Failed to cleanup records by status", e);
        }
    }

    /**
     * 获取指定状态的记录
     */
    @NotNull
    public List<BugRecord> getRecordsByStatus(@NotNull BugStatus status, int days) {
        return getRecentRecords(days).stream()
                .filter(record -> record.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 验证所有记录的状态
     */
    public void validateAllRecords() {
        try {
            int totalRecords = 0;
            int migratedRecords = 0;
            
            for (Map.Entry<String, CopyOnWriteArrayList<BugRecord>> entry : cache.entrySet()) {
                String dateKey = entry.getKey();
                List<BugRecord> records = entry.getValue();
                List<BugRecord> validatedRecords = new ArrayList<>();
                
                for (BugRecord record : records) {
                    totalRecords++;
                    
                    if (record.getStatus() == null) {
                        // 根据resolved字段确定状态
                        BugStatus newStatus = record.isResolved() ? BugStatus.RESOLVED : BugStatus.PENDING;
                        BugRecord updatedRecord = record.withStatus(newStatus);
                        validatedRecords.add(updatedRecord);
                        migratedRecords++;
                        LOG.info("Validated bug record: " + record.getId() + " -> " + newStatus.getDisplayName());
                    } else {
                        validatedRecords.add(record);
                    }
                }
                
                if (migratedRecords > 0) {
                    cache.put(dateKey, new CopyOnWriteArrayList<>(validatedRecords));
                    saveDailyRecords(dateKey, validatedRecords);
                }
            }
            
            LOG.info("Validation completed. Total records: " + totalRecords + ", Migrated: " + migratedRecords);
            
        } catch (Exception e) {
            LOG.error("Failed to validate records", e);
        }
    }

    /**
     * 测试方法 - 创建示例Bug记录
     */
    public void createTestRecords() {
        try {
            // 创建一些测试记录
            BugRecord testRecord1 = new BugRecord.Builder()
                    .project("测试项目")
                    .timestamp(LocalDateTime.now())
                    .errorType(ErrorType.DATABASE)
                    .exceptionClass("java.sql.SQLException")
                    .errorMessage("数据库连接失败")
                    .summary("数据库连接超时")
                    .rawText("2025-07-29 10:29:11,503 ERROR [main] Database connection failed")
                    .status(BugStatus.PENDING)
                    .build();

            BugRecord testRecord2 = new BugRecord.Builder()
                    .project("测试项目")
                    .timestamp(LocalDateTime.now().minusMinutes(5))
                    .errorType(ErrorType.NETWORK)
                    .exceptionClass("java.net.ConnectException")
                    .errorMessage("网络连接失败")
                    .summary("API服务不可达")
                    .rawText("2025-07-29 10:24:06,447 ERROR [main] Network connection failed")
                    .status(BugStatus.IN_PROGRESS)
                    .build();

            BugRecord testRecord3 = new BugRecord.Builder()
                    .project("测试项目")
                    .timestamp(LocalDateTime.now().minusMinutes(10))
                    .errorType(ErrorType.UNKNOWN)
                    .exceptionClass("java.lang.ClassNotFoundException")
                    .errorMessage("类未找到")
                    .summary("com.torchv.TorchV类未找到")
                    .rawText("java.lang.ClassNotFoundException: com.torchv.TorchV")
                    .status(BugStatus.RESOLVED)
                    .build();

            // 保存测试记录
            saveBugRecord(testRecord1);
            saveBugRecord(testRecord2);
            saveBugRecord(testRecord3);

            LOG.info("Created test records successfully");

        } catch (Exception e) {
            LOG.error("Failed to create test records", e);
        }
    }

    /**
     * 测试数据库错误识别逻辑
     */
    public void testDatabaseErrorRecognition() {
        try {
            // 测试不同类型的数据库错误
            String[] testErrors = {
                // MySQL连接错误
                "com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure\n" +
                "The last packet sent successfully to the server was 0 milliseconds ago. The driver has not received any packets from the server.",
                
                // PostgreSQL连接错误
                "org.postgresql.util.PSQLException: Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.",
                
                // Hibernate错误
                "org.hibernate.exception.JDBCConnectionException: Could not open connection",
                
                // Spring Data JPA错误
                "org.springframework.dao.DataAccessException: Could not open JPA EntityManager for transaction",
                
                // 网络连接错误（应该被识别为网络错误，不是数据库错误）
                "java.net.ConnectException: Connection refused (Connection refused)",
                
                // SQL语法错误
                "java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax"
            };

            ErrorParser errorParser = new ErrorParser();
            
            for (int i = 0; i < testErrors.length; i++) {
                BugRecord record = errorParser.parseError(testErrors[i], project);
                if (record != null) {
                    LOG.info(String.format("测试错误 %d: %s -> %s", 
                        i + 1, 
                        record.getExceptionClass(), 
                        record.getErrorType().getDisplayName()));
                }
            }

        } catch (Exception e) {
            LOG.error("Failed to test database error recognition", e);
        }
    }

    /**
     * 获取统计信息
     */
    @NotNull
    public BugStatistics getStatistics(int days) {
        List<BugRecord> records = getRecentRecords(days);

        Map<ErrorType, Long> errorTypeCounts = records.stream()
                .collect(Collectors.groupingBy(BugRecord::getErrorType, Collectors.counting()));

        long resolvedCount = records.stream()
                .mapToLong(record -> record.isResolved() ? 1 : 0)
                .sum();

        return new BugStatistics(records.size(), resolvedCount, errorTypeCounts);
    }

    /**
     * 确保存储目录存在
     */
    private void ensureStorageDirectory() {
        try {
            Path storageDir = getStorageDirectory();
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (IOException e) {
            LOG.error("Failed to create storage directory", e);
        }
    }

    /**
     * 获取存储目录路径
     */
    private Path getStorageDirectory() {
        return Paths.get(project.getBasePath(), BUG_RECORDS_DIR);
    }

    /**
     * 加载今日记录到缓存
     */
    private void loadTodayRecords() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        loadRecordsByDate(today);
    }

    /**
     * 加载指定日期的记录
     */
    private void loadRecordsByDate(String dateKey) {
        try {
            Path filePath = getStorageDirectory().resolve(DAILY_LOG_PREFIX + dateKey + DAILY_LOG_SUFFIX);

            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);

                // ✅ 使用 JsonReader 并设置 LENIENT 模式来容忍格式不严格的 JSON
                try (StringReader stringReader = new StringReader(content)) {
                    JsonReader jsonReader = new JsonReader(stringReader);
                    jsonReader.setStrictness(Strictness.LENIENT);

                    Type listType = new TypeToken<List<BugRecord>>(){}.getType();
                    List<BugRecord> records = gson.fromJson(jsonReader, listType);

                    if (records != null) {
                        // 确保所有记录都有正确的状态
                        List<BugRecord> migratedRecords = new ArrayList<>();
                        boolean hasChanges = false;

                        for (BugRecord record : records) {
                            if (record.getStatus() == null) {
                                // 根据resolved字段确定状态
                                BugStatus newStatus = record.isResolved() ? BugStatus.RESOLVED : BugStatus.PENDING;
                                BugRecord updatedRecord = record.withStatus(newStatus);
                                migratedRecords.add(updatedRecord);
                                hasChanges = true;
                                LOG.info("Migrated loaded bug record: " + record.getId() + " -> " + newStatus.getDisplayName());
                            } else {
                                migratedRecords.add(record);
                            }
                        }

                        if (hasChanges) {
                            // 保存迁移后的记录
                            cache.put(dateKey, new CopyOnWriteArrayList<>(migratedRecords));
                            saveDailyRecords(dateKey, migratedRecords);
                        } else {
                            cache.put(dateKey, new CopyOnWriteArrayList<>(records));
                        }
                    }
                } catch (JsonSyntaxException e) {
                    // ✅ JSON 格式错误，备份损坏的文件
                    LOG.error("Bug records file is corrupted for date: " + dateKey + ", backing up", e);
                    backupCorruptedFile(filePath);
                }
            }

        } catch (Exception e) {
            LOG.warn("Failed to load records for date: " + dateKey, e);
        }
    }

    /**
     * 备份损坏的记录文件
     */
    private void backupCorruptedFile(Path filePath) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".corrupted." + timestamp);

            Files.move(filePath, backupPath);
            LOG.info("Corrupted bug records file backed up to: " + backupPath.getFileName());
        } catch (Exception e) {
            LOG.error("Error backing up corrupted bug records file", e);
        }
    }

    /**
     * 保存每日记录到文件
     */
    private void saveDailyRecords(String dateKey, List<BugRecord> records) throws IOException {
        Path filePath = getStorageDirectory().resolve(DAILY_LOG_PREFIX + dateKey + DAILY_LOG_SUFFIX);
        // 使用快照避免序列化期间的并发修改
        List<BugRecord> snapshot = (records instanceof CopyOnWriteArrayList)
                ? records
                : new ArrayList<>(records);
        String content = gson.toJson(snapshot);
        Files.writeString(filePath, content);
    }

    /**
     * Bug统计信息
     */
    public static class BugStatistics {
        private final int totalCount;
        private final long resolvedCount;
        private final Map<ErrorType, Long> errorTypeCounts;

        public BugStatistics(int totalCount, long resolvedCount, Map<ErrorType, Long> errorTypeCounts) {
            this.totalCount = totalCount;
            this.resolvedCount = resolvedCount;
            this.errorTypeCounts = errorTypeCounts;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public long getResolvedCount() {
            return resolvedCount;
        }

        public long getPendingCount() {
            return totalCount - resolvedCount;
        }

        public double getResolvedRate() {
            return totalCount > 0 ? (double) resolvedCount / totalCount : 0.0;
        }

        public Map<ErrorType, Long> getErrorTypeCounts() {
            return errorTypeCounts;
        }
    }
}