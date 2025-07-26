package com.shuyixiao.spring.boot.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import com.shuyixiao.spring.boot.yaml.YamlConfigReader;
import com.shuyixiao.spring.boot.yaml.YamlTechStackRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * YAML配置服务
 * 提供项目范围内的YAML配置文件管理和技术栈分析
 */
@Service
public final class YamlConfigService {

    private static final Logger LOG = Logger.getInstance(YamlConfigService.class);

    private final Project project;

    /**
     * 获取项目的YAML配置服务实例
     *
     * @param project 当前项目
     * @return YAML配置服务实例
     */
    public static YamlConfigService getInstance(@NotNull Project project) {
        return project.getService(YamlConfigService.class);
    }

    public YamlConfigService(Project project) {
        this.project = project;
        LOG.info("YamlConfigService initialized for project: " + project.getName());

        // 注册文件编辑器监听器
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        if (SpringBootFileDetector.isSpringBootYamlFile(file)) {
                            LOG.debug("Spring Boot YAML file opened: " + file.getPath());
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Editor editor = source.getSelectedTextEditor();
                                if (editor != null) {
                                    YamlTechStackRenderer.renderTechStackIcons(editor, file);
                                }
                            });
                        }
                    }

                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        VirtualFile file = event.getNewFile();
                        if (file != null && SpringBootFileDetector.isSpringBootYamlFile(file)) {
                            LOG.debug("Selection changed to Spring Boot YAML file: " + file.getPath());
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Editor editor = event.getManager().getSelectedTextEditor();
                                if (editor != null) {
                                    YamlTechStackRenderer.renderTechStackIcons(editor, file);
                                }
                            });
                        }
                    }
                });
    }

    /**
     * 扫描项目中的所有Spring Boot YAML配置文件
     *
     * @return YAML配置文件列表
     */
    public List<PsiFile> scanProjectYamlFiles() {
        LOG.info("Scanning for YAML config files in project: " + project.getName());
        List<PsiFile> yamlFiles = YamlConfigReader.getAllSpringBootYamlFiles(project);
        LOG.info("Found " + yamlFiles.size() + " YAML config files");
        return yamlFiles;
    }

    /**
     * 更新所有打开的YAML编辑器
     */
    public void updateOpenEditors() {
        LOG.debug("Updating all open YAML editors");
        YamlTechStackRenderer.updateOpenEditors(project);
    }
}
