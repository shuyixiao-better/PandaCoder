# 如何在项目中启用 SQL Monitor

## 📋 前置条件

SQL Monitor 需要你的项目满足以下条件：

1. ✅ 使用 **MyBatis** 或类似的 ORM 框架
2. ✅ 在 **IntelliJ IDEA** 中运行项目
3. ✅ 项目日志级别设置为 **DEBUG**

---

## 🚀 快速启用（3步）

### 步骤1: 配置日志级别

在项目的配置文件中设置日志级别为 DEBUG。

#### Spring Boot 项目 (application.yml)
```yaml
logging:
  level:
    # 设置MyBatis Mapper接口的日志级别为DEBUG
    com.yourpackage.mapper: DEBUG
    
    # 或者设置整个包的日志级别
    com.yourpackage: DEBUG
    
    # 如果使用MyBatis-Plus，可以这样配置
    com.baomidou.mybatisplus: DEBUG
```

#### Spring Boot 项目 (application.properties)
```properties
# MyBatis Mapper日志
logging.level.com.yourpackage.mapper=DEBUG

# 整个包
logging.level.com.yourpackage=DEBUG

# MyBatis-Plus
logging.level.com.baomidou.mybatisplus=DEBUG
```

#### logback.xml 配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 其他配置... -->
    
    <!-- MyBatis SQL日志 -->
    <logger name="com.yourpackage.mapper" level="DEBUG"/>
    
    <!-- 或者配置整个包 -->
    <logger name="com.yourpackage" level="DEBUG"/>
    
    <!-- MyBatis-Plus -->
    <logger name="com.baomidou.mybatisplus" level="DEBUG"/>
</configuration>
```

### 步骤2: 打开 SQL Monitor 工具窗口

1. 在 IntelliJ IDEA 底部工具栏找到 **"SQL Monitor"**
2. 点击打开工具窗口
3. 确保 **"启用SQL监听"** 复选框是**选中**状态

### 步骤3: 运行项目

1. 在 IntelliJ IDEA 中运行你的项目（不要用外部终端）
2. 执行一些数据库操作
3. SQL Monitor 会自动捕获并显示所有 SQL 查询

---

## 🔧 详细配置说明

### 1. MyBatis 配置

#### MyBatis 原生配置 (mybatis-config.xml)
```xml
<configuration>
    <settings>
        <!-- 开启SQL日志 -->
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>
</configuration>
```

#### Spring Boot + MyBatis
```yaml
mybatis:
  configuration:
    # 开启SQL日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

#### MyBatis-Plus 配置
```yaml
mybatis-plus:
  configuration:
    # 开启SQL日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2. 日志框架配置

#### Slf4j + Logback (推荐)
```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss,SSS} %level (%file:%line)- %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- MyBatis SQL日志 -->
    <logger name="com.yourpackage.mapper" level="DEBUG"/>
    
    <!-- 根日志 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

#### Log4j2
```xml
<!-- log4j2.xml -->
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} %level (%F:%L)- %msg%n"/>
        </Console>
    </Appenders>
    
    <Loggers>
        <!-- MyBatis SQL日志 -->
        <Logger name="com.yourpackage.mapper" level="DEBUG"/>
        
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### 3. API路径追踪配置（可选）

为了让 SQL Monitor 能够显示 API 路径，建议在 Controller 中添加日志：

```java
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    
    @GetMapping("/list")
    public Result list(@RequestParam int page) {
        // 添加API日志（格式：API:路径）
        log.info("API:/api/user/list");
        
        // 业务逻辑...
        return userService.list(page);
    }
}
```

或者使用 AOP 统一处理：

```java
@Aspect
@Component
@Slf4j
public class ApiLogAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String uri = request.getRequestURI();
            
            // 输出API日志（格式：API:路径）
            log.info("API:" + uri);
        }
        
        return joinPoint.proceed();
    }
}
```

---

## 🎯 不同框架的配置示例

### Spring Boot + MyBatis

**pom.xml**:
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.0</version>
    </dependency>
    
    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
</dependencies>
```

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.yourpackage.entity
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.yourpackage.mapper: DEBUG
```

### Spring Boot + MyBatis-Plus

**pom.xml**:
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.3</version>
    </dependency>
    
    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
</dependencies>
```

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.yourpackage.entity
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.yourpackage.mapper: DEBUG
    com.baomidou.mybatisplus: DEBUG
