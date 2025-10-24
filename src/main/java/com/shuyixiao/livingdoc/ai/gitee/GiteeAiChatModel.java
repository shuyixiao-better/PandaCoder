package com.shuyixiao.livingdoc.ai.gitee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gitee AI (模力方舟) ChatModel 实现
 * 
 * <p>API文档: <a href="https://ai.gitee.com/docs/products/apis">https://ai.gitee.com/docs/products/apis</a>
 * 
 * <p>支持的模型：
 * <ul>
 *   <li>qwen-plus - 通义千问Plus（推荐）</li>
 *   <li>qwen-max - 通义千问Max（最强性能）</li>
 *   <li>deepseek-chat - DeepSeek（编程能力强）</li>
 *   <li>glm-4 - 智谱GLM4（多模态）</li>
 *   <li>doubao-pro - 字节豆包Pro（性价比高）</li>
 * </ul>
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class GiteeAiChatModel {
    
    private static final Logger log = LoggerFactory.getLogger(GiteeAiChatModel.class);
    
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public GiteeAiChatModel(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 调用 Chat Completion API
     * 
     * @param messages 对话消息列表
     * @return AI响应内容
     */
    public String chat(List<ChatMessage> messages) {
        return chat(messages, null);
    }
    
    /**
     * 调用 Chat Completion API（带参数）
     * 
     * @param messages 对话消息列表
     * @param options 可选参数
     * @return AI响应内容
     */
    public String chat(List<ChatMessage> messages, ChatOptions options) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", convertMessages(messages));
            
            if (options != null) {
                if (options.getTemperature() != null) {
                    requestBody.put("temperature", options.getTemperature());
                }
                if (options.getMaxTokens() != null) {
                    requestBody.put("max_tokens", options.getMaxTokens());
                }
                if (options.getTopP() != null) {
                    requestBody.put("top_p", options.getTopP());
                }
            } else {
                // 默认参数
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2000);
            }
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-Failover-Enabled", "true")  // 启用故障转移
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(60))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("API call failed: " + response.statusCode() + " - " + response.body());
            }
            
            // 解析响应
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in response");
            }
            
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // 记录Token使用量
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
            if (usage != null) {
                log.debug("Token usage - Prompt: {}, Completion: {}, Total: {}",
                    usage.get("prompt_tokens"),
                    usage.get("completion_tokens"),
                    usage.get("total_tokens"));
            }
            
            return content;
            
        } catch (Exception e) {
            log.error("Failed to call Gitee AI chat API", e);
            throw new RuntimeException("Failed to call Gitee AI chat API", e);
        }
    }
    
    /**
     * 流式调用（实时返回）
     * 
     * @param messages 对话消息列表
     * @param streamHandler 流式响应处理器
     */
    public void chatStream(List<ChatMessage> messages, StreamHandler streamHandler) {
        chatStream(messages, null, streamHandler);
    }
    
    /**
     * 流式调用（带参数）
     */
    public void chatStream(List<ChatMessage> messages, ChatOptions options, StreamHandler streamHandler) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", convertMessages(messages));
            requestBody.put("stream", true);
            
            if (options != null) {
                if (options.getTemperature() != null) {
                    requestBody.put("temperature", options.getTemperature());
                }
                if (options.getMaxTokens() != null) {
                    requestBody.put("max_tokens", options.getMaxTokens());
                }
            } else {
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2000);
            }
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-Failover-Enabled", "true")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(120))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 处理SSE流式响应
            String[] lines = response.body().split("\n");
            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        streamHandler.onComplete();
                        break;
                    }
                    
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = (String) delta.get("content");
                                streamHandler.onChunk(content);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse chunk: {}", data, e);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to call Gitee AI chat stream API", e);
            streamHandler.onError(e);
        }
    }
    
    /**
     * 转换消息格式
     */
    private List<Map<String, String>> convertMessages(List<ChatMessage> messages) {
        List<Map<String, String>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("role", msg.getRole().toString().toLowerCase());
            messageMap.put("content", msg.getContent());
            result.add(messageMap);
        }
        return result;
    }
    
    /**
     * 聊天消息
     */
    public static class ChatMessage {
        private Role role;
        private String content;
        
        public ChatMessage(Role role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public static ChatMessage user(String content) {
            return new ChatMessage(Role.USER, content);
        }
        
        public static ChatMessage assistant(String content) {
            return new ChatMessage(Role.ASSISTANT, content);
        }
        
        public static ChatMessage system(String content) {
            return new ChatMessage(Role.SYSTEM, content);
        }
        
        public Role getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    /**
     * 消息角色
     */
    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
    
    /**
     * 聊天选项
     */
    public static class ChatOptions {
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Double temperature;
            private Integer maxTokens;
            private Double topP;
            
            public Builder temperature(double temperature) {
                this.temperature = temperature;
                return this;
            }
            
            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }
            
            public Builder topP(double topP) {
                this.topP = topP;
                return this;
            }
            
            public ChatOptions build() {
                ChatOptions options = new ChatOptions();
                options.temperature = this.temperature;
                options.maxTokens = this.maxTokens;
                options.topP = this.topP;
                return options;
            }
        }
        
        public Double getTemperature() {
            return temperature;
        }
        
        public Integer getMaxTokens() {
            return maxTokens;
        }
        
        public Double getTopP() {
            return topP;
        }
    }
    
    /**
     * 流式响应处理器
     */
    public interface StreamHandler {
        void onChunk(String chunk);
        void onComplete();
        void onError(Exception e);
    }
}

