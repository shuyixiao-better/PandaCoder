package com.shuyixiao.jenkins.gdsl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.shuyixiao.jenkins.model.Descriptor;
import com.shuyixiao.jenkins.util.JenkinsFileDetector;
import com.shuyixiao.jenkins.util.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.List;
import java.util.Map;

/**
 * Jenkins GDSL成员提供器
 * 提供Jenkins Pipeline的智能补全支持
 */
public class JenkinsGdslMembersProvider {

    /**
     * 处理动态元素补全
     */
    public void processDynamicElements(@NotNull PsiElement context) {
        PsiFile file = context.getContainingFile();
        if (file == null || !JenkinsFileDetector.isJenkinsFile(file)) {
            return;
        }

        JenkinsGdslService service = JenkinsGdslService.getInstance(file.getProject());
        if (service == null) {
            return;
        }

        // 获取所有Jenkins描述符
        Map<String, Descriptor> descriptors = service.getAllDescriptors("jenkins");
        if (descriptors.isEmpty()) {
            return;
        }

        // 根据上下文提供不同的补全 - 这里可以扩展实际的补全逻辑
        // 目前作为基础实现，实际补全功能可以通过其他方式提供
    }

    /**
     * 获取Jenkins Pipeline相关的补全建议
     */
    public List<String> getJenkinsPipelineCompletions(@NotNull PsiElement context) {
        PsiFile file = context.getContainingFile();
        if (file == null || !JenkinsFileDetector.isJenkinsFile(file)) {
            return List.of();
        }

        JenkinsGdslService service = JenkinsGdslService.getInstance(file.getProject());
        return service.getAllDefinitionNames();
    }

    /**
     * 获取在指定上下文中可用的Jenkins指令
     */
    public List<String> getAvailableDirectives(@NotNull PsiElement context) {
        if (isInPipelineBlock(context)) {
            return List.of("agent", "stages", "environment", "options", "parameters", "tools", "post", "triggers");
        } else if (isInStagesBlock(context)) {
            return List.of("stage");
        } else if (isInStageBlock(context)) {
            return List.of("steps", "agent", "when", "post", "environment", "tools");
        } else if (isInStepsBlock(context)) {
            return List.of("sh", "bat", "powershell", "echo", "checkout", "git", "script", 
                          "parallel", "build", "archiveArtifacts", "publishTestResults", 
                          "emailext", "input", "timeout", "retry", "catchError", "stash", 
                          "unstash", "dir", "deleteDir", "pwd", "readFile", "writeFile", 
                          "fileExists", "isUnix", "tool", "withEnv", "withCredentials");
        } else if (isInPostBlock(context)) {
            return List.of("always", "success", "failure", "unstable", "changed");
        } else if (isInParametersBlock(context)) {
            return List.of("string", "booleanParam", "choice", "password", "text", "file");
        } else if (PsiUtils.isTopLevel(context)) {
            return List.of("pipeline", "node");
        }
        return List.of();
    }

    /**
     * 检查是否为特定属性的引用
     */
    public boolean isPropertyOf(@NotNull String name, @NotNull PsiElement context) {
        PsiElement parent = context.getParent();
        if (parent instanceof GrReferenceExpression) {
            GrExpression qualifier = ((GrReferenceExpression) parent).getQualifierExpression();
            return qualifier != null && name.equals(qualifier.getText());
        }
        return false;
    }

    // 上下文检查辅助方法

    private boolean isInPipelineBlock(@NotNull PsiElement context) {
        return PsiUtils.isInClosureBlock(context, "pipeline");
    }

    private boolean isInStagesBlock(@NotNull PsiElement context) {
        return PsiUtils.isInClosureBlock(context, "stages");
    }

    private boolean isInStageBlock(@NotNull PsiElement context) {
        return PsiUtils.isInClosureBlock(context, "stage");
    }

    private boolean isInStepsBlock(@NotNull PsiElement context) {
        return PsiUtils.isInClosureBlock(context, "steps");
    }

    private boolean isInPostBlock(@NotNull PsiElement context) {
        return PsiUtils.isInClosureBlock(context, "post");
    }

    private boolean isInParametersBlock(@NotNull PsiElement context) {
        return PsiUtils.isInClosureBlock(context, "parameters");
    }
} 