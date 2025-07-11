package com.shuyixiao.jenkins.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

/**
 * PSI工具类
 * 提供PSI元素操作的便利方法
 */
public class PsiUtils {
    
    /**
     * 获取包含指定元素的方法
     */
    @Nullable
    public static GrMethod getContainingMethod(@NotNull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, GrMethod.class);
    }
    
    /**
     * 获取包含指定元素的闭包块
     */
    @Nullable
    public static GrClosableBlock getContainingClosure(@NotNull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, GrClosableBlock.class);
    }
    
    /**
     * 获取方法调用的名称
     */
    @Nullable
    public static String getMethodName(@NotNull GrMethodCall methodCall) {
        PsiElement invokedExpression = methodCall.getInvokedExpression();
        if (invokedExpression instanceof GrReferenceExpression) {
            return ((GrReferenceExpression) invokedExpression).getReferenceName();
        }
        return invokedExpression != null ? invokedExpression.getText() : null;
    }
    
    /**
     * 检查是否为Jenkins内置方法
     */
    public static boolean isJenkinsBuiltinMethod(@Nullable PsiElement method) {
        if (method == null) {
            return false;
        }
        
        String methodName = method.getText();
        return isJenkinsBuiltinMethodName(methodName);
    }
    
    /**
     * 检查方法名是否为Jenkins内置方法
     */
    public static boolean isJenkinsBuiltinMethodName(@Nullable String methodName) {
        if (methodName == null) {
            return false;
        }
        
        // Jenkins Pipeline 内置步骤
        return methodName.equals("sh") ||
               methodName.equals("bat") ||
               methodName.equals("powershell") ||
               methodName.equals("echo") ||
               methodName.equals("checkout") ||
               methodName.equals("git") ||
               methodName.equals("stage") ||
               methodName.equals("steps") ||
               methodName.equals("script") ||
               methodName.equals("parallel") ||
               methodName.equals("build") ||
               methodName.equals("archiveArtifacts") ||
               methodName.equals("publishTestResults") ||
               methodName.equals("emailext") ||
               methodName.equals("input") ||
               methodName.equals("timeout") ||
               methodName.equals("retry") ||
               methodName.equals("catchError") ||
               methodName.equals("unstash") ||
               methodName.equals("stash") ||
               methodName.equals("dir") ||
               methodName.equals("deleteDir") ||
               methodName.equals("pwd") ||
               methodName.equals("readFile") ||
               methodName.equals("writeFile") ||
               methodName.equals("fileExists") ||
               methodName.equals("isUnix") ||
               methodName.equals("tool") ||
               methodName.equals("withEnv") ||
               methodName.equals("withCredentials") ||
               methodName.equals("node") ||
               methodName.equals("pipeline") ||
               methodName.equals("agent") ||
               methodName.equals("stages") ||
               methodName.equals("post") ||
               methodName.equals("environment") ||
               methodName.equals("parameters") ||
               methodName.equals("options") ||
               methodName.equals("tools") ||
               methodName.equals("when") ||
               methodName.equals("matrix") ||
               methodName.equals("triggers");
    }
    
    /**
     * 检查元素是否在顶层代码中（不在任何方法或闭包内）
     */
    public static boolean isTopLevel(@NotNull PsiElement element) {
        return getContainingMethod(element) == null && getContainingClosure(element) == null;
    }
    
    /**
     * 获取元素的文本内容，如果为null则返回空字符串
     */
    @NotNull
    public static String getTextSafe(@Nullable PsiElement element) {
        return element != null ? element.getText() : "";
    }
    
    /**
     * 检查是否为特定类型的引用表达式
     */
    public static boolean isReferenceExpression(@NotNull PsiElement element, @NotNull String referenceName) {
        if (element instanceof GrReferenceExpression) {
            GrReferenceExpression ref = (GrReferenceExpression) element;
            return referenceName.equals(ref.getReferenceName());
        }
        return false;
    }
    
    /**
     * 检查元素是否在指定名称的闭包块内
     */
    public static boolean isInClosureBlock(@NotNull PsiElement element, @NotNull String blockName) {
        GrClosableBlock closureBlock = getContainingClosure(element);
        if (closureBlock == null) {
            return false;
        }
        
        PsiElement parent = closureBlock.getParent();
        if (parent instanceof GrMethodCall) {
            String methodName = getMethodName((GrMethodCall) parent);
            return blockName.equals(methodName);
        }
        
        return false;
    }
} 