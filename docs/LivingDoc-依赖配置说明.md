# PandaCoder 活文档功能 - 依赖配置说明

## Gradle 依赖配置

在项目的 `build.gradle` 文件中添加以下依赖：

```gradle
plugins {
    id 'java'
    id 'org.jetbrains.intellij' version '1.16.0'
}

dependencies {
    // ==================== 核心Spring依赖 ====================
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter:3.2.0'
    implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
    implementation 'org.springframework.boot:spring-boot-starter-webflux:3.2.0'
    implementation 'org.springframework.boot:spring-boot-configuration-processor:3.2.0'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor:3.2.0'
    
    // ==================== Elasticsearch ====================
    
    // Elasticsearch High Level REST Client
    implementation 'org.elasticsearch.client:elasticsearch-rest-high-level-client:7.17.9'
    implementation 'org.elasticsearch.client:elasticsearch-rest-client:7.17.9'
    implementation 'org.elasticsearch:elasticsearch:7.17.9'
    
    // ==================== HTTP客户端 ====================
    
    // Java 11+ 内置HttpClient（无需额外依赖）
    // 如果需要更多功能，可以添加 OkHttp
    // implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // ==================== JSON处理 ====================
    
    // Jackson（Spring Boot已包含）
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2'
    
    // ==================== 日志 ====================
    
    // SLF4J + Logback（Spring Boot已包含）
    implementation 'org.slf4j:slf4j-api:2.0.9'
    implementation 'ch.qos.logback:logback-classic:1.4.11'
    
    // ==================== 工具类 ====================
    
    // Apache Commons
    implementation 'org.apache.commons:commons-lang3:3.13.0'
    implementation 'org.apache.commons:commons-collections4:4.4'
    
    // Guava（可选）
    implementation 'com.google.guava:guava:32.1.3-jre'
    
    // ==================== 测试依赖 ====================
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test:3.2.0'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.mockito:mockito-core:5.5.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.5.0'
    
    // ==================== IntelliJ Platform ====================
    
    // 由 org.jetbrains.intellij 插件自动管理
    // 无需手动添加
}

// Java版本配置
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 编译配置
compileJava {
    options.encoding = 'UTF-8'
    options.compilerArgs << '-parameters'
}

// IntelliJ 插件配置
intellij {
    version = '2023.2'
    type = 'IC' // IC = Community, IU = Ultimate
    plugins = ['java']
}

// 测试配置
test {
    useJUnitPlatform()
    
    // 设置测试环境变量（从系统环境变量读取）
    environment "GITEE_AI_API_KEY", System.getenv("GITEE_AI_API_KEY") ?: ""
    environment "ES_HOST", System.getenv("ES_HOST") ?: "localhost"
}
```

## Maven 依赖配置（如果使用Maven）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <properties>
        <java.version>17</java.version>
        <elasticsearch.version>7.17.9</elasticsearch.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Elasticsearch -->
        <dependency>
            <groupId>org.elasticsearch.client</groupId>
            <artifactId>elasticsearch-rest-high-level-client</artifactId>
            <version>${elasticsearch.version}</version>
        </dependency>
        
        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## 依赖说明

### 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用基础框架、自动配置 |
| Elasticsearch Client | 7.17.9 | 向量存储（默认） |
| Jackson | 2.15.2 | JSON序列化/反序列化 |
| SLF4J + Logback | 2.0.9/1.4.11 | 日志框架 |

### 可选依赖

| 依赖 | 用途 | 何时需要 |
|------|------|----------|
| PostgreSQL JDBC | PGVector支持 | 使用PGVector时 |
| Chroma Client | Chroma向量库 | 使用Chroma时 |
| OkHttp | HTTP客户端 | 需要更多HTTP功能时 |

### 版本兼容性

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| Java | 17 | 17/21 | 需要Java 17+的HttpClient |
| Elasticsearch | 7.10 | 7.17.9 | 需要dense_vector支持 |
| Spring Boot | 3.0 | 3.2.0 | 需要Java 17支持 |
| IntelliJ IDEA | 2023.1 | 2023.2+ | 插件开发需要 |

## 环境变量配置

### 必需环境变量

```bash
# Gitee AI API密钥
export GITEE_AI_API_KEY="your_api_key_here"
```