```

### Spring Boot + JPA (暂不支持)

**注意**: SQL Monitor 当前版本仅支持 MyBatis 日志格式，JPA 支持将在后续版本中添加。

---

## 🐛 常见问题排查

### 问题1: SQL Monitor 没有捕获到任何 SQL

**检查清单**:
1. ✅ 确认项目在 IntelliJ IDEA 中运行（不是外部终端）
2. ✅ 确认 "启用SQL监听" 开关是打开的
3. ✅ 确认日志级别设置为 DEBUG
4. ✅ 确认控制台有 SQL 日志输出
5. ✅ 确认项目使用 MyBatis 或类似框架

**验证日志输出**:

在控制台查找类似这样的日志：
```
==>  Preparing: SELECT * FROM user WHERE id = ?
==> Parameters: 123(String)
<==      Total: 1
```

如果看到这样的日志，说明配置正确。

### 问题2: 控制台没有 SQL 日志

**解决方案**:

1. 检查日志级别配置：
   ```yaml
   logging:
     level:
       com.yourpackage.mapper: DEBUG
   ```

2. 检查 MyBatis 配置：
   ```yaml
   mybatis:
     configuration:
       log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
   ```

3. 重启项目

### 问题3: API 路径显示 "N/A"

**解决方案**:

在 Controller 中添加 API 日志：
```java
log.info("API:/api/user/list");
```

或使用 AOP 统一处理（参考上面的配置）。

### 问题4: 调用类显示 "N/A" 或不准确

**原因**: MyBatis 日志中没有包含调用类信息。

**解决方案**: 这是 MyBatis 的限制，SQL Monitor 会尽可能从日志中提取相关信息。

---

## 📊 验证配置是否成功

### 1. 检查控制台日志

运行项目后，在控制台应该能看到：
```
2025-10-18 22:21:30,509 DEBUG (BaseJdbcLogger.java:135)- ==>  Preparing: SELECT id,name FROM user WHERE id = ?
2025-10-18 22:21:30,509 DEBUG (BaseJdbcLogger.java:135)- ==> Parameters: 123(String)
2025-10-18 22:21:30,511 DEBUG (BaseJdbcLogger.java:135)- <==      Total: 1
```

### 2. 检查 SQL Monitor 工具窗口

打开 SQL Monitor 工具窗口，应该能看到：
- 表格中显示 SQL 记录
- 状态栏显示统计信息
- 监听状态显示 "启用"

### 3. 执行数据库操作

1. 访问一个 API 接口
2. 在 SQL Monitor 中应该立即看到对应的 SQL
3. 点击记录查看详情

---

## 🎨 完整示例项目配置

### 项目结构
```
my-project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/yourpackage/
│   │   │       ├── controller/
│   │   │       │   └── UserController.java
│   │   │       ├── service/
│   │   │       │   └── UserService.java
│   │   │       ├── mapper/
│   │   │       │   └── UserMapper.java
│   │   │       └── entity/
│   │   │           └── User.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       └── mapper/
│   │           └── UserMapper.xml
│   └── test/
├── pom.xml
└── README.md
```

### UserController.java
```java
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/list")
    public Result list() {
        log.info("API:/api/user/list");
        List<User> users = userService.list();
        return Result.success(users);
    }
}
```

### application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.yourpackage.entity
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
    map-underscore-to-camel-case: true

logging:
  level:
    root: INFO
    com.yourpackage: DEBUG
    com.yourpackage.mapper: DEBUG
```

### logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss,SSS} %level (%file:%line)- %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.yourpackage.mapper" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## 🎯 最佳实践

### 1. 开发环境配置

**application-dev.yml**:
```yaml
logging:
  level:
    com.yourpackage.mapper: DEBUG
```

### 2. 生产环境配置

**application-prod.yml**:
```yaml
logging:
  level:
    com.yourpackage.mapper: INFO  # 生产环境不输出SQL详情
```

### 3. 使用配置文件切换

**application.yml**:
```yaml
spring:
  profiles:
    active: dev  # 开发环境
    # active: prod  # 生产环境
```

---

## 📞 需要帮助？

如果配置过程中遇到问题：

1. 查看 [SQL Monitor 快速使用指南](./SQL_Monitor快速使用指南.md)
2. 查看 [SQL Monitor 完整实现总结](./SQL_Monitor完整实现总结.md)
3. 联系作者：yixiaoshu88@163.com

---

**文档版本**: 1.0.0  
**更新时间**: 2025-10-18  
**作者**: PandaCoder Team

