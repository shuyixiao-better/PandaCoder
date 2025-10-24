package com.shuyixiao.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateSettingsState.java
 * author 舒一笑不秃头
 * version 1.0.0
 * Description 更新设置状态持久化组件 保存更新检查相关的用户设置
 * createTime 2025年07月31日 11:34:35
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
@State(
        name = "com.shuyixiao.update.UpdateSettingsState",
        storages = @Storage("pandacoder-update-settings.xml")
)
public class UpdateSettingsState implements PersistentStateComponent<UpdateSettingsState> {

    /**
     * 提醒频率枚举
     */
    public enum ReminderFrequency {
        STARTUP("每次启动", 0),
        DAILY("每天一次", 24 * 60 * 60 * 1000L),
        WEEKLY("每周一次", 7 * 24 * 60 * 60 * 1000L),
        DISABLED("从不", Long.MAX_VALUE);

        private final String displayName;
        private final long intervalMillis;

        ReminderFrequency(String displayName, long intervalMillis) {
            this.displayName = displayName;
            this.intervalMillis = intervalMillis;
        }

        public String getDisplayName() {
            return displayName;
        }

        public long getIntervalMillis() {
            return intervalMillis;
        }
    }

    // 上次显示的版本号
    private String lastShownVersion = "";
    
    // 上次显示的时间戳
    private long lastShownTime = 0;
    
    // 上次检查更新的时间戳
    private long lastCheckTime = 0;
    
    // 是否启用更新通知
    private boolean updateNotificationsEnabled = true;
    
    // 是否启用自动检查
    private boolean autoCheckEnabled = true;
    
    // 提醒频率
    private String reminderFrequency = ReminderFrequency.DAILY.name();
    
    // 已忽略的版本列表
    private List<String> ignoredVersions = new ArrayList<>();
    
    // 今天是否已经检查过（用于每天首次检查）
    private String lastCheckDate = "";

    public static UpdateSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(UpdateSettingsState.class);
    }

    @Nullable
    @Override
    public UpdateSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull UpdateSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // ==================== Getters and Setters ====================

    public String getLastShownVersion() {
        return lastShownVersion;
    }

    public void setLastShownVersion(String lastShownVersion) {
        this.lastShownVersion = lastShownVersion;
    }

    public long getLastShownTime() {
        return lastShownTime;
    }

    public void setLastShownTime(long lastShownTime) {
        this.lastShownTime = lastShownTime;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(long lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public boolean isUpdateNotificationsEnabled() {
        return updateNotificationsEnabled;
    }

    public void setUpdateNotificationsEnabled(boolean updateNotificationsEnabled) {
        this.updateNotificationsEnabled = updateNotificationsEnabled;
    }

    public boolean isAutoCheckEnabled() {
        return autoCheckEnabled;
    }

    public void setAutoCheckEnabled(boolean autoCheckEnabled) {
        this.autoCheckEnabled = autoCheckEnabled;
    }

    public String getReminderFrequency() {
        return reminderFrequency;
    }

    public void setReminderFrequency(String reminderFrequency) {
        this.reminderFrequency = reminderFrequency;
    }

    public ReminderFrequency getReminderFrequencyEnum() {
        try {
            return ReminderFrequency.valueOf(reminderFrequency);
        } catch (IllegalArgumentException e) {
            return ReminderFrequency.DAILY;
        }
    }

    public List<String> getIgnoredVersions() {
        return ignoredVersions;
    }

    public void setIgnoredVersions(List<String> ignoredVersions) {
        this.ignoredVersions = ignoredVersions;
    }

    public String getLastCheckDate() {
        return lastCheckDate;
    }

    public void setLastCheckDate(String lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
    }

    // ==================== Helper Methods ====================

    /**
     * 检查版本是否已被忽略
     */
    public boolean isVersionIgnored(String version) {
        return ignoredVersions != null && ignoredVersions.contains(version);
    }

    /**
     * 添加忽略的版本
     */
    public void addIgnoredVersion(String version) {
        if (ignoredVersions == null) {
            ignoredVersions = new ArrayList<>();
        }
        if (!ignoredVersions.contains(version)) {
            ignoredVersions.add(version);
        }
    }

    /**
     * 移除忽略的版本
     */
    public void removeIgnoredVersion(String version) {
        if (ignoredVersions != null) {
            ignoredVersions.remove(version);
        }
    }

    /**
     * 检查是否应该显示通知（基于时间间隔）
     */
    public boolean shouldShowNotification() {
        if (!updateNotificationsEnabled) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long interval = getReminderFrequencyEnum().getIntervalMillis();
        
        return currentTime - lastShownTime >= interval;
    }

    /**
     * 更新显示记录
     */
    public void markNotificationShown(String version) {
        this.lastShownVersion = version;
        this.lastShownTime = System.currentTimeMillis();
    }

    /**
     * 检查是否应该执行检查更新（用于每天首次检查）
     */
    public boolean shouldCheckUpdate() {
        if (!autoCheckEnabled || !updateNotificationsEnabled) {
            return false;
        }

        // 获取今天的日期
        String today = java.time.LocalDate.now().toString();
        
        // 如果今天还没有检查过，则需要检查
        if (!today.equals(lastCheckDate)) {
            return true;
        }

        // 如果是STARTUP频率，每次启动都检查
        return getReminderFrequencyEnum() == ReminderFrequency.STARTUP;
    }

    /**
     * 标记已执行检查
     */
    public void markCheckPerformed() {
        this.lastCheckTime = System.currentTimeMillis();
        this.lastCheckDate = java.time.LocalDate.now().toString();
    }
}
