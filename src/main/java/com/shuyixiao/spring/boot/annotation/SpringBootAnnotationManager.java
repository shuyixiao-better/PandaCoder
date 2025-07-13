package com.shuyixiao.spring.boot.annotation;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Boot注解管理器
 * 管理常见的Spring Boot注解，提供注解信息和智能补全支持
 */
@Service(Service.Level.PROJECT)
public class SpringBootAnnotationManager {

    private final Map<String, SpringBootAnnotation> annotations = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> annotationGroups = new ConcurrentHashMap<>();

    public SpringBootAnnotationManager(@NotNull Project project) {
        initializeCommonAnnotations();
    }

    /**
     * 获取注解信息
     */
    @Nullable
    public SpringBootAnnotation getAnnotation(@NotNull String name) {
        return annotations.get(name);
    }

    /**
     * 获取所有注解
     */
    @NotNull
    public Collection<SpringBootAnnotation> getAllAnnotations() {
        return annotations.values();
    }

    /**
     * 根据类型获取注解
     */
    @NotNull
    public List<SpringBootAnnotation> getAnnotationsByType(@NotNull AnnotationType type) {
        List<SpringBootAnnotation> result = new ArrayList<>();
        for (SpringBootAnnotation annotation : annotations.values()) {
            if (annotation.getType() == type) {
                result.add(annotation);
            }
        }
        return result;
    }

    /**
     * 搜索注解
     */
    @NotNull
    public List<SpringBootAnnotation> searchAnnotations(@NotNull String query) {
        List<SpringBootAnnotation> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (SpringBootAnnotation annotation : annotations.values()) {
            if (annotation.getName().toLowerCase().contains(lowerQuery) ||
                annotation.getDescription().toLowerCase().contains(lowerQuery)) {
                result.add(annotation);
            }
        }
        
        return result;
    }

    /**
     * 获取注解组
     */
    @NotNull
    public Set<String> getAnnotationGroup(@NotNull String groupName) {
        return annotationGroups.getOrDefault(groupName, Collections.emptySet());
    }

