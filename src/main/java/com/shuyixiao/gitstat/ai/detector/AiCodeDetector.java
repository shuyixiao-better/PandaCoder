package com.shuyixiao.gitstat.ai.detector;

/**
 * AI 代码识别算法
 * 基于输入速度和代码块大小判断是否为 AI 生成
 * 
 * 核心原理：
 * - 人工输入速度：平均 3-5 字符/秒
 * - AI 生成速度：几乎瞬时（整块插入）
 * - 粘贴操作：瞬时插入大段代码
 */
public class AiCodeDetector {
    
    // 阈值配置
    private static final int MIN_AI_CHARS = 20;              // AI 生成最小字符数
    private static final int LARGE_BLOCK_THRESHOLD = 100;    // 大代码块阈值
    private static final int PASTE_TIME_THRESHOLD = 50;      // 粘贴操作时间阈值（毫秒）
    private static final int MIN_MULTILINE_THRESHOLD = 5;    // 多行代码阈值
    private static final long MULTILINE_TIME_THRESHOLD = 2000; // 多行代码时间阈值（毫秒）
    
    /**
     * 判断代码变更是否为 AI 生成
     * 
     * @param newLength 新增代码长度（字符数）
     * @param duration 输入耗时（毫秒）
     * @param lineCount 新增行数
     * @return AI 生成概率（0-100）
     */
    public static int calculateAiProbability(int newLength, long duration, int lineCount) {
        
        // 1. 小量代码变更，认为是人工输入
        if (newLength < MIN_AI_CHARS) {
            return 0; // 0% AI 概率
        }
        
        // 2. 大代码块瞬时插入（粘贴或 AI 生成）
        if (newLength >= LARGE_BLOCK_THRESHOLD && duration < PASTE_TIME_THRESHOLD) {
            return 95; // 95% AI 概率
        }
        
        // 3. 中等代码块快速插入
        if (newLength >= MIN_AI_CHARS) {
            // 计算输入速度（字符/秒）
            double speed = duration > 0 ? (newLength * 1000.0 / duration) : Double.MAX_VALUE;
            
            // 人工输入速度：3-5 字符/秒
            // AI/粘贴速度：> 100 字符/秒
            if (speed > 100) {
                return 90; // 90% AI 概率
            } else if (speed > 20) {
                return 70; // 70% AI 概率
            } else if (speed > 10) {
                return 50; // 50% AI 概率
            }
        }
        
        // 4. 多行代码短时间内插入
        if (lineCount >= MIN_MULTILINE_THRESHOLD && duration < MULTILINE_TIME_THRESHOLD) {
            return 80; // 80% AI 概率
        }
        
        // 5. 默认认为是人工输入
        return 10; // 10% AI 概率（基本是人工）
    }
    
    /**
     * 判断是否应该记录为 AI 代码
     * 
     * @param aiProbability AI 概率
     * @return true 如果应该记录为 AI 代码
     */
    public static boolean shouldRecordAsAi(int aiProbability) {
        return aiProbability >= 70; // 70% 以上概率认为是 AI
    }
    
    /**
     * 获取 AI 概率等级描述
     * 
     * @param aiProbability AI 概率
     * @return 概率等级描述
     */
    public static String getAiProbabilityLevel(int aiProbability) {
        if (aiProbability >= 90) {
            return "极高";
        } else if (aiProbability >= 70) {
            return "高";
        } else if (aiProbability >= 50) {
            return "中等";
        } else if (aiProbability >= 30) {
            return "较低";
        } else {
            return "低";
        }
    }
}

