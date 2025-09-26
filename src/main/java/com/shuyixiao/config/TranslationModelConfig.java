package com.shuyixiao.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 翻译模型配置管理器
 * 支持从配置文件读取翻译模型相关配置
 */
public class TranslationModelConfig {

    private static final String CONFIG_FILE = "/translation-config.properties";
    private static Properties properties;
    private static TranslationModelConfig instance;

    static {
        loadConfig();
    }

    private TranslationModelConfig() {}

    public static TranslationModelConfig getInstance() {
        if (instance == null) {
            synchronized (TranslationModelConfig.class) {
                if (instance == null) {
                    instance = new TranslationModelConfig();
                }
            }
        }
        return instance;
    }

    private static void loadConfig() {
        properties = new Properties();
        try (InputStream is = TranslationModelConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                System.out.println("[INFO] 翻译模型配置文件加载成功");
            } else {
                System.out.println("[WARN] 翻译模型配置文件未找到，将使用默认配置");
                setDefaultProperties();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 读取翻译模型配置文件失败: " + e.getMessage());
            setDefaultProperties();
        }
    }

    private static void setDefaultProperties() {
        properties.setProperty("domestic.ai.default.model", "hunyuan");
        properties.setProperty("domestic.ai.fallback.model", "qianwen");
        properties.setProperty("domestic.ai.model.priority", "hunyuan,qianwen,zhipu,wenxin");
        properties.setProperty("translation.enable.fallback", "true");
        properties.setProperty("translation.timeout.seconds", "10");
    }

    /**
     * 获取默认的国内AI翻译模型
     * @return 默认模型名称
     */
    public String getDefaultDomesticAIModel() {
        return properties.getProperty("domestic.ai.default.model", "hunyuan");
    }

    /**
     * 获取回退模型
     * @return 回退模型名称
     */
    public String getFallbackModel() {
        return properties.getProperty("domestic.ai.fallback.model", "qianwen");
    }

    /**
     * 获取模型优先级列表
     * @return 模型优先级数组
     */
    public String[] getModelPriority() {
        String priority = properties.getProperty("domestic.ai.model.priority", "hunyuan,qianwen,zhipu,wenxin");
        return priority.split(",");
    }

    /**
     * 是否启用回退机制
     * @return true表示启用回退
     */
    public boolean isEnableFallback() {
        return Boolean.parseBoolean(properties.getProperty("translation.enable.fallback", "true"));
    }

    /**
     * 获取翻译超时时间（秒）
     * @return 超时时间
     */
    public int getTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("translation.timeout.seconds", "10"));
    }

    /**
     * 根据模型名称获取模型显示名称
     * @param modelKey 模型key
     * @return 显示名称
     */
    public String getModelDisplayName(String modelKey) {
        switch (modelKey.toLowerCase()) {
            case "hunyuan":
                return "腾讯混元";
            case "qianwen":
                return "通义千问";
            case "zhipu":
                return "智谱AI";
            case "wenxin":
                return "文心一言";
            default:
                return modelKey;
        }
    }

    /**
     * 重新加载配置文件
     */
    public void reload() {
        loadConfig();
    }
}
