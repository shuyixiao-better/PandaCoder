package com.shuyixiao.spring.boot.yaml;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * YAML技术栈图标渲染器
 * 用于在编辑器左侧沟槽区域显示技术栈图标
 */
public class YamlTechStackGutterIconRenderer extends GutterIconRenderer {

    private final Icon icon;
    private final String tooltip;

    public YamlTechStackGutterIconRenderer(@NotNull Icon icon, @NotNull String tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return icon;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "技术栈: " + tooltip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YamlTechStackGutterIconRenderer that = (YamlTechStackGutterIconRenderer) o;
        return Objects.equals(icon, that.icon) && Objects.equals(tooltip, that.tooltip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icon, tooltip);
    }
}
