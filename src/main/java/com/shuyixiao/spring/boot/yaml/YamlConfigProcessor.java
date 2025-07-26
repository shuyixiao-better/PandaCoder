package com.shuyixiao.spring.boot.yaml;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML配置处理器
 * 用于解析YAML配置内容并提取关键配置信息
 */
public class YamlConfigProcessor {

    private static final Logger LOG = Logger.getInstance(YamlConfigProcessor.class);

    // YAML配置行正则表达式
    private static final Pattern CONFIG_LINE_PATTERN = Pattern.compile("^(\\s*)(\\w+)\\s*:\\s*(.*)$");

    // URL正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(https?|ftp|file|jdbc|redis|mongodb|amqp|kafka)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    /**
     * 从YAML配置中提取所有配置项
     *
     * @param yamlContent YAML配置内容
     * @return 配置项列表 (缩进级别, 键, 值)
     */
    @NotNull
    public static List<YamlConfigEntry> extractConfigEntries(@NotNull String yamlContent) {
        List<YamlConfigEntry> entries = new ArrayList<>();
        String[] lines = yamlContent.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = CONFIG_LINE_PATTERN.matcher(line);

            if (matcher.find()) {
                String indentation = matcher.group(1);
                String key = matcher.group(2);
                String value = matcher.group(3);

                int indentLevel = indentation.length();
                YamlConfigEntry entry = new YamlConfigEntry(indentLevel, key, value, i + 1);
                entries.add(entry);

                // 检查值中是否包含URL
                Matcher urlMatcher = URL_PATTERN.matcher(value);
                if (urlMatcher.find()) {
                    entry.setContainsUrl(true);
                    entry.setUrl(urlMatcher.group(0));
                }
            }
        }

        return entries;
    }

    /**
     * 构建YAML配置的层次结构
     *
     * @param entries 配置项列表
     * @return 层次结构配置映射
     */
    @NotNull
    public static Map<String, Object> buildConfigHierarchy(@NotNull List<YamlConfigEntry> entries) {
        Map<String, Object> rootConfig = new HashMap<>();
        Map<Integer, Map<String, Object>> levelMaps = new HashMap<>();
        levelMaps.put(0, rootConfig);

        for (YamlConfigEntry entry : entries) {
            int level = entry.getIndentLevel();
            String key = entry.getKey();
            String value = entry.getValue();

            Map<String, Object> parentMap = levelMaps.get(level);
            if (parentMap == null) {
                // 查找最近的父级
                for (int i = level - 1; i >= 0; i--) {
                    if (levelMaps.containsKey(i)) {
                        parentMap = levelMaps.get(i);
                        break;
                    }
                }

                if (parentMap == null) {
                    // 如果没有找到父级，使用根配置
                    parentMap = rootConfig;
                }
            }

            if (value.isEmpty() || value.equals("\"\"")) {
                // 如果值为空，创建新的映射
                Map<String, Object> newMap = new HashMap<>();
                parentMap.put(key, newMap);
                levelMaps.put(level + 2, newMap); // YAML通常使用2空格缩进
            } else {
                // 如果值不为空，直接添加到父级映射
                parentMap.put(key, value);
            }
        }

        return rootConfig;
    }

    /**
     * 查找配置中的特定技术栈标识
     *
     * @param entries 配置项列表
     * @param keywords 关键词列表
     * @return 包含关键词的配置项
     */
    @NotNull
    public static List<YamlConfigEntry> findTechStackEntries(
            @NotNull List<YamlConfigEntry> entries,
            @NotNull List<String> keywords) {

        List<YamlConfigEntry> matchedEntries = new ArrayList<>();

        for (YamlConfigEntry entry : entries) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue().toLowerCase();

            for (String keyword : keywords) {
                keyword = keyword.toLowerCase();
                if (key.contains(keyword) || value.contains(keyword) || 
                        (entry.isContainsUrl() && entry.getUrl().toLowerCase().contains(keyword))) {
                    matchedEntries.add(entry);
                    break;
                }
            }
        }

        return matchedEntries;
    }

    /**
     * YAML配置项
     */
    public static class YamlConfigEntry {
        private final int indentLevel;
        private final String key;
        private final String value;
        private final int lineNumber;
        private boolean containsUrl;
        private String url;

        public YamlConfigEntry(int indentLevel, String key, String value, int lineNumber) {
            this.indentLevel = indentLevel;
            this.key = key;
            this.value = value;
            this.lineNumber = lineNumber;
            this.containsUrl = false;
            this.url = null;
        }

        public int getIndentLevel() { return indentLevel; }
        public String getKey() { return key; }
        public String getValue() { return value; }
        public int getLineNumber() { return lineNumber; }
        public boolean isContainsUrl() { return containsUrl; }
        public String getUrl() { return url; }

        public void setContainsUrl(boolean containsUrl) { this.containsUrl = containsUrl; }
        public void setUrl(String url) { this.url = url; }

        @Override
        public String toString() {
            return String.format("[L%d] %s%s: %s", lineNumber, 
                    "  ".repeat(indentLevel / 2), key, value);
        }
    }
}
