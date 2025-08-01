package com.shuyixiao;

import com.shuyixiao.setting.PluginSettings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GoogleCloudTranslationAPI {
    private static final String API_URL_FORMAT = "https://translation.googleapis.com/language/translate/v2?key=%s";

    public static String translate(String text) throws IOException {
        PluginSettings settings = PluginSettings.getInstance();
        String apiKey = settings.getGoogleApiKey();
        String projectId = settings.getGoogleProjectId();
        String region = settings.getGoogleRegion();
        if (apiKey == null || apiKey.isEmpty() || projectId == null || projectId.isEmpty()) {
            throw new IOException("Google Cloud Translation API Key和Project ID不能为空");
        }
        String apiUrl = String.format(API_URL_FORMAT, apiKey);
        // 构建POST请求体
        String requestBody = String.format("{\"q\":\"%s\",\"target\":\"en\",\"format\":\"text\",\"project\":\"%s\"}",
                escapeJson(text), projectId);
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
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
            throw new IOException("Google翻译API请求失败: " + response.toString());
        }
        // 解析JSON响应
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
        if (json.has("error")) {
            throw new IOException("Google翻译API错误: " + json.get("error").toString());
        }
        try {
            String translated = json.getAsJsonObject("data")
                    .getAsJsonArray("translations")
                    .get(0).getAsJsonObject()
                    .get("translatedText").getAsString();
            return translated;
        } catch (Exception e) {
            throw new IOException("Google翻译API响应解析失败: " + e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
} 