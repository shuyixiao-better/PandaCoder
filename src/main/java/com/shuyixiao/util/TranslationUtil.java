package com.shuyixiao.util;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.shuyixiao.BaiduAPI;
import com.shuyixiao.setting.PluginSettings;
import com.shuyixiao.ui.EnhancedNotificationUtil;

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
        return checkApiConfiguration(null);
    }
    
    /**
     * 检查翻译API配置是否已设置（带项目参数）
     * 支持三级翻译引擎：国内大模型 > Google翻译 > 百度翻译
     * 
     * @param project 当前项目
     * @return 配置已设置返回true，否则返回false
     */
    public static boolean checkApiConfiguration(Project project) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        
        PluginSettings settings = PluginSettings.getInstance();
        
        // 检查国内大模型是否可用
        if (settings.isEnableDomesticAI() && 
            settings.getDomesticAIApiKey() != null && 
            !settings.getDomesticAIApiKey().trim().isEmpty()) {
            return validateDomesticAICredentials(project);
        }
        
        // 检查Google翻译是否可用
        if (settings.isEnableGoogleTranslation() && 
            settings.getGoogleApiKey() != null && 
            !settings.getGoogleApiKey().trim().isEmpty() &&
            settings.getGoogleProjectId() != null && 
            !settings.getGoogleProjectId().trim().isEmpty()) {
            return validateGoogleCredentials(project);
        }
        
        // 检查百度翻译是否可用（备用）
        if (!BaiduAPI.isApiConfigured()) {
            showTranslationSetupGuide(project);
            return false;
        }

        // 验证百度API配置是否正确
        return validateBaiduCredentials(project);
    }
    
    /**
     * 显示翻译设置向导
     */
    private static void showTranslationSetupGuide(Project project) {
        EnhancedNotificationUtil.showTranslationSetupGuide(project, () -> {
            openPluginSettings(project);
        });
    }
    
    /**
     * 打开插件设置页面
     */
    private static void openPluginSettings(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "PandaCoder");
    }
    
    /**
     * 验证国内大模型API配置
     */
    private static boolean validateDomesticAICredentials(Project project) {
        long currentTime = System.currentTimeMillis();

        // 如果验证缓存未过期，直接返回缓存结果
        if (currentTime - lastValidationTime < VALIDATION_CACHE_TIMEOUT) {
            return apiValidationCache;
        }

        try {
            // 显示验证进度
            EnhancedNotificationUtil.showProgress(project, "🔍 验证中", "正在验证国内大模型API配置...");
            
            // 简单测试翻译
            String testResult = com.shuyixiao.DomesticAITranslationAPI.translate("测试");
            
            // 更新缓存
            apiValidationCache = testResult != null && !testResult.trim().isEmpty();
            lastValidationTime = currentTime;

            if (!apiValidationCache) {
                EnhancedNotificationUtil.showApiConfigError(
                    project,
                    "国内大模型",
                    "API验证失败，返回结果为空",
                    null,
                    () -> openPluginSettings(project)
                );
                return false;
            }

            return true;
        } catch (Exception e) {
            // 更新缓存为失败状态
            apiValidationCache = false;
            lastValidationTime = currentTime;

            EnhancedNotificationUtil.showApiConfigError(
                project,
                "国内大模型",
                e.getMessage(),
                null,
                () -> openPluginSettings(project)
            );
            return false;
        }
    }
    
    /**
     * 验证Google翻译API配置
     */
    private static boolean validateGoogleCredentials(Project project) {
        try {
            EnhancedNotificationUtil.showProgress(project, "🔍 验证中", "正在验证Google翻译API配置...");
            
            String testResult = com.shuyixiao.GoogleCloudTranslationAPI.translate("测试");
            if (testResult != null && !testResult.trim().isEmpty()) {
                return true;
            } else {
                EnhancedNotificationUtil.showApiConfigError(
                    project,
                    "Google翻译",
                    "API返回结果为空，请检查配置",
                    "https://cloud.google.com/translate/docs",
                    () -> openPluginSettings(project)
                );
                return false;
            }
        } catch (Exception e) {
            EnhancedNotificationUtil.showApiConfigError(
                project,
                "Google翻译",
                e.getMessage(),
                "https://cloud.google.com/translate/docs",
                () -> openPluginSettings(project)
            );
            return false;
        }
    }

    /**
     * 验证百度API配置是否正确
     * 
     * @return 配置正确返回true，否则返回false
     */
    private static boolean validateBaiduCredentials(Project project) {
        long currentTime = System.currentTimeMillis();

        // 如果验证缓存未过期，直接返回缓存结果
        if (currentTime - lastValidationTime < VALIDATION_CACHE_TIMEOUT) {
            return apiValidationCache;
        }

        try {
            EnhancedNotificationUtil.showProgress(project, "🔍 验证中", "正在验证百度翻译API配置...");
            
            boolean isValid = BaiduAPI.validateApiConfiguration();

            // 更新缓存
            apiValidationCache = isValid;
            lastValidationTime = currentTime;

            if (!isValid) {
                EnhancedNotificationUtil.showApiConfigError(
                    project,
                    "百度翻译",
                    "API密钥或应用ID不正确",
                    "https://fanyi-api.baidu.com/manage/developer",
                    () -> openPluginSettings(project)
                );
                return false;
            }

            return true;
        } catch (Exception e) {
            // 更新缓存为失败状态
            apiValidationCache = false;
            lastValidationTime = currentTime;

            EnhancedNotificationUtil.showApiConfigError(
                project,
                "百度翻译",
                e.getMessage(),
                "https://fanyi-api.baidu.com/manage/developer",
                () -> openPluginSettings(project)
            );
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
    
    /**
     * 显示翻译成功通知
     */
    public static void showTranslationSuccess(Project project, String original, String translated, String engine) {
        EnhancedNotificationUtil.showTranslationSuccess(project, original, translated, engine);
    }
}