    /**
     * 初始化常见的Spring Boot注解
     */
    private void initializeCommonAnnotations() {
        // 核心注解
        addAnnotation("SpringBootApplication", "org.springframework.boot.autoconfigure.SpringBootApplication",
                     AnnotationType.CONFIGURATION, "Spring Boot应用程序的主配置类注解", 
                     Arrays.asList("exclude", "excludeName", "scanBasePackages", "scanBasePackageClasses"));
        
        addAnnotation("EnableAutoConfiguration", "org.springframework.boot.autoconfigure.EnableAutoConfiguration",
                     AnnotationType.CONFIGURATION, "启用Spring Boot自动配置", 
                     Arrays.asList("exclude", "excludeName"));
        
        addAnnotation("ComponentScan", "org.springframework.context.annotation.ComponentScan",
                     AnnotationType.CONFIGURATION, "组件扫描注解", 
                     Arrays.asList("value", "basePackages", "basePackageClasses"));
        
        addAnnotation("Configuration", "org.springframework.context.annotation.Configuration",
                     AnnotationType.CONFIGURATION, "配置类注解", 
                     Arrays.asList("value", "proxyBeanMethods"));
        
        // Web注解
        addAnnotation("RestController", "org.springframework.web.bind.annotation.RestController",
                     AnnotationType.CONTROLLER, "REST控制器注解", 
                     Arrays.asList("value"));
        
        addAnnotation("Controller", "org.springframework.stereotype.Controller",
                     AnnotationType.CONTROLLER, "控制器注解", 
                     Arrays.asList("value"));
        
        addAnnotation("RequestMapping", "org.springframework.web.bind.annotation.RequestMapping",
                     AnnotationType.MAPPING, "请求映射注解", 
                     Arrays.asList("value", "path", "method", "params", "headers", "consumes", "produces"));
        
        addAnnotation("GetMapping", "org.springframework.web.bind.annotation.GetMapping",
                     AnnotationType.MAPPING, "GET请求映射注解", 
                     Arrays.asList("value", "path", "params", "headers", "consumes", "produces"));
        
        addAnnotation("PostMapping", "org.springframework.web.bind.annotation.PostMapping",
                     AnnotationType.MAPPING, "POST请求映射注解", 
                     Arrays.asList("value", "path", "params", "headers", "consumes", "produces"));
        
        addAnnotation("PutMapping", "org.springframework.web.bind.annotation.PutMapping",
                     AnnotationType.MAPPING, "PUT请求映射注解", 
                     Arrays.asList("value", "path", "params", "headers", "consumes", "produces"));
        
        addAnnotation("DeleteMapping", "org.springframework.web.bind.annotation.DeleteMapping",
                     AnnotationType.MAPPING, "DELETE请求映射注解", 
                     Arrays.asList("value", "path", "params", "headers", "consumes", "produces"));
        
        addAnnotation("PatchMapping", "org.springframework.web.bind.annotation.PatchMapping",
                     AnnotationType.MAPPING, "PATCH请求映射注解", 
                     Arrays.asList("value", "path", "params", "headers", "consumes", "produces"));
        
        // 参数注解
        addAnnotation("RequestParam", "org.springframework.web.bind.annotation.RequestParam",
                     AnnotationType.PARAMETER, "请求参数注解", 
                     Arrays.asList("value", "name", "required", "defaultValue"));
        
        addAnnotation("PathVariable", "org.springframework.web.bind.annotation.PathVariable",
                     AnnotationType.PARAMETER, "路径变量注解", 
                     Arrays.asList("value", "name", "required"));
        
        addAnnotation("RequestBody", "org.springframework.web.bind.annotation.RequestBody",
                     AnnotationType.PARAMETER, "请求体注解", 
                     Arrays.asList("required"));
        
        addAnnotation("RequestHeader", "org.springframework.web.bind.annotation.RequestHeader",
                     AnnotationType.PARAMETER, "请求头注解", 
                     Arrays.asList("value", "name", "required", "defaultValue"));
        
        // 依赖注入注解
        addAnnotation("Autowired", "org.springframework.beans.factory.annotation.Autowired",
                     AnnotationType.INJECTION, "自动装配注解", 
                     Arrays.asList("required"));
        
        addAnnotation("Qualifier", "org.springframework.beans.factory.annotation.Qualifier",
                     AnnotationType.INJECTION, "限定符注解", 
                     Arrays.asList("value"));
        
        addAnnotation("Value", "org.springframework.beans.factory.annotation.Value",
                     AnnotationType.INJECTION, "值注入注解", 
                     Arrays.asList("value"));
        
        // 数据访问注解
        addAnnotation("Repository", "org.springframework.stereotype.Repository",
                     AnnotationType.STEREOTYPE, "数据访问层注解", 
                     Arrays.asList("value"));
        
        addAnnotation("Service", "org.springframework.stereotype.Service",
                     AnnotationType.STEREOTYPE, "服务层注解", 
                     Arrays.asList("value"));
        
        addAnnotation("Component", "org.springframework.stereotype.Component",
                     AnnotationType.STEREOTYPE, "组件注解", 
                     Arrays.asList("value"));
        
        addAnnotation("Entity", "javax.persistence.Entity",
                     AnnotationType.JPA, "JPA实体注解", 
                     Arrays.asList("name"));
        
        addAnnotation("Table", "javax.persistence.Table",
                     AnnotationType.JPA, "JPA表注解", 
                     Arrays.asList("name", "catalog", "schema", "uniqueConstraints", "indexes"));
        
        addAnnotation("Id", "javax.persistence.Id",
                     AnnotationType.JPA, "JPA主键注解", 
                     Collections.emptyList());
        
        addAnnotation("GeneratedValue", "javax.persistence.GeneratedValue",
                     AnnotationType.JPA, "JPA主键生成策略注解", 
                     Arrays.asList("strategy", "generator"));
        
        addAnnotation("Column", "javax.persistence.Column",
                     AnnotationType.JPA, "JPA列注解", 
                     Arrays.asList("name", "unique", "nullable", "insertable", "updatable", "columnDefinition", "table", "length", "precision", "scale"));
        
        // 缓存注解
        addAnnotation("Cacheable", "org.springframework.cache.annotation.Cacheable",
                     AnnotationType.CACHE, "缓存注解", 
                     Arrays.asList("value", "cacheNames", "key", "keyGenerator", "cacheManager", "cacheResolver", "condition", "unless", "sync"));
        
        addAnnotation("CacheEvict", "org.springframework.cache.annotation.CacheEvict",
                     AnnotationType.CACHE, "缓存清除注解", 
                     Arrays.asList("value", "cacheNames", "key", "keyGenerator", "cacheManager", "cacheResolver", "condition", "allEntries", "beforeInvocation"));
        
        addAnnotation("CachePut", "org.springframework.cache.annotation.CachePut",
                     AnnotationType.CACHE, "缓存更新注解", 
                     Arrays.asList("value", "cacheNames", "key", "keyGenerator", "cacheManager", "cacheResolver", "condition", "unless"));
        
        addAnnotation("EnableCaching", "org.springframework.cache.annotation.EnableCaching",
                     AnnotationType.CONFIGURATION, "启用缓存注解", 
                     Arrays.asList("mode", "proxyTargetClass", "order"));
        
        // 事务注解
        addAnnotation("Transactional", "org.springframework.transaction.annotation.Transactional",
                     AnnotationType.TRANSACTION, "事务注解", 
                     Arrays.asList("value", "transactionManager", "propagation", "isolation", "timeout", "readOnly", "rollbackFor", "rollbackForClassName", "noRollbackFor", "noRollbackForClassName"));
        
        addAnnotation("EnableTransactionManagement", "org.springframework.transaction.annotation.EnableTransactionManagement",
                     AnnotationType.CONFIGURATION, "启用事务管理注解", 
                     Arrays.asList("mode", "proxyTargetClass", "order"));
        
        // 测试注解
        addAnnotation("SpringBootTest", "org.springframework.boot.test.context.SpringBootTest",
                     AnnotationType.TEST, "Spring Boot测试注解", 
                     Arrays.asList("value", "properties", "args", "classes", "webEnvironment"));
        
        addAnnotation("Test", "org.junit.jupiter.api.Test",
                     AnnotationType.TEST, "JUnit测试注解", 
                     Collections.emptyList());
        
        addAnnotation("MockBean", "org.springframework.boot.test.mock.mockito.MockBean",
                     AnnotationType.TEST, "Mock Bean注解", 
                     Arrays.asList("value", "name", "classes", "extraInterfaces", "answer", "serializable", "reset"));
        
        // 创建注解组
        createAnnotationGroups();
    }

