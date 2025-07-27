package com.shuyixiao.spring.boot.yaml;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * YAML配置文件读取器
 * 用于扫描项目中的YAML配置文件并提供访问接口
 */
public class YamlConfigReader {

    private static final Logger LOG = Logger.getInstance(YamlConfigReader.class);

    /**
     * 获取项目中所有的Spring Boot YAML配置文件
     *
     * @param project 当前项目
     * @return YAML配置文件列表
     */
    @NotNull
    public static List<PsiFile> getAllSpringBootYamlFiles(@NotNull Project project) {
        LOG.debug("Scanning for Spring Boot YAML files in project: " + project.getName());

        // 使用ReadAction确保在正确的线程上下文中访问文件索引和PSI
        return ReadAction.compute(() -> {
            List<PsiFile> yamlFiles = new ArrayList<>();

            // 查找所有.yml文件
            Collection<VirtualFile> ymlFiles = FilenameIndex.getAllFilesByExt(project, "yml", GlobalSearchScope.projectScope(project));
            for (VirtualFile file : ymlFiles) {
                if (SpringBootFileDetector.isSpringBootYamlFile(file)) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null) {
                        yamlFiles.add(psiFile);
                        LOG.debug("Found Spring Boot YAML file: " + file.getPath());
                    }
                }
            }

            // 查找所有.yaml文件
            Collection<VirtualFile> yamlFileExt = FilenameIndex.getAllFilesByExt(project, "yaml", GlobalSearchScope.projectScope(project));
            for (VirtualFile file : yamlFileExt) {
                if (SpringBootFileDetector.isSpringBootYamlFile(file)) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null) {
                        yamlFiles.add(psiFile);
                        LOG.debug("Found Spring Boot YAML file: " + file.getPath());
                    }
                }
            }

            LOG.info("Found " + yamlFiles.size() + " Spring Boot YAML files in project");
            return yamlFiles;
        });
    }

    /**
     * 获取指定YAML文件的配置内容
     *
     * @param file YAML文件
     * @return 配置内容文本
     */
    @Nullable
    public static String getYamlContent(@NotNull PsiFile file) {
        return file.getText();
    }

    /**
     * 判断当前文件是否为Spring Boot配置文件
     *
     * @param file 文件
     * @return 是否为Spring Boot配置文件
     */
    public static boolean isSpringBootConfigFile(@NotNull PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null && SpringBootFileDetector.isSpringBootConfigFile(virtualFile);
    }
}
