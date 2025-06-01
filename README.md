# YiXiaoPlugin

## 项目说明

这是一个基于IntelliJ IDEA的插件项目，提供百度翻译API集成功能。

## 配置说明

### 百度翻译API配置

为了安全起见，百度翻译API的密钥需要通过系统属性配置：

```
-Dbaidu.api.key=您的百度API密钥
-Dbaidu.app.id=您的百度APP_ID
```

您可以在IDEA的运行配置中添加这些VM选项，或在系统环境变量中设置。

## 构建说明

本项目使用Gradle构建，支持Java 24。

```bash
./gradlew build
```

## 许可证

请参阅项目中的LICENSE文件。
