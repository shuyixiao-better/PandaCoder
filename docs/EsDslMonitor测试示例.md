# ES DSL Monitor 测试示例

## 📝 测试前准备

### 1. 启动 Elasticsearch

确保 Elasticsearch 服务正在运行：

```bash
# Docker 方式
docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.17.0

# 验证服务
curl http://localhost:9200
```

### 2. 配置日志级别

在 Spring Boot 项目的 `application.yml` 中添加：

```yaml
logging:
  level:
    org.elasticsearch.client: DEBUG
    org.springframework.data.elasticsearch: DEBUG
    org.springframework.data.elasticsearch.core: DEBUG
```

## 🧪 测试用例

### 测试用例 1：简单查询

**代码示例（Java + Spring Data Elasticsearch）：**

```java
@Service
public class UserService {
    
    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;
    
    public List<User> searchUsers(String name) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchQuery("name", name))
            .build();
        
        SearchHits<User> searchHits = elasticsearchTemplate.search(searchQuery, User.class);
        return searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }
}
```

**预期输出（控制台）：**

```
2024-10-17 10:30:45.123  DEBUG o.e.c.RestClient - request [POST http://localhost:9200/users/_search] {"query":{"match":{"name":"张三"}}}
```

**插件捕获结果：**

- 方法: POST
- 索引: users
- DSL: 
  ```json
  {
    "query": {
      "match": {
        "name": "张三"
      }
    }
  }
  ```

---

### 测试用例 2：复杂聚合查询

**代码示例：**

```java
public Map<String, Long> getUserCountByDepartment() {
    NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .addAggregation(AggregationBuilders.terms("by_department")
            .field("department.keyword")
            .size(10))
        .build();
    
    SearchHits<User> searchHits = elasticsearchTemplate.search(searchQuery, User.class);
    // 处理聚合结果...
}
```

**预期输出：**

```
POST http://localhost:9200/users/_search
{
  "query": {
    "match_all": {}
  },
  "aggs": {
    "by_department": {
      "terms": {
        "field": "department.keyword",
        "size": 10
      }
    }
  }
}
```

**插件捕获结果：**

- 方法: POST
- 索引: users
- DSL: 包含聚合查询的完整 JSON

---

### 测试用例 3：分页查询

**代码示例：**

```java
public Page<User> getUsersByPage(int page, int size) {
    NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .withPageable(PageRequest.of(page, size))
        .build();
    
    SearchHits<User> searchHits = elasticsearchTemplate.search(searchQuery, User.class);
    return SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());
}
```

**预期输出：**

```
POST http://localhost:9200/users/_search
{
  "query": {
    "match_all": {}
  },
  "from": 0,
  "size": 20
}
```

---

### 测试用例 4：布尔查询

**代码示例：**

```java
public List<User> searchActiveUsersInDepartment(String department) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("active", true))
        .must(QueryBuilders.termQuery("department.keyword", department));
    
    NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withQuery(boolQuery)
        .build();
    
    return elasticsearchTemplate.search(searchQuery, User.class)
        .stream()
        .map(SearchHit::getContent)
        .collect(Collectors.toList());
}
```

