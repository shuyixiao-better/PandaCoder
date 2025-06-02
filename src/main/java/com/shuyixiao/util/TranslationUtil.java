package com.shuyixiao.util;

import com.intellij.openapi.ui.Messages;
import com.shuyixiao.BaiduAPI;

public class TranslationUtil {

    // 存储API验证状态的缓存
    private static boolean apiValidationCache = false;
    private static long lastValidationTime = 0;
    private static final long VALIDATION_CACHE_TIMEOUT = 5 * 60 * 1000; // 5分钟缓存过期

    /**
     * 检查百度翻译API配置是否已设置
     * 
     * @return 配置已设置返回true，否则返回false
     */
    public static boolean checkApiConfiguration() {
        if (!BaiduAPI.isApiConfigured()) {
            Messages.showErrorDialog(
                    "您需要先配置百度翻译API才能使用此功能。\n\n" +
                    "请前往 设置 > 工具 > PandaCoder 配置百度API密钥和应用ID。\n" +
                    "如果您还没有百度翻译API账号，请访问百度翻译开放平台注册：\n" +
                    "https://fanyi-api.baidu.com/",
                    "需要API配置");
            return false;
        }

        // API配置已设置，现在验证API配置是否正确
        return validateApiCredentials();
    }

    /**
     * 验证API配置是否正确
     * 
     * @return 配置正确返回true，否则返回false
     */
    private static boolean validateApiCredentials() {
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
