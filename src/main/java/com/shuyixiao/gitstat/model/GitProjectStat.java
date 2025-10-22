package com.shuyixiao.gitstat.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Git 项目代码统计模型
 * 记录项目当前的代码量统计信息
 */
public class GitProjectStat {
    
    private int totalFiles;                          // 总文件数
    private int totalLines;                          // 总行数
    private int totalCodeLines;                      // 总代码行数（排除空行和注释）
    private int totalBlankLines;                     // 总空行数
    private int totalCommentLines;                   // 总注释行数
    private Map<String, Integer> filesByExtension;   // 按文件扩展名统计
    private Map<String, Integer> linesByExtension;   // 按文件扩展名统计行数
    
    public GitProjectStat() {
        this.totalFiles = 0;
        this.totalLines = 0;
        this.totalCodeLines = 0;
        this.totalBlankLines = 0;
        this.totalCommentLines = 0;
        this.filesByExtension = new HashMap<>();
        this.linesByExtension = new HashMap<>();
    }
    
    public void addFile(String extension, int lines, int codeLines, int blankLines, int commentLines) {
        this.totalFiles++;
        this.totalLines += lines;
        this.totalCodeLines += codeLines;
        this.totalBlankLines += blankLines;
        this.totalCommentLines += commentLines;
        
        // 更新按扩展名统计
        filesByExtension.put(extension, filesByExtension.getOrDefault(extension, 0) + 1);
        linesByExtension.put(extension, linesByExtension.getOrDefault(extension, 0) + lines);
    }
    
    // Getters and Setters
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public int getTotalLines() {
        return totalLines;
    }
    
    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }
    
    public int getTotalCodeLines() {
        return totalCodeLines;
    }
    
    public void setTotalCodeLines(int totalCodeLines) {
        this.totalCodeLines = totalCodeLines;
    }
    
    public int getTotalBlankLines() {
        return totalBlankLines;
    }
    
    public void setTotalBlankLines(int totalBlankLines) {
        this.totalBlankLines = totalBlankLines;
    }
    
    public int getTotalCommentLines() {
        return totalCommentLines;
    }
    
    public void setTotalCommentLines(int totalCommentLines) {
        this.totalCommentLines = totalCommentLines;
    }
    
    public Map<String, Integer> getFilesByExtension() {
        return filesByExtension;
    }
    
    public void setFilesByExtension(Map<String, Integer> filesByExtension) {
        this.filesByExtension = filesByExtension;
    }
    
    public Map<String, Integer> getLinesByExtension() {
        return linesByExtension;
    }
    
    public void setLinesByExtension(Map<String, Integer> linesByExtension) {
        this.linesByExtension = linesByExtension;
    }
    
    @Override
    public String toString() {
        return "GitProjectStat{" +
                "totalFiles=" + totalFiles +
                ", totalLines=" + totalLines +
                ", totalCodeLines=" + totalCodeLines +
                ", totalBlankLines=" + totalBlankLines +
                ", totalCommentLines=" + totalCommentLines +
                '}';
    }
}

