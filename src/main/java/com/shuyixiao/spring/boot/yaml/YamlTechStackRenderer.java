package com.shuyixiao.spring.boot.yaml;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.shuyixiao.spring.boot.SpringBootFileDetector;
import com.shuyixiao.spring.boot.icon.SpringBootYamlLineMarkerProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML技术栈渲染器
 * 用于在编辑器中渲染YAML配置文件的技术栈图标
 */
public class YamlTechStackRenderer implements EditorFactoryListener {

    private static final Logger LOG = Logger.getInstance(YamlTechStackRenderer.class);

    // 编辑器到高亮显示的映射，用于清理之前的高亮
    private static final Map<Editor, Map<Integer, RangeHighlighter>> EDITOR_HIGHLIGHTERS = new HashMap<>();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);

        if (file != null && SpringBootFileDetector.isSpringBootYamlFile(file)) {
            LOG.debug("Spring Boot YAML file opened: " + file.getPath());
            renderTechStackIcons(editor, file);
        }
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        // 清理编辑器高亮显示
        if (EDITOR_HIGHLIGHTERS.containsKey(editor)) {
            EDITOR_HIGHLIGHTERS.remove(editor);
        }
    }

    /**
     * 为指定编辑器渲染技术栈图标
     *
     * @param editor 当前编辑器
     * @param file 虚拟文件
     */
    public static void renderTechStackIcons(@NotNull Editor editor, @NotNull VirtualFile file) {
        Project project = editor.getProject();
        if (project == null) return;

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) return;

        // 清理现有高亮
        clearHighlighters(editor);

        // 读取YAML内容
        String yamlContent = editor.getDocument().getText();
        List<YamlConfigProcessor.YamlConfigEntry> entries = YamlConfigProcessor.extractConfigEntries(yamlContent);

        // 为每个技术栈添加图标
        for (SpringBootYamlLineMarkerProvider.TechStackConfig techStack : 
                SpringBootYamlLineMarkerProvider.getTechStackConfigs().values()) {

            // 查找匹配该技术栈的配置项
            List<YamlConfigProcessor.YamlConfigEntry> matchedEntries = YamlConfigProcessor.findTechStackEntries(
                    entries,
                    techStack.getConfigKeywords());

            // 添加技术栈图标高亮
            for (YamlConfigProcessor.YamlConfigEntry entry : matchedEntries) {
                addTechStackIconHighlighter(editor, entry.getLineNumber() - 1, techStack);
            }
        }
    }

    /**
     * 添加技术栈图标高亮显示
     *
     * @param editor 编辑器
     * @param lineNumber 行号
     * @param techStack 技术栈配置
     */
    private static void addTechStackIconHighlighter(
            @NotNull Editor editor,
            int lineNumber,
            @NotNull SpringBootYamlLineMarkerProvider.TechStackConfig techStack) {

        Document document = editor.getDocument();
        if (lineNumber < 0 || lineNumber >= document.getLineCount()) return;

        int startOffset = document.getLineStartOffset(lineNumber);
        int endOffset = document.getLineEndOffset(lineNumber);

        Icon icon = techStack.getIcon();
        if (icon == null) return;

        // 创建自定义文本属性
        TextAttributes attributes = new TextAttributes();
        attributes.setEffectType(null);
        attributes.setEffectColor(JBColor.LIGHT_GRAY);

        // 添加高亮显示
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                attributes,
                HighlighterTargetArea.LINES_IN_RANGE);

        // 设置图标和工具提示
        highlighter.setGutterIconRenderer(new YamlTechStackGutterIconRenderer(
                icon,
                techStack.getDisplayName()));

        // 保存高亮显示以便后续清理
        EDITOR_HIGHLIGHTERS
                .computeIfAbsent(editor, k -> new HashMap<>())
                .put(lineNumber, highlighter);

        LOG.debug("Added tech stack icon for " + techStack.getDisplayName() + " at line " + (lineNumber + 1));
    }

    /**
     * 清理编辑器的所有高亮显示
     *
     * @param editor 编辑器
     */
    private static void clearHighlighters(@NotNull Editor editor) {
        if (EDITOR_HIGHLIGHTERS.containsKey(editor)) {
            Map<Integer, RangeHighlighter> highlighters = EDITOR_HIGHLIGHTERS.get(editor);
            for (RangeHighlighter highlighter : highlighters.values()) {
                editor.getMarkupModel().removeHighlighter(highlighter);
            }
            highlighters.clear();
        }
    }

    /**
     * 更新当前打开的编辑器
     *
     * @param project 项目
     */
    public static void updateOpenEditors(@NotNull Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

        for (VirtualFile file : openFiles) {
            if (SpringBootFileDetector.isSpringBootYamlFile(file)) {
                Editor editor = fileEditorManager.getSelectedTextEditor();
                if (editor != null && FileDocumentManager.getInstance().getFile(editor.getDocument()) == file) {
                    renderTechStackIcons(editor, file);
                }
            }
        }
    }
}
