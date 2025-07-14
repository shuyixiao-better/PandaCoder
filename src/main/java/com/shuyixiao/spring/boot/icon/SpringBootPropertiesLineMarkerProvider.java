package com.shuyixiao.spring.boot.icon;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Spring Boot Properties配置文件行标记提供器
 * 智能识别配置内容并显示相应的技术栈图标
 */
public class SpringBootPropertiesLineMarkerProvider implements LineMarkerProvider {

    private static final Logger LOG = Logger.getInstance(SpringBootPropertiesLineMarkerProvider.class);
    
    // 技术栈图标配置
    private static final Map<String, TechStackConfig> TECH_STACK_CONFIGS = new HashMap<>();
    
    static {
        initializeTechStackConfigs();
    }
    
    /**
     * 技术栈配置类
     */
    private static class TechStackConfig {
        private final String iconPath;
        private final String displayName;
        private final List<Pattern> configPatterns;
        private final List<Pattern> urlPatterns;
        private final Icon icon;
        
        public TechStackConfig(String iconPath, String displayName, List<String> configKeywords, List<String> urlKeywords) {
            this.iconPath = iconPath;
            this.displayName = displayName;
            this.configPatterns = new ArrayList<>();
            this.urlPatterns = new ArrayList<>();
            this.icon = loadIcon(iconPath);
            
            // 创建配置项匹配模式 (Properties格式)
            for (String keyword : configKeywords) {
                // Properties格式：keyword= 或 spring.keyword=
                configPatterns.add(Pattern.compile("^\\s*" + keyword + "\\s*=", Pattern.CASE_INSENSITIVE));
                configPatterns.add(Pattern.compile("^\\s*spring\\." + keyword + "(\\.|=)", Pattern.CASE_INSENSITIVE));
                configPatterns.add(Pattern.compile("^\\s*.*\\." + keyword + "(\\.|=)", Pattern.CASE_INSENSITIVE));
            }
            
            // 创建URL匹配模式
            for (String urlKeyword : urlKeywords) {
                urlPatterns.add(Pattern.compile(".*" + urlKeyword + ".*", Pattern.CASE_INSENSITIVE));
            }
        }
        
        public boolean matches(String text) {
            // 检查配置项匹配
            for (Pattern pattern : configPatterns) {
                if (pattern.matcher(text).find()) {
                    return true;
                }
            }
            
            // 检查URL匹配
            for (Pattern pattern : urlPatterns) {
                if (pattern.matcher(text).find()) {
                    return true;
                }
            }
            
            return false;
        }
        
