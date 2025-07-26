# SpringBoot配置文件图标功能使用说明

## ✨ 功能概述

PandaCoder插件现已完美支持SpringBoot配置文件的技术栈图标显示功能！当您在IntelliJ IDEA中打开SpringBoot的配置文件时，系统会自动分析配置内容，并在编辑器左侧的gutter区域显示对应的技术栈图标。

## 🚀 主要特性

### 🎯 智能识别
- **自动检测**：自动识别SpringBoot项目的配置文件
- **多格式支持**：同时支持YAML (.yml/.yaml) 和Properties (.properties) 格式
- **精准匹配**：根据配置键名和URL自动匹配对应的技术栈

### 🎨 丰富图标
系统支持以下技术栈的图标显示：

| 技术栈 | 图标文件 | 匹配规则 | 示例配置 |
|--------|----------|----------|----------|
| 🍃 Spring Boot | `springboot.svg` | spring, server, management, logging, security | `spring.application.name` |
| 🐬 MySQL | `mysql.svg` | mysql, jdbc:mysql:// | `spring.datasource.url=jdbc:mysql://...` |
| 🐘 PostgreSQL | `postgresql.svg` | postgresql, postgres, jdbc:postgresql:// | `spring.datasource.url=jdbc:postgresql://...` |
| 🔶 Oracle | `oracle.svg` | oracle, jdbc:oracle: | `spring.datasource.url=jdbc:oracle:...` |
| 🟦 SQL Server | `sqlserver.svg` | sqlserver, mssql, jdbc:sqlserver:// | `spring.datasource.url=jdbc:sqlserver://...` |
| ⚡ Redis | `redis.svg` | redis, cache, redis:// | `spring.redis.host` |
| 🔍 Elasticsearch | `elasticsearch.svg` | elasticsearch, elastic | `spring.elasticsearch.uris` |
| 📨 Apache Kafka | `kafka.svg` | kafka, :9092 | `spring.kafka.bootstrap-servers` |
| 🐰 RabbitMQ | `rabbitmq.svg` | rabbitmq, amqp, :5672 | `spring.rabbitmq.host` |
| 🗃️ 通用数据库 | `mysql.svg` | datasource, jpa, hibernate, hikari, druid | `spring.datasource.*` |

### 🧠 智能优先级匹配
系统采用智能优先级匹配算法：
1. **特定技术栈优先**：如MySQL图标优先于通用数据库图标
2. **URL识别优先**：通过JDBC URL自动识别具体数据库类型
3. **配置层级识别**：支持嵌套配置的准确识别

## 📁 支持的文件类型

### YAML格式文件
- `application.yml` / `application.yaml`
- `application-{env}.yml` (如 `application-dev.yml`)
- `bootstrap.yml` / `bootstrap.yaml`
- `bootstrap-{env}.yml`

### Properties格式文件
- `application.properties`
- `application-{env}.properties` (如 `application-prod.properties`)
- `bootstrap.properties`
- `bootstrap-{env}.properties`

## 🎯 使用示例

### YAML配置示例
```yaml
# 🍃 Spring Boot图标
spring:
  application:
    name: my-app
    
  # 🐬 MySQL图标（URL自动识别）
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
    
  # ⚡ Redis图标
  redis:
    host: localhost
    port: 6379
    
  # 📨 Kafka图标
  kafka:
    bootstrap-servers: localhost:9092

# 🔍 Elasticsearch图标
elasticsearch:
  uris: http://localhost:9200
```

### Properties配置示例
```properties
# 🍃 Spring Boot图标
spring.application.name=my-app

# 🐬 MySQL图标（URL自动识别）
spring.datasource.url=jdbc:mysql://localhost:3306/mydb

# ⚡ Redis图标
spring.redis.host=localhost

# 📨 Kafka图标
spring.kafka.bootstrap-servers=localhost:9092

# 🔍 Elasticsearch图标
spring.elasticsearch.uris=http://localhost:9200
```

## 🔧 安装与使用

### 安装步骤
1. **构建插件**：运行 `./gradlew clean build`
2. **安装插件**：在IntelliJ IDEA中安装生成的插件文件
3. **重启IDE**：重启IntelliJ IDEA使插件生效

### 使用方法
1. **打开SpringBoot项目**
2. **编辑配置文件**：打开任意SpringBoot配置文件（.yml/.yaml/.properties）
3. **查看图标**：在编辑器左侧的gutter区域会自动显示技术栈图标
4. **鼠标悬停**：将鼠标悬停在图标上可查看技术栈名称

## 🎨 图标展示效果

当您打开SpringBoot配置文件时，会看到：
- **配置行左侧**：显示对应技术栈的彩色SVG图标
- **鼠标悬停**：显示"技术栈: [名称]"的提示信息
- **智能排序**：避免图标冲突，显示最相关的图标

## 🔍 技术实现

### 核心组件
- **SpringBootYamlLineMarkerProvider**：YAML文件行标记提供器
- **SpringBootPropertiesLineMarkerProvider**：Properties文件行标记提供器
- **SpringBootFileDetector**：SpringBoot文件类型检测器
- **SpringBootIconProvider**：图标提供器
- **YamlConfigService**：YAML配置服务

### 扩展点注册
```xml
<!-- YAML配置文件行标记提供器 -->
<codeInsight.lineMarkerProvider language="yaml" 
    implementationClass="com.shuyixiao.spring.boot.icon.SpringBootYamlLineMarkerProvider"/>

<!-- Properties配置文件行标记提供器 -->
<codeInsight.lineMarkerProvider language="Properties" 
    implementationClass="com.shuyixiao.spring.boot.icon.SpringBootPropertiesLineMarkerProvider"/>
```

## 🚀 未来扩展

### 计划支持的技术栈
- MongoDB
- Docker
- Kubernetes
- 微服务相关配置

### 扩展方法
1. **添加图标**：在 `src/main/resources/icons/` 目录下添加SVG图标
2. **更新配置**：在相应的LineMarkerProvider中添加匹配规则
3. **重新构建**：运行构建命令即可

## 📝 注意事项

1. **文件位置**：配置文件需要位于SpringBoot项目的resources目录下
2. **图标加载**：首次使用时图标会进行预加载，可能有轻微延迟
3. **性能优化**：系统采用缓存机制，避免重复加载图标
4. **错误处理**：图标加载失败时会优雅降级，不影响其他功能

## 🎉 结语

SpringBoot配置文件图标功能让您的配置文件变得更加直观和专业！通过一目了然的技术栈图标，快速识别项目使用的各项技术，大幅提升开发效率。

enjoy coding! 🚀 