package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.shuyixiao.bugrecorder.model.BugRecord;
import com.shuyixiao.bugrecorder.model.BugStatus;
import com.shuyixiao.bugrecorder.model.ErrorType;
import com.shuyixiao.bugrecorder.service.BugRecordService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 增强版Bug记录服务
 * 集成上下文捕获和智能分析功能
 */
public class EnhancedBugRecordService {

    private static final Logger LOG = Logger.getInstance(EnhancedBugRecordService.class);
    
    private final Project project;
    private final BugRecordService bugRecordService;
    private final EnhancedContextCaptureService contextService;
    private final EnhancedErrorParser errorParser;
    
    // 内存缓存
    private final Map<String, CopyOnWriteArrayList<BugRecord>> memoryCache = new ConcurrentHashMap<>();
    private final Map<String, BugRecord> fingerprintCache = new ConcurrentHashMap<>();
    
    // 统计信息
    private final Map<ErrorType, Integer> errorTypeStats = new ConcurrentHashMap<>();
    private final Map<String, Integer> severityStats = new ConcurrentHashMap<>();
    private final Map<String, Integer> categoryStats = new ConcurrentHashMap<>();
    
    public EnhancedBugRecordService(@NotNull Project project) {
        this.project = project;
        this.bugRecordService = project.getService(BugRecordService.class);
        this.contextService = project.getService(EnhancedContextCaptureService.class);
        this.errorParser = new EnhancedErrorParser(project);
        
        // 初始化统计信息
        initializeStats();
    }
    
    /**
     * 初始化统计信息
     */
    private void initializeStats() {
        for (ErrorType type : ErrorType.values()) {
            errorTypeStats.put(type, 0);
        }
        
        severityStats.put("CRITICAL", 0);
        severityStats.put("ERROR", 0);
        severityStats.put("WARNING", 0);
        severityStats.put("INFO", 0);
        severityStats.put("UNKNOWN", 0);
        
        categoryStats.put("DATABASE", 0);
        categoryStats.put("HTTP", 0);
        categoryStats.put("SPRING", 0);
        categoryStats.put("VALIDATION", 0);
        categoryStats.put("NETWORK", 0);
        categoryStats.put("RESOURCE", 0);
        categoryStats.put("GENERAL", 0);
    }
    
    /**
     * 创建增强的Bug记录
     */
    public BugRecord createEnhancedBugRecord(@NotNull String errorText) {
        try {
            // 使用增强解析器解析错误
            BugRecord bugRecord = errorParser.parseEnhancedError(errorText, project);
            
            if (bugRecord != null) {
                // 保存到基础服务
                bugRecordService.saveBugRecord(bugRecord);
                
                // 更新内存缓存
                updateMemoryCache(bugRecord);
                
                // 更新统计信息
                updateStats(bugRecord);
                
                // 记录日志
                LOG.info("Enhanced bug record created: " + bugRecord.getSummary() + 
                        " (Type: " + bugRecord.getErrorType() + 
                        ", Fingerprint: " + bugRecord.getFingerprint() + ")");
            }
            
            return bugRecord;
            
        } catch (Exception e) {
            LOG.error("Failed to create enhanced bug record", e);
            return null;
        }
    }
    
