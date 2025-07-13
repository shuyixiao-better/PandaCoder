package com.shuyixiao.spring.boot.analyzer;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 技术栈分析器
 * 分析Spring Boot配置文件内容，识别使用的技术栈
 */
public class TechStackAnalyzer {

    /**
     * 技术栈枚举
     */
    public enum TechStack {
        MYSQL("mysql", "MySQL数据库"),
        POSTGRESQL("postgresql", "PostgreSQL数据库"),
        REDIS("redis", "Redis缓存"),
        ELASTICSEARCH("elasticsearch", "Elasticsearch搜索引擎"),
        MONGODB("mongodb", "MongoDB数据库"),
        RABBITMQ("rabbitmq", "RabbitMQ消息队列"),
        KAFKA("kafka", "Apache Kafka"),
        ORACLE("oracle", "Oracle数据库"),
        SQLSERVER("sqlserver", "SQL Server数据库"),
        SPRING_BOOT("springboot", "Spring Boot框架");

        private final String iconName;
        private final String description;

        TechStack(String iconName, String description) {
            this.iconName = iconName;
            this.description = description;
        }

        public String getIconName() {
            return iconName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 技术栈检测规则
     */
    private static final Map<TechStack, List<Pattern>> TECH_PATTERNS = new HashMap<>();

    static {
        // MySQL检测规则
        TECH_PATTERNS.put(TechStack.MYSQL, Arrays.asList(
            Pattern.compile(".*mysql.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*MariaDB.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*com\\.mysql\\..*", Pattern.CASE_INSENSITIVE)
        ));

        // PostgreSQL检测规则
        TECH_PATTERNS.put(TechStack.POSTGRESQL, Arrays.asList(
            Pattern.compile(".*postgresql.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*postgres.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*org\\.postgresql\\..*", Pattern.CASE_INSENSITIVE)
        ));

        // Redis检测规则
        TECH_PATTERNS.put(TechStack.REDIS, Arrays.asList(
            Pattern.compile(".*redis.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*lettuce.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*jedis.*", Pattern.CASE_INSENSITIVE)
        ));

        // Elasticsearch检测规则
        TECH_PATTERNS.put(TechStack.ELASTICSEARCH, Arrays.asList(
            Pattern.compile(".*elasticsearch.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*elastic.*", Pattern.CASE_INSENSITIVE)
        ));

        // MongoDB检测规则
        TECH_PATTERNS.put(TechStack.MONGODB, Arrays.asList(
            Pattern.compile(".*mongodb.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*mongo.*", Pattern.CASE_INSENSITIVE)
        ));

        // RabbitMQ检测规则
        TECH_PATTERNS.put(TechStack.RABBITMQ, Arrays.asList(
            Pattern.compile(".*rabbitmq.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*amqp.*", Pattern.CASE_INSENSITIVE)
        ));

        // Kafka检测规则
        TECH_PATTERNS.put(TechStack.KAFKA, Arrays.asList(
            Pattern.compile(".*kafka.*", Pattern.CASE_INSENSITIVE)
        ));

        // Oracle检测规则
        TECH_PATTERNS.put(TechStack.ORACLE, Arrays.asList(
            Pattern.compile(".*oracle.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*ojdbc.*", Pattern.CASE_INSENSITIVE)
        ));

        // SQL Server检测规则
        TECH_PATTERNS.put(TechStack.SQLSERVER, Arrays.asList(
            Pattern.compile(".*sqlserver.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*mssql.*", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * 分析配置文件中使用的技术栈
     */
    public static Set<TechStack> analyzeTechStacks(@NotNull VirtualFile configFile) {
        Set<TechStack> detectedStacks = new HashSet<>();
        
        try {
            String content = new String(configFile.contentsToByteArray(), configFile.getCharset());
            
            // 检测每种技术栈
            for (Map.Entry<TechStack, List<Pattern>> entry : TECH_PATTERNS.entrySet()) {
                TechStack techStack = entry.getKey();
                List<Pattern> patterns = entry.getValue();
                
                for (Pattern pattern : patterns) {
                    if (pattern.matcher(content).find()) {
                        detectedStacks.add(techStack);
                        break; // 找到一个匹配就够了
                    }
                }
            }
            
            // 如果没有检测到特定技术栈，默认显示Spring Boot图标
            if (detectedStacks.isEmpty()) {
                detectedStacks.add(TechStack.SPRING_BOOT);
            }
            
        } catch (IOException e) {
            // 发生错误时默认显示Spring Boot图标
            detectedStacks.add(TechStack.SPRING_BOOT);
        }
        
        return detectedStacks;
    }

    /**
     * 分析PSI文件中使用的技术栈
     */
    public static Set<TechStack> analyzeTechStacks(@NotNull PsiFile psiFile) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null) {
            return analyzeTechStacks(virtualFile);
        }
        return Collections.singleton(TechStack.SPRING_BOOT);
    }

    /**
     * 获取主要技术栈（优先级最高的）
     */
    public static TechStack getPrimaryTechStack(@NotNull Set<TechStack> techStacks) {
        // 按优先级排序：数据库 > 缓存 > 消息队列 > 搜索引擎 > 框架
        List<TechStack> priorityOrder = Arrays.asList(
            TechStack.MYSQL, TechStack.POSTGRESQL, TechStack.ORACLE, TechStack.SQLSERVER,
            TechStack.MONGODB, TechStack.REDIS, TechStack.RABBITMQ, TechStack.KAFKA,
            TechStack.ELASTICSEARCH, TechStack.SPRING_BOOT
        );
        
        for (TechStack techStack : priorityOrder) {
            if (techStacks.contains(techStack)) {
                return techStack;
            }
        }
        
        return TechStack.SPRING_BOOT;
    }
} 