package com.shuyixiao.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.shuyixiao.version.VersionInfo;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateCheckService.java
 * author 舒一笑不秃头
 * version 1.0.0
 * Description 版本更新检查服务 智能检测插件版本更新并显示优雅提醒
 * 检测时机：IDE启动时（延迟5秒）、每天首次打开项目时
 * createTime 2025年10月24日 15:45:00
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class UpdateCheckService implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(UpdateCheckService.class);
    
    // 插件ID和版本信息
    private static final String PLUGIN_ID = "27533";
    private static final String CURRENT_VERSION = VersionInfo.getVersion();
    
    // JetBrains插件市场API
    private static final String PLUGIN_API_URL = "https://plugins.jetbrains.com/api/plugins/" + PLUGIN_ID + "/updates";
    
    // 启动延迟时间（秒）
    private static final int STARTUP_DELAY_SECONDS = 5;
    
    // HTTP超时时间（秒）
    private static final int HTTP_TIMEOUT_SECONDS = 10;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 延迟5秒后在后台线程中检查更新，避免启动卡顿
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 延迟执行
                TimeUnit.SECONDS.sleep(STARTUP_DELAY_SECONDS);
                
                // 检查更新
                checkForUpdates(project);
            } catch (InterruptedException e) {
                LOG.debug("更新检查被中断", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.warn("版本更新检查失败", e);
            }
        });

        return null;
    }

    /**
     * 检查版本更新
     * 智能检测时机：
     * 1. 每天首次打开项目时
     * 2. 根据用户设置的提醒频率
     * 3. 遵循防打扰机制
     */
    private void checkForUpdates(@NotNull Project project) {
        UpdateSettingsState settings = UpdateSettingsState.getInstance();
        
        // 检查是否应该执行更新检查
        if (!settings.shouldCheckUpdate()) {
            LOG.info("跳过更新检查：未启用或未到检查时间");
            return;
        }

        try {
            LOG.info("开始检查插件更新，当前版本: " + CURRENT_VERSION);
            
            // 获取最新版本信息
            PluginUpdateInfo updateInfo = getLatestVersionInfo();
            
            if (updateInfo == null) {
                LOG.warn("无法获取最新版本信息");
                settings.markCheckPerformed();
                return;
            }

            LOG.info("获取到最新版本: " + updateInfo.version);
            
            // 比较版本号
            if (isNewerVersion(updateInfo.version, CURRENT_VERSION)) {
                LOG.info("发现新版本: " + CURRENT_VERSION + " -> " + updateInfo.version);
                
                // 标记已执行检查
                settings.markCheckPerformed();
                
                // 在UI线程中显示更新通知
                ApplicationManager.getApplication().invokeLater(() -> {
                    showUpdateNotification(project, updateInfo);
                });
            } else {
                LOG.info("当前版本已是最新版本");
                settings.markCheckPerformed();
            }
            
        } catch (Exception e) {
            LOG.warn("版本更新检查异常", e);
            settings.markCheckPerformed();
        }
    }

    /**
     * 从JetBrains插件市场获取最新版本信息
     */
    @Nullable
    private PluginUpdateInfo getLatestVersionInfo() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                    .build();

            // 先获取插件更新列表
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PLUGIN_API_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parsePluginUpdateInfo(response.body());
            } else {
                LOG.warn("获取插件更新信息失败，状态码: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("请求插件市场API失败", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return null;
    }

    /**
     * 解析插件更新信息
     * JetBrains API返回的是更新列表，格式如：
     * [
     *   {
     *     "id": 123456,
     *     "version": "2.2.0",
     *     "channel": "",
     *     "notes": "更新说明..."
     *   }
     * ]
     */
    @Nullable
    private PluginUpdateInfo parsePluginUpdateInfo(@NotNull String responseBody) {
        try {
            JsonArray updates = JsonParser.parseString(responseBody).getAsJsonArray();
            
            if (updates.size() == 0) {
                return null;
            }

            // 获取第一个更新（最新版本）
            JsonObject latestUpdate = updates.get(0).getAsJsonObject();
            
            PluginUpdateInfo info = new PluginUpdateInfo();
            info.version = getJsonString(latestUpdate, "version", "");
            info.releaseNotes = getJsonString(latestUpdate, "notes", "");
            
            // 移除HTML标签，简化更新说明
            info.releaseNotes = simplifyReleaseNotes(info.releaseNotes);
            
            return info;
            
        } catch (Exception e) {
            LOG.warn("解析插件更新信息失败", e);
            return null;
        }
    }

    /**
     * 从JSON对象中安全获取字符串
     */
    private String getJsonString(@NotNull JsonObject json, @NotNull String key, @NotNull String defaultValue) {
        JsonElement element = json.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : defaultValue;
    }

    /**
     * 简化更新说明
     */
    private String simplifyReleaseNotes(@NotNull String notes) {
        if (notes.isEmpty()) {
            return "";
        }
        
        // 移除HTML标签
        String simplified = notes.replaceAll("<[^>]+>", "");
        
        // 提取前3-5行主要内容
        String[] lines = simplified.split("[\n\r]+");
        StringBuilder result = new StringBuilder();
        
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && count < 5) {
                result.append(line).append("\n");
                count++;
            }
        }
        
        return result.toString().trim();
    }

    /**
     * 比较版本号
     * 支持格式：1.0.0, 2.1.5 等
     * 
     * @param latestVersion 最新版本号
     * @param currentVersion 当前版本号
     * @return true 如果最新版本更高
     */
    private boolean isNewerVersion(@NotNull String latestVersion, @NotNull String currentVersion) {
        try {
            // 移除可能的v前缀
            latestVersion = latestVersion.replaceFirst("^v", "");
            currentVersion = currentVersion.replaceFirst("^v", "");
            
            String[] latestParts = latestVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");

            int maxLength = Math.max(latestParts.length, currentParts.length);

            for (int i = 0; i < maxLength; i++) {
                int latest = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
                int current = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

                if (latest > current) {
                    return true;
                } else if (latest < current) {
                    return false;
                }
            }
        } catch (Exception e) {
            LOG.warn("版本号比较失败: " + latestVersion + " vs " + currentVersion, e);
        }

        return false;
    }

    /**
     * 解析版本号部分，支持数字和数字+字母组合
     */
    private int parseVersionPart(@NotNull String part) {
        try {
            // 提取数字部分
            StringBuilder number = new StringBuilder();
            for (char c : part.toCharArray()) {
                if (Character.isDigit(c)) {
                    number.append(c);
                } else {
                    break;
                }
            }
            return number.length() > 0 ? Integer.parseInt(number.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 显示更新通知
     */
    private void showUpdateNotification(@NotNull Project project, @NotNull PluginUpdateInfo updateInfo) {
        // 判断是否为重大版本更新（主版本号变化）
        if (isMajorVersionUpdate(updateInfo.version, CURRENT_VERSION)) {
            UpdateNotificationService.showMajorUpdateNotification(
                project, 
                CURRENT_VERSION, 
                updateInfo.version, 
                updateInfo.releaseNotes
            );
        } else {
            UpdateNotificationService.showUpdateNotification(
                project, 
                CURRENT_VERSION, 
                updateInfo.version, 
                updateInfo.releaseNotes
            );
        }
    }

    /**
     * 判断是否为重大版本更新
     */
    private boolean isMajorVersionUpdate(@NotNull String latestVersion, @NotNull String currentVersion) {
        try {
            String[] latestParts = latestVersion.replaceFirst("^v", "").split("\\.");
            String[] currentParts = currentVersion.replaceFirst("^v", "").split("\\.");
            
            if (latestParts.length > 0 && currentParts.length > 0) {
                int latestMajor = Integer.parseInt(latestParts[0]);
                int currentMajor = Integer.parseInt(currentParts[0]);
                return latestMajor > currentMajor;
            }
        } catch (Exception e) {
            LOG.debug("判断重大版本更新失败", e);
        }
        
        return false;
    }

    /**
     * 插件更新信息类
     */
    private static class PluginUpdateInfo {
        String version;
        String releaseNotes;
    }

    /**
     * 手动检查更新（供用户主动触发）
     */
    public static void manualCheckUpdate(@Nullable Project project) {
        UpdateCheckService service = new UpdateCheckService();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                service.checkForUpdates(project);
            } catch (Exception e) {
                LOG.warn("手动检查更新失败", e);
            }
        });
    }
}
