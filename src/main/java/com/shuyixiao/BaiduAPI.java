package com.shuyixiao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.net.URL;

/**
 * Copyright © 2024年 integration-projects-maven. All rights reserved.
 * ClassName BaiduAPI.java
 * author 舒一笑 yixiaoshu88@163.com
 * version 1.0.0
 * Description 百度翻译API
 * createTime 2024年08月21日 21:41:00
 */
public class BaiduAPI {

    private static final String API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate?";

    // 获取百度API密钥，优先级：
    // 1. 插件设置中的配置
    // 2. 系统属性 (通过 -D 参数传入)
    // 3. 环境变量 (如果配置了)
    // 4. 空字符串 (如果都没有配置，则使用模拟翻译)
    private static String getApiKey() {
        String settingsApiKey = com.shuyixiao.setting.PluginSettings.getInstance().getBaiduApiKey();
        if (settingsApiKey != null && !settingsApiKey.isEmpty()) {
            return settingsApiKey;
        }
        return System.getProperty("baidu.api.key", System.getenv().getOrDefault("BAIDU_API_KEY", ""));
    }

    // 获取百度应用ID，优先级与API密钥相同
    private static String getAppId() {
        String settingsAppId = com.shuyixiao.setting.PluginSettings.getInstance().getBaiduAppId();
        if (settingsAppId != null && !settingsAppId.isEmpty()) {
            return settingsAppId;
        }
        return System.getProperty("baidu.app.id", System.getenv().getOrDefault("BAIDU_APP_ID", ""));
    }

    public static String translate(String Chinese) throws UnsupportedEncodingException {
        // 检查API密钥和ID是否已配置
        String apiKey = getApiKey();
        String appId = getAppId();

        if (apiKey.isEmpty() || appId.isEmpty()) {
            // 当API密钥未配置时，使用模拟翻译并提供友好提示
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "未配置百度翻译API密钥和应用ID，无法完成翻译操作。\n" +
                "请在 设置 > 工具 > PandaCoder 中配置百度翻译API以获得翻译功能。",
                "翻译API未配置");
            throw new UnsupportedEncodingException("API配置缺失，无法进行翻译");
        }

        // 预处理：移除不必要的字符，例如括号、冒号等
        Chinese = Chinese.replaceAll("[\\(\\)\\[\\]\\{\\}:：，,。.]+", " ").trim();

        // 生成随机数
        String salt = generateSalt();
        StringBuilder translatedText = new StringBuilder();

