package com.shuyixiao.bugrecorder.enhanced;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.shuyixiao.bugrecorder.model.BugRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 增强上下文捕获服务
 * 捕获更丰富的错误上下文信息，包括系统环境、项目状态、运行配置等
 */
public class EnhancedContextCaptureService {

    private static final Logger LOG = Logger.getInstance(EnhancedContextCaptureService.class);
    
    private final Project project;
    private final ScheduledExecutorService contextCollector;
    private final Map<String, Object> systemContext = new ConcurrentHashMap<>();
    private final Map<String, Object> projectContext = new ConcurrentHashMap<>();
    private final Map<String, Object> runtimeContext = new ConcurrentHashMap<>();
    
    // 上下文收集间隔（秒）
    private static final int CONTEXT_COLLECTION_INTERVAL = 30;
    
    public EnhancedContextCaptureService(@NotNull Project project) {
        this.project = project;
        this.contextCollector = Executors.newSingleThreadScheduledExecutor();
        
        // 启动定期上下文收集
        startContextCollection();
        
        // 立即收集一次上下文
        collectAllContext();
    }
    
    /**
     * 启动定期上下文收集
     */
    private void startContextCollection() {
        contextCollector.scheduleWithFixedDelay(
            this::collectAllContext,
            CONTEXT_COLLECTION_INTERVAL,
            CONTEXT_COLLECTION_INTERVAL,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * 收集所有上下文信息
     */
    private void collectAllContext() {
        try {
            collectSystemContext();
            collectProjectContext();
            collectRuntimeContext();
            LOG.debug("Context collection completed for project: " + project.getName());
        } catch (Exception e) {
            LOG.warn("Failed to collect context information", e);
        }
    }
    
    /**
     * 收集系统环境上下文
     */
    private void collectSystemContext() {
        try {
            // 操作系统信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            systemContext.put("os.name", SystemInfo.OS_NAME);
            systemContext.put("os.version", SystemInfo.OS_VERSION);
            systemContext.put("os.arch", SystemInfo.OS_ARCH);
            systemContext.put("os.family", SystemInfo.isWindows ? "Windows" : 
                SystemInfo.isMac ? "macOS" : SystemInfo.isLinux ? "Linux" : "Unknown");
            systemContext.put("os.processors", osBean.getAvailableProcessors());
            systemContext.put("os.load.average", osBean.getSystemLoadAverage());
            
            // Java运行时信息
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            systemContext.put("java.version", System.getProperty("java.version"));
            systemContext.put("java.vendor", System.getProperty("java.vendor"));
            systemContext.put("java.home", System.getProperty("java.home"));
            systemContext.put("java.class.path", System.getProperty("java.class.path"));
            systemContext.put("java.library.path", System.getProperty("java.library.path"));
            systemContext.put("java.start.time", runtimeBean.getStartTime());
            systemContext.put("java.uptime", runtimeBean.getUptime());
            
            // 内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            systemContext.put("memory.heap.used", memoryBean.getHeapMemoryUsage().getUsed());
            systemContext.put("memory.heap.max", memoryBean.getHeapMemoryUsage().getMax());
            systemContext.put("memory.nonheap.used", memoryBean.getNonHeapMemoryUsage().getUsed());
            systemContext.put("memory.nonheap.max", memoryBean.getNonHeapMemoryUsage().getMax());
            
            // 线程信息
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            systemContext.put("thread.count", threadBean.getThreadCount());
            systemContext.put("thread.daemon.count", threadBean.getDaemonThreadCount());
            systemContext.put("thread.peak.count", threadBean.getPeakThreadCount());
            
            // 用户信息
            systemContext.put("user.name", System.getProperty("user.name"));
            systemContext.put("user.home", System.getProperty("user.home"));
            systemContext.put("user.dir", System.getProperty("user.dir"));
            systemContext.put("user.timezone", System.getProperty("user.timezone"));
            
            // 文件编码
            systemContext.put("file.encoding", System.getProperty("file.encoding"));
            systemContext.put("sun.jnu.encoding", System.getProperty("sun.jnu.encoding"));
            
        } catch (Exception e) {
            LOG.warn("Failed to collect system context", e);
        }
    }
    
    /**
     * 收集项目上下文信息
     */
    private void collectProjectContext() {
        try {
            // 项目基本信息
            projectContext.put("project.name", project.getName());
            projectContext.put("project.base.path", project.getBasePath());
            projectContext.put("project.workspace.file", project.getWorkspaceFile() != null ? 
                project.getWorkspaceFile().getPath() : null);
            projectContext.put("project.is.open", project.isOpen());
            projectContext.put("project.is.initialized", project.isInitialized());
            
            // 模块信息
            Module[] modules = ModuleManager.getInstance(project).getModules();
            List<Map<String, Object>> moduleInfo = new ArrayList<>();
            for (Module module : modules) {
                Map<String, Object> moduleData = new HashMap<>();
                moduleData.put("name", module.getName());
                moduleData.put("file.path", module.getModuleFilePath());
                
                // 模块依赖
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                List<String> dependencies = new ArrayList<>();
                for (Module dependency : rootManager.getModuleDependencies()) {
                    dependencies.add(dependency.getName());
                }
                moduleData.put("dependencies", dependencies);
                
                moduleInfo.add(moduleData);
            }
            projectContext.put("modules", moduleInfo);
            
            // 项目根目录
            VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
            List<String> contentRootPaths = new ArrayList<>();
            for (VirtualFile root : contentRoots) {
                contentRootPaths.add(root.getPath());
            }
            projectContext.put("content.roots", contentRootPaths);
            
            // 项目文件统计
            collectProjectFileStats();
            
        } catch (Exception e) {
            LOG.warn("Failed to collect project context", e);
        }
    }
    
    /**
     * 收集项目文件统计信息
     */
    private void collectProjectFileStats() {
        try {
            String basePath = project.getBasePath();
            if (basePath == null) return;
            
            Path projectPath = Paths.get(basePath);
            if (!Files.exists(projectPath)) return;
            
            // 统计文件数量和大小
            long fileCount = Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .count();
            
            long totalSize = Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .sum();
            
            projectContext.put("file.count", fileCount);
            projectContext.put("total.size.bytes", totalSize);
            projectContext.put("total.size.mb", totalSize / (1024 * 1024));
            
            // 统计不同文件类型
            Map<String, Long> fileTypeStats = new HashMap<>();
            Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String extension = getFileExtension(path.toString());
                    fileTypeStats.merge(extension, 1L, Long::sum);
                });
            projectContext.put("file.type.stats", fileTypeStats);
            
        } catch (Exception e) {
            LOG.warn("Failed to collect project file stats", e);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "no-extension";
    }
    
    /**
     * 收集运行时上下文信息
     */
    private void collectRuntimeContext() {
        try {
            // 当前时间
            runtimeContext.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // IDE信息
            runtimeContext.put("ide.name", "IntelliJ IDEA");
            runtimeContext.put("ide.version", System.getProperty("idea.version"));
            runtimeContext.put("ide.build", System.getProperty("idea.build.number"));
            
            // 插件信息
            collectPluginInfo();
            
            // 运行配置信息
            collectRunConfigurationInfo();
            
            // 最近修改的文件
            collectRecentlyModifiedFiles();
            
        } catch (Exception e) {
            LOG.warn("Failed to collect runtime context", e);
        }
    }
    
    /**
     * 收集插件信息
     */
    private void collectPluginInfo() {
        try {
            // 这里可以添加插件相关的信息收集
            // 由于插件API的限制，可能需要通过其他方式获取
            runtimeContext.put("plugin.name", "PandaCoder");
            runtimeContext.put("plugin.version", com.shuyixiao.version.VersionInfo.getVersion());
            
        } catch (Exception e) {
            LOG.warn("Failed to collect plugin info", e);
        }
    }
    
    /**
     * 收集运行配置信息
     */
    private void collectRunConfigurationInfo() {
        try {
            // 这里可以添加运行配置相关的信息收集
            // 需要访问RunManager API
            runtimeContext.put("run.configuration", "Not available");
            
        } catch (Exception e) {
            LOG.warn("Failed to collect run configuration info", e);
        }
    }
    
    /**
     * 收集最近修改的文件
     */
    private void collectRecentlyModifiedFiles() {
        try {
            // 这里可以添加最近修改文件的信息收集
            // 需要访问VFS API
            runtimeContext.put("recently.modified.files", "Not available");
            
        } catch (Exception e) {
            LOG.warn("Failed to collect recently modified files info", e);
        }
    }
    
    /**
     * 获取完整的上下文信息
     */
    public Map<String, Object> getFullContext() {
        Map<String, Object> fullContext = new HashMap<>();
        fullContext.put("system", new HashMap<>(systemContext));
        fullContext.put("project", new HashMap<>(projectContext));
        fullContext.put("runtime", new HashMap<>(runtimeContext));
        return fullContext;
    }
    
    /**
     * 获取系统上下文
     */
    public Map<String, Object> getSystemContext() {
        return new HashMap<>(systemContext);
    }
    
    /**
     * 获取项目上下文
     */
    public Map<String, Object> getProjectContext() {
        return new HashMap<>(projectContext);
    }
    
    /**
     * 获取运行时上下文
     */
    public Map<String, Object> getRuntimeContext() {
        return new HashMap<>(runtimeContext);
    }
    
    /**
     * 获取特定类型的上下文信息
     */
    @Nullable
    public Object getContextValue(String contextType, String key) {
        switch (contextType.toLowerCase()) {
            case "system":
                return systemContext.get(key);
            case "project":
                return projectContext.get(key);
            case "runtime":
                return runtimeContext.get(key);
            default:
                return null;
        }
    }
    
    /**
     * 手动触发上下文收集
     */
    public void refreshContext() {
        collectAllContext();
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        if (contextCollector != null && !contextCollector.isShutdown()) {
            contextCollector.shutdown();
            try {
                if (!contextCollector.awaitTermination(5, TimeUnit.SECONDS)) {
                    contextCollector.shutdownNow();
                }
            } catch (InterruptedException e) {
                contextCollector.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
} 