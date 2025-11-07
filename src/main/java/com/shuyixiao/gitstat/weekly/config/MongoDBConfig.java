package com.shuyixiao.gitstat.weekly.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * MongoDB配置管理类
 * 从配置文件读取MongoDB连接信息
 * 
 * @author PandaCoder Team
 * @since 2.2.0
 */
public class MongoDBConfig {
    
    private static final String CONFIG_FILE = "/mongodb-config.properties";
    private static Properties properties;
    private static boolean configLoaded = false;
    
    // 配置项键名
    private static final String KEY_URL = "mongodb.url";
    private static final String KEY_DATABASE = "mongodb.database";
    private static final String KEY_COLLECTION = "mongodb.collection";
    private static final String KEY_USERNAME = "mongodb.username";
    private static final String KEY_PASSWORD = "mongodb.password";
    private static final String KEY_AUTH_DATABASE = "mongodb.auth.database";
    private static final String KEY_CONNECTION_TIMEOUT = "mongodb.connection.timeout";
    private static final String KEY_SOCKET_TIMEOUT = "mongodb.socket.timeout";
    private static final String KEY_MAX_POOL_SIZE = "mongodb.connection.pool.max.size";
    private static final String KEY_MIN_POOL_SIZE = "mongodb.connection.pool.min.size";
    