    private void addAnnotation(String name, String fullyQualifiedName, AnnotationType type, String description, 
                             List<String> attributes) {
        SpringBootAnnotation annotation = new SpringBootAnnotation(name, fullyQualifiedName, type, description, attributes);
        annotations.put(name, annotation);
    }

    private void createAnnotationGroups() {
        annotationGroups.put("web", getAnnotationNamesByType(AnnotationType.CONTROLLER, AnnotationType.MAPPING));
        annotationGroups.put("data", getAnnotationNamesByType(AnnotationType.JPA, AnnotationType.REPOSITORY));
        annotationGroups.put("core", getAnnotationNamesByType(AnnotationType.CONFIGURATION, AnnotationType.STEREOTYPE));
        annotationGroups.put("test", getAnnotationNamesByType(AnnotationType.TEST));
        annotationGroups.put("cache", getAnnotationNamesByType(AnnotationType.CACHE));
        annotationGroups.put("transaction", getAnnotationNamesByType(AnnotationType.TRANSACTION));
    }

    private Set<String> getAnnotationNamesByType(AnnotationType... types) {
        Set<String> names = new HashSet<>();
        Set<AnnotationType> typeSet = new HashSet<>(Arrays.asList(types));
        
        for (SpringBootAnnotation annotation : annotations.values()) {
            if (typeSet.contains(annotation.getType())) {
                names.add(annotation.getName());
            }
        }
        
        return names;
    }

    /**
     * 注解类型枚举
     */
    public enum AnnotationType {
        CONFIGURATION,
        CONTROLLER,
        MAPPING,
        PARAMETER,
        INJECTION,
        STEREOTYPE,
        JPA,
        CACHE,
        TRANSACTION,
        TEST,
        REPOSITORY
    }

    /**
     * Spring Boot注解类
     */
    public static class SpringBootAnnotation {
        private final String name;
        private final String fullyQualifiedName;
        private final AnnotationType type;
        private final String description;
        private final List<String> attributes;

        public SpringBootAnnotation(String name, String fullyQualifiedName, AnnotationType type, 
                                   String description, List<String> attributes) {
            this.name = name;
            this.fullyQualifiedName = fullyQualifiedName;
            this.type = type;
            this.description = description;
            this.attributes = new ArrayList<>(attributes);
        }

        public String getName() { return name; }
        public String getFullyQualifiedName() { return fullyQualifiedName; }
        public AnnotationType getType() { return type; }
        public String getDescription() { return description; }
        public List<String> getAttributes() { return attributes; }
        
        public boolean hasAttributes() {
            return !attributes.isEmpty();
        }
    }

    /**
     * 获取项目实例
     */
    public static SpringBootAnnotationManager getInstance(@NotNull Project project) {
        return project.getService(SpringBootAnnotationManager.class);
    }
} 