### 可选环境变量

```bash
# Elasticsearch配置
export ES_HOST="localhost"
export ES_PORT="9200"
export ES_USERNAME="elastic"
export ES_PASSWORD="your_password"

# 数据库配置（使用PGVector时）
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
```

## Docker Compose 配置（可选）

如果需要完整的开发环境，可以使用Docker Compose：

```yaml
# docker-compose.yml
version: '3.8'

services:
  elasticsearch:
    image: elasticsearch:7.17.9
    container_name: livingdoc-elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    networks:
      - livingdoc

  # 可选：Kibana（ES可视化）
  kibana:
    image: kibana:7.17.9
    container_name: livingdoc-kibana
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - livingdoc

  # 可选：PostgreSQL（使用PGVector时）
  postgres:
    image: ankane/pgvector:latest
    container_name: livingdoc-postgres
    environment:
      POSTGRES_DB: livingdoc
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - pg_data:/var/lib/postgresql/data
    networks:
      - livingdoc

volumes:
  es_data:
  pg_data:

networks:
  livingdoc:
    driver: bridge
```

启动环境：

```bash
docker-compose up -d
```

## 配置文件位置

### 主配置文件

```
src/main/resources/
├── livingdoc-config.yml       # 活文档配置
├── application.yml            # Spring Boot配置（如果需要）
└── logback-spring.xml         # 日志配置（如果需要）
```

### 开发环境配置

```yaml
# src/main/resources/application-dev.yml
spring:
  profiles:
    active: dev

livingdoc:
  ai:
    provider: gitee-ai
  vector-store:
    type: elasticsearch
    elasticsearch:
      host: localhost
      port: 9200

logging:
  level:
    com.shuyixiao.livingdoc: DEBUG
    org.elasticsearch: INFO
```

### 生产环境配置

```yaml
# src/main/resources/application-prod.yml
spring:
  profiles:
    active: prod

livingdoc:
  ai:
    provider: gitee-ai
  vector-store:
    type: elasticsearch
    elasticsearch:
      host: ${ES_HOST}
      port: ${ES_PORT}
      username: ${ES_USERNAME}
      password: ${ES_PASSWORD}

logging:
  level:
    com.shuyixiao.livingdoc: INFO
    org.elasticsearch: WARN
```

## 构建和运行

### 构建项目

```bash
# 清理并构建
./gradlew clean build

# 跳过测试构建
./gradlew clean build -x test

# 构建插件
./gradlew buildPlugin
```

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行指定测试
./gradlew test --tests GiteeAiIntegrationTest
./gradlew test --tests RagServiceTest

# 查看测试报告
open build/reports/tests/test/index.html
```

### 在IntelliJ IDEA中运行

```bash
# 启动插件开发环境
./gradlew runIde

# 调试模式启动
./gradlew runIde --debug-jvm
```

## 常见问题

### Q1: Elasticsearch依赖冲突

**问题**: 多个Elasticsearch版本冲突

**解决**:
```gradle
configurations.all {
    resolutionStrategy {
        force 'org.elasticsearch:elasticsearch:7.17.9'
        force 'org.elasticsearch.client:elasticsearch-rest-client:7.17.9'
    }
}
```

### Q2: Jackson版本问题

**问题**: Jackson版本不匹配

**解决**: 使用Spring Boot管理的版本
```gradle
dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    // 不要指定版本，让Spring Boot管理
}
```

### Q3: Java版本问题

**问题**: 代码需要Java 17+

**解决**:
```gradle
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

### Q4: 插件依赖冲突

**问题**: IntelliJ插件和Spring Boot冲突

**解决**: 使用provided scope
```gradle
configurations {
    providedCompile
}

dependencies {
    providedCompile 'org.springframework.boot:spring-boot-starter'
}
```

## 总结

所有依赖都已配置完成，可以直接使用。主要依赖：

✅ **Spring Boot 3.2.0** - 应用框架  
✅ **Elasticsearch 7.17.9** - 向量数据库  
✅ **Jackson** - JSON处理  
✅ **SLF4J/Logback** - 日志  
✅ **JUnit 5** - 测试框架  

运行 `./gradlew build` 即可下载所有依赖并构建项目。