    // 默认值
    private static final String DEFAULT_URL = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE = "pandacoder";
    private static final String DEFAULT_COLLECTION = "weekly_reports";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_POOL_SIZE = 1;
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        properties = new Properties();
        try (InputStream is = MongoDBConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                configLoaded = true;
                System.out.println("[INFO] MongoDB配置文件加载成功: " + CONFIG_FILE);
                System.out.println("[INFO] 配置项数量: " + properties.size());
                // 打印配置信息（隐藏密码）
                properties.forEach((key, value) -> {
                    if (key.toString().toLowerCase().contains("password")) {
                        System.out.println("[INFO]   " + key + " = ****");
                    } else {
                        System.out.println("[INFO]   " + key + " = " + value);
                    }
                });
            } else {
                System.out.println("[WARN] MongoDB配置文件未找到: " + CONFIG_FILE);
                System.out.println("[WARN] 将使用默认配置");
                setDefaultProperties();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 读取MongoDB配置文件失败: " + e.getMessage());
            e.printStackTrace();
            setDefaultProperties();
        }
    }
    
    /**
     * 设置默认配置
     */
    private static void setDefaultProperties() {
        properties.setProperty(KEY_URL, DEFAULT_URL);
        properties.setProperty(KEY_DATABASE, DEFAULT_DATABASE);
        properties.setProperty(KEY_COLLECTION, DEFAULT_COLLECTION);
        properties.setProperty(KEY_CONNECTION_TIMEOUT, String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        properties.setProperty(KEY_SOCKET_TIMEOUT, String.valueOf(DEFAULT_SOCKET_TIMEOUT));
        properties.setProperty(KEY_MAX_POOL_SIZE, String.valueOf(DEFAULT_MAX_POOL_SIZE));
        properties.setProperty(KEY_MIN_POOL_SIZE, String.valueOf(DEFAULT_MIN_POOL_SIZE));
        configLoaded = false;
    }
    
    /**
     * 检查配置是否已加载
     */
    public static boolean isConfigured() {
        return configLoaded;
    }
    
    /**
     * 获取MongoDB连接URL
     */
    public static String getUrl() {
        return properties.getProperty(KEY_URL, DEFAULT_URL);
    }
    
    /**
     * 获取数据库名称
     */
    public static String getDatabase() {
        return properties.getProperty(KEY_DATABASE, DEFAULT_DATABASE);
    }
    
    /**
     * 获取集合名称
     */
    public static String getCollection() {
        return properties.getProperty(KEY_COLLECTION, DEFAULT_COLLECTION);
    }
    
    /**
     * 获取用户名
     */
    public static String getUsername() {
        String username = properties.getProperty(KEY_USERNAME, "");
        return username.trim().isEmpty() ? null : username.trim();
    }
    
    /**
     * 获取密码
     */
    public static String getPassword() {
        String password = properties.getProperty(KEY_PASSWORD, "");
        return password.trim().isEmpty() ? null : password.trim();
    }

    /**
     * 获取认证数据库
     */
    public static String getAuthDatabase() {
        String authDb = properties.getProperty(KEY_AUTH_DATABASE, "admin");
        return authDb.trim().isEmpty() ? "admin" : authDb.trim();
    }
    
    /**
     * 获取连接超时时间（毫秒）
     */
    public static int getConnectionTimeout() {
        try {
            return Integer.parseInt(properties.getProperty(KEY_CONNECTION_TIMEOUT, String.valueOf(DEFAULT_CONNECTION_TIMEOUT)));
        } catch (NumberFormatException e) {
            return DEFAULT_CONNECTION_TIMEOUT;
        }
    }
    
    /**
     * 获取Socket超时时间（毫秒）
     */
    public static int getSocketTimeout() {
        try {
            return Integer.parseInt(properties.getProperty(KEY_SOCKET_TIMEOUT, String.valueOf(DEFAULT_SOCKET_TIMEOUT)));
        } catch (NumberFormatException e) {
            return DEFAULT_SOCKET_TIMEOUT;
        }
    }
    
    /**
     * 获取最大连接池大小
     */
    public static int getMaxPoolSize() {
        try {
            return Integer.parseInt(properties.getProperty(KEY_MAX_POOL_SIZE, String.valueOf(DEFAULT_MAX_POOL_SIZE)));
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_POOL_SIZE;
        }
    }
    
    /**
     * 获取最小连接池大小
     */
    public static int getMinPoolSize() {
        try {
            return Integer.parseInt(properties.getProperty(KEY_MIN_POOL_SIZE, String.valueOf(DEFAULT_MIN_POOL_SIZE)));
        } catch (NumberFormatException e) {
            return DEFAULT_MIN_POOL_SIZE;
        }
    }
    
    /**
     * 重新加载配置
     */
    public static void reload() {
        loadConfig();
    }
    
    /**
     * 获取完整的连接字符串（包含认证信息）
     */
    public static String getConnectionString() {
        String url = getUrl();
        String username = getUsername();
        String password = getPassword();
        String authDatabase = getAuthDatabase();

        System.out.println("[DEBUG] 原始URL: " + url);
        System.out.println("[DEBUG] 用户名: " + username);
        System.out.println("[DEBUG] 密码长度: " + (password != null ? password.length() : 0));
        System.out.println("[DEBUG] 认证数据库: " + authDatabase);

        // 如果URL中已经包含认证信息，直接返回
        if (url.contains("@")) {
            System.out.println("[DEBUG] URL已包含认证信息，直接返回");
            return url;
        }

        // 如果提供了用户名和密码，构建包含认证的连接字符串
        if (username != null && password != null) {
            // URL编码用户名和密码中的特殊字符
            String encodedUsername = urlEncode(username);
            String encodedPassword = urlEncode(password);

            System.out.println("[DEBUG] 编码后用户名: " + encodedUsername);
            System.out.println("[DEBUG] 编码后密码: " + encodedPassword);

            // 解析URL，插入认证信息
            if (url.startsWith("mongodb://")) {
                String newUrl = url.replace("mongodb://", "mongodb://" + encodedUsername + ":" + encodedPassword + "@");
                // 添加认证数据库参数（注意：需要在主机地址后加 / 才能添加查询参数）
                if (!newUrl.contains("?")) {
                    // 如果URL没有查询参数，需要先加 / 再加查询参数
                    if (!newUrl.endsWith("/")) {
                        newUrl += "/";
                    }
                    newUrl += "?authSource=" + authDatabase;
                } else if (!newUrl.contains("authSource=")) {
                    newUrl += "&authSource=" + authDatabase;
                }
                System.out.println("[DEBUG] 最终连接字符串: " + newUrl);
                return newUrl;
            } else if (url.startsWith("mongodb+srv://")) {
                String newUrl = url.replace("mongodb+srv://", "mongodb+srv://" + encodedUsername + ":" + encodedPassword + "@");
                // 添加认证数据库参数
                if (!newUrl.contains("?")) {
                    // mongodb+srv 不需要加 /，可以直接加查询参数
                    newUrl += "?authSource=" + authDatabase;
                } else if (!newUrl.contains("authSource=")) {
                    newUrl += "&authSource=" + authDatabase;
                }
                System.out.println("[DEBUG] 最终连接字符串: " + newUrl);
                return newUrl;
            }
        }

        System.out.println("[DEBUG] 返回原始URL");
        return url;
    }

    /**
     * URL编码（简单实现，处理常见特殊字符）
     */
    private static String urlEncode(String value) {
        if (value == null) {
            return null;
        }
        return value
            .replace("%", "%25")
            .replace(":", "%3A")
            .replace("/", "%2F")
            .replace("?", "%3F")
            .replace("#", "%23")
            .replace("[", "%5B")
            .replace("]", "%5D")
            .replace("@", "%40")
            .replace("!", "%21")
            .replace("$", "%24")
            .replace("&", "%26")
            .replace("'", "%27")
            .replace("(", "%28")
            .replace(")", "%29")
            .replace("*", "%2A")
            .replace("+", "%2B")
            .replace(",", "%2C")
            .replace(";", "%3B")
            .replace("=", "%3D");
    }
}

