package com.shuyixiao.setting;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName PluginSettings.java
 * author 舒一笑 yixiaoshu88@163.com
 * version 1.0.0
 * Description TODO
 * createTime 2024年09月07日 21:01:00
 */
@State(
    name = "PandaCoderSettings",
    storages = @Storage("pandacoder-settings.xml")
)
public class PluginSettings implements PersistentStateComponent<PluginSettings> {

    private String template = "/**\n" +
            " * Copyright © ${YEAR} integration-projects-maven. All rights reserved.\n" +
            " * ClassName ${NAME}.java\n" +
            " * author 舒一笑\n" +
            " * version 1.0.0\n" +
            " * Description TODO\n" +
            " * createTime ${TIME}\n" +
            " */\n";

                // 类名前缀配置，多个前缀用逗号分隔
                private String classPrefixes = "Service,Repository,Controller,Component,Util,Manager,Factory,Builder,Handler";

    // 单例模式获取实例
    public static PluginSettings getInstance() {
        // 使用新的API代替已弃用的ServiceManager
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(PluginSettings.class);
    }

    // 获取模板，确保不会返回null
    public String getTemplate() {
        if (template == null || template.isEmpty()) {
            template = "/**\n" +
                    " * Copyright © ${YEAR} integration-projects-maven. All rights reserved.\n" +
                    " * ClassName ${NAME}.java\n" +
                    " * author 舒一笑\n" +
                    " * version 1.0.0\n" +
                    " * Description TODO\n" +
                    " * createTime ${TIME}\n" +
                    " */\n"; // 设置默认模板
        }
        return template;
    }

    // 设置模板
    public void setTemplate(String template) {
        this.template = template;
    }

    public String getClassPrefixes() {
        if (classPrefixes == null || classPrefixes.isEmpty()) {
            classPrefixes = "Service,Repository,Controller,Component,Util,Manager,Factory,Builder,Handler";
        }
        return classPrefixes;
    }

    public void setClassPrefixes(String classPrefixes) {
        this.classPrefixes = classPrefixes;
    }

    @Nullable
    @Override
    public PluginSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PluginSettings state) {
        // 确保state不为空
        XmlSerializerUtil.copyBean(state, this);
        if (this.template == null || this.template.isEmpty()) {
            this.template = "/**\n" +
                    " * Copyright © ${YEAR} integration-projects-maven. All rights reserved.\n" +
                    " * ClassName ${NAME}.java\n" +
                    " * author 舒一笑\n" +
                    " * version 1.0.0\n" +
                    " * Description TODO\n" +
                    " * createTime ${TIME}\n" +
                    " */\n";  // 初始化默认模板
        }

        // 确保classPrefixes不为空
        if (this.classPrefixes == null || this.classPrefixes.isEmpty()) {
            this.classPrefixes = "Service,Repository,Controller,Component,Util,Manager,Factory,Builder,Handler";
        }
    }
}


