## SQL Monitor 功能概览

### 一、核心监控功能

#### 1. 实时SQL监控
- 自动监听应用程序控制台输出
- 实时捕获MyBatis日志中的SQL查询
- 无需修改代码，零侵入监控
- 支持多进程监听，自动附加到新启动的进程

![](https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/BlogPicture/image-20251031115601904.png)

#### 2. 智能SQL解析

- 支持MyBatis标准日志格式（`Preparing` + `Parameters` + `Total`）
- 自动识别SQL操作类型：SELECT、INSERT、UPDATE、DELETE
- 智能提取数据库表名
- 完整记录SQL参数及类型
- 支持大JSON参数（最多1MB缓冲区）

![](https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/BlogPicture/image-20251031115920282.png)

#### 3. 可执行SQL生成

- 自动将参数替换到SQL语句中
- 生成可直接在数据库客户端执行的SQL
- 正确处理字符串、数字、JSON、日期时间等类型
- 自动转义SQL注入字符

![](https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/BlogPicture/image-20251031120014662.png)

### 二、追踪与分析功能

#### 4. API路径追踪
- 自动提取触发SQL的API接口路径
- 显示完整的API到SQL调用链路
- 支持从日志上下文中智能提取API信息

#### 5. 调用链追踪
- 记录调用SQL的Java类和方法
- 显示Mapper接口信息
- 帮助定位SQL执行源头

#### 6. 统计信息
- 实时显示总查询数量
- 按操作类型统计（SELECT/INSERT/UPDATE/DELETE）
- 显示涉及的表数量
- 监听器状态显示

### 三、界面交互功能

#### 7. 多维度筛选
- 操作类型筛选：按SELECT/INSERT/UPDATE/DELETE过滤
- 时间范围筛选：最近1小时/6小时/12小时/24小时/全部
- 关键词搜索：支持搜索SQL语句、表名、API路径
- 组合筛选：可同时使用多个条件

#### 8. 数据展示
- 表格展示：操作、表名、API路径、SQL摘要、结果数、时间戳
- 详情面板：显示完整的SQL语句、参数、可执行SQL
- 颜色标识：
  - SELECT（蓝色 #61AFFE）
  - INSERT（绿色 #49CC90）
  - UPDATE（橙色 #FCA130）
  - DELETE（红色 #F93E3E）

#### 9. 实时更新
- 新SQL自动显示，无需手动刷新
- 每10秒自动刷新（备用机制）
- 实时UI更新，不阻塞主线程

### 四、数据管理功能

#### 10. 导出功能
- 导出选中记录到剪贴板
- 支持复制可执行SQL
- 支持导出完整SQL详情

#### 11. 数据持久化
- 自动保存到`.idea/sql-records.json`文件
- 支持最大1000条记录（自动清理最旧记录）
- 项目关闭后数据依然保留

#### 12. 清空功能
- 一键清空所有历史记录
- 支持清空后重新开始监控

### 五、高级特性

#### 13. 大JSON支持
- 支持超大JSON参数（3376字符+）
- 正确处理跨多行的JSON数据
- 完整保留JSON结构用于SQL生成

#### 14. 智能缓冲机制
- 200KB主缓冲区用于SQL日志捕获
- 50KB上下文缓冲区用于API路径提取
- 自动清理机制，防止内存溢出

#### 15. 异步处理
- SQL解析在后台线程执行
- 不阻塞UI线程
- 提升响应速度

### 六、使用场景

1. 慢查询分析：找出执行频繁或复杂的SQL
2. API调用追踪：查看某个API执行了哪些SQL
3. 表访问监控：监控特定表的访问情况
4. N+1查询检测：发现潜在的查询性能问题
5. 参数调试：查看SQL参数绑定是否正确
6. 数据验证：复制可执行SQL在数据库中验证
7. 代码审查：检查SQL执行的合理性

### 七、技术特点

- 零性能损耗：异步处理，不影响IDEA运行
- 高解析准确率：测试51条SQL 100%解析成功
- 跨项目隔离：每个项目独立监控，互不干扰
- 自动监听管理：进程启动时自动附加监听器

![](https://shuyixiao.oss-cn-hangzhou.aliyuncs.com/BlogPicture/image-20251031105834680.png)