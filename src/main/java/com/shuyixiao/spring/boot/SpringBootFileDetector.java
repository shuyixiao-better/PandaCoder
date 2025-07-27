package com.shuyixiao.spring.boot;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Boot文件检测器
 * 用于识别Spring Boot相关的配置文件和项目结构
 */
public class SpringBootFileDetector {

    // Spring Boot配置文件名模式
    private static final List<String> CONFIG_FILE_PATTERNS = Arrays.asList(
        "application.properties",
        "application.yml",
        "application.yaml",
        "bootstrap.properties",
        "bootstrap.yml",
        "bootstrap.yaml"
    );

    // Spring Boot配置文件前缀模式
    private static final List<String> CONFIG_FILE_PREFIX_PATTERNS = Arrays.asList(
        "application-",
        "bootstrap-"
    );

    // Spring Boot配置文件后缀
    private static final List<String> CONFIG_FILE_EXTENSIONS = Arrays.asList(
        ".properties",
        ".yml",
        ".yaml"
    );

    /**
     * 检测是否为Spring Boot项目
     */
    public static boolean isSpringBootProject(@NotNull Project project) {
        // 查找Spring Boot相关的配置文件
        VirtualFile[] contentRoots = getAllContentRoots(project);
        
        for (VirtualFile contentRoot : contentRoots) {
            if (hasSpringBootConfigFiles(contentRoot)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 检测是否为Spring Boot配置文件
     */
    public static boolean isSpringBootConfigFile(@NotNull VirtualFile file) {
        String fileName = file.getName();
        
        // 检查精确匹配
        if (CONFIG_FILE_PATTERNS.contains(fileName)) {
            return true;
        }
        
        // 检查前缀匹配
        for (String prefix : CONFIG_FILE_PREFIX_PATTERNS) {
            if (fileName.startsWith(prefix)) {
                for (String extension : CONFIG_FILE_EXTENSIONS) {
                    if (fileName.endsWith(extension)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * 检测是否为Spring Boot配置文件
     */
    public static boolean isSpringBootConfigFile(@NotNull PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null && isSpringBootConfigFile(virtualFile);
    }

    /**
     * 检测是否为Properties格式的Spring Boot配置文件
     */
    public static boolean isSpringBootPropertiesFile(@NotNull VirtualFile file) {
        return isSpringBootConfigFile(file) && file.getName().endsWith(".properties");
    }

    /**
     * 检测是否为YAML格式的Spring Boot配置文件
     */
    public static boolean isSpringBootYamlFile(@NotNull VirtualFile file) {
        return isSpringBootConfigFile(file) && 
               (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"));
    }

    /**
     * 检测文件是否在Spring Boot资源目录中
     */
    public static boolean isInSpringBootResourcesDirectory(@NotNull VirtualFile file, @NotNull Project project) {
        // 使用ReadAction包装模块和根目录访问操作
        return ReadAction.compute(() -> {
            Module module = ModuleUtilCore.findModuleForFile(file, project);
            if (module == null) {
                return false;
            }

            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            VirtualFile[] sourceRoots = rootManager.getSourceRoots();
            
            for (VirtualFile sourceRoot : sourceRoots) {
                // 检查是否在resources目录中
                if (isResourcesDirectory(sourceRoot) && VfsUtil.isAncestor(sourceRoot, file, false)) {
                    return true;
                }
            }
            
            return false;
        });
    }

    /**
     * 获取Spring Boot配置文件的环境名称
     * 例如：application-dev.properties -> "dev"
     */
    @Nullable
    public static String getConfigEnvironment(@NotNull VirtualFile file) {
        String fileName = file.getName();
        
        for (String prefix : CONFIG_FILE_PREFIX_PATTERNS) {
            if (fileName.startsWith(prefix)) {
                for (String extension : CONFIG_FILE_EXTENSIONS) {
                    if (fileName.endsWith(extension)) {
                        String middle = fileName.substring(prefix.length(), fileName.length() - extension.length());
                        return middle.isEmpty() ? null : middle;
                    }
                }
            }
        }
        
        return null;
    }

    // 私有辅助方法

    private static VirtualFile[] getAllContentRoots(@NotNull Project project) {
        return VfsUtil.getCommonAncestors(
            Arrays.stream(ModuleManager.getInstance(project).getModules())
                .flatMap(module -> Arrays.stream(ModuleRootManager.getInstance(module).getContentRoots()))
                .toArray(VirtualFile[]::new)
        );
    }

    private static boolean hasSpringBootConfigFiles(@NotNull VirtualFile contentRoot) {
        VirtualFile resourcesDir = contentRoot.findChild("src");
        if (resourcesDir != null) {
            resourcesDir = resourcesDir.findChild("main");
            if (resourcesDir != null) {
                resourcesDir = resourcesDir.findChild("resources");
                if (resourcesDir != null) {
                    for (String configFile : CONFIG_FILE_PATTERNS) {
                        if (resourcesDir.findChild(configFile) != null) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isResourcesDirectory(@NotNull VirtualFile directory) {
        return directory.isDirectory() && "resources".equals(directory.getName());
    }
} 