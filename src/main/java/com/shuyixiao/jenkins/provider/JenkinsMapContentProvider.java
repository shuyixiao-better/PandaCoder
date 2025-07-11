package com.shuyixiao.jenkins.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.shuyixiao.jenkins.util.JenkinsFileDetector;
import com.shuyixiao.jenkins.util.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Jenkins环境变量和参数映射内容提供器
 * 为env和params对象提供键值补全和类型推断
 */
public class JenkinsMapContentProvider {

    // 预定义的Jenkins环境变量
    private static final Set<String> ENV_DEFAULTS = Set.of(
        "BRANCH_NAME", "CHANGE_ID", "CHANGE_URL", "CHANGE_TITLE", "CHANGE_AUTHOR",
        "CHANGE_AUTHOR_DISPLAY_NAME", "CHANGE_AUTHOR_EMAIL", "CHANGE_TARGET", 
        "BUILD_NUMBER", "BUILD_ID", "BUILD_DISPLAY_NAME", "BUILD_TAG", "BUILD_URL",
        "JOB_NAME", "JOB_BASE_NAME", "JOB_URL", "JENKINS_URL", "JENKINS_HOME",
        "EXECUTOR_NUMBER", "NODE_NAME", "NODE_LABELS", "WORKSPACE", "WORKSPACE_TMP",
        "GIT_COMMIT", "GIT_PREVIOUS_COMMIT", "GIT_PREVIOUS_SUCCESSFUL_COMMIT",
        "GIT_BRANCH", "GIT_LOCAL_BRANCH", "GIT_URL", "GIT_COMMITTER_NAME",
        "GIT_COMMITTER_EMAIL", "GIT_AUTHOR_NAME", "GIT_AUTHOR_EMAIL"
    );

    // 参数方法类型映射
    private static final Map<String, String> PARAM_METHODS = Map.of(
        "booleanParam", "java.lang.Boolean",
        "string", "java.lang.String",
        "text", "java.lang.String",
        "password", "java.lang.String",
        "choice", "java.lang.String",
        "file", "java.lang.String",
        "run", "java.lang.String",
        "credentials", "java.lang.String",
        "gitParameter", "java.lang.String"
    );

    // 环境变量定义的正则表达式
    private static final Pattern ENV_ASSIGNMENT_PATTERN = 
        Pattern.compile("([A-Z_][A-Z0-9_]*)\\s*=\\s*['\"]?([^'\"\\n]*)['\"]?");

    /**
     * 获取键的变体集合
     */
    public Collection<String> getKeyVariants(@NotNull GrExpression qualifier, @Nullable PsiElement resolve) {
        if (resolve == null) {
            return Collections.emptyList();
        }

        PsiFile file = qualifier.getContainingFile();
        if (file == null || !JenkinsFileDetector.isJenkinsFile(file)) {
            return Collections.emptyList();
        }

        if (isEnvObject(resolve)) {
            return collectEnvironmentVariables(qualifier);
        } else if (isParamsObject(resolve)) {
            return collectParameterNames(qualifier);
        }

        return Collections.emptyList();
    }

    /**
     * 获取值的类型
     */
    public PsiType getValueType(@NotNull GrExpression qualifier, @Nullable PsiElement resolve, @NotNull String key) {
        if (resolve == null) {
            return null;
        }

        PsiFile file = qualifier.getContainingFile();
        if (file == null || !JenkinsFileDetector.isJenkinsFile(file)) {
            return null;
        }

        if (isEnvObject(resolve)) {
            // 环境变量都是字符串类型
            return GroovyPsiElementFactory.getInstance(qualifier.getProject())
                    .createTypeByFQClassName("java.lang.String", qualifier.getResolveScope());
        } else if (isParamsObject(resolve)) {
            // 根据参数定义返回相应类型
            String paramType = getParameterType(qualifier, key);
            return GroovyPsiElementFactory.getInstance(qualifier.getProject())
                    .createTypeByFQClassName(paramType, qualifier.getResolveScope());
        }

        return null;
    }

    /**
     * 检查是否为env对象
     */
    private boolean isEnvObject(@NotNull PsiElement element) {
        // 检查是否为env引用
        if (element instanceof GrReferenceExpression) {
            GrReferenceExpression ref = (GrReferenceExpression) element;
            return "env".equals(ref.getReferenceName());
        }
        
        // 检查文本内容
        String text = element.getText();
        return "env".equals(text);
    }

    /**
     * 检查是否为params对象
     */
    private boolean isParamsObject(@NotNull PsiElement element) {
        // 检查是否为params引用
        if (element instanceof GrReferenceExpression) {
            GrReferenceExpression ref = (GrReferenceExpression) element;
            return "params".equals(ref.getReferenceName());
        }
        
        // 检查文本内容
        String text = element.getText();
        return "params".equals(text);
    }

