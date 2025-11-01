# Git 统计邮件配置存储优化说明

## 📋 问题描述

**问题**: 用户反馈在重新打包插件后，之前配置的 SMTP 邮箱信息、密码、定时发送等配置会丢失，需要重新配置。

**用户需求**:
- ✅ 不同项目应该有**独立的配置**（因为每个项目的团队成员不同）
- ✅ 同一个项目中配置过后，**插件升级或重装**时配置不丢失
- ❌ 不需要所有项目共享同一个配置

**影响**: 每次插件更新或重新安装后，同一个项目需要重新输入所有邮件配置信息，体验不佳。

---

## 🔍 问题根源分析

### 原有配置存储方式

```java
@State(
    name = "GitStatEmailConfig",
    storages = {
        @Storage(value = StoragePathMacros.WORKSPACE_FILE),  // 存储在 workspace.xml
        @Storage(value = "gitStatEmailConfig.xml", deprecated = true)
    }
)
```

**问题所在**:
1. **存储在 workspace.xml**: 配置存储在项目的 `.idea/workspace.xml` 文件中
2. **workspace.xml 不稳定**: workspace.xml 是工作空间文件，可能被 IDEA 清理或重置
3. **容易丢失**: 当 workspace.xml 被清理、重置或损坏时，配置会丢失
4. **插件重装可能丢失**: 某些情况下重新安装插件会导致 workspace.xml 被重置

### 配置存储位置对比

| 存储方式 | 存储位置 | 稳定性 | 是否跨项目 | 插件重装后 | 适用场景 |
|---------|---------|--------|-----------|-----------|---------|
| **workspace.xml** | `.idea/workspace.xml` | ⚠️ 不稳定 | ❌ 否 | ❌ 可能丢失 | 临时工作状态 |
| **独立项目配置文件** | `.idea/gitStatEmailConfig.xml` | ✅ 稳定 | ❌ 否 | ✅ 保留 | 项目级别配置 |
| **应用级别配置** | IDEA 全局配置目录 | ✅ 稳定 | ✅ 是 | ✅ 保留 | 全局用户配置 |

---

## ✅ 解决方案

### 1. 使用独立的项目配置文件

将配置从 **workspace.xml** 改为**独立的项目配置文件** `.idea/gitStatEmailConfig.xml`，确保：
- ✅ 每个项目有独立的配置（不同项目团队成员不同）
- ✅ 配置文件独立存储，不受 workspace.xml 清理影响
- ✅ 插件升级或重装后配置保留（只要 `.idea` 目录存在）

#### 修改后的配置

```java
@Service(Service.Level.PROJECT)
@State(
    name = "GitStatEmailConfig",
    storages = @Storage("gitStatEmailConfig.xml")  // 独立的项目配置文件
)
public class GitStatEmailConfigState implements PersistentStateComponent<GitStatEmailConfigState> {
    // ... 配置字段
}
```

#### 服务注册（保持不变）

```xml
<projectService serviceImplementation="com.shuyixiao.gitstat.email.config.GitStatEmailConfigState"/>
```

### 2. 配置存储位置

配置存储在**项目的 `.idea` 目录**下的独立文件中：

```
{项目根目录}/.idea/gitStatEmailConfig.xml
```

**示例**:
```
E:\Project\GitHub\PandaCoder\.idea\gitStatEmailConfig.xml
```

### 3. 关键改进点

#### 改进前（workspace.xml）
- ❌ 配置存储在 `.idea/workspace.xml` 中
- ❌ workspace.xml 可能被 IDEA 清理或重置
- ❌ 插件重装时可能丢失配置

#### 改进后（独立配置文件）
- ✅ 配置存储在独立的 `.idea/gitStatEmailConfig.xml` 文件中
- ✅ 独立文件不会被 IDEA 自动清理
- ✅ 插件重装后配置保留（只要 `.idea` 目录存在）
- ✅ 每个项目有独立的配置
- ✅ 可以将配置文件加入版本控制（如果团队需要共享配置）

---

## 🎯 优化效果

### 修改前（存储在 workspace.xml）
- ❌ 配置存储在 workspace.xml 中，不稳定
- ❌ workspace.xml 被清理时配置丢失
- ❌ 插件重装后可能配置丢失
- ❌ 某些 IDEA 操作会重置 workspace.xml

### 修改后（独立配置文件）
- ✅ 每个项目有独立的配置文件
- ✅ 配置文件独立存储，不受 workspace.xml 影响
- ✅ 插件重装后配置保留（只要 `.idea` 目录存在）
- ✅ 不同项目可以有不同的配置（适合不同团队）
- ✅ 可选：配置文件可以加入版本控制，团队共享配置

---

## 📝 用户使用说明

### 首次配置

1. 打开你的项目
2. 在 Git 统计工具窗口中配置邮件信息
3. 点击"保存配置"
4. 配置会自动保存到项目的 `.idea/gitStatEmailConfig.xml` 文件

### 不同项目的配置

- ✅ 每个项目有独立的配置
- ✅ 不同项目可以配置不同的邮件接收者
- ✅ 适合不同项目有不同团队成员的场景
- ✅ 切换项目时，自动加载该项目的配置

### 配置备份

配置文件位于项目的 `.idea` 目录下：

```
{项目根目录}/.idea/gitStatEmailConfig.xml
```

**备份方法**:
1. 手动复制 `.idea/gitStatEmailConfig.xml` 文件
2. 或者将 `.idea/gitStatEmailConfig.xml` 加入版本控制（如果团队需要共享配置）

### 配置共享（可选）

如果你的团队希望共享邮件配置，可以将配置文件加入版本控制：

**方法 1: 修改 .gitignore**
```bash
# 在 .gitignore 中移除对 gitStatEmailConfig.xml 的忽略
# 或者添加例外规则
!.idea/gitStatEmailConfig.xml
```

**方法 2: 手动提交**
```bash
git add .idea/gitStatEmailConfig.xml
git commit -m "添加 Git 统计邮件配置"
git push
```

### 配置迁移

如果你之前在某个项目中配置过邮件信息（存储在 workspace.xml 中）：

1. 打开该项目
2. 插件会自动检测旧配置
3. 如果新配置为空，会提示是否迁移旧配置
4. 点击"迁移配置"即可自动迁移到新的配置文件

---

## 🔧 技术实现细节

### 修改的文件

1. **GitStatEmailConfigState.java**
   - 修改 `@State` 注解，移除 `StoragePathMacros.WORKSPACE_FILE`
   - 添加 `getInstance()` 静态方法（无参数）
   - 保留 `getInstance(Project)` 方法并标记为 `@Deprecated`

2. **plugin.xml**
   - 将 `projectService` 改为 `applicationService`

3. **GitStatToolWindow.java**
   - 更新调用方式：`getInstance()` 替代 `getInstance(project)`

4. **EmailConfigMigration.java**
   - 更新配置获取方式
   - 调整备份逻辑（应用级别配置无需手动备份）

5. **GitStatEmailStartupActivity.java**
   - 更新配置获取方式

### 配置文件格式

```xml
<application>
  <component name="GitStatEmailConfig">
    <option name="configVersion" value="1" />
    <option name="smtpHost" value="smtp.gmail.com" />
    <option name="smtpPort" value="587" />
    <option name="enableTLS" value="true" />
    <option name="enableSSL" value="false" />
    <option name="senderEmail" value="your@email.com" />
    <option name="senderPassword" value="encrypted_password" />
    <option name="senderName" value="Git 统计" />
    <option name="recipientEmail" value="recipient@email.com" />
    <option name="enableScheduled" value="false" />
    <option name="scheduledTime" value="18:00" />
    <option name="filterAuthor" value="" />
    <option name="includeTrends" value="true" />
    <option name="sendHtml" value="true" />
    <option name="emailSubject" value="📊 Git 统计日报 - {DATE}" />
  </component>
</application>
```

---

## 🚀 升级指南

### 对于用户

1. **更新插件**: 安装新版本插件
2. **打开项目**: 打开之前配置过的项目
3. **自动迁移**: 插件会自动检测 workspace.xml 中的旧配置并提示迁移
4. **验证配置**: 打开 Git 统计工具窗口，检查配置是否正确
5. **测试功能**: 点击"测试连接"验证配置
6. **检查配置文件**: 确认 `.idea/gitStatEmailConfig.xml` 文件已创建

### 对于开发者

如果你在开发环境中测试：

1. 查看旧配置位置：
   ```bash
   # 旧配置在 workspace.xml 中
   cat .idea/workspace.xml | grep GitStatEmailConfig
   ```

2. 安装新版本插件后，配置会自动迁移到：
   ```bash
   # 新配置在独立文件中
   cat .idea/gitStatEmailConfig.xml
   ```

3. 验证配置文件格式正确

---

## ⚠️ 注意事项

### 配置安全

- ✅ 密码使用加密存储
- ⚠️ 配置文件包含敏感信息（SMTP 密码）
- ⚠️ **默认情况下不要将配置文件提交到版本控制系统**
- ✅ 如果需要团队共享配置，确保使用私有仓库

### .gitignore 配置

默认情况下，`.idea/gitStatEmailConfig.xml` 应该被忽略：

```gitignore
# .gitignore
.idea/
!.idea/runConfigurations/
# 如果需要共享配置，添加例外：
# !.idea/gitStatEmailConfig.xml
```

### 配置文件位置

- 配置文件位于：`{项目根目录}/.idea/gitStatEmailConfig.xml`
- 每个项目有独立的配置文件
- 删除 `.idea` 目录会删除配置

### 配置清理

如果需要清理某个项目的配置：

```bash
# 删除配置文件
rm .idea/gitStatEmailConfig.xml

# 或者删除整个 .idea 目录（会删除所有 IDEA 配置）
rm -rf .idea/
```

---

## 📊 版本历史

| 版本 | 日期 | 变更说明 |
|-----|------|---------|
| 2.2.0 | 2025-11-01 | 将配置从项目级别改为应用级别，解决配置丢失问题 |
| 2.1.0 | 2025-10-23 | 使用 WORKSPACE_FILE 存储配置（项目级别） |
| 2.0.0 | 2025-10-22 | 初始版本，邮件发送功能上线 |

---

## 🔗 相关文档

- [Git统计邮件发送功能设计方案](Git统计邮件发送功能设计方案.md)
- [Git统计邮件配置丢失问题修复报告](Git统计邮件配置丢失问题修复报告.md)
- [IntelliJ Platform SDK - Persisting State of Components](https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html)

---

## 💡 总结

通过将配置从 **workspace.xml** 改为**独立的项目配置文件** `.idea/gitStatEmailConfig.xml`，彻底解决了配置丢失的问题：

### 核心改进

1. **配置稳定性**: 独立配置文件不受 workspace.xml 清理影响
2. **项目独立性**: 每个项目有独立配置，适合不同团队
3. **插件升级安全**: 插件升级或重装后配置保留
4. **可选共享**: 可以选择将配置加入版本控制，团队共享

### 适用场景

- ✅ 不同项目有不同的团队成员
- ✅ 需要为每个项目配置不同的邮件接收者
- ✅ 希望插件升级后配置不丢失
- ✅ 可选：团队希望共享邮件配置

这大大提升了用户体验，确保配置的稳定性和灵活性。

