package com.shuyixiao.bugrecorder.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 堆栈跟踪元素
 * 存储单个堆栈跟踪行的信息
 */
public class StackTraceElement {

    private final String className;
    private final String methodName;
    private final String location;
    private final String jarFile;

    public StackTraceElement(@NotNull String className, @NotNull String methodName,
                             @Nullable String location, @Nullable String jarFile) {
        this.className = className;
        this.methodName = methodName;
        this.location = location;
        this.jarFile = jarFile;
    }

    @NotNull
    public String getClassName() {
        return className;
    }

    @NotNull
    public String getMethodName() {
        return methodName;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    @Nullable
    public String getJarFile() {
        return jarFile;
    }

    /**
     * 获取简短的类名（不包含包路径）
     */
    public String getSimpleClassName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * 获取包名
     */
    public String getPackageName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * 判断是否为用户代码（非第三方库）
     */
    public boolean isUserCode() {
        // 简单判断：不包含常见的第三方库包名
        return !className.startsWith("java.") &&
                !className.startsWith("javax.") &&
                !className.startsWith("org.springframework.") &&
                !className.startsWith("org.apache.") &&
                !className.startsWith("com.mysql.") &&
                !className.startsWith("org.hibernate.") &&
                !className.startsWith("sun.") &&
                !className.startsWith("com.sun.");
    }

    /**
     * 获取行号（如果location包含行号信息）
     */
    public int getLineNumber() {
        if (location == null) {
            return -1;
        }

        try {
            // 尝试从location中提取行号，格式如：FileName.java:123
            int colonIndex = location.lastIndexOf(':');
            if (colonIndex > 0 && colonIndex < location.length() - 1) {
                String lineStr = location.substring(colonIndex + 1);
                return Integer.parseInt(lineStr);
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }

        return -1;
    }

    /**
     * 获取文件名
     */
    public String getFileName() {
        if (location == null) {
            return null;
        }

        int colonIndex = location.lastIndexOf(':');
        return colonIndex > 0 ? location.substring(0, colonIndex) : location;
    }

    /**
     * 格式化显示堆栈跟踪元素
     */
    public String getFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("at ").append(className).append(".").append(methodName);

        if (location != null) {
            sb.append("(").append(location).append(")");
        }

        if (jarFile != null) {
            sb.append(" [").append(jarFile).append("]");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackTraceElement that = (StackTraceElement) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(location, that.location) &&
                Objects.equals(jarFile, that.jarFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, location, jarFile);
    }

    @Override
    public String toString() {
        return getFormattedString();
    }
}