    /**
     * 收集环境变量
     */
    @NotNull
    private Collection<String> collectEnvironmentVariables(@NotNull GrExpression context) {
        Set<String> envVars = new HashSet<>(ENV_DEFAULTS);
        
        // 收集Pipeline脚本中定义的环境变量
        PsiFile file = context.getContainingFile();
        if (file != null) {
            String fileContent = file.getText();
            
            // 查找environment块中定义的变量
            envVars.addAll(findEnvironmentBlockVariables(fileContent));
            
            // 查找withEnv调用中定义的变量
            envVars.addAll(findWithEnvVariables(fileContent));
        }
        
        return envVars.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 收集参数名称
     */
    @NotNull
    private List<String> collectParameterNames(@NotNull GrExpression context) {
        List<String> paramNames = new ArrayList<>();
        
        PsiFile file = context.getContainingFile();
        if (file != null) {
            // 查找parameters块中的参数定义
            paramNames.addAll(findParametersBlockParameters(file));
        }
        
        return paramNames.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 获取参数类型
     */
    @NotNull
    private String getParameterType(@NotNull GrExpression context, @NotNull String paramName) {
        PsiFile file = context.getContainingFile();
        if (file != null) {
            String paramType = findParameterTypeInFile(file, paramName);
            if (paramType != null) {
                return paramType;
            }
        }
        
        // 默认返回字符串类型
        return "java.lang.String";
    }

    /**
     * 在文件中查找environment块中定义的变量
     */
    @NotNull
    private Set<String> findEnvironmentBlockVariables(@NotNull String content) {
        Set<String> envVars = new HashSet<>();
        
        // 简单的模式匹配查找environment块
        Pattern envBlockPattern = Pattern.compile(
            "environment\\s*\\{([^}]*?)\\}", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher envBlockMatcher = envBlockPattern.matcher(content);
        while (envBlockMatcher.find()) {
            String envBlock = envBlockMatcher.group(1);
            
            // 在environment块中查找变量定义
            Matcher varMatcher = ENV_ASSIGNMENT_PATTERN.matcher(envBlock);
            while (varMatcher.find()) {
                String varName = varMatcher.group(1);
                if (varName != null && !varName.isEmpty()) {
                    envVars.add(varName);
                }
            }
        }
        
        return envVars;
    }

    /**
     * 查找withEnv调用中定义的变量
     */
    @NotNull
    private Set<String> findWithEnvVariables(@NotNull String content) {
        Set<String> envVars = new HashSet<>();
        
        // 查找withEnv调用
        Pattern withEnvPattern = Pattern.compile(
            "withEnv\\s*\\(\\s*\\[(.*?)\\]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher withEnvMatcher = withEnvPattern.matcher(content);
        while (withEnvMatcher.find()) {
            String envList = withEnvMatcher.group(1);
            
            // 查找环境变量定义
            Pattern varPattern = Pattern.compile("['\"]([A-Z_][A-Z0-9_]*)\\s*=");
            Matcher varMatcher = varPattern.matcher(envList);
            while (varMatcher.find()) {
                String varName = varMatcher.group(1);
                if (varName != null && !varName.isEmpty()) {
                    envVars.add(varName);
                }
            }
        }
        
        return envVars;
    }

    /**
     * 查找parameters块中的参数定义
     */
    @NotNull
    private List<String> findParametersBlockParameters(@NotNull PsiFile file) {
        List<String> paramNames = new ArrayList<>();
        
        // 使用PSI API查找参数定义
        Collection<GrMethodCall> methodCalls = PsiTreeUtil.findChildrenOfType(file, GrMethodCall.class);
        
        for (GrMethodCall methodCall : methodCalls) {
            if (isInParametersBlock(methodCall)) {
                String methodName = PsiUtils.getMethodName(methodCall);
                if (methodName != null && PARAM_METHODS.containsKey(methodName)) {
                    String paramName = extractParameterName(methodCall);
                    if (paramName != null && !paramName.isEmpty()) {
                        paramNames.add(paramName);
                    }
                }
            }
        }
        
        return paramNames;
    }

    /**
     * 检查方法调用是否在parameters块内
     */
    private boolean isInParametersBlock(@NotNull GrMethodCall methodCall) {
        return PsiUtils.isInClosureBlock(methodCall, "parameters");
    }

    /**
     * 从参数方法调用中提取参数名称
     */
    @Nullable
    private String extractParameterName(@NotNull GrMethodCall methodCall) {
        // 查找name参数
        return extractNamedParameter(methodCall, "name");
    }

    /**
     * 提取命名参数的值
     */
    @Nullable
    private String extractNamedParameter(@NotNull GrMethodCall methodCall, @NotNull String paramName) {
        // 这是一个简化的实现，实际情况下需要更复杂的AST解析
        String text = methodCall.getText();
        
        Pattern pattern = Pattern.compile(paramName + "\\s*[=:]\\s*['\"]([^'\"]*)['\"]");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * 在文件中查找指定参数的类型
     */
    @Nullable
    private String findParameterTypeInFile(@NotNull PsiFile file, @NotNull String paramName) {
        Collection<GrMethodCall> methodCalls = PsiTreeUtil.findChildrenOfType(file, GrMethodCall.class);
        
        for (GrMethodCall methodCall : methodCalls) {
            if (isInParametersBlock(methodCall)) {
                String extractedName = extractParameterName(methodCall);
                if (paramName.equals(extractedName)) {
                    String methodName = PsiUtils.getMethodName(methodCall);
                    if (methodName != null) {
                        return PARAM_METHODS.getOrDefault(methodName, "java.lang.String");
                    }
                }
            }
        }
        
        return null;
    }
} 