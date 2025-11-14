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

public class OllamaChatClient {

    public static class Message {
        public String role;
        public String content;
        public Message(String role, String content) { this.role = role; this.content = content; }
    }

    public static String chat(String baseUrl, String model, List<Message> messages) throws Exception {
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
        req.addProperty("stream", false);

        URL url = URI.create(endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
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
            throw new RuntimeException("Ollama Chat 请求失败: HTTP " + code + " - " + sb);
        }

        return parseChatResponse(sb.toString());
    }

    private static String normalizeEndpoint(String baseUrl) {
        String u = baseUrl == null ? "" : baseUrl.trim();
        if (!u.endsWith("/")) u = u + "/";
        if (u.contains("/api/chat")) return u;
        if (u.endsWith("/api/") || u.endsWith("/api/")) return u + "chat";
        if (u.endsWith("/api")) return u + "/chat";
        // 默认追加 /api/chat
        return u + "api/chat";
    }

    private static String parseChatResponse(String jsonStr) {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
        if (json.has("message")) {
            JsonObject msg = json.getAsJsonObject("message");
            if (msg.has("content")) return msg.get("content").getAsString();
        }
        if (json.has("content")) return json.get("content").getAsString();
        return jsonStr;
    }
}
