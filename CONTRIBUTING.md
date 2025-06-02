# PandaCoder 贡献指南

感谢您考虑为 PandaCoder 项目做出贡献！您的参与对于我们非常宝贵。

## 如何贡献

### 报告问题

如果您发现了 bug 或有功能建议，请通过 GitHub Issues 告诉我们：

1. 检查现有 issues，避免重复报告
2. 使用清晰的标题和详细描述
3. 如果是 bug，请提供重现步骤和环境信息
4. 如果是功能请求，请说明为什么这个功能对用户有价值

### 提交代码

1. Fork 本仓库
2. 创建您的特性分支：`git checkout -b feature/amazing-feature`
3. 提交您的更改：`git commit -m 'Add some amazing feature'`
4. 推送到分支：`git push origin feature/amazing-feature`
5. 提交 Pull Request

### 代码风格

- 遵循 Java 编码规范
- 类名使用 UpperCamelCase
- 方法名使用 lowerCamelCase
- 添加必要的注释，特别是对于复杂逻辑
- 确保新代码有适当的测试覆盖率

## 开发环境设置

```bash
# 克隆仓库
git clone https://github.com/shuyixiao-better/PandaCoder.git
cd PandaCoder

# 构建项目
./gradlew build

# 运行 IDE 进行测试
./gradlew runIde
```

## 测试

请确保您的更改通过所有测试：

```bash
./gradlew test
```

## 发布流程

项目维护者将负责版本发布。如果您的更改被合并，它们将包含在下一个版本中。

## 联系我们

如果您有任何问题，请通过 GitHub Issues 或直接联系项目维护者：

- 舒一笑：yixiaoshu88@163.com

再次感谢您的贡献！
