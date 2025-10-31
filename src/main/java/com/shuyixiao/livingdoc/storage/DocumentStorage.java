package com.shuyixiao.livingdoc.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.livingdoc.analyzer.model.ProjectDocumentation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文档存储管理器
 * 
 * <p>负责将生成的文档保存到本地文件系统
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class DocumentStorage {

    private static final Logger LOG = Logger.getInstance(DocumentStorage.class);
    private static final String STORAGE_DIR = ".livingdoc";
    private static final String DOC_FILE = "api-documentation.json";
    private static final String MD_FILE = "API文档.md";

    private final Project project;
    private final Gson gson;

    public DocumentStorage(@NotNull Project project) {
        this.project = project;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * 保存文档（JSON 格式）
     */
    public void saveDocumentation(@NotNull ProjectDocumentation doc) throws IOException {
        File storageDir = getStorageDir();
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        File docFile = new File(storageDir, DOC_FILE);
        try (FileWriter writer = new FileWriter(docFile)) {
            gson.toJson(doc, writer);
        }
    }
    
    /**
     * 保存 Markdown 文档
     */
    public void saveMarkdown(@NotNull String markdown) throws IOException {
        File storageDir = getStorageDir();
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        File mdFile = new File(storageDir, MD_FILE);
        try (FileWriter writer = new FileWriter(mdFile)) {
            writer.write(markdown);
        }
    }
    
    /**
     * 读取文档
     */
    public ProjectDocumentation loadDocumentation() throws IOException {
        File docFile = new File(getStorageDir(), DOC_FILE);
        if (!docFile.exists()) {
            return null;
        }

        try {
            String json = new String(Files.readAllBytes(docFile.toPath()));

            // ✅ 使用 JsonReader 并设置 LENIENT 模式来容忍格式不严格的 JSON
            try (StringReader stringReader = new StringReader(json)) {
                JsonReader jsonReader = new JsonReader(stringReader);
                jsonReader.setStrictness(Strictness.LENIENT);

                return gson.fromJson(jsonReader, ProjectDocumentation.class);
            }
        } catch (JsonSyntaxException e) {
            // ✅ JSON 格式错误，备份损坏的文件
            LOG.error("Documentation file is corrupted, backing up", e);
            backupCorruptedFile(docFile);
            return null;
        }
    }

    /**
     * 备份损坏的记录文件
     */
    private void backupCorruptedFile(File file) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backupFile = new File(file.getParent(), file.getName() + ".corrupted." + timestamp);

            if (file.renameTo(backupFile)) {
                LOG.info("Corrupted documentation file backed up to: " + backupFile.getName());
            } else {
                LOG.warn("Failed to backup corrupted documentation file");
            }
        } catch (Exception e) {
            LOG.error("Error backing up corrupted documentation file", e);
        }
    }
    
    /**
     * 读取 Markdown 文档
     */
    public String loadMarkdown() throws IOException {
        File mdFile = new File(getStorageDir(), MD_FILE);
        if (!mdFile.exists()) {
            return null;
        }
        
        return new String(Files.readAllBytes(mdFile.toPath()));
    }
    
    /**
     * 获取存储目录
     */
    private File getStorageDir() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new IllegalStateException("Project base path is null");
        }
        return new File(basePath, STORAGE_DIR);
    }
    
    /**
     * 获取 Markdown 文件路径
     */
    public String getMarkdownPath() {
        return new File(getStorageDir(), MD_FILE).getAbsolutePath();
    }
    
    /**
     * 检查文档是否存在
     */
    public boolean exists() {
        return new File(getStorageDir(), DOC_FILE).exists();
    }
}

