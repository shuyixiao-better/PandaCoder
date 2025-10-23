package com.shuyixiao.gitstat.ai.model;

/**
 * AI 代码记录
 * 记录单次 AI 代码生成的详细信息
 */
public class AiCodeRecord {
    
    private String filePath;              // 文件路径
    private long timestamp;               // 时间戳
    private int startOffset;              // 起始偏移
    private int endOffset;                // 结束偏移
    private String codeContent;           // 代码内容
    private int aiProbability;            // AI 概率 (0-100)
    private String aiTool;                // AI 工具名称
    private DetectionMethod detectionMethod; // 检测方法
    private String commitHash;            // 关联的 commit hash（commit 后填充）
    private int lineCount;                // 代码行数
    
    /**
     * 检测方法枚举
     */
    public enum DetectionMethod {
        REALTIME_SPEED_ANALYSIS("实时速度分析"),
        COMMIT_MESSAGE("Commit Message 标记"),
        GIT_DIFF_ANALYSIS("Git Diff 分析"),
        MANUAL_MARK("手动标记"),
        HYBRID("混合识别");
        
        private final String description;
        
        DetectionMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public AiCodeRecord() {
    }
    
    public AiCodeRecord(String filePath, long timestamp) {
        this.filePath = filePath;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getStartOffset() {
        return startOffset;
    }
    
    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }
    
    public int getEndOffset() {
        return endOffset;
    }
    
    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }
    
    public String getCodeContent() {
        return codeContent;
    }
    
    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
        this.lineCount = countLines(codeContent);
    }
    
    public int getAiProbability() {
        return aiProbability;
    }
    
    public void setAiProbability(int aiProbability) {
        this.aiProbability = aiProbability;
    }
    
    public String getAiTool() {
        return aiTool;
    }
    
    public void setAiTool(String aiTool) {
        this.aiTool = aiTool;
    }
    
    public DetectionMethod getDetectionMethod() {
        return detectionMethod;
    }
    
    public void setDetectionMethod(DetectionMethod detectionMethod) {
        this.detectionMethod = detectionMethod;
    }
    
    public String getCommitHash() {
        return commitHash;
    }
    
    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }
    
    public int getLineCount() {
        return lineCount;
    }
    
    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
    
    /**
     * 计算代码行数
     */
    private int countLines(String code) {
        if (code == null || code.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
    
    @Override
    public String toString() {
        return "AiCodeRecord{" +
                "filePath='" + filePath + '\'' +
                ", timestamp=" + timestamp +
                ", aiProbability=" + aiProbability +
                ", aiTool='" + aiTool + '\'' +
                ", detectionMethod=" + detectionMethod +
                ", lineCount=" + lineCount +
                '}';
    }
}

