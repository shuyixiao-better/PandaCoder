package com.shuyixiao.converter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.shuyixiao.BaiduAPI;
import com.shuyixiao.setting.PluginSettings;

import java.io.UnsupportedEncodingException;

/**
 * 翻译转换工具类
 * 封装各种文本转换的方法
 */
public class TranslationConverter {

    // 最大词数限制，用于智能截取过长的中文输入
    private static final int MAX_WORDS = 4;

    /**
     * 将中文文本转换为小驼峰命名
     * 
     * @param chineseText 中文文本
     * @param project 当前项目
     * @return 转换后的小驼峰文本，如果转换失败返回null
     */
    public static String convertToLowerCamelCase(String chineseText, Project project) {
        // 智能处理中文文本
        String processedText = preprocessChineseText(chineseText);

        // 进行翻译
        String translatedText;
        try {
            if (PluginSettings.getInstance().isEnableGoogleTranslation()) {
                translatedText = com.shuyixiao.GoogleCloudTranslationAPI.translate(processedText);
            } else {
                translatedText = BaiduAPI.translate(processedText);
            }
            if (translatedText == null || translatedText.trim().isEmpty()) {
                Messages.showErrorDialog(project, "翻译结果为空，无法进行转换。请检查您的API配置是否正确。", "翻译失败");
                return null;
            }
        } catch (UnsupportedEncodingException ex) {
            // API未配置或配置错误，TranslationUtil已经显示了错误信息
            return null;
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "翻译过程中发生错误: " + ex.getMessage(), "翻译错误");
            return null;
        }

        // 转换为小驼峰命名
        return toCamelCase(translatedText);
    }

    /**
     * 预处理中文文本，智能截取重要内容，去除无用词
     * 
     * @param chineseText 原始中文文本
     * @return 处理后的中文文本
     */
    private static String preprocessChineseText(String chineseText) {
        // 移除标点符号和特殊字符
        String cleanText = chineseText.replaceAll("[\\pP\\p{Punct}]+", " ").trim();

        // 移除常见的无意义词
        String[] stopWords = {"的", "了", "和", "与", "或", "及", "等", "对于", "一个", "这个", "那个"};
        for (String stopWord : stopWords) {
            cleanText = cleanText.replace(stopWord, " ");
        }

        // 按空格分词
        String[] words = cleanText.split("\\s+");

        // 如果词数超过限制，只保留前几个重要词
        if (words.length > MAX_WORDS) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < MAX_WORDS; i++) {
                builder.append(words[i]).append(" ");
            }
            return builder.toString().trim();
        }

        return cleanText;
    }

    /**
     * 将英文文本转换为小驼峰命名格式
     * 
     * @param text 英文文本
     * @return 小驼峰命名格式的文本
     */
    private static String toCamelCase(String text) {
        // 清理文本，去除多余的空格和标点
        text = text.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        // 分割单词
        String[] words = text.split("\\s+");
        StringBuilder camelCaseText = new StringBuilder();

        // 过滤常见的无用英文单词
        String[] stopWords = {"the", "a", "an", "of", "for", "to", "in", "on", "at", "with", "by", "as"};

        // 从所有单词中筛选出重要单词
        java.util.List<String> importantWords = new java.util.ArrayList<>();
        for (String word : words) {
            if (word.isEmpty()) continue;
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (word.equalsIgnoreCase(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            if (!isStopWord) {
                importantWords.add(word);
            }
        }

        // 如果提取出的重要单词为空，使用原始单词
        if (importantWords.isEmpty() && words.length > 0) {
            importantWords.add(words[0]);
        }

        // 生成小驼峰命名
        if (!importantWords.isEmpty()) {
            camelCaseText.append(importantWords.get(0).toLowerCase());

            for (int i = 1; i < importantWords.size(); i++) {
                camelCaseText.append(capitalize(importantWords.get(i)));
            }
        }

        return camelCaseText.toString();
    }

    /**
     * 将单词首字母大写
     * 
     * @param word 单词
     * @return 首字母大写的单词
     */
    private static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    /**
     * 将英文文本转换为大驼峰命名格式
     * 
     * @param text 英文文本
     * @return 大驼峰命名格式的文本
     */
    public static String toPascalCase(String text) {
        String[] words = text.split(" ");
        StringBuilder pascalCaseText = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                pascalCaseText.append(capitalize(word));
            }
        }

        return pascalCaseText.toString();
    }
}
