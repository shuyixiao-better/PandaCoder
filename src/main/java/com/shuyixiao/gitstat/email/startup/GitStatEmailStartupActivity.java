package com.shuyixiao.gitstat.email.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.shuyixiao.gitstat.email.config.GitStatEmailConfigState;
import com.shuyixiao.gitstat.email.migration.EmailConfigMigration;
import org.jetbrains.annotations.NotNull;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName GitStatEmailStartupActivity.java
 * author 舒一笑不秃头
 * version 2.1.0
 * Description Git统计邮件功能启动活动，负责在项目启动时检查配置迁移、验证配置完整性等初始化工作
 * createTime 2025-10-23
 * 技术分享 · 公众号：舒一笑的架构笔记
 * 
 * 功能说明：
 * 1. 检查并迁移旧版本的邮件配置
 * 2. 验证配置完整性
 * 3. 创建配置备份
 * 4. 如果配置丢失，提示用户重新配置
 */
public class GitStatEmailStartupActivity implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(GitStatEmailStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("初始化 Git 统计邮件功能 for project: " + project.getName());
        
        try {
            // 获取当前配置（项目级别）
            GitStatEmailConfigState configState = GitStatEmailConfigState.getInstance(project);

            // 检查并迁移配置
            EmailConfigMigration.checkAndMigrate(project);

            // 验证配置完整性
            boolean isValid = EmailConfigMigration.validateConfig(configState);
            if (isValid) {
                LOG.info("Git 统计邮件配置验证通过");
                // 创建配置备份
                EmailConfigMigration.backupConfig(project);
            } else {
                if (configState.isConfigured()) {
                    LOG.warn("Git 统计邮件配置不完整，部分功能可能无法正常使用");
                } else {
                    LOG.info("Git 统计邮件功能尚未配置");
                }
            }
            
            LOG.info("Git 统计邮件功能初始化完成 for project: " + project.getName());
            
        } catch (Exception e) {
            LOG.error("初始化 Git 统计邮件功能失败", e);
        }
    }
}

