package com.shuyixiao.gitstat.weekly.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.shuyixiao.gitstat.weekly.config.MongoDBConfig;
import com.shuyixiao.gitstat.weekly.model.WeeklyReportArchive;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 周报MongoDB归档服务
 * 负责将周报数据上传到MongoDB数据库
 * 
 * @author PandaCoder Team
 * @since 2.2.0
 */
@Service(Service.Level.PROJECT)
public final class WeeklyReportMongoService {
    
    private final Project project;
    private MongoClient mongoClient;
    
    public WeeklyReportMongoService(Project project) {
        this.project = project;
    }
    
    /**
     * 获取MongoDB客户端
     * 如果客户端不存在或已关闭，则创建新的客户端
     */
    private MongoClient getMongoClient() {
        if (mongoClient == null) {
            mongoClient = createMongoClient();
        }
        return mongoClient;
    }
    
    /**
     * 创建MongoDB客户端
     */
    private MongoClient createMongoClient() {
        try {
            String connectionString = MongoDBConfig.getConnectionString();
            
            // 使用连接字符串创建客户端（推荐方式，支持所有MongoDB URL格式）
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .applyConnectionString(new com.mongodb.ConnectionString(connectionString))
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(MongoDBConfig.getMaxPoolSize())
                               .minSize(MongoDBConfig.getMinPoolSize())
                    )
                    .applyToSocketSettings(builder ->
                        builder.connectTimeout(MongoDBConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                               .readTimeout(MongoDBConfig.getSocketTimeout(), TimeUnit.MILLISECONDS)
                    );
            
            MongoClientSettings settings = settingsBuilder.build();
            return MongoClients.create(settings);
            
        } catch (Exception e) {
            throw new RuntimeException("创建MongoDB客户端失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 测试MongoDB连接
     *
     * @return 连接成功返回true，否则返回false
     */
    public boolean testConnection() {
        try {
            System.out.println("[INFO] 开始测试MongoDB连接...");
            System.out.println("[INFO] 数据库: " + MongoDBConfig.getDatabase());
            System.out.println("[INFO] 集合: " + MongoDBConfig.getCollection());

            // 获取连接字符串（隐藏密码）
            String connectionString = MongoDBConfig.getConnectionString();
            String safeConnectionString = connectionString.replaceAll(":[^:@]+@", ":****@");
            System.out.println("[INFO] 连接字符串: " + safeConnectionString);

            MongoClient client = getMongoClient();
            MongoDatabase database = client.getDatabase(MongoDBConfig.getDatabase());
            // 尝试执行一个简单的命令来测试连接
            database.runCommand(new Document("ping", 1));
            System.out.println("[INFO] MongoDB连接测试成功！");
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] MongoDB连接测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 归档周报到MongoDB
     * 
     * @param archive 周报归档对象
     * @return 归档成功返回true，否则返回false
     * @throws Exception 归档过程中的异常
     */
    public boolean archiveReport(@NotNull WeeklyReportArchive archive) throws Exception {
        try {
            MongoClient client = getMongoClient();
            MongoDatabase database = client.getDatabase(MongoDBConfig.getDatabase());
            MongoCollection<Document> collection = database.getCollection(MongoDBConfig.getCollection());
            
            // 转换为MongoDB文档
            Map<String, Object> docMap = archive.toDocument();
            Document document = new Document(docMap);
            
            // 插入文档
            collection.insertOne(document);
            
            System.out.println("[INFO] 周报归档成功，文档ID: " + document.get("_id"));
            return true;
            
        } catch (Exception e) {
            System.err.println("[ERROR] 周报归档失败: " + e.getMessage());
            throw new Exception("周报归档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查询指定日期范围的周报
     * 
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 周报文档列表
     */
    public List<Document> queryReportsByDateRange(String startDate, String endDate) {
        try {
            MongoClient client = getMongoClient();
            MongoDatabase database = client.getDatabase(MongoDBConfig.getDatabase());
            MongoCollection<Document> collection = database.getCollection(MongoDBConfig.getCollection());
            
            // 构建查询条件
            Document query = new Document();
            if (startDate != null && endDate != null) {
                query.append("weekStartDate", new Document("$gte", startDate))
                     .append("weekEndDate", new Document("$lte", endDate));
            }
            
            // 执行查询
            List<Document> results = new ArrayList<>();
            collection.find(query).into(results);
            
            return results;
            
        } catch (Exception e) {
            System.err.println("[ERROR] 查询周报失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 查询指定项目的所有周报
     * 
     * @param projectName 项目名称
     * @return 周报文档列表
     */
    public List<Document> queryReportsByProject(String projectName) {
        try {
            MongoClient client = getMongoClient();
            MongoDatabase database = client.getDatabase(MongoDBConfig.getDatabase());
            MongoCollection<Document> collection = database.getCollection(MongoDBConfig.getCollection());
            
            // 构建查询条件
            Document query = new Document("projectName", projectName);
            
            // 执行查询
            List<Document> results = new ArrayList<>();
            collection.find(query).into(results);
            
            return results;
            
        } catch (Exception e) {
            System.err.println("[ERROR] 查询周报失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取周报总数
     * 
     * @return 周报总数
     */
    public long getReportCount() {
        try {
            MongoClient client = getMongoClient();
            MongoDatabase database = client.getDatabase(MongoDBConfig.getDatabase());
            MongoCollection<Document> collection = database.getCollection(MongoDBConfig.getCollection());
            
            return collection.countDocuments();
            
        } catch (Exception e) {
            System.err.println("[ERROR] 获取周报总数失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 关闭MongoDB连接
     */
    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                mongoClient = null;
                System.out.println("[INFO] MongoDB连接已关闭");
            } catch (Exception e) {
                System.err.println("[ERROR] 关闭MongoDB连接失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查MongoDB配置是否可用
     * 
     * @return 配置可用返回true，否则返回false
     */
    public boolean isConfigured() {
        return MongoDBConfig.isConfigured();
    }
}

