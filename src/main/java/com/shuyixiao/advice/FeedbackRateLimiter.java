package com.shuyixiao.advice;

import com.intellij.openapi.application.PathManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName: FeedbackRateLimiter.java
 * author: 舒一笑不秃头
 * version: 2.2.0
 * Description: 反馈限流器，用于限制用户每天发送反馈邮件的次数，防止邮件轰炸
 * createTime: 2025-10-24
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class FeedbackRateLimiter {
    
    private static final int MAX_DAILY_SENDS = 6;
    private static final String RATE_LIMIT_FILE = "feedback-rate-limit.dat";
    
    /**
     * 获取限流数据文件路径
     */
    private static Path getRateLimitFilePath() {
        String configPath = PathManager.getConfigPath();
        Path pandaCoderDir = Paths.get(configPath, "PandaCoder");
        try {
            Files.createDirectories(pandaCoderDir);
        } catch (IOException e) {
            // 忽略错误
        }
        return pandaCoderDir.resolve(RATE_LIMIT_FILE);
    }
    
    /**
     * 检查今天是否还可以发送
     * @return true 如果可以发送，false 如果已达到限制
     */
    public static boolean canSendToday() {
        int todaySendCount = getTodaySendCount();
        return todaySendCount < MAX_DAILY_SENDS;
    }
    
    /**
     * 获取今天已发送的次数
     */
    public static int getTodaySendCount() {
        Map<String, Integer> rateLimitData = loadRateLimitData();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return rateLimitData.getOrDefault(today, 0);
    }
    
    /**
     * 获取今天还可以发送的次数
     */
    public static int getRemainingCount() {
        return MAX_DAILY_SENDS - getTodaySendCount();
    }
    
    /**
     * 记录一次发送
     */
    public static void recordSend() {
        Map<String, Integer> rateLimitData = loadRateLimitData();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        int currentCount = rateLimitData.getOrDefault(today, 0);
        rateLimitData.put(today, currentCount + 1);
        saveRateLimitData(rateLimitData);
    }
    
    /**
     * 加载限流数据
     */
    private static Map<String, Integer> loadRateLimitData() {
        Map<String, Integer> data = new HashMap<>();
        Path filePath = getRateLimitFilePath();
        
        if (!Files.exists(filePath)) {
            return data;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    data.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            // 如果读取失败，返回空数据
        }
        
        // 清理过期数据（保留最近7天）
        cleanupOldData(data);
        
        return data;
    }
    
    /**
     * 保存限流数据
     */
    private static void saveRateLimitData(Map<String, Integer> data) {
        Path filePath = getRateLimitFilePath();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            // 忽略保存错误
        }
    }
    
    /**
     * 清理7天前的旧数据
     */
    private static void cleanupOldData(Map<String, Integer> data) {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        
        data.entrySet().removeIf(entry -> {
            try {
                LocalDate date = LocalDate.parse(entry.getKey(), formatter);
                return date.isBefore(sevenDaysAgo);
            } catch (Exception e) {
                // 如果解析失败，删除该条目
                return true;
            }
        });
    }
}

