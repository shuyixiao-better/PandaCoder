# 类名前缀功能说明

## 功能介绍

熊猫编码助手（PandaCoder）现在支持在创建类文件时添加自定义前缀，让代码生成更加灵活和符合项目规范。

## 前缀使用方法

创建新类文件时，您可以使用以下两种格式输入类名：

1. **前缀:中文名** - 例如：`Service:用户管理`
2. **前缀中文名** - 例如：`Controller用户登录`

系统会自动识别前缀，将中文部分翻译为英文并转换为驼峰命名，然后在前面添加您指定的前缀。

## 预定义前缀

系统默认提供以下常用前缀：
- Service
- Repository
- Controller
- Component
- Util
- Manager
- Factory
- Builder
- Handler

## 前缀配置

您可以通过插件设置页面自定义前缀列表：

1. 打开 Settings/Preferences (Ctrl+Alt+S)
2. 导航至 Tools → Yixiao Plugin
3. 在「类名前缀」输入框中，输入您需要的前缀，多个前缀用逗号分隔

## 使用示例

| 输入内容 | 生成的类名 |
|---------|----------|
| `Service:用户管理` | ServiceUserManagement |
| `Controller用户登录` | ControllerUserLogin |
| `Repository:订单查询` | RepositoryOrderQuery |
| `Util文件处理` | UtilFileProcessing |

## 适用场景

- 遵循特定命名规范的项目
- 需要快速创建标准化服务层、控制层、数据访问层等类文件
- 团队协作时保持代码命名一致性

## 注意事项

- 前缀区分大小写，请确保输入的前缀与配置的前缀大小写一致
- 如果输入的前缀不在预定义列表中，将被视为普通中文进行处理
- 异常类会自动在名称后添加「Exception」后缀
