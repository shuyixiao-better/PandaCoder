package com.shuyixiao.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright © 2025 integration-projects-maven. All rights reserved.
 * ClassName UpdateCheckService.java
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

    private String lastShownVersion = "";
    private long lastShownTime = 0;
    private boolean updateNotificationsEnabled = true;

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

    public boolean isUpdateNotificationsEnabled() {
        return updateNotificationsEnabled;
    }

    public void setUpdateNotificationsEnabled(boolean updateNotificationsEnabled) {
        this.updateNotificationsEnabled = updateNotificationsEnabled;
    }
}
