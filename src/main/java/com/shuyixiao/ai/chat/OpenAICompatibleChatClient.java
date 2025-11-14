package com.shuyixiao.ai.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OpenAICompatibleChatClient {

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static String quickTest(String baseUrl, String apiKey, String model, String content) throws Exception {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", content));
        return chat(baseUrl, apiKey, model, messages);
    }

    public static String chat(String baseUrl, String apiKey, String model, List<Message> messages) throws Exception {
        String endpoint = normalizeEndpoint(baseUrl);

        JsonObject req = new JsonObject();
        req.addProperty("model", model);
        JsonArray msgs = new JsonArray();
        for (Message m : messages) {
            JsonObject jm = new JsonObject();
            jm.addProperty("role", m.role);
            jm.addProperty("content", m.content);
            msgs.add(jm);
        }
        req.add("messages", msgs);
        req.addProperty("temperature", 0.2);

        URL url = URI.create(endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(req.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        if (code != 200) {
            throw new RuntimeException("Chat API 请求失败: HTTP " + code + " - " + sb);
        }

        return parseChatResponse(sb.toString());
    }

    private static String normalizeEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL 不能为空");
        }
        String u = baseUrl.trim();
        if (!u.endsWith("/")) u = u + "/";
        // 如果已经包含chat/completions则直接使用
        if (u.contains("chat/completions")) return u;
        // 如果以 /v1/ 结尾，追加 chat/completions
        if (u.endsWith("v1/") || u.endsWith("v1/")) {
            return u + "chat/completions";
        }
        // 常见 OpenAI 兼容：直接补上 v1/chat/completions
        return u + "v1/chat/completions";
    }

    private static String parseChatResponse(String jsonStr) {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
        if (json.has("error")) {
            JsonObject err = json.getAsJsonObject("error");
            String msg = err.has("message") ? err.get("message").getAsString() : err.toString();
            throw new RuntimeException("Chat API 错误: " + msg);
        }
        if (json.has("choices")) {
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject c0 = choices.get(0).getAsJsonObject();
                if (c0.has("message")) {
                    JsonObject m = c0.getAsJsonObject("message");
                    if (m.has("content")) {
                        return m.get("content").getAsString();
                    }
                }
                // 某些实现直接返回 text
                if (c0.has("text")) {
                    return c0.get("text").getAsString();
                }
            }
        }
        // 常见兼容实现可能返回 content 字段
        if (json.has("content")) {
            return json.get("content").getAsString();
        }
        return jsonStr;
    }
}
