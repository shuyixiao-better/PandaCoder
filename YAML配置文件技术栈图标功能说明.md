# Spring Boot 配置文件技术栈图标功能 (全面升级版)

## 🎯 功能概述

**全新升级！**现在支持 `resources/icons` 目录下所有可用图标的智能识别和显示。系统会根据Spring Boot配置文件（YAML和Properties）的内容，自动识别技术栈并显示相应的图标。

## ✨ 核心特性

### 🤖 智能图标匹配系统
- **自动发现所有可用图标**：自动使用 `resources/icons` 目录下的所有图标
- **优先级匹配**：特定技术栈图标优先于通用图标
- **多重匹配模式**：支持配置键名、URL、关键词等多种匹配方式
- **智能排序**：避免图标冲突，显示最相关的图标

### 📦 支持的技术栈 (基于可用图标)

根据您的 `resources/icons` 目录，现在支持以下技术栈：

#### 🍃 Spring Boot Framework
- **图标**: `springboot.svg`
- **匹配**: `spring:`, `server:`, `management:`, `logging:`, `security:`
- **示例**: `spring.application.name`, `server.port`

#### 🗃️ 数据库系统

**MySQL**
- **图标**: `mysql.svg`
- **匹配**: `mysql`, `jdbc:mysql://`
- **示例**: URL中包含mysql的配置

**PostgreSQL**
- **图标**: `postgresql.svg`
- **匹配**: `postgresql`, `postgres`, `jdbc:postgresql://`
- **示例**: PostgreSQL数据库连接配置

**Oracle**
- **图标**: `oracle.svg`
- **匹配**: `oracle`, `jdbc:oracle:`
- **示例**: Oracle数据库连接配置

**SQL Server**
- **图标**: `sqlserver.svg`
- **匹配**: `sqlserver`, `mssql`, `jdbc:sqlserver://`
- **示例**: SQL Server数据库连接配置

**通用数据库配置**
- **图标**: `mysql.svg` (作为默认数据库图标)
- **匹配**: `datasource:`, `jpa:`, `hibernate:`, `hikari:`, `druid:`, `dbcp:`
- **示例**: `spring.datasource.*`, `spring.jpa.*`

#### ⚡ 缓存系统
- **图标**: `redis.svg`
- **匹配**: `redis:`, `cache:`, `redis://`
- **示例**: `spring.redis.*`, `spring.cache.*`

#### 🔍 搜索引擎
- **图标**: `elasticsearch.svg`
- **匹配**: `elasticsearch:`, `elastic:`, 端口9200
- **示例**: `elasticsearch.uris`, ES集群配置

#### 📨 消息队列

**Apache Kafka**
- **图标**: `kafka.svg`
- **匹配**: `kafka:`, 端口9092
- **示例**: `spring.kafka.*`

**RabbitMQ**
- **图标**: `rabbitmq.svg`
- **匹配**: `rabbitmq:`, `amqp:`, 端口5672
- **示例**: `spring.rabbitmq.*`

## 🎨 显示效果示例

### YAML格式
```yaml
spring:                    # 🍃 Spring Boot图标
  application:
    name: my-app
    
  datasource:             # 🗃️ 数据库图标 (通用)
    url: jdbc:mysql://localhost:3306/db  # 🐬 MySQL图标 (特定)
    username: root
    
  redis:                  # ⚡ Redis图标
    host: localhost
    port: 6379
    
elasticsearch:            # 🔍 Elasticsearch图标
  uris: http://localhost:9200
  
spring:
  kafka:                  # 📨 Apache Kafka图标
    bootstrap-servers: localhost:9092
```

### Properties格式
```properties
# 🍃 Spring Boot图标
spring.application.name=my-app

# 🐬 MySQL图标 (URL自动识别)
spring.datasource.url=jdbc:mysql://localhost:3306/db

# 🐘 PostgreSQL图标
spring.datasource.url=jdbc:postgresql://localhost:5432/db

# ⚡ Redis图标
spring.redis.host=localhost

# 🔍 Elasticsearch图标
spring.elasticsearch.uris=http://localhost:9200
```

## 🔍 智能匹配算法

### 匹配优先级 (从高到低)
1. **URL特定匹配**: `jdbc:mysql://` → MySQL图标
2. **配置键特定匹配**: `redis:` → Redis图标
3. **通用配置匹配**: `datasource:` → 通用数据库图标
4. **框架通用匹配**: `spring:` → Spring Boot图标

### 匹配模式

#### YAML文件
- **直接配置**: `redis:`, `kafka:`, `elasticsearch:`
- **嵌套配置**: `spring: redis:`, `spring: datasource:`
- **URL内容**: 检测值中的数据库类型

#### Properties文件
- **直接配置**: `redis=`, `kafka=`
- **Spring配置**: `spring.redis.*`, `spring.kafka.*`
- **URL内容**: 检测值中的数据库类型

## 🚀 使用方法

### 自动生效
1. **安装插件**后功能自动启用
2. **打开配置文件** (支持 `.yml`, `.yaml`, `.properties`)
3. **查看左侧图标** - 相关配置行会自动显示技术栈图标
4. **鼠标悬停** - 查看技术栈名称详情

### 支持的文件模式
- `application.yml` / `application.yaml`
- `application-{env}.yml` (如 `application-dev.yml`)
- `bootstrap.yml` / `bootstrap.yaml`
- `application.properties`
- `application-{env}.properties` (如 `application-dev.properties`)

## 📊 技术优势

### 🔧 智能特性
- **零配置**: 自动发现和使用所有可用图标
- **智能排序**: 避免图标冲突，显示最相关的图标
- **多重匹配**: 支持配置名、URL、端口等多种识别方式
- **优先级控制**: 特定技术栈优先于通用配置

### ⚡ 性能优化
- **图标预加载**: 启动时预加载所有图标
- **缓存机制**: 避免重复图标加载
- **异常安全**: 图标加载失败不影响其他功能
- **最小开销**: 对IDE性能影响极小

### 🛡️ 鲁棒性
- **容错处理**: 图标缺失时优雅降级
- **日志记录**: 详细的调试和错误日志
- **兼容性**: 支持各种文件格式和配置模式

## 🔧 扩展性

### 🎯 未来扩展
如果您添加了新的图标到 `resources/icons` 目录，可以轻松扩展匹配规则：

1. **添加图标文件** (如 `mongodb.svg`)
2. **在配置中添加匹配规则**:
   ```java
   // MongoDB
   TECH_STACK_CONFIGS.put("mongodb", new TechStackConfig(
       "/icons/mongodb.svg",
       "MongoDB",
       Arrays.asList("mongodb", "mongo"),
       Arrays.asList("mongodb://", ":27017")
   ));
   ```

### 📈 自动发现 (未来版本)
计划在未来版本中实现：
- 自动扫描 `icons` 目录
- 根据图标文件名自动生成匹配规则
- 动态配置系统

## 🎉 总结

这个升级版的技术栈图标功能：

- ✅ **支持所有可用图标** - 充分利用 `resources/icons` 目录
- ✅ **智能匹配算法** - 准确识别技术栈类型
- ✅ **优先级控制** - 避免图标冲突
- ✅ **高性能实现** - 最小化系统开销
- ✅ **易于扩展** - 支持未来添加更多技术栈

现在您的Spring Boot配置文件将变得更加直观和专业！🎊 