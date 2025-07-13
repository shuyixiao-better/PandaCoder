package com.shuyixiao.spring.boot.config;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Boot配置属性管理器
 * 管理常见的Spring Boot配置属性，提供智能补全和验证
 */
@Service(Service.Level.PROJECT)
public class SpringBootConfigPropertyManager {

    private final Map<String, ConfigProperty> properties = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> propertyGroups = new ConcurrentHashMap<>();

    public SpringBootConfigPropertyManager(@NotNull Project project) {
        initializeCommonProperties();
    }

    /**
     * 获取配置属性
     */
    @Nullable
    public ConfigProperty getProperty(@NotNull String key) {
        return properties.get(key);
    }

    /**
     * 获取所有配置属性
     */
    @NotNull
    public Collection<ConfigProperty> getAllProperties() {
        return properties.values();
    }

    /**
     * 根据前缀获取配置属性
     */
    @NotNull
    public List<ConfigProperty> getPropertiesByPrefix(@NotNull String prefix) {
        List<ConfigProperty> result = new ArrayList<>();
        for (ConfigProperty property : properties.values()) {
            if (property.getKey().startsWith(prefix)) {
                result.add(property);
            }
        }
        return result;
    }

    /**
     * 获取属性组
     */
    @NotNull
    public Set<String> getPropertyGroup(@NotNull String groupName) {
        return propertyGroups.getOrDefault(groupName, Collections.emptySet());
    }

    /**
     * 搜索配置属性
     */
    @NotNull
    public List<ConfigProperty> searchProperties(@NotNull String query) {
        List<ConfigProperty> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (ConfigProperty property : properties.values()) {
            if (property.getKey().toLowerCase().contains(lowerQuery) ||
                property.getDescription().toLowerCase().contains(lowerQuery)) {
                result.add(property);
            }
        }
        
        return result;
    }

    /**
     * 检查属性是否已废弃
     */
    public boolean isDeprecated(@NotNull String key) {
        ConfigProperty property = properties.get(key);
        return property != null && property.isDeprecated();
    }

    /**
     * 获取属性的推荐替代
     */
    @Nullable
    public String getReplacementProperty(@NotNull String key) {
        ConfigProperty property = properties.get(key);
        return property != null ? property.getReplacementProperty() : null;
    }

    /**
     * 初始化常见的Spring Boot配置属性
     */
    private void initializeCommonProperties() {
        // 服务器配置
        addProperty("server.port", PropertyType.INTEGER, "8080", "服务器端口号");
        addProperty("server.servlet.context-path", PropertyType.STRING, "/", "应用上下文路径");
        addProperty("server.address", PropertyType.STRING, "", "服务器绑定地址");
        addProperty("server.connection-timeout", PropertyType.DURATION, "20000ms", "连接超时时间");
        addProperty("server.max-http-header-size", PropertyType.DATA_SIZE, "8KB", "HTTP头最大大小");
        
        // 数据库配置
        addProperty("spring.datasource.url", PropertyType.STRING, "", "数据库连接URL");
        addProperty("spring.datasource.username", PropertyType.STRING, "", "数据库用户名");
        addProperty("spring.datasource.password", PropertyType.STRING, "", "数据库密码");
        addProperty("spring.datasource.driver-class-name", PropertyType.STRING, "", "数据库驱动类名");
        addProperty("spring.datasource.hikari.connection-timeout", PropertyType.DURATION, "30000ms", "HikariCP连接超时时间");
        addProperty("spring.datasource.hikari.maximum-pool-size", PropertyType.INTEGER, "10", "HikariCP最大连接池大小");
        
        // JPA配置
        addProperty("spring.jpa.hibernate.ddl-auto", PropertyType.ENUM, "none", "Hibernate DDL模式", 
                   Arrays.asList("none", "create", "create-drop", "update", "validate"));
        addProperty("spring.jpa.show-sql", PropertyType.BOOLEAN, "false", "是否显示SQL语句");
        addProperty("spring.jpa.properties.hibernate.format_sql", PropertyType.BOOLEAN, "false", "是否格式化SQL语句");
        addProperty("spring.jpa.properties.hibernate.dialect", PropertyType.STRING, "", "Hibernate数据库方言");
        
        // 日志配置
        addProperty("logging.level.root", PropertyType.ENUM, "INFO", "根日志级别", 
                   Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF"));
        addProperty("logging.pattern.console", PropertyType.STRING, "", "控制台日志格式");
        addProperty("logging.pattern.file", PropertyType.STRING, "", "文件日志格式");
        addProperty("logging.file.name", PropertyType.STRING, "", "日志文件名");
        addProperty("logging.file.path", PropertyType.STRING, "", "日志文件路径");
        
        // 缓存配置
        addProperty("spring.cache.type", PropertyType.ENUM, "simple", "缓存类型", 
                   Arrays.asList("simple", "redis", "caffeine", "ehcache", "hazelcast", "infinispan", "jcache"));
        addProperty("spring.cache.cache-names", PropertyType.STRING_ARRAY, "", "缓存名称列表");
        
        // Redis配置
        addProperty("spring.redis.host", PropertyType.STRING, "localhost", "Redis服务器地址");
        addProperty("spring.redis.port", PropertyType.INTEGER, "6379", "Redis服务器端口");
        addProperty("spring.redis.password", PropertyType.STRING, "", "Redis服务器密码");
        addProperty("spring.redis.database", PropertyType.INTEGER, "0", "Redis数据库索引");
        addProperty("spring.redis.timeout", PropertyType.DURATION, "2000ms", "Redis连接超时时间");
        addProperty("spring.redis.lettuce.pool.max-active", PropertyType.INTEGER, "8", "Lettuce连接池最大活跃连接数");
        addProperty("spring.redis.lettuce.pool.max-idle", PropertyType.INTEGER, "8", "Lettuce连接池最大空闲连接数");
        
        // 安全配置
        addProperty("spring.security.user.name", PropertyType.STRING, "user", "默认用户名");
        addProperty("spring.security.user.password", PropertyType.STRING, "", "默认用户密码");
        addProperty("spring.security.user.roles", PropertyType.STRING_ARRAY, "", "默认用户角色");
        
        // 应用配置
        addProperty("spring.application.name", PropertyType.STRING, "", "应用名称");
        addProperty("spring.profiles.active", PropertyType.STRING_ARRAY, "", "激活的配置文件");
        addProperty("spring.profiles.include", PropertyType.STRING_ARRAY, "", "包含的配置文件");
        
        // 管理端点配置
        addProperty("management.endpoints.web.exposure.include", PropertyType.STRING_ARRAY, "health,info", "暴露的Web端点");
        addProperty("management.endpoint.health.show-details", PropertyType.ENUM, "never", "健康检查详情显示级别",
                   Arrays.asList("never", "when-authorized", "always"));
        addProperty("management.server.port", PropertyType.INTEGER, "", "管理端口");
        
        // 创建属性组
        createPropertyGroups();
    }

    private void addProperty(String key, PropertyType type, String defaultValue, String description) {
        addProperty(key, type, defaultValue, description, null);
    }

    private void addProperty(String key, PropertyType type, String defaultValue, String description, 
                           List<String> enumValues) {
        ConfigProperty property = new ConfigProperty(key, type, defaultValue, description, enumValues);
        properties.put(key, property);
    }

    private void createPropertyGroups() {
        propertyGroups.put("server", getPropertyKeysWithPrefix("server"));
        propertyGroups.put("spring.datasource", getPropertyKeysWithPrefix("spring.datasource"));
        propertyGroups.put("spring.jpa", getPropertyKeysWithPrefix("spring.jpa"));
        propertyGroups.put("logging", getPropertyKeysWithPrefix("logging"));
        propertyGroups.put("spring.cache", getPropertyKeysWithPrefix("spring.cache"));
        propertyGroups.put("spring.redis", getPropertyKeysWithPrefix("spring.redis"));
        propertyGroups.put("spring.security", getPropertyKeysWithPrefix("spring.security"));
        propertyGroups.put("management", getPropertyKeysWithPrefix("management"));
    }

    private Set<String> getPropertyKeysWithPrefix(String prefix) {
        Set<String> keys = new HashSet<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * 配置属性类型枚举
     */
    public enum PropertyType {
        STRING,
        INTEGER,
        LONG,
        BOOLEAN,
        DOUBLE,
        ENUM,
        DURATION,
        DATA_SIZE,
        STRING_ARRAY,
        CLASS_NAME
    }

    /**
     * 配置属性类
     */
    public static class ConfigProperty {
        private final String key;
        private final PropertyType type;
        private final String defaultValue;
        private final String description;
        private final List<String> enumValues;
        private final boolean deprecated;
        private final String replacementProperty;

        public ConfigProperty(String key, PropertyType type, String defaultValue, String description, 
                            List<String> enumValues) {
            this(key, type, defaultValue, description, enumValues, false, null);
        }

        public ConfigProperty(String key, PropertyType type, String defaultValue, String description, 
                            List<String> enumValues, boolean deprecated, String replacementProperty) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.description = description;
            this.enumValues = enumValues != null ? new ArrayList<>(enumValues) : Collections.emptyList();
            this.deprecated = deprecated;
            this.replacementProperty = replacementProperty;
        }

        public String getKey() { return key; }
        public PropertyType getType() { return type; }
        public String getDefaultValue() { return defaultValue; }
        public String getDescription() { return description; }
        public List<String> getEnumValues() { return enumValues; }
        public boolean isDeprecated() { return deprecated; }
        public String getReplacementProperty() { return replacementProperty; }

        public boolean hasEnumValues() {
            return !enumValues.isEmpty();
        }
    }

    /**
     * 获取项目实例
     */
    public static SpringBootConfigPropertyManager getInstance(@NotNull Project project) {
        return project.getService(SpringBootConfigPropertyManager.class);
    }
} 