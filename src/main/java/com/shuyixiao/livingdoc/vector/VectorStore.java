package com.shuyixiao.livingdoc.vector;

import java.util.List;
import java.util.Map;

/**
 * 向量数据库抽象接口
 * 
 * <p>提供统一的向量存储和检索接口，支持多种向量数据库实现：
 * <ul>
 *   <li>Elasticsearch（默认，推荐生产环境）</li>
 *   <li>PGVector（PostgreSQL扩展）</li>
 *   <li>Chroma（专业RAG向量库）</li>
 *   <li>Redis（高性能缓存）</li>
 *   <li>Simple（内存/文件，开发测试）</li>
 * </ul>
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public interface VectorStore {
    
    /**
     * 存储单个文档向量
     * 
     * @param id 文档唯一标识
     * @param vector 向量表示（float数组）
     * @param content 文档内容
     * @param metadata 元数据（可选）
     */
    void store(String id, float[] vector, String content, Map<String, Object> metadata);
    
    /**
     * 批量存储文档向量
     * 
     * @param documents 文档列表
     */
    void storeBatch(List<VectorDocument> documents);
    
    /**
     * 向量相似度搜索
     * 
     * @param queryVector 查询向量
     * @param topK 返回最相似的K个结果
     * @return 搜索结果列表，按相似度降序排列
     */
    List<SearchResult> search(float[] queryVector, int topK);
    
    /**
     * 向量相似度搜索（带过滤条件）
     * 
     * @param queryVector 查询向量
     * @param topK 返回最相似的K个结果
     * @param filter 元数据过滤条件
     * @return 搜索结果列表
     */
    List<SearchResult> search(float[] queryVector, int topK, Map<String, Object> filter);
    
    /**
     * 向量相似度搜索（带相似度阈值）
     * 
     * @param queryVector 查询向量
     * @param topK 返回最相似的K个结果
     * @param similarityThreshold 相似度阈值（0-1之间）
     * @return 搜索结果列表
     */
    List<SearchResult> searchWithThreshold(float[] queryVector, int topK, double similarityThreshold);
    
    /**
     * 根据ID获取文档
     * 
     * @param id 文档ID
     * @return 向量文档，如果不存在返回null
     */
    VectorDocument getById(String id);
    
    /**
     * 更新文档
     * 
     * @param id 文档ID
     * @param vector 新的向量
     * @param content 新的内容
     * @param metadata 新的元数据
     */
    void update(String id, float[] vector, String content, Map<String, Object> metadata);
    
    /**
     * 删除文档
     * 
     * @param id 文档ID
     */
    void delete(String id);
    
    /**
     * 批量删除文档
     * 
     * @param ids 文档ID列表
     */
    void deleteBatch(List<String> ids);
    
    /**
     * 清空所有文档
     */
    void clear();
    
    /**
     * 获取文档总数
     * 
     * @return 文档数量
     */
    long count();
    
    /**
     * 检查向量库是否健康
     * 
     * @return true表示健康，false表示存在问题
     */
    boolean healthCheck();
    
    /**
     * 初始化向量库（创建索引等）
     */
    void initialize();
    
    /**
     * 关闭向量库连接
     */
    void close();
}

