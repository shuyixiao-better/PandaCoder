package com.shuyixiao;

import com.shuyixiao.setting.PluginSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 国内大模型翻译API
 * 支持通义千问、文心一言、智谱等国内主流AI模型
 */
public class DomesticAITranslationAPI {

    // 通义千问API
    private static final String QIANWEN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    
    // 文心一言API
    private static final String WENXIN_API_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/eb-instant";
    
    // 智谱API
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    // 腾讯混元API（码云）
    private static final String HUNYUAN_API_URL = "https://ai.gitee.com/api/v1/chat/completions";

    /**
     * 使用国内大模型进行翻译
     * @param text 待翻译的中文文本
     * @return 翻译后的英文文本
     * @throws IOException 翻译过程中的异常
     */
    public static String translate(String text) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String modelType = settings.getDomesticAIModel();
        String apiKey = settings.getDomesticAIApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("国内大模型API密钥不能为空");
        }
        
        switch (modelType) {
            case "qianwen":
                return translateWithQianwen(text, apiKey);
            case "wenxin":
                return translateWithWenxin(text, apiKey);
            case "zhipu":
                return translateWithZhipu(text, apiKey);
            case "hunyuan":
                return translateWithHunyuan(text, apiKey);
            default:
                return translateWithQianwen(text, apiKey); // 默认使用通义千问
        }
    }
    
    /**
     * 使用国内大模型进行AI文本分析（非纯翻译场景）
     * @param prompt 分析提示词
     * @param instruction 具体指令
     * @return AI分析结果
     * @throws IOException 分析过程中的异常
     */
    public String translateTextWithAI(String prompt, String instruction) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String modelType = settings.getDomesticAIModel();
        String apiKey = settings.getDomesticAIApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("国内大模型API密钥不能为空");
        }
        
        // 构建AI分析的提示词，结合用户指令
        String fullPrompt = instruction + "\n\n" + prompt;
        
        switch (modelType) {
            case "qianwen":
                return analyzeWithQianwen(fullPrompt, apiKey);
            case "wenxin":
                return analyzeWithWenxin(fullPrompt, apiKey);
            case "zhipu":
                return analyzeWithZhipu(fullPrompt, apiKey);
            case "hunyuan":
                return analyzeWithHunyuan(fullPrompt, apiKey);
            default:
                return analyzeWithQianwen(fullPrompt, apiKey); // 默认使用通义千问
        }
    }

    /**
     * 使用通义千问进行翻译
     */
    private static String translateWithQianwen(String text, String apiKey) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String prompt = buildTranslationPrompt(text, settings);
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "qwen-turbo");
        
        JsonObject input = new JsonObject();
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        input.add("messages", messages);
        requestBody.add("input", input);
        
        JsonObject parameters = new JsonObject();
        parameters.addProperty("max_tokens", 100);
        parameters.addProperty("temperature", 0.1);
        requestBody.add("parameters", parameters);
        
        return sendRequest(QIANWEN_API_URL, requestBody.toString(), apiKey, "qianwen");
    }
    
    /**
     * 使用文心一言进行翻译
     */
    private static String translateWithWenxin(String text, String apiKey) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String prompt = buildTranslationPrompt(text, settings);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.1);
        requestBody.addProperty("max_output_tokens", 100);
        
        String urlWithToken = WENXIN_API_URL + "?access_token=" + apiKey;
        return sendRequest(urlWithToken, requestBody.toString(), "", "wenxin");
    }
    
    /**
     * 使用腾讯混元进行翻译
     */
    private static String translateWithHunyuan(String text, String apiKey) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String prompt = buildTranslationPrompt(text, settings);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "Hunyuan-MT-Chimera-7B");
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);
        requestBody.addProperty("max_tokens", 100);
        requestBody.addProperty("temperature", 0.1);

        return sendRequest(HUNYUAN_API_URL, requestBody.toString(), apiKey, "hunyuan");
    }

    /**
     * 使用智谱AI进行翻译
     */
    private static String translateWithZhipu(String text, String apiKey) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String prompt = buildTranslationPrompt(text, settings);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "glm-4");
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.1);
        requestBody.addProperty("max_tokens", 100);
        
        return sendRequest(ZHIPU_API_URL, requestBody.toString(), apiKey, "zhipu");
    }

    /**
     * 使用通义千问进行AI分析
     */
    private String analyzeWithQianwen(String prompt, String apiKey) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "qwen-turbo");
        
        JsonObject input = new JsonObject();
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        input.add("messages", messages);
        requestBody.add("input", input);
        
        JsonObject parameters = new JsonObject();
        parameters.addProperty("max_tokens", 1000); // 增加token数量用于详细分析
        parameters.addProperty("temperature", 0.3); // 稍微提高创造性
        requestBody.add("parameters", parameters);
        
        return sendRequest(QIANWEN_API_URL, requestBody.toString(), apiKey, "qianwen");
    }
    
    /**
     * 使用文心一言进行AI分析
     */
    private String analyzeWithWenxin(String prompt, String apiKey) throws IOException {
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.3);
        requestBody.addProperty("max_output_tokens", 1000);
        
        String urlWithToken = WENXIN_API_URL + "?access_token=" + apiKey;
        return sendRequest(urlWithToken, requestBody.toString(), "", "wenxin");
    }
    
    /**
     * 使用腾讯混元进行分析
     */
    private String analyzeWithHunyuan(String prompt, String apiKey) throws IOException {
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "Hunyuan-MT-Chimera-7B");
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);
        requestBody.addProperty("max_tokens", 1000);
        requestBody.addProperty("temperature", 0.3);

        return sendRequest(HUNYUAN_API_URL, requestBody.toString(), apiKey, "hunyuan");
    }

    /**
     * 使用智谱AI进行分析
     */
    private String analyzeWithZhipu(String prompt, String apiKey) throws IOException {
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "glm-4");
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.3);
        requestBody.addProperty("max_tokens", 1000);
        
        return sendRequest(ZHIPU_API_URL, requestBody.toString(), apiKey, "zhipu");
    }
    
    /**
     * 构建翻译提示词
     */
    private static String buildTranslationPrompt(String text, PluginSettings settings) {
        if (settings.isUseCustomPrompt() && 
            settings.getTranslationPrompt() != null && 
            !settings.getTranslationPrompt().trim().isEmpty()) {
            // 使用自定义提示词
            return settings.getTranslationPrompt() + text;
        } else {
            // 使用默认简单提示词
            return "请将以下中文翻译为英文，只返回翻译结果，不要解释：" + text;
        }
    }
    
    /**
     * 发送HTTP请求
     */
    private static String sendRequest(String urlString, String requestBody, String apiKey, String modelType) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        // 根据不同模型设置不同的请求头
        switch (modelType) {
            case "qianwen":
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                break;
            case "wenxin":
                // 文心一言的token已经在URL中
                break;
            case "zhipu":
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                break;
            case "hunyuan":
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                break;
        }
        
        // 发送请求体
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        if (responseCode != 200) {
            throw new IOException("国内大模型API请求失败 (HTTP " + responseCode + "): " + response.toString());
        }
        
        // 解析响应
        return parseResponse(response.toString(), modelType);
    }
    
    /**
     * 解析不同模型的响应格式
     */
    private static String parseResponse(String response, String modelType) throws IOException {
        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response, JsonObject.class);
            
            // 添加调试日志
            System.out.println("[DEBUG] " + modelType + " API 响应: " + response);
            
            switch (modelType) {
                case "qianwen":
                    return parseQianwenResponse(json);
                case "wenxin":
                    return parseWenxinResponse(json);
                case "zhipu":
                    return parseZhipuResponse(json);
                case "hunyuan":
                    return parseHunyuanResponse(json);
                default:
                    throw new IOException("不支持的模型类型: " + modelType);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 解析" + modelType + "API响应失败: " + e.getMessage());
            System.err.println("[ERROR] 原始响应: " + response);
            throw new IOException("解析国内大模型API响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析通义千问响应
     */
    private static String parseQianwenResponse(JsonObject json) throws IOException {
        try {
            // 检查是否有错误
            if (json.has("code") && !json.get("code").getAsString().equals("200")) {
                String errorMsg = json.has("message") ? json.get("message").getAsString() : "未知错误";
                throw new IOException("通义千问API错误: " + errorMsg);
            }
            
            // 尝试多种可能的响应格式
            if (json.has("output")) {
                JsonObject output = json.getAsJsonObject("output");
                
                // 格式1: output.choices[0].message.content
                if (output.has("choices")) {
                    JsonArray choices = output.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        if (choice.has("message")) {
                            JsonObject message = choice.getAsJsonObject("message");
                            if (message.has("content")) {
                                return message.get("content").getAsString().trim();
                            }
                        }
                    }
                }
                
                // 格式2: output.text
                if (output.has("text")) {
                    return output.get("text").getAsString().trim();
                }
                
                // 格式3: output.finish_reason存在时，可能在其他字段
                if (output.has("finish_reason")) {
                    // 有些情况下文本在output直接下面
                    for (String key : new String[]{"content", "response", "result"}) {
                        if (output.has(key)) {
                            return output.get(key).getAsString().trim();
                        }
                    }
                }
            }
            
            // 直接在根级别查找常见字段
            for (String key : new String[]{"text", "content", "response", "result"}) {
                if (json.has(key)) {
                    return json.get(key).getAsString().trim();
                }
            }
            
            throw new IOException("通义千问响应格式不识别，无法提取翻译结果");
            
        } catch (Exception e) {
            throw new IOException("解析通义千问响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析文心一言响应
     */
    private static String parseWenxinResponse(JsonObject json) throws IOException {
        try {
            // 检查错误
            if (json.has("error_code")) {
                String errorMsg = json.has("error_msg") ? json.get("error_msg").getAsString() : "未知错误";
                throw new IOException("文心一言API错误: " + errorMsg);
            }
            
            // 标准格式
            if (json.has("result")) {
                return json.get("result").getAsString().trim();
            }
            
            throw new IOException("文心一言响应格式不识别");
            
        } catch (Exception e) {
            throw new IOException("解析文心一言响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析腾讯混元响应
     */
    private static String parseHunyuanResponse(JsonObject json) throws IOException {
        try {
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMessage = error.has("message") ? error.get("message").getAsString() : "未知错误";
                throw new IOException("腾讯混元API错误: " + errorMessage);
            }

            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    return message.get("content").getAsString().trim();
                }
            }

            throw new IOException("腾讯混元响应格式异常");
        } catch (Exception e) {
            throw new IOException("腾讯混元响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析智谱AI响应
     */
    private static String parseZhipuResponse(JsonObject json) throws IOException {
        try {
            // 检查错误
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMsg = error.has("message") ? error.get("message").getAsString() : "未知错误";
                throw new IOException("智谱AI错误: " + errorMsg);
            }
            
            // 标准格式
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message.has("content")) {
                            return message.get("content").getAsString().trim();
                        }
                    }
                }
            }
            
            throw new IOException("智谱AI响应格式不识别");
            
        } catch (Exception e) {
            throw new IOException("解析智谱AI响应失败: " + e.getMessage());
        }
    }
} 