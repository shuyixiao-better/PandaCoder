package com.shuyixiao.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.shuyixiao.ui.UpdateNotificationDialog;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateCheckService.java
 * author 舒一笑
 * version 1.0.0
 * Description 版本更新检查服务 在项目启动时检查插件版本更新
 * createTime 2025年07月31日 11:48:56
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class UpdateCheckService implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(UpdateCheckService.class);
    private static final String CURRENT_VERSION = com.shuyixiao.version.VersionInfo.getVersion();
    private static final String UPDATE_CHECK_URL = "https://api.github.com/repos/your-username/PandaCoder/releases/latest";
    private static final String PLUGIN_PAGE_URL = "https://plugins.jetbrains.com/plugin/your-plugin-id";

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 在后台线程中检查更新
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                checkForUpdates(project);
            } catch (Exception e) {
                LOG.warn("版本更新检查失败", e);
            }
        });

        return null;
    }

    /**
     * 检查版本更新
     */
    private void checkForUpdates(Project project) {
        try {
            // 检查是否已经显示过更新提示
            if (isUpdateNotificationShown()) {
                return;
            }

            // 获取最新版本信息
            String latestVersion = getLatestVersion();
            if (latestVersion != null && isNewerVersion(latestVersion)) {
                // 在UI线程中显示更新提示
                ApplicationManager.getApplication().invokeLater(() -> {
                    UpdateNotificationDialog.show(project, CURRENT_VERSION, latestVersion);
                });
            }
        } catch (Exception e) {
            LOG.warn("版本更新检查异常", e);
        }
    }

    /**
     * 获取最新版本号
     */
    private String getLatestVersion() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPDATE_CHECK_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 解析JSON响应获取版本号
                // 这里需要根据实际的GitHub API响应格式来解析
                return parseVersionFromResponse(response.body());
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("获取最新版本失败", e);
        }

        return null;
    }

    /**
     * 从响应中解析版本号
     */
    private String parseVersionFromResponse(String responseBody) {
        // 这里需要根据实际的GitHub API响应格式来解析
        // 示例：从JSON中提取tag_name字段
        if (responseBody.contains("\"tag_name\"")) {
            int start = responseBody.indexOf("\"tag_name\"") + 12;
            int end = responseBody.indexOf("\"", start);
            if (end > start) {
                return responseBody.substring(start, end);
            }
        }
        return null;
    }

    /**
     * 比较版本号
     */
    private boolean isNewerVersion(String latestVersion) {
        try {
            String[] currentParts = CURRENT_VERSION.split("\\.");
            String[] latestParts = latestVersion.split("\\.");

            int maxLength = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < maxLength; i++) {
                int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latest = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latest > current) {
                    return true;
                } else if (latest < current) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            LOG.warn("版本号格式错误", e);
        }

        return false;
    }

    /**
     * 检查是否已经显示过更新提示
     */
    private boolean isUpdateNotificationShown() {
        Properties properties = new Properties();
        try {
            // 从用户配置中读取上次显示时间
            String lastShown = properties.getProperty("pandacoder.update.last.shown", "");
            String currentVersion = properties.getProperty("pandacoder.update.last.version", "");

            // 如果当前版本已经显示过，则不重复显示
            return CURRENT_VERSION.equals(currentVersion);
        } catch (Exception e) {
            LOG.warn("读取更新提示状态失败", e);
        }

        return false;
    }

    /**
     * 标记更新提示已显示
     */
    public static void markUpdateNotificationShown() {
        Properties properties = new Properties();
        properties.setProperty("pandacoder.update.last.shown", String.valueOf(System.currentTimeMillis()));
        properties.setProperty("pandacoder.update.last.version", CURRENT_VERSION);

        // 保存到用户配置
        try {
            // 这里需要实现配置保存逻辑
            // 可以使用IntelliJ的PersistentStateComponent
        } catch (Exception e) {
            LOG.warn("保存更新提示状态失败", e);
        }
    }
}