    /**
     * 更新内存缓存
     */
    private void updateMemoryCache(BugRecord bugRecord) {
        try {
            // 按日期分组缓存
            String dateKey = bugRecord.getTimestamp().toLocalDate().toString();
            memoryCache.computeIfAbsent(dateKey, k -> new CopyOnWriteArrayList<>()).add(bugRecord);
            
            // 按指纹缓存
            if (bugRecord.getFingerprint() != null) {
                fingerprintCache.put(bugRecord.getFingerprint(), bugRecord);
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to update memory cache", e);
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStats(BugRecord bugRecord) {
        try {
            // 更新错误类型统计
            if (bugRecord.getErrorType() != null) {
                errorTypeStats.merge(bugRecord.getErrorType(), 1, Integer::sum);
            }
            
            // 更新严重程度统计（从AI分析字段中提取）
            String aiAnalysis = bugRecord.getAiAnalysis();
            if (aiAnalysis != null && aiAnalysis.contains("Severity:")) {
                String severity = extractSeverityFromAnalysis(aiAnalysis);
                if (severity != null) {
                    severityStats.merge(severity, 1, Integer::sum);
                }
            }
            
            // 更新类别统计（从AI分析字段中提取）
            if (aiAnalysis != null && aiAnalysis.contains("category")) {
                String category = extractCategoryFromAnalysis(aiAnalysis);
                if (category != null) {
                    categoryStats.merge(category, 1, Integer::sum);
                }
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to update stats", e);
        }
    }
    
    /**
     * 从AI分析中提取严重程度
     */
    private String extractSeverityFromAnalysis(String aiAnalysis) {
        try {
            if (aiAnalysis.contains("CRITICAL")) return "CRITICAL";
            if (aiAnalysis.contains("ERROR")) return "ERROR";
            if (aiAnalysis.contains("WARNING")) return "WARNING";
            if (aiAnalysis.contains("INFO")) return "INFO";
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * 从AI分析中提取类别
     */
    private String extractCategoryFromAnalysis(String aiAnalysis) {
        try {
            if (aiAnalysis.contains("DATABASE")) return "DATABASE";
            if (aiAnalysis.contains("HTTP")) return "HTTP";
            if (aiAnalysis.contains("SPRING")) return "SPRING";
            if (aiAnalysis.contains("VALIDATION")) return "VALIDATION";
            if (aiAnalysis.contains("NETWORK")) return "NETWORK";
            if (aiAnalysis.contains("RESOURCE")) return "RESOURCE";
            return "GENERAL";
        } catch (Exception e) {
            return "GENERAL";
        }
    }
    
    /**
     * 获取所有Bug记录
     */
    public List<BugRecord> getAllBugRecords() {
        List<BugRecord> allRecords = new ArrayList<>();
        
        try {
            // 从内存缓存获取
            for (CopyOnWriteArrayList<BugRecord> records : memoryCache.values()) {
                allRecords.addAll(records);
            }
            
            // 按时间排序
            allRecords.sort(Comparator.comparing(BugRecord::getTimestamp).reversed());
            
        } catch (Exception e) {
            LOG.warn("Failed to get all bug records from cache", e);
        }
        
        return allRecords;
    }
    
    /**
     * 根据指纹查找Bug记录
     */
    @Nullable
    public BugRecord findBugRecordByFingerprint(String fingerprint) {
        return fingerprintCache.get(fingerprint);
    }
    
    /**
     * 根据错误类型查找Bug记录
     */
    public List<BugRecord> findBugRecordsByType(ErrorType errorType) {
        return getAllBugRecords().stream()
            .filter(record -> errorType.equals(record.getErrorType()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据严重程度查找Bug记录
     */
    public List<BugRecord> findBugRecordsBySeverity(String severity) {
        return getAllBugRecords().stream()
            .filter(record -> {
                String aiAnalysis = record.getAiAnalysis();
                if (aiAnalysis != null && aiAnalysis.contains("Severity:")) {
                    String recordSeverity = extractSeverityFromAnalysis(aiAnalysis);
                    return severity.equals(recordSeverity);
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 根据类别查找Bug记录
     */
    public List<BugRecord> findBugRecordsByCategory(String category) {
        return getAllBugRecords().stream()
            .filter(record -> {
                String aiAnalysis = record.getAiAnalysis();
                if (aiAnalysis != null && aiAnalysis.contains("category")) {
                    String recordCategory = extractCategoryFromAnalysis(aiAnalysis);
                    return category.equals(recordCategory);
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 搜索Bug记录
     */
    public List<BugRecord> searchBugRecords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBugRecords();
        }
        
        String lowerQuery = query.toLowerCase();
        return getAllBugRecords().stream()
            .filter(record -> 
                (record.getErrorMessage() != null && record.getErrorMessage().toLowerCase().contains(lowerQuery)) ||
                (record.getSummary() != null && record.getSummary().toLowerCase().contains(lowerQuery)) ||
                (record.getExceptionClass() != null && record.getExceptionClass().toLowerCase().contains(lowerQuery)) ||
                (record.getRawText() != null && record.getRawText().toLowerCase().contains(lowerQuery))
            )
            .collect(Collectors.toList());
    }
    
    /**
     * 获取错误类型统计
     */
    public Map<ErrorType, Integer> getErrorTypeStats() {
        return new HashMap<>(errorTypeStats);
    }
    
    /**
     * 获取严重程度统计
     */
    public Map<String, Integer> getSeverityStats() {
        return new HashMap<>(severityStats);
    }
    
    /**
     * 获取类别统计
     */
    public Map<String, Integer> getCategoryStats() {
        return new HashMap<>(categoryStats);
    }
    
    /**
     * 获取总体统计信息
     */
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<BugRecord> allRecords = getAllBugRecords();
        
        stats.put("totalCount", allRecords.size());
        stats.put("resolvedCount", allRecords.stream().filter(BugRecord::isResolved).count());
        stats.put("pendingCount", allRecords.stream().filter(r -> !r.isResolved()).count());
        stats.put("errorTypeStats", getErrorTypeStats());
        stats.put("severityStats", getSeverityStats());
        stats.put("categoryStats", getCategoryStats());
        
        // 时间统计
        if (!allRecords.isEmpty()) {
            LocalDateTime earliest = allRecords.stream()
                .map(BugRecord::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            LocalDateTime latest = allRecords.stream()
                .map(BugRecord::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            stats.put("earliestRecord", earliest);
            stats.put("latestRecord", latest);
        }
        
        return stats;
    }
    
    /**
     * 更新Bug记录状态
     */
    public boolean updateBugRecordStatus(String bugId, BugStatus newStatus) {
        try {
            // 查找Bug记录
            BugRecord record = findBugRecordById(bugId);
            if (record != null) {
                // 创建更新后的记录
                BugRecord updatedRecord = new BugRecord.Builder()
                    .id(record.getId())
                    .project(record.getProject())
                    .timestamp(record.getTimestamp())
                    .errorType(record.getErrorType())
                    .exceptionClass(record.getExceptionClass())
                    .errorMessage(record.getErrorMessage())
                    .summary(record.getSummary())
                    .stackTrace(record.getStackTrace())
                    .rawText(record.getRawText())
                    .resolved(newStatus == BugStatus.RESOLVED)
                    .aiAnalysis(record.getAiAnalysis())
                    .solution(record.getSolution())
                    .status(newStatus)
                    .rootCause(record.getRootCause())
                    .causeChain(record.getCauseChain())
                    .topFrame(record.getTopFrame())
                    .fingerprint(record.getFingerprint())
                    .occurrenceCount(record.getOccurrenceCount())
                    .build();
                
                // 保存更新后的记录
                bugRecordService.saveBugRecord(updatedRecord);
                
                // 更新内存缓存
                updateMemoryCache(updatedRecord);
                
                LOG.info("Bug record status updated: " + bugId + " -> " + newStatus);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LOG.error("Failed to update bug record status", e);
            return false;
        }
    }
    
    /**
     * 根据ID查找Bug记录
     */
    @Nullable
    private BugRecord findBugRecordById(String bugId) {
        return getAllBugRecords().stream()
            .filter(record -> bugId.equals(record.getId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 刷新上下文信息
     */
    public void refreshContext() {
        try {
            contextService.refreshContext();
            LOG.info("Context information refreshed");
        } catch (Exception e) {
            LOG.warn("Failed to refresh context", e);
        }
    }
    
    /**
     * 获取完整的上下文信息
     */
    public Map<String, Object> getFullContext() {
        return contextService.getFullContext();
    }
    
    /**
     * 清理内存缓存
     */
    public void clearMemoryCache() {
        try {
            memoryCache.clear();
            fingerprintCache.clear();
            LOG.info("Memory cache cleared");
        } catch (Exception e) {
            LOG.warn("Failed to clear memory cache", e);
        }
    }
} 