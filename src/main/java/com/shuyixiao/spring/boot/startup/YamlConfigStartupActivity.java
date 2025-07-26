package com.shuyixiao.spring.boot.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.shuyixiao.spring.boot.service.YamlConfigService;
import com.shuyixiao.spring.boot.yaml.YamlTechStackRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * YAML配置启动活动
 * 在项目启动时初始化YAML配置服务和渲染器
 */
public class YamlConfigStartupActivity implements StartupActivity.DumbAware {

    private static final Logger LOG = Logger.getInstance(YamlConfigStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing YAML config services for project: " + project.getName());

        // 获取YAML配置服务实例
        YamlConfigService configService = YamlConfigService.getInstance(project);

        // 注册编辑器监听器，使用正确的Disposable
        EditorFactory.getInstance().addEditorFactoryListener(
                new YamlTechStackRenderer(),
                project
        );

        // 扫描项目YAML文件
        configService.scanProjectYamlFiles();

        // 更新已打开的编辑器
        configService.updateOpenEditors();

        LOG.info("YAML config services initialization completed");
    }
}
