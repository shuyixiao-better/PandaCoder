package com.shuyixiao.bugrecorder.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.util.LocalDateTimeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final Map<String, List<BugRecord>> cache = new ConcurrentHashMap<>();

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
    }

    /**
     * 保存Bug记录
     */
    public void saveBugRecord(@NotNull BugRecord bugRecord) {
        try {
            String dateKey = bugRecord.getTimestamp().format(DATE_FORMATTER);

            // 添加到缓存
            cache.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(bugRecord);

            // 持久化到文件
            saveDailyRecords(dateKey, cache.get(dateKey));

            LOG.info("Saved bug record: " + bugRecord.getId());

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
        return cache.getOrDefault(today, new ArrayList<>());
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

        return cache.getOrDefault(dateKey, new ArrayList<>());
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
            List<BugRecord> dayRecords = cache.get(dateKey);

            if (dayRecords != null) {
                // 找到并替换对应的记录
                for (int i = 0; i < dayRecords.size(); i++) {
                    if (dayRecords.get(i).getId().equals(updatedRecord.getId())) {
                        dayRecords.set(i, updatedRecord);
                        break;
                    }
                }

                // 持久化更新
                saveDailyRecords(dateKey, dayRecords);
                LOG.info("Updated bug record: " + updatedRecord.getId());
            }

        } catch (Exception e) {
            LOG.error("Failed to update bug record", e);
        }
    }

    /**
     * 删除Bug记录
     */
    public void deleteBugRecord(@NotNull String recordId) {
        try {
            // 在所有缓存中查找并删除
            for (Map.Entry<String, List<BugRecord>> entry : cache.entrySet()) {
                List<BugRecord> records = entry.getValue();
                boolean removed = records.removeIf(record -> record.getId().equals(recordId));

                if (removed) {
                    // 持久化更新
                    saveDailyRecords(entry.getKey(), records);
                    LOG.info("Deleted bug record: " + recordId);
                    return;
                }
            }

        } catch (Exception e) {
            LOG.error("Failed to delete bug record", e);
        }
    }

    /**
     * 清理过期的Bug记录（超过指定天数）
     */
    public void cleanupOldRecords(int keepDays) {
        try {
            Path recordsDir = getStorageDirectory();
            if (!Files.exists(recordsDir)) {
                return;
            }

            LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);

            Files.list(recordsDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(DAILY_LOG_PREFIX))
                    .filter(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String dateStr = fileName.substring(DAILY_LOG_PREFIX.length(),
                                    fileName.length() - DAILY_LOG_SUFFIX.length());
                            LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                            return fileDate.isBefore(cutoffDate);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            LOG.info("Deleted old bug record file: " + path.getFileName());
                        } catch (IOException e) {
                            LOG.warn("Failed to delete old file: " + path, e);
                        }
                    });

        } catch (Exception e) {
            LOG.error("Failed to cleanup old records", e);
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
                Type listType = new TypeToken<List<BugRecord>>(){}.getType();
                List<BugRecord> records = gson.fromJson(content, listType);

                if (records != null) {
                    cache.put(dateKey, records);
                }
            }

        } catch (Exception e) {
            LOG.warn("Failed to load records for date: " + dateKey, e);
        }
    }

    /**
     * 保存每日记录到文件
     */
    private void saveDailyRecords(String dateKey, List<BugRecord> records) throws IOException {
        Path filePath = getStorageDirectory().resolve(DAILY_LOG_PREFIX + dateKey + DAILY_LOG_SUFFIX);
        String content = gson.toJson(records);
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