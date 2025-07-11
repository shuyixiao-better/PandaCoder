package com.shuyixiao.jenkins.documentation;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.shuyixiao.jenkins.gdsl.JenkinsGdslService;
import com.shuyixiao.jenkins.model.Descriptor;
import com.shuyixiao.jenkins.util.JenkinsFileDetector;
import com.shuyixiao.jenkins.util.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.documentation.GroovyDocumentationProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.List;

/**
 * Jenkins文档提供器
 * 为Jenkins Pipeline语法提供上下文文档，委托给GroovyDocumentationProvider处理基础功能
 */
public class JenkinsDocumentationProvider implements DocumentationProvider {
    
    private final GroovyDocumentationProvider delegated = new GroovyDocumentationProvider();

    @Override
    public String generateDoc(@NotNull PsiElement element, @Nullable PsiElement originalElement) {
        // 首先检查是否为Jenkins文件
        PsiFile file = element.getContainingFile();
        if (file == null || !JenkinsFileDetector.isJenkinsFile(file)) {
            return delegated.generateDoc(element, originalElement);
        }

        // 检查是否为Jenkins Pipeline方法
        String jenkinsDoc = generateJenkinsDocumentation(element, originalElement);
        if (jenkinsDoc != null) {
            return jenkinsDoc;
        }

        // 回退到标准文档
        return delegated.generateDoc(element, originalElement);
    }

    @Override
    public PsiElement getDocumentationElementForLookupItem(@NotNull PsiManager psiManager, 
                                                          @NotNull Object object, 
                                                          @NotNull PsiElement element) {
        return delegated.getDocumentationElementForLookupItem(psiManager, object, element);
    }

    @Override
    public PsiElement getDocumentationElementForLink(@NotNull PsiManager psiManager, 
                                                    @NotNull String link, 
                                                    @NotNull PsiElement context) {
        return delegated.getDocumentationElementForLink(psiManager, link, context);
    }

    @Override
    public List<String> getUrlFor(@NotNull PsiElement element, @NotNull PsiElement originalElement) {
        // 首先尝试获取Jenkins文档URL
        List<String> jenkinsUrls = getJenkinsDocumentationUrls(element, originalElement);
        if (jenkinsUrls != null && !jenkinsUrls.isEmpty()) {
            return jenkinsUrls;
        }

        return null;
    }

    /**
     * 生成Jenkins特定的文档
     */
    @Nullable
    private String generateJenkinsDocumentation(@NotNull PsiElement element, @Nullable PsiElement originalElement) {
        // 检查是否为方法调用
        if (element instanceof GrReferenceExpression) {
            GrReferenceExpression ref = (GrReferenceExpression) element;
            String methodName = ref.getReferenceName();
            
            if (methodName != null && PsiUtils.isJenkinsBuiltinMethodName(methodName)) {
                return buildJenkinsMethodDocumentation(methodName, element);
            }
        }

        // 检查父元素是否为方法调用
        PsiElement parent = element.getParent();
        if (parent instanceof GrMethodCall) {
            GrMethodCall methodCall = (GrMethodCall) parent;
            String methodName = PsiUtils.getMethodName(methodCall);
            
            if (methodName != null && PsiUtils.isJenkinsBuiltinMethodName(methodName)) {
                return buildJenkinsMethodDocumentation(methodName, element);
            }
        }

        return null;
    }

    /**
     * 构建Jenkins方法文档
     */
    @NotNull
    private String buildJenkinsMethodDocumentation(@NotNull String methodName, @NotNull PsiElement context) {
        Project project = context.getProject();
        JenkinsGdslService service = JenkinsGdslService.getInstance(project);
        
        Descriptor descriptor = service.getDescriptor("jenkins", methodName);
        if (descriptor != null) {
            return buildGdslDocumentation(descriptor);
        }

        // 如果没有找到描述符，构建基本文档
        return buildBasicMethodDocumentation(methodName);
    }