**预期 DSL：**

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "term": {
            "active": true
          }
        },
        {
          "term": {
            "department.keyword": "研发部"
          }
        }
      ]
    }
  }
}
```

---

### 测试用例 5：范围查询

**代码示例：**

```java
public List<User> getUsersByAgeRange(int minAge, int maxAge) {
    RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("age")
        .gte(minAge)
        .lte(maxAge);
    
    NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withQuery(rangeQuery)
        .build();
    
    return elasticsearchTemplate.search(searchQuery, User.class)
        .stream()
        .map(SearchHit::getContent)
        .collect(Collectors.toList());
}
```

**预期 DSL：**

```json
{
  "query": {
    "range": {
      "age": {
        "gte": 25,
        "lte": 35
      }
    }
  }
}
```

---

## 🎯 测试步骤

### 步骤 1：启动插件

1. 打开 IntelliJ IDEA
2. 打开包含 Elasticsearch 的项目
3. 在底部工具栏找到 "ES DSL Monitor"
4. 确保"启用ES监听"已勾选

### 步骤 2：运行测试

创建测试类：

```java
@SpringBootTest
public class EsMonitorTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    public void testSearchUsers() {
        // 这会触发 ES 查询，插件会自动捕获
        List<User> users = userService.searchUsers("张三");
        System.out.println("找到 " + users.size() + " 个用户");
    }
    
    @Test
    public void testMultipleQueries() {
        // 触发多个查询
        userService.searchUsers("张三");
        userService.getUsersByPage(0, 10);
        userService.getUserCountByDepartment();
        userService.searchActiveUsersInDepartment("研发部");
        userService.getUsersByAgeRange(25, 35);
    }
}
```

### 步骤 3：查看结果

运行测试后：

1. 切换到 "ES DSL Monitor" 工具窗口
2. 查看捕获到的查询列表
3. 点击任意查询查看详细信息
4. 使用搜索和过滤功能定位特定查询

## 🔍 验证清单

- [ ] 能够看到捕获的查询记录
- [ ] 查询列表显示正确的方法（POST/GET等）
- [ ] 索引名称显示正确
- [ ] DSL 查询格式化显示
- [ ] 可以点击查看详细信息
- [ ] 搜索功能正常工作
- [ ] 方法过滤功能正常
- [ ] 时间过滤功能正常
- [ ] 可以导出选中的查询
- [ ] 查询记录在重启 IDEA 后仍然保留

## 💡 调试技巧

### 如果没有捕获到查询

1. **检查日志级别**
   ```yaml
   logging:
     level:
       root: INFO
       org.elasticsearch: DEBUG  # 确保这行存在
   ```

2. **手动打印日志**
   ```java
   System.out.println("Elasticsearch Query: " + query.toString());
   ```

3. **使用 RestHighLevelClient**
   ```java
   @Bean
   public RestHighLevelClient client() {
       ClientConfiguration config = ClientConfiguration.builder()
           .connectedTo("localhost:9200")
           .withClientConfigurer(
               RestClientBuilder.HttpClientConfigCallback config -> {
                   // 启用请求日志
                   return config;
               }
           )
           .build();
       return RestClients.create(config).rest();
   }
   ```

### 查看原始日志

如果插件解析失败，可以查看 IDEA 的原始控制台输出：

1. Run 窗口中查看完整日志
2. 确认是否有 Elasticsearch 相关的日志输出
3. 将日志复制给开发者反馈

## 📊 性能测试

### 大量查询测试

```java
@Test
public void testMassiveQueries() {
    for (int i = 0; i < 100; i++) {
        userService.searchUsers("用户" + i);
    }
    // 检查插件是否能正常处理大量查询
}
```

**预期结果：**
- 插件保持响应
- 查询记录正确捕获
- 内存使用稳定
- 最多保存 1000 条记录

## 🐛 常见问题

### Q1: 查询显示为 "N/A"

**A:** 这是正常的，表示某些信息无法从日志中提取。常见情况：
- 索引名称在某些查询格式中不明显
- 执行时间在日志中不可用

### Q2: 查询记录丢失

**A:** 检查以下内容：
- 记录是否超过 1000 条（会自动清理旧记录）
- 是否手动清空了记录
- 项目的 `.idea` 目录是否被删除

### Q3: DSL 格式不正确

**A:** 插件会尽力格式化 JSON，但某些复杂格式可能不完美。可以：
- 使用"复制 DSL"功能
- 在外部 JSON 格式化工具中美化

## 📚 参考资源

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)
- [ES DSL Monitor 使用指南](EsDslMonitor使用指南.md)

---

**祝测试顺利！** 🚀

如有问题，请联系：yixiaoshu88@163.com

