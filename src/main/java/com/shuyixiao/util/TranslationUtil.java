package com.shuyixiao.util;

import com.intellij.openapi.ui.Messages;
import com.shuyixiao.BaiduAPI;
import com.shuyixiao.setting.PluginSettings;

public class TranslationUtil {

    // 存储API验证状态的缓存
    private static boolean apiValidationCache = false;
    private static long lastValidationTime = 0;
    private static final long VALIDATION_CACHE_TIMEOUT = 5 * 60 * 1000; // 5分钟缓存过期

    /**
     * 检查翻译API配置是否已设置
     * 支持三级翻译引擎：国内大模型 > Google翻译 > 百度翻译
     * 
     * @return 配置已设置返回true，否则返回false
     */
    public static boolean checkApiConfiguration() {
        PluginSettings settings = PluginSettings.getInstance();
        
        // 检查国内大模型是否可用
        if (settings.isEnableDomesticAI() && 
            settings.getDomesticAIApiKey() != null && 
            !settings.getDomesticAIApiKey().trim().isEmpty()) {
            return validateDomesticAICredentials();
        }
        
        // 检查Google翻译是否可用
        if (settings.isEnableGoogleTranslation() && 
            settings.getGoogleApiKey() != null && 
            !settings.getGoogleApiKey().trim().isEmpty() &&
            settings.getGoogleProjectId() != null && 
            !settings.getGoogleProjectId().trim().isEmpty()) {
            return validateGoogleCredentials();
        }
        
        // 检查百度翻译是否可用（备用）
        if (!BaiduAPI.isApiConfigured()) {
            Messages.showErrorDialog(
                    "未配置任何翻译引擎，无法使用翻译功能。\n\n" +
                    "请在 设置 > 工具 > PandaCoder 中配置以下任一翻译引擎：\n" +
                    "• 国内大模型翻译（通义千问/文心一言/智谱AI）\n" +
                    "• Google Cloud Translation\n" +
                    "• 百度翻译API",
                    "需要翻译API配置");
            return false;
        }

        // 验证百度API配置是否正确
        return validateBaiduCredentials();
    }
    
    /**
     * 验证国内大模型API配置
     */
    private static boolean validateDomesticAICredentials() {
        long currentTime = System.currentTimeMillis();

        // 如果验证缓存未过期，直接返回缓存结果
        if (currentTime - lastValidationTime < VALIDATION_CACHE_TIMEOUT) {
            return apiValidationCache;
        }

        try {
            // 简单测试翻译
            String testResult = com.shuyixiao.DomesticAITranslationAPI.translate("测试");
            
            // 更新缓存
            apiValidationCache = testResult != null && !testResult.trim().isEmpty();
            lastValidationTime = currentTime;

            if (!apiValidationCache) {
                Messages.showErrorDialog(
                        "国内大模型API验证失败，返回结果为空。\n\n" +
                        "请检查API密钥和模型配置是否正确。",
                        "API配置无效");
                return false;
            }

            return true;
        } catch (Exception e) {
            // 更新缓存为失败状态
            apiValidationCache = false;
            lastValidationTime = currentTime;

            Messages.showErrorDialog(
                    "国内大模型API连接失败：" + e.getMessage() + "\n\n" +
                    "请检查API密钥、网络连接和模型配置是否正确。",
                    "API连接错误");
            return false;
        }
    }
    
    /**
     * 验证Google翻译API配置
     */
    private static boolean validateGoogleCredentials() {
        try {
            String testResult = com.shuyixiao.GoogleCloudTranslationAPI.translate("测试");
            if (testResult != null && !testResult.trim().isEmpty()) {
                return true;
            } else {
                Messages.showErrorDialog(
                        "Google Cloud Translation API返回结果为空。\n\n" +
                        "请检查API密钥和项目ID配置是否正确。",
                        "API配置无效");
                return false;
            }
        } catch (Exception e) {
            Messages.showErrorDialog(
                    "Google Cloud Translation API连接失败：" + e.getMessage() + "\n\n" +
                    "请检查API密钥、项目ID和网络连接是否正确。",
                    "API连接错误");
            return false;
        }
    }

    /**
     * 验证百度API配置是否正确
     * 
     * @return 配置正确返回true，否则返回false
     */
    private static boolean validateBaiduCredentials() {
        long currentTime = System.currentTimeMillis();

        // 如果验证缓存未过期，直接返回缓存结果
        if (currentTime - lastValidationTime < VALIDATION_CACHE_TIMEOUT) {
            return apiValidationCache;
        }

        try {
            boolean isValid = BaiduAPI.validateApiConfiguration();

            // 更新缓存
            apiValidationCache = isValid;
            lastValidationTime = currentTime;

            if (!isValid) {
                Messages.showErrorDialog(
                        "百度翻译API验证失败，您配置的API密钥或应用ID不正确。\n\n" +
                        "请前往 设置 > 工具 > PandaCoder 检查并更新您的配置。\n" +
                        "您可以从百度翻译开放平台获取正确的API密钥和应用ID：\n" +
                        "https://fanyi-api.baidu.com/manage/developer",
                        "API配置无效");
                return false;
            }

            return true;
        } catch (Exception e) {
            // 更新缓存为失败状态
            apiValidationCache = false;
            lastValidationTime = currentTime;

            Messages.showErrorDialog(
                    "百度翻译API连接失败：" + e.getMessage() + "\n\n" +
                    "请检查您的网络连接和API配置是否正确。\n" +
                    "如果问题持续存在，请联系插件开发者。",
                    "API连接错误");
            return false;
        }
    }

    /**
     * 清除API验证缓存
     * 当用户更改API配置后调用此方法
     */
    public static void clearValidationCache() {
        apiValidationCache = false;
        lastValidationTime = 0;
    }
}