        try {
            // 为整句生成签名，而不是分词处理
            String sign = generateSign(appId, Chinese, salt); // 生成签名

            // 构建HTTP请求
            Map<String, String> params = new java.util.HashMap<>();
            params.put("q", Chinese);
            params.put("from", "zh"); // 明确指定从中文
            params.put("to", "en");  // 翻译到英文
            params.put("appid", appId);
            params.put("salt", salt);
            params.put("sign", sign);

            String result = sendRequest(params); // 发送HTTP请求并获取翻译结果
            return result.trim();  // 移除多余空格
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return simulateTranslation(Chinese);
        } catch (IOException e) {
            System.out.println("翻译API调用失败：" + e.getMessage());
            return simulateTranslation(Chinese);
        }
    }

    // 生成随机数
    private static String generateSalt() {
        return String.valueOf(new Random().nextInt(10001)); // 这里可以根据需要生成随机数，例如随机字符串或数字
    }

    // 生成签名
    private static String generateSign(String appId, String q, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String rawString = appId + q + salt + getApiKey();
        return md5(rawString).toLowerCase(); // 将MD5哈希值转换为小写格式作为签名
    }

    // 生成签名（使用指定的API密钥）
    private static String generateSign(String appId, String q, String salt, String customApiKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String rawString = appId + q + salt + customApiKey;
        return md5(rawString).toLowerCase(); // 将MD5哈希值转换为小写格式作为签名
    }

    // 计算MD5哈希值
    private static String md5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(input.getBytes());
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 发送HTTP请求
    private static String sendRequest(Map<String, String> params) throws IOException {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                // 构建POST参数
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (postData.length() > 0) {
                        postData.append("&");
                    }
                    postData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
                    postData.append("=");
                    postData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
                }
                wr.write(postData.toString());
                wr.flush();
            }

            // 获取响应
            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }
            } else {
                // 读取错误流，获取更详细的错误信息
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }

                String errorMsg = "百度翻译API请求失败: 状态码=" + responseCode;
                if (errorResponse.length() > 0) {
                    errorMsg += ", 错误信息=" + errorResponse.toString();
                }

                System.err.println(errorMsg);
                throw new IOException(errorMsg);
            }

            try {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);

                // 检查是否包含错误代码
                if (jsonObject.has("error_code")) {
                    String errorCode = jsonObject.get("error_code").getAsString();
                    String errorMsg = jsonObject.has("error_msg") ? 
                                     jsonObject.get("error_msg").getAsString() : 
                                     "未知错误";

                    // 根据错误代码提供更友好的错误信息
                    String userFriendlyError = "未知错误";
                    switch (errorCode) {
                        case "52000": // 成功
                            userFriendlyError = "成功";
                            break; // 不应该走到这里
                        case "52001":
                            userFriendlyError = "请求超时，请重试";
                            break;
                        case "52002":
                            userFriendlyError = "系统错误，请稍后重试";
                            break;
                        case "52003":
                            userFriendlyError = "未授权用户，API密钥或应用ID不正确";
                            break;
                        case "54000":
                            userFriendlyError = "必填参数为空，请检查应用ID或API密钥";
                            break;
                        case "54001":
                            userFriendlyError = "签名错误，请检查您的API密钥";
                            break;
                        case "54003":
                            userFriendlyError = "访问频率受限，请降低调用频率";
                            break;
                        case "54004":
                            userFriendlyError = "账户余额不足，请前往百度翻译开放平台充值";
                            break;
                        case "54005":
                            userFriendlyError = "长query请求频繁，请降低长文本翻译频率";
                            break;
                        case "58000":
                            userFriendlyError = "客户端IP非法，请检查您的IP访问权限";
                            break;
                        case "58001":
                            userFriendlyError = "译文语言方向不支持，请检查语言设置";
                            break;
                        case "58002":
                            userFriendlyError = "服务当前已关闭，请稍后再试";
                            break;
                        default:
                            userFriendlyError = "未知错误，代码：" + errorCode;
                    }

                    String detailedError = "百度翻译API错误: " + userFriendlyError + " (代码: " + errorCode + ", 原始信息: " + errorMsg + ")";
                    System.err.println(detailedError);
                    throw new IOException(detailedError);
                }

                JsonArray transResultArray = jsonObject.getAsJsonArray("trans_result");

                if (transResultArray != null && transResultArray.size() > 0) {
                    JsonObject transResultObject = transResultArray.get(0).getAsJsonObject(); // 取第一个元素
                    String dstValue = transResultObject.get("dst").getAsString();

                    // 将 Unicode 编码字符串转换成中文
                    return unicodeToChinese(dstValue);
                } else {
                    throw new IOException("百度翻译API返回的结果中未包含翻译内容");
                }
            } catch (Exception e) {
                System.err.println("JSON解析或API调用错误: " + e.getMessage());
                throw new IOException("百度翻译API调用失败: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Network error: " + e.getMessage());
            return simulateTranslation(params.get("q"));
        }
    }

    // Unicode 编码字符串转换成中文
    private static String unicodeToChinese(String unicodeStr) {
        if (unicodeStr == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int pos = 0;

        int i = unicodeStr.indexOf("\\u", pos);
        while (i != -1) {
            sb.append(unicodeStr, pos, i);
            if (i + 5 < unicodeStr.length()) {
                pos = i + 6;
                sb.append((char) Integer.parseInt(unicodeStr.substring(i + 2, i + 6), 16));
            }
            i = unicodeStr.indexOf("\\u", pos);
        }

        sb.append(unicodeStr.substring(pos));

        return sb.toString();
    }

    /**
     * 检查API配置是否有效
     * @return 如果API密钥和应用ID都已配置则返回true，否则返回false
     */
    public static boolean isApiConfigured() {
        String apiKey = getApiKey();
        String appId = getAppId();
        return !apiKey.isEmpty() && !appId.isEmpty();
    }

    /**
     * 验证API配置是否正确
     * @return 如果API配置正确返回true，否则返回false
     * @throws Exception 验证过程中出现错误时抛出
     */
    public static boolean validateApiConfiguration() throws Exception {
        String apiKey = getApiKey();
        String appId = getAppId();

        if (apiKey.isEmpty() || appId.isEmpty()) {
            throw new Exception("API密钥或应用ID为空");
        }

        try {
            // 实际调用百度翻译API进行测试
            // 使用一个简单的中文短语
            String testPhrase = "测试翻译API";
            String url = "https://fanyi-api.baidu.com/api/trans/vip/translate";
            String salt = String.valueOf(System.currentTimeMillis());
            // 使用提供的apiKey而不是从配置中获取
            String sign = generateSign(appId, testPhrase, salt, apiKey);

            // 准备API请求参数
            Map<String, String> params = new HashMap<>();
            params.put("q", testPhrase);
            params.put("from", "zh");
            params.put("to", "en");
            params.put("appid", appId);
            params.put("salt", salt);
            params.put("sign", sign);

            // 发送实际请求到百度API
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // 构建请求参数
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }

            // 发送请求
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(postData.toString());
                wr.flush();
            }

            // 获取响应
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }

                // 解析JSON响应
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);

                // 检查是否包含错误代码
                if (jsonObject.has("error_code")) {
                    String errorCode = jsonObject.get("error_code").getAsString();
                    String errorMsg = jsonObject.has("error_msg") ? 
                                    jsonObject.get("error_msg").getAsString() : 
                                    "未知错误";
                    throw new Exception("API错误: " + errorMsg + " (代码: " + errorCode + ")");
                }

                // 检查是否包含翻译结果
                if (jsonObject.has("trans_result")) {
                    JsonArray transResultArray = jsonObject.getAsJsonArray("trans_result");
                    if (transResultArray != null && transResultArray.size() > 0) {
                        // 测试成功
                        return true;
                    }
                }

                throw new Exception("API响应格式不正确");
            } else {
                // 读取错误响应
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        errorResponse.append(line);
                    }
                } catch (Exception e) {
                    // 忽略错误流读取错误
                }

                throw new Exception("API请求失败: 状态码=" + responseCode + 
                                  (errorResponse.length() > 0 ? ", 错误信息=" + errorResponse.toString() : ""));
            }
        } catch (Exception e) {
            throw new Exception("百度翻译API验证失败：" + e.getMessage());
        }
    }

    /**
     * 模拟翻译功能，用于API密钥未配置时
     */
    private static String simulateTranslation(String chinese) {
        // 扩展的中文到英文映射词典
        return chinese.replaceAll("用户", "user")
                     .replaceAll("管理", "manage")
                     .replaceAll("系统", "system")
                     .replaceAll("服务", "service")
                     .replaceAll("数据", "data")
                     .replaceAll("配置", "config")
                     .replaceAll("信息", "info")
                     .replaceAll("控制器", "controller")
                     .replaceAll("工具", "util")
                     .replaceAll("接口", "interface")
                     .replaceAll("实现", "impl")
                     .replaceAll("业务", "business")
                     .replaceAll("层", "layer")
                     .replaceAll("模型", "model")
                     .replaceAll("视图", "view")
                     .replaceAll("请求", "request")
                     .replaceAll("响应", "response")
                     .replaceAll("异常", "exception")
                     .replaceAll("工厂", "factory")
                     .replaceAll("构建", "builder")
                     .replaceAll("转换", "converter")
                     .replaceAll("解析", "parser")
                     .replaceAll("处理", "handler")
                     .replaceAll("存储", "storage")
                     .replaceAll("缓存", "cache")
                     .replaceAll("日志", "log")
                     .replaceAll("消息", "message")
                     .replaceAll("队列", "queue")
                     .replaceAll("订阅", "subscribe")
                     .replaceAll("发布", "publish")
                     .replaceAll("测试", "test")
                     .replaceAll("设置", "setting")
                     .replaceAll("中心", "center")
                     .replaceAll("引擎", "engine")
                     .replaceAll("助手", "assistant")
                     .replaceAll(" ", "");
    }
}