    /**
     * 构建GDSL描述符文档
     */
    @NotNull
    private String buildGdslDocumentation(@NotNull Descriptor descriptor) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class='definition'>");
        html.append("<pre>");
        
        // 构建方法签名
        html.append("<b>").append(descriptor.getName()).append("</b>");
        
        List<Descriptor.Parameter> parameters = descriptor.getParameters();
        if (!parameters.isEmpty()) {
            html.append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) html.append(", ");
                
                Descriptor.Parameter param = parameters.get(i);
                html.append("<i>").append(param.getType()).append("</i> ");
                html.append(param.getName());
                
                if (!param.isRequired()) {
                    html.append(" <span style='color: #888;'>[可选]</span>");
                }
            }
            html.append(")");
        } else {
            html.append("()");
        }
        
        html.append("</pre>");
        html.append("</div>");
        
        // 添加描述内容
        if (!descriptor.getDocumentation().isEmpty()) {
            html.append("<div class='content'>");
            html.append("<p>").append(descriptor.getDocumentation()).append("</p>");
            html.append("</div>");
        }
        
        // 添加参数文档
        if (!parameters.isEmpty()) {
            html.append("<div class='sections'>");
            html.append("<p><b>参数:</b></p>");
            html.append("<table>");
            
            for (Descriptor.Parameter param : parameters) {
                html.append("<tr>");
                html.append("<td valign='top'><code>").append(param.getName()).append("</code></td>");
                html.append("<td valign='top'>").append(param.getType());
                if (param.isRequired()) {
                    html.append(" <span style='color: #d73a49;'>(必需)</span>");
                }
                html.append("</td>");
                html.append("<td>").append(param.getDocumentation()).append("</td>");
                html.append("</tr>");
            }
            
            html.append("</table>");
            html.append("</div>");
        }
        
        return html.toString();
    }

    /**
     * 构建基本方法文档
     */
    @NotNull
    private String buildBasicMethodDocumentation(@NotNull String methodName) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class='definition'>");
        html.append("<pre><b>").append(methodName).append("</b></pre>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        html.append("<p>Jenkins Pipeline内置步骤: <code>").append(methodName).append("</code></p>");
        html.append("<p>请参考Jenkins官方文档了解详细用法。</p>");
        html.append("</div>");
        
        return html.toString();
    }

    /**
     * 获取Jenkins文档URL
     */
    @Nullable
    private List<String> getJenkinsDocumentationUrls(@NotNull PsiElement element, @NotNull PsiElement originalElement) {
        // 检查是否为Jenkins方法
        String methodName = null;
        
        if (element instanceof GrReferenceExpression) {
            methodName = ((GrReferenceExpression) element).getReferenceName();
        } else if (element.getParent() instanceof GrMethodCall) {
            methodName = PsiUtils.getMethodName((GrMethodCall) element.getParent());
        }
        
        if (methodName != null && PsiUtils.isJenkinsBuiltinMethodName(methodName)) {
            // 返回Jenkins Pipeline文档URL
            String baseUrl = "https://www.jenkins.io/doc/pipeline/steps/";
            return List.of(baseUrl + "workflow-basic-steps/");
        }
        
        return null;
    }

    /**
     * 检查是否有Jenkins文档
     */
    private boolean hasJenkinsDocumentation(@NotNull PsiElement element) {
        if (element instanceof GrReferenceExpression) {
            String methodName = ((GrReferenceExpression) element).getReferenceName();
            return methodName != null && PsiUtils.isJenkinsBuiltinMethodName(methodName);
        }
        
        PsiElement parent = element.getParent();
        if (parent instanceof GrMethodCall) {
            String methodName = PsiUtils.getMethodName((GrMethodCall) parent);
            return methodName != null && PsiUtils.isJenkinsBuiltinMethodName(methodName);
        }
        
        return false;
    }
} 