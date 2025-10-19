# 测试脚本目录

本目录包含用于测试 PandaCoder 插件各项功能的批处理脚本。

## 脚本说明

### 基础测试脚本
- `RunTest.bat` - 基础测试脚本
- `RunSimpleTest.bat` - 简单测试
- `RunQuickCheck.bat` - 快速检查

### 功能测试脚本
- `RunApiPathTest.bat` - API 路径提取测试
- `RunCharTest.bat` - 字符处理测试
- `RunDebugTest.bat` - 调试功能测试
- `RunRegexTest.bat` - 正则表达式测试
- `RunDirectGetTest.bat` - 直接获取测试

### 综合测试脚本
- `RunFinalTest.bat` - 最终测试
- `RunFullScenarioTest.bat` - 完整场景测试
- `RunDetailCheck.bat` - 详细检查

### 错误分析脚本
- `RunAnalyzeError.bat` - 错误分析

## 使用方法

在项目根目录下运行：
```bash
scripts\RunTest.bat
```

或者直接在 scripts 目录下运行：
```bash
cd scripts
RunTest.bat
```

## 注意事项

- 这些脚本需要在项目根目录或 scripts 目录下运行
- 确保已经正确配置了 Java 环境
- 某些测试可能需要先编译项目