        public Icon getIcon() { return icon; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * 初始化所有技术栈配置
     */
    private static void initializeTechStackConfigs() {
        // Spring Boot
        TECH_STACK_CONFIGS.put("springboot", new TechStackConfig(
            "/icons/springboot.svg",
            "Spring Boot",
            Arrays.asList("spring", "server", "management", "logging", "security"),
            Arrays.asList()
        ));
        
        // MySQL
        TECH_STACK_CONFIGS.put("mysql", new TechStackConfig(
            "/icons/mysql.svg", 
            "MySQL",
            Arrays.asList("mysql"),
            Arrays.asList("mysql", "jdbc:mysql")
        ));
        
        // PostgreSQL
        TECH_STACK_CONFIGS.put("postgresql", new TechStackConfig(
            "/icons/postgresql.svg",
            "PostgreSQL", 
            Arrays.asList("postgresql", "postgres"),
            Arrays.asList("postgresql", "postgres", "jdbc:postgresql")
        ));
        
        // Oracle
        TECH_STACK_CONFIGS.put("oracle", new TechStackConfig(
            "/icons/oracle.svg",
            "Oracle",
            Arrays.asList("oracle"),
            Arrays.asList("oracle", "jdbc:oracle")
        ));
        
        // SQL Server
        TECH_STACK_CONFIGS.put("sqlserver", new TechStackConfig(
            "/icons/sqlserver.svg",
            "SQL Server",
            Arrays.asList("sqlserver", "mssql"),
            Arrays.asList("sqlserver", "mssql", "jdbc:sqlserver")
        ));
        
        // Redis
        TECH_STACK_CONFIGS.put("redis", new TechStackConfig(
            "/icons/redis.svg",
            "Redis",
            Arrays.asList("redis", "cache"),
            Arrays.asList("redis://")
        ));
        
        // Elasticsearch
        TECH_STACK_CONFIGS.put("elasticsearch", new TechStackConfig(
            "/icons/elasticsearch.svg",
            "Elasticsearch",
            Arrays.asList("elasticsearch", "elastic"),
            Arrays.asList("elasticsearch", "http://.*:9200", "https://.*:9200")
        ));
        
        // Apache Kafka
        TECH_STACK_CONFIGS.put("kafka", new TechStackConfig(
            "/icons/kafka.svg",
            "Apache Kafka",
            Arrays.asList("kafka"),
            Arrays.asList("kafka://", ":9092")
        ));
        
        // RabbitMQ
        TECH_STACK_CONFIGS.put("rabbitmq", new TechStackConfig(
            "/icons/rabbitmq.svg",
            "RabbitMQ",
            Arrays.asList("rabbitmq", "amqp"),
            Arrays.asList("amqp://", ":5672")
        ));
        
        // 数据库通用配置
        TECH_STACK_CONFIGS.put("database", new TechStackConfig(
            "/icons/mysql.svg", // 使用MySQL图标作为通用数据库图标
            "数据库",
            Arrays.asList("datasource", "jpa", "hibernate", "hikari", "druid", "dbcp"),
            Arrays.asList("jdbc:")
        ));
    }
    
    /**
     * 安全加载图标
     */
    private static Icon loadIcon(String path) {
        try {
            Icon icon = IconLoader.getIcon(path, SpringBootPropertiesLineMarkerProvider.class);
            LOG.debug("Successfully loaded icon: " + path);
            return icon;
        } catch (Exception e) {
            LOG.warn("Failed to load icon: " + path, e);
            return null;
        }
    }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // 检查是否为Spring Boot配置文件
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null || !SpringBootFileDetector.isSpringBootConfigFile(containingFile)) {
            return null;
        }
        
        // 检查是否为Properties文件
        String fileName = containingFile.getName();
        if (!fileName.endsWith(".properties")) {
            return null;
        }
        
        // 获取元素文本
        String elementText = element.getText();
        if (elementText == null || elementText.trim().isEmpty()) {
            return null;
        }
        
        // 查找匹配的技术栈图标（优先级匹配）
        TechStackConfig matchedConfig = findBestMatch(elementText);
        
        if (matchedConfig != null && matchedConfig.getIcon() != null) {
            LOG.debug("Found matching icon for text: " + elementText + " -> " + matchedConfig.getDisplayName());
            return createLineMarkerInfo(element, matchedConfig.getIcon(), matchedConfig.getDisplayName());
        }
        
        return null;
    }
    
    /**
     * 查找最佳匹配的技术栈配置
     * 优先级：特定技术栈 > 通用配置
     */
    private TechStackConfig findBestMatch(String text) {
        List<TechStackConfig> matches = new ArrayList<>();
        
        // 收集所有匹配项
        for (TechStackConfig config : TECH_STACK_CONFIGS.values()) {
            if (config.matches(text)) {
                matches.add(config);
            }
        }
        
        if (matches.isEmpty()) {
            return null;
        }
        
        // 优先级排序：特定技术栈优先于通用配置
        matches.sort((a, b) -> {
            // database 配置优先级最低
            if ("数据库".equals(a.getDisplayName()) && !"数据库".equals(b.getDisplayName())) {
                return 1;
            }
            if (!"数据库".equals(a.getDisplayName()) && "数据库".equals(b.getDisplayName())) {
                return -1;
            }
            
            // Spring Boot 配置优先级较低
            if ("Spring Boot".equals(a.getDisplayName()) && !"Spring Boot".equals(b.getDisplayName())) {
                return 1;
            }
            if (!"Spring Boot".equals(a.getDisplayName()) && "Spring Boot".equals(b.getDisplayName())) {
                return -1;
            }
            
            return 0;
        });
        
        return matches.get(0);
    }
    
    /**
     * 创建行标记信息
     */
    private LineMarkerInfo<PsiElement> createLineMarkerInfo(@NotNull PsiElement element, 
                                                           @NotNull Icon icon, 
                                                           @NotNull String techStackName) {
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            icon,
            (e) -> "技术栈: " + techStackName,
            null,
            GutterIconRenderer.Alignment.LEFT,
            () -> "技术栈标识: " + techStackName
        );
    }
} 