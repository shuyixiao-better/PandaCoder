package com.shuyixiao.gitstat.ai.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.gitstat.ai.model.AiCodeRecord;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 代码记录存储服务
 * 负责持久化 AI 代码检测记录
 * 
 * 性能优化：
 * 1. 批量写入（每30秒或累积10条记录）
 * 2. LRU 缓存（最多缓存100天数据）
 * 3. 异步保存，不阻塞主线程
 */
@Service(Service.Level.PROJECT)
public final class AiCodeRecordStorage {
    
    private static final Logger LOG = Logger.getInstance(AiCodeRecordStorage.class);
    private static final String TRACKING_FILE = ".ai-code-tracking.json";
    
    private final Project project;
    private final Gson gson;
    
    // 内存缓存（LRU，限制大小）
    private final Map<String, List<AiCodeRecord>> recordCache = new LinkedHashMap<String, List<AiCodeRecord>>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<AiCodeRecord>> eldest) {
            return size() > 100; // 最多缓存 100 天的数据
        }
    };
    
    // 待保存队列（批量写入）
    private final Queue<AiCodeRecord> pendingRecords = new LinkedList<>();
    private long lastSaveTime = System.currentTimeMillis();
    
    private final Object saveLock = new Object();
    
    public AiCodeRecordStorage(Project project) {
        this.project = project;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        loadFromDisk();
        startBackgroundSaver();
        LOG.info("AiCodeRecordStorage initialized for project: " + project.getName());
    }
    
    /**
     * 保存 AI 代码记录（异步）
     */
    public void saveRecord(AiCodeRecord record) {
        synchronized (saveLock) {
            // 添加到待保存队列
            pendingRecords.offer(record);
            
            // 添加到内存缓存
            String date = getDateKey(record.getTimestamp());
            recordCache.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
            
            // 如果队列过大或距离上次保存时间过长，触发保存
            if (pendingRecords.size() >= 10 || 
                System.currentTimeMillis() - lastSaveTime > 30000) {
                flushToDisk();
            }
        }
    }
    
    /**
     * 批量写入磁盘
     */
    private synchronized void flushToDisk() {
        if (pendingRecords.isEmpty()) {
            return;
        }
        
        try {
            File trackingFile = new File(project.getBasePath(), TRACKING_FILE);
            
            // 读取现有数据
            AiCodeTrackingData data;
            if (trackingFile.exists()) {
                try (FileReader fileReader = new FileReader(trackingFile)) {
                    // ✅ 使用 JsonReader 并设置 LENIENT 模式来容忍格式不严格的 JSON
                    JsonReader jsonReader = new JsonReader(fileReader);
                    jsonReader.setStrictness(Strictness.LENIENT);

                    data = gson.fromJson(jsonReader, AiCodeTrackingData.class);
                    if (data == null) {
                        data = new AiCodeTrackingData();
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to read existing tracking file, creating new one", e);
                    data = new AiCodeTrackingData();
                }
            } else {
                data = new AiCodeTrackingData();
            }
            
            // 添加新记录
            while (!pendingRecords.isEmpty()) {
                data.addRecord(pendingRecords.poll());
            }
            
            // 写入文件
            try (FileWriter writer = new FileWriter(trackingFile)) {
                gson.toJson(data, writer);
            }
            
            lastSaveTime = System.currentTimeMillis();
            LOG.debug("Flushed AI code records to disk");
            
        } catch (Exception e) {
            LOG.error("Failed to save AI code records", e);
        }
    }
    
    /**
     * 从磁盘加载数据
     */
    private void loadFromDisk() {
        try {
            File trackingFile = new File(project.getBasePath(), TRACKING_FILE);
            if (trackingFile.exists()) {
                try (FileReader fileReader = new FileReader(trackingFile)) {
                    // ✅ 使用 JsonReader 并设置 LENIENT 模式来容忍格式不严格的 JSON
                    JsonReader jsonReader = new JsonReader(fileReader);
                    jsonReader.setStrictness(Strictness.LENIENT);

                    AiCodeTrackingData data = gson.fromJson(jsonReader, AiCodeTrackingData.class);

                    if (data != null && data.getRecords() != null) {
                        // 加载到缓存
                        for (AiCodeRecord record : data.getRecords()) {
                            String date = getDateKey(record.getTimestamp());
                            recordCache.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
                        }

                        LOG.info("Loaded " + data.getRecords().size() + " AI code records");
                    }
                } catch (JsonSyntaxException e) {
                    // ✅ JSON 格式错误，备份损坏的文件并重新开始
                    LOG.error("AI code tracking file is corrupted, backing up and starting fresh", e);
                    backupCorruptedFile(trackingFile);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load AI code records", e);
        }
    }

    /**
     * 备份损坏的记录文件
     */
    private void backupCorruptedFile(File file) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            File backupFile = new File(file.getParent(), file.getName() + ".corrupted." + timestamp);

            if (file.renameTo(backupFile)) {
                LOG.info("Corrupted AI code tracking file backed up to: " + backupFile.getName());
            } else {
                LOG.warn("Failed to backup corrupted AI code tracking file");
            }
        } catch (Exception e) {
            LOG.error("Error backing up corrupted AI code tracking file", e);
        }
    }
    
    /**
     * 启动后台保存线程
     */
    private void startBackgroundSaver() {
        Timer timer = new Timer("AiCodeRecordSaver-" + project.getName(), true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (saveLock) {
                    flushToDisk();
                }
            }
        }, 30000, 30000); // 每30秒保存一次
    }
    
    /**
     * 获取指定文件的 AI 代码记录
     */
    @NotNull
    public List<AiCodeRecord> getRecordsByFile(String filePath) {
        return recordCache.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getFilePath().equals(filePath))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定日期范围的 AI 代码记录
     */
    @NotNull
    public List<AiCodeRecord> getRecordsByDateRange(long startTime, long endTime) {
        return recordCache.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getTimestamp() >= startTime && r.getTimestamp() <= endTime)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有 AI 代码记录
     */
    @NotNull
    public List<AiCodeRecord> getAllRecords() {
        return recordCache.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    
    /**
     * 清理过期记录（保留最近30天）
     */
    public void cleanupOldRecords() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        synchronized (saveLock) {
            recordCache.entrySet().removeIf(entry -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = sdf.parse(entry.getKey());
                    return date.getTime() < thirtyDaysAgo;
                } catch (Exception e) {
                    return false;
                }
            });
            
            flushToDisk();
        }
    }
    
    /**
     * 强制保存所有待保存的记录
     */
    public void forceFlush() {
        synchronized (saveLock) {
            flushToDisk();
        }
    }
    
    private String getDateKey(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp));
    }
    
    /**
     * AI 代码追踪数据
     */
    public static class AiCodeTrackingData {
        private String version = "1.0";
        private List<AiCodeRecord> records = new ArrayList<>();
        private Map<String, AiToolStatistics> toolStats = new HashMap<>();
        
        public void addRecord(AiCodeRecord record) {
            records.add(record);
            updateToolStats(record);
        }
        
        private void updateToolStats(AiCodeRecord record) {
            String tool = record.getAiTool();
            if (tool != null) {
                AiToolStatistics stats = toolStats.computeIfAbsent(tool, k -> new AiToolStatistics());
                stats.incrementUsage();
                stats.addLines(record.getLineCount());
            }
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public List<AiCodeRecord> getRecords() {
            return records;
        }
        
        public void setRecords(List<AiCodeRecord> records) {
            this.records = records;
        }
        
        public Map<String, AiToolStatistics> getToolStats() {
            return toolStats;
        }
        
        public void setToolStats(Map<String, AiToolStatistics> toolStats) {
            this.toolStats = toolStats;
        }
    }
    
    /**
     * AI 工具统计
     */
    public static class AiToolStatistics {
        private int usageCount = 0;
        private int totalLines = 0;
        
        public void incrementUsage() {
            this.usageCount++;
        }
        
        public void addLines(int lines) {
            this.totalLines += lines;
        }
        
        public int getUsageCount() {
            return usageCount;
        }
        
        public void setUsageCount(int usageCount) {
            this.usageCount = usageCount;
        }
        
        public int getTotalLines() {
            return totalLines;
        }
        
        public void setTotalLines(int totalLines) {
            this.totalLines = totalLines;
        }
    }
}

