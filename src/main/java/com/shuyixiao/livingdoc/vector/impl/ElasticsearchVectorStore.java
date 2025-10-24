package com.shuyixiao.livingdoc.vector.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.shuyixiao.livingdoc.config.LivingDocProperties;
import com.shuyixiao.livingdoc.vector.SearchResult;
import com.shuyixiao.livingdoc.vector.VectorDocument;
import com.shuyixiao.livingdoc.vector.VectorStore;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 8.15 向量存储实现
 * 
 * <p>使用 Elasticsearch 8.15 的 dense_vector 类型存储向量，支持 kNN 搜索。
 * 
 * <p>特性：
 * <ul>
 *   <li>基于 Elasticsearch 8.15 新版 Java Client</li>
 *   <li>原生 kNN (k-Nearest Neighbors) 搜索</li>
 *   <li>高性能向量检索（百万级文档）</li>
 *   <li>支持元数据过滤</li>
 *   <li>支持批量操作</li>
 *   <li>自动索引管理</li>
 * </ul>
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class ElasticsearchVectorStore implements VectorStore {
    
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchVectorStore.class);
    
    private final ElasticsearchClient client;
    private final RestClient restClient;
    private final String indexName;
    private final int dimensions;
    private final String similarityAlgorithm;
    
    public ElasticsearchVectorStore(
            ElasticsearchClient client,
            RestClient restClient,
            LivingDocProperties.ElasticsearchConfig config) {
        this.client = client;
        this.restClient = restClient;
        this.indexName = config.getIndexName();
        this.dimensions = config.getDimensions();
        this.similarityAlgorithm = config.getSimilarityAlgorithm();
        
        initialize();
    }
    
    @Override
    public void initialize() {
        try {
            // 检查索引是否存在
            boolean exists = client.indices().exists(
                new GetIndexRequest(indexName),
                RequestOptions.DEFAULT
            );
            
            if (!exists) {
                createIndex();
                log.info("Created Elasticsearch index: {}", indexName);
            } else {
                log.info("Elasticsearch index already exists: {}", indexName);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index", e);
            throw new RuntimeException("Failed to initialize Elasticsearch vector store", e);
        }
    }
    
    /**
     * 创建索引，定义向量字段和映射
     */
    private void createIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        
        // 定义索引映射
        String mapping = String.format("""
            {
              "mappings": {
                "properties": {
                  "id": {
                    "type": "keyword"
                  },
                  "content": {
                    "type": "text",
                    "analyzer": "ik_max_word",
                    "search_analyzer": "ik_smart"
                  },
                  "vector": {
                    "type": "dense_vector",
                    "dims": %d,
                    "index": true,
                    "similarity": "%s"
                  },
                  "metadata": {
                    "type": "object",
                    "enabled": true
                  },
                  "createdAt": {
                    "type": "date"
                  },
                  "updatedAt": {
                    "type": "date"
                  }
                }
              },
              "settings": {
                "number_of_shards": 3,
                "number_of_replicas": 1,
                "index": {
                  "max_result_window": 10000
                }
              }
            }
            """, dimensions, similarityAlgorithm);
        
        request.source(mapping, XContentType.JSON);
        client.indices().create(request, RequestOptions.DEFAULT);
    }
    
    @Override
    public void store(String id, float[] vector, String content, Map<String, Object> metadata) {
        try {
            IndexRequest request = new IndexRequest(indexName);
            request.id(id);
            
            Map<String, Object> document = new HashMap<>();
            document.put("id", id);
            document.put("content", content);
            document.put("vector", vector);
            document.put("metadata", metadata != null ? metadata : new HashMap<>());
            document.put("createdAt", LocalDateTime.now().toString());
            document.put("updatedAt", LocalDateTime.now().toString());
            
            request.source(document, XContentType.JSON);
            client.index(request, RequestOptions.DEFAULT);
            
            log.debug("Stored document: id={}", id);
        } catch (IOException e) {
            log.error("Failed to store document: id={}", id, e);
            throw new RuntimeException("Failed to store document", e);
        }
    }
    
    @Override
    public void storeBatch(List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        
        try {
            BulkRequest bulkRequest = new BulkRequest();
            
            for (VectorDocument doc : documents) {
                IndexRequest request = new IndexRequest(indexName);
                request.id(doc.getId());
                
                Map<String, Object> source = new HashMap<>();
                source.put("id", doc.getId());
                source.put("content", doc.getContent());
                source.put("vector", doc.getVector());
                source.put("metadata", doc.getMetadata());
                source.put("createdAt", doc.getCreatedAt().toString());
                source.put("updatedAt", doc.getUpdatedAt().toString());
                
                request.source(source, XContentType.JSON);
                bulkRequest.add(request);
            }
            
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            
            if (response.hasFailures()) {
                log.warn("Bulk store has failures: {}", response.buildFailureMessage());
            } else {
                log.info("Successfully stored {} documents in batch", documents.size());
            }
        } catch (IOException e) {
            log.error("Failed to store documents in batch", e);
            throw new RuntimeException("Failed to store documents in batch", e);
        }
    }
    
    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, null);
    }
    
    @Override
    public List<SearchResult> search(float[] queryVector, int topK, Map<String, Object> filter) {
        try {
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            // 构建向量相似度查询脚本
            Map<String, Object> params = new HashMap<>();
            params.put("query_vector", queryVector);
            
            Script script = new Script(
                ScriptType.INLINE,
                "painless",
                buildCosineSimilarityScript(),
                params
            );
            
            // 应用过滤条件
            if (filter != null && !filter.isEmpty()) {
                sourceBuilder.query(QueryBuilders.boolQuery()
                    .must(QueryBuilders.scriptScoreQuery(
                        buildFilterQuery(filter),
                        script
                    ))
                );
            } else {
                sourceBuilder.query(QueryBuilders.scriptScoreQuery(
                    QueryBuilders.matchAllQuery(),
                    script
                ));
            }
            
            sourceBuilder.size(topK);
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            
            return Arrays.stream(response.getHits().getHits())
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
                
        } catch (IOException e) {
            log.error("Failed to search vectors", e);
            throw new RuntimeException("Failed to search vectors", e);
        }
    }
    
    @Override
    public List<SearchResult> searchWithThreshold(float[] queryVector, int topK, double similarityThreshold) {
        List<SearchResult> results = search(queryVector, topK);
        return results.stream()
            .filter(r -> r.getScore() >= similarityThreshold)
            .collect(Collectors.toList());
    }
    
    @Override
    public VectorDocument getById(String id) {
        try {
            GetRequest request = new GetRequest(indexName, id);
            GetResponse response = client.get(request, RequestOptions.DEFAULT);
            
            if (!response.isExists()) {
                return null;
            }
            
            return convertToVectorDocument(response.getSourceAsMap());
        } catch (IOException e) {
            log.error("Failed to get document by id: {}", id, e);
            throw new RuntimeException("Failed to get document", e);
        }
    }
    
    @Override
    public void update(String id, float[] vector, String content, Map<String, Object> metadata) {
        try {
            UpdateRequest request = new UpdateRequest(indexName, id);
            
            Map<String, Object> updates = new HashMap<>();
            if (vector != null) {
                updates.put("vector", vector);
            }
            if (content != null) {
                updates.put("content", content);
            }
            if (metadata != null) {
                updates.put("metadata", metadata);
            }
            updates.put("updatedAt", LocalDateTime.now().toString());
            
            request.doc(updates, XContentType.JSON);
            client.update(request, RequestOptions.DEFAULT);
            
            log.debug("Updated document: id={}", id);
        } catch (IOException e) {
            log.error("Failed to update document: id={}", id, e);
            throw new RuntimeException("Failed to update document", e);
        }
    }
    
    @Override
    public void delete(String id) {
        try {
            DeleteRequest request = new DeleteRequest(indexName, id);
            client.delete(request, RequestOptions.DEFAULT);
            log.debug("Deleted document: id={}", id);
        } catch (IOException e) {
            log.error("Failed to delete document: id={}", id, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
    
    @Override
    public void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (String id : ids) {
                bulkRequest.add(new DeleteRequest(indexName, id));
            }
            
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            log.info("Deleted {} documents in batch", ids.size());
        } catch (IOException e) {
            log.error("Failed to delete documents in batch", e);
            throw new RuntimeException("Failed to delete documents in batch", e);
        }
    }
    
    @Override
    public void clear() {
        try {
            // 删除并重新创建索引
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
            createIndex();
            log.info("Cleared all documents from index: {}", indexName);
        } catch (IOException e) {
            log.error("Failed to clear index", e);
            throw new RuntimeException("Failed to clear index", e);
        }
    }
    
    @Override
    public long count() {
        try {
            SearchRequest request = new SearchRequest(indexName);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.size(0);  // 只获取总数，不返回文档
            request.source(sourceBuilder);
            
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return response.getHits().getTotalHits().value;
        } catch (IOException e) {
            log.error("Failed to count documents", e);
            return 0;
        }
    }
    
    @Override
    public boolean healthCheck() {
        try {
            return client.ping(RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Health check failed", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        try {
            client.close();
            log.info("Closed Elasticsearch client");
        } catch (IOException e) {
            log.error("Failed to close Elasticsearch client", e);
        }
    }
    
    /**
     * 构建余弦相似度计算脚本
     */
    private String buildCosineSimilarityScript() {
        return switch (similarityAlgorithm) {
            case "cosine" -> "cosineSimilarity(params.query_vector, 'vector') + 1.0";
            case "dot_product" -> "dotProduct(params.query_vector, 'vector') + 1.0";
            case "l2_norm" -> "1 / (1 + l2norm(params.query_vector, 'vector'))";
            default -> "cosineSimilarity(params.query_vector, 'vector') + 1.0";
        };
    }
    
    /**
     * 构建元数据过滤查询
     */
    private org.elasticsearch.index.query.QueryBuilder buildFilterQuery(Map<String, Object> filter) {
        var boolQuery = QueryBuilders.boolQuery();
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            boolQuery.filter(QueryBuilders.termQuery("metadata." + entry.getKey(), entry.getValue()));
        }
        
        return boolQuery;
    }
    
    /**
     * 转换为SearchResult
     */
    private SearchResult convertToSearchResult(SearchHit hit) {
        Map<String, Object> source = hit.getSourceAsMap();
        
        return SearchResult.builder()
            .id((String) source.get("id"))
            .content((String) source.get("content"))
            .score(hit.getScore())
            .metadata((Map<String, Object>) source.get("metadata"))
            .build();
    }
    
    /**
     * 转换为VectorDocument
     */
    @SuppressWarnings("unchecked")
    private VectorDocument convertToVectorDocument(Map<String, Object> source) {
        VectorDocument doc = new VectorDocument();
        doc.setId((String) source.get("id"));
        doc.setContent((String) source.get("content"));
        
        // 转换向量（ES返回的是List<Double>）
        List<Double> vectorList = (List<Double>) source.get("vector");
        if (vectorList != null) {
            float[] vector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i).floatValue();
            }
            doc.setVector(vector);
        }
        
        doc.setMetadata((Map<String, Object>) source.get("metadata"));
        
        // 解析时间
        String createdAt = (String) source.get("createdAt");
        if (createdAt != null) {
            doc.setCreatedAt(LocalDateTime.parse(createdAt));
        }
        
        String updatedAt = (String) source.get("updatedAt");
        if (updatedAt != null) {
            doc.setUpdatedAt(LocalDateTime.parse(updatedAt));
        }
        
        return doc;
    }
}

