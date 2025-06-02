package com.shuyixiao.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 转换设置类
 * 存储用户的转换首选项
 */
@State(
    name = "PandaCoderConversionSettings",
    storages = @Storage("pandacoder-conversion-settings.xml")
)
public class ConversionSettings implements PersistentStateComponent<ConversionSettings> {

    // 默认转换模式
    private String defaultConversionMode = "REPLACE_DIRECTLY";

    // 是否显示转换后的通知
    private boolean showConversionNotification = true;

    // 是否在转换后自动复制到剪贴板
    private boolean autoCopyToClipboard = false;

    // 单例模式获取实例
    public static ConversionSettings getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(ConversionSettings.class);
    }

    public String getDefaultConversionMode() {
        return defaultConversionMode;
    }

    public void setDefaultConversionMode(String defaultConversionMode) {
        this.defaultConversionMode = defaultConversionMode;
    }

    public boolean isShowConversionNotification() {
        return showConversionNotification;
    }

    public void setShowConversionNotification(boolean showConversionNotification) {
        this.showConversionNotification = showConversionNotification;
    }

    public boolean isAutoCopyToClipboard() {
        return autoCopyToClipboard;
    }

    public void setAutoCopyToClipboard(boolean autoCopyToClipboard) {
        this.autoCopyToClipboard = autoCopyToClipboard;
    }

    @Nullable
    @Override
    public ConversionSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ConversionSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
