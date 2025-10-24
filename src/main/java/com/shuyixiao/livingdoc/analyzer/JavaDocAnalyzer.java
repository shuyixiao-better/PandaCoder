package com.shuyixiao.livingdoc.analyzer;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTypesUtil;
import com.shuyixiao.livingdoc.analyzer.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Java 代码分析器
 * 
 * <p>使用 IntelliJ PSI API 分析 Spring Boot 项目代码，提取 API 接口信息
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class JavaDocAnalyzer {
    
    private final Project project;
    
    public JavaDocAnalyzer(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * 分析项目，提取所有 API 文档信息
     */
    public ProjectDocumentation analyze() {
        ProjectDocumentation doc = new ProjectDocumentation();
        doc.setProjectName(project.getName());
        doc.setProjectPath(project.getBasePath());
        
        // 查找所有 @RestController 和 @Controller 类
        List<PsiClass> controllers = findControllers();
        
        // 分析每个 Controller
        List<ApiEndpoint> endpoints = new ArrayList<>();
        for (PsiClass controller : controllers) {
            endpoints.addAll(analyzeController(controller));
        }
        
        doc.setEndpoints(endpoints);
        return doc;
    }
    
    /**
     * 查找所有 Controller 类
     */
    private List<PsiClass> findControllers() {
        List<PsiClass> controllers = new ArrayList<>();
        
        // 获取 Java Facade
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        
        // 使用 allScope 而不是 projectScope，以确保搜索所有依赖
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        
        System.out.println("[LivingDoc] 开始搜索 Controller...");
        System.out.println("[LivingDoc] 项目路径: " + project.getBasePath());
        System.out.println("[LivingDoc] 搜索范围: " + scope);
        
        // 查找 @RestController
        PsiClass restControllerClass = javaPsiFacade.findClass(
            "org.springframework.web.bind.annotation.RestController", scope);
        System.out.println("[LivingDoc] RestController 注解类: " + (restControllerClass != null ? "找到" : "未找到"));
        
        if (restControllerClass != null) {
            Collection<PsiClass> restControllers = AnnotatedElementsSearch
                .searchPsiClasses(restControllerClass, GlobalSearchScope.projectScope(project))
                .findAll();
            System.out.println("[LivingDoc] 找到 @RestController 类数量: " + restControllers.size());
            controllers.addAll(restControllers);
        }
        
        // 查找 @Controller
        PsiClass controllerClass = javaPsiFacade.findClass(
            "org.springframework.stereotype.Controller", scope);
        System.out.println("[LivingDoc] Controller 注解类: " + (controllerClass != null ? "找到" : "未找到"));
        
        if (controllerClass != null) {
            Collection<PsiClass> stdControllers = AnnotatedElementsSearch
                .searchPsiClasses(controllerClass, GlobalSearchScope.projectScope(project))
                .findAll();
            System.out.println("[LivingDoc] 找到 @Controller 类数量: " + stdControllers.size());
            controllers.addAll(stdControllers);
        }
        
        System.out.println("[LivingDoc] 总共找到 Controller 类数量: " + controllers.size());
        
        return controllers;
    }
    
    /**
     * 分析单个 Controller 类
     */
    private List<ApiEndpoint> analyzeController(@NotNull PsiClass controller) {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        
        // 获取类级别的 @RequestMapping
        String baseUrl = extractRequestMapping(controller);
        
        // 遍历所有方法
        for (PsiMethod method : controller.getMethods()) {
            // 检查是否有 @RequestMapping 或 @GetMapping 等注解
            ApiEndpoint endpoint = analyzeMethod(method, baseUrl, controller);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }
        
        return endpoints;
    }
    
    /**
     * 分析单个方法
     */
    private ApiEndpoint analyzeMethod(@NotNull PsiMethod method, String baseUrl, @NotNull PsiClass controller) {
        // 检查方法是否有映射注解
        PsiAnnotation[] annotations = method.getAnnotations();
        String methodUrl = null;
        String httpMethod = "GET";
        
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) continue;
            
            if (qualifiedName.endsWith("RequestMapping")) {
                methodUrl = extractAnnotationValue(annotation, "value");
                String method2 = extractAnnotationValue(annotation, "method");
                if (method2 != null && !method2.isEmpty()) {
                    httpMethod = method2.replace("RequestMethod.", "");
                }
            } else if (qualifiedName.endsWith("GetMapping")) {
                methodUrl = extractAnnotationValue(annotation, "value");
                httpMethod = "GET";
            } else if (qualifiedName.endsWith("PostMapping")) {
                methodUrl = extractAnnotationValue(annotation, "value");
                httpMethod = "POST";
            } else if (qualifiedName.endsWith("PutMapping")) {
                methodUrl = extractAnnotationValue(annotation, "value");
                httpMethod = "PUT";
            } else if (qualifiedName.endsWith("DeleteMapping")) {
                methodUrl = extractAnnotationValue(annotation, "value");
                httpMethod = "DELETE";
            } else if (qualifiedName.endsWith("PatchMapping")) {
                methodUrl = extractAnnotationValue(annotation, "value");
                httpMethod = "PATCH";
            }
        }
        
        // 如果没有映射注解，跳过
        if (methodUrl == null) {
            return null;
        }
        
        // 构建完整 URL
        String fullUrl = combineUrls(baseUrl, methodUrl);
        
        // 创建 ApiEndpoint
        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setUrl(fullUrl);
        endpoint.setMethod(httpMethod);
        endpoint.setMethodName(method.getName());
        endpoint.setClassName(controller.getQualifiedName());
        endpoint.setFilePath(getFilePath(controller));
        endpoint.setLineNumber(getLineNumber(method));
        
        // 提取方法注释作为描述
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            String description = extractDescription(docComment);
            endpoint.setDescription(description);
        }
        
        // 提取参数
        List<Parameter> parameters = extractParameters(method);
        endpoint.setParameters(parameters);
        
        // 提取返回值类型
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            ResponseModel response = new ResponseModel();
            response.setType(returnType.getPresentableText());
            endpoint.setResponse(response);
        }
        
        return endpoint;
    }
    
    /**
     * 提取 @RequestMapping 的 value 值
     */
    private String extractRequestMapping(@NotNull PsiClass psiClass) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.endsWith("RequestMapping")) {
                return extractAnnotationValue(annotation, "value");
            }
        }
        return "";
    }
    
    /**
     * 提取注解的属性值
     */
    private String extractAnnotationValue(@NotNull PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            value = annotation.findAttributeValue("value");
        }
        if (value != null) {
            String text = value.getText()
                .replace("\"", "")
                .replace("{", "")
                .replace("}", "")
                .trim();
            return text;
        }
        return "";
    }
    
    /**
     * 合并 URL
     */
    private String combineUrls(String base, String path) {
        if (base == null) base = "";
        if (path == null) path = "";
        
        base = base.trim();
        path = path.trim();
        
        if (base.isEmpty()) return path;
        if (path.isEmpty()) return base;
        
        // 确保只有一个斜杠
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!path.startsWith("/")) path = "/" + path;
        
        return base + path;
    }
    
    /**
     * 提取方法参数
     */
    private List<Parameter> extractParameters(@NotNull PsiMethod method) {
        List<Parameter> parameters = new ArrayList<>();
        
        for (PsiParameter psiParam : method.getParameterList().getParameters()) {
            Parameter param = new Parameter();
            param.setName(psiParam.getName());
            param.setType(psiParam.getType().getPresentableText());
            
            // 检查参数注解
            for (PsiAnnotation annotation : psiParam.getAnnotations()) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) continue;
                
                if (qualifiedName.endsWith("RequestParam")) {
                    param.setIn("query");
                    String required = extractAnnotationValue(annotation, "required");
                    param.setRequired(!"false".equals(required));
                } else if (qualifiedName.endsWith("PathVariable")) {
                    param.setIn("path");
                    param.setRequired(true);
                } else if (qualifiedName.endsWith("RequestBody")) {
                    param.setIn("body");
                    param.setRequired(true);
                } else if (qualifiedName.endsWith("RequestHeader")) {
                    param.setIn("header");
                }
            }
            
            // 提取参数注释
            PsiDocComment docComment = method.getDocComment();
            if (docComment != null) {
                param.setDescription(extractParamDescription(docComment, psiParam.getName()));
            }
            
            parameters.add(param);
        }
        
        return parameters;
    }
    
    /**
     * 从 JavaDoc 提取描述
     */
    private String extractDescription(@NotNull PsiDocComment docComment) {
        StringBuilder description = new StringBuilder();
        for (PsiElement element : docComment.getDescriptionElements()) {
            description.append(element.getText());
        }
        return description.toString().trim();
    }
    
    /**
     * 从 JavaDoc 提取参数描述
     */
    private String extractParamDescription(@NotNull PsiDocComment docComment, String paramName) {
        for (PsiDocTag tag : docComment.getTags()) {
            if ("param".equals(tag.getName())) {
                PsiElement[] dataElements = tag.getDataElements();
                if (dataElements.length > 0 && paramName.equals(dataElements[0].getText())) {
                    StringBuilder desc = new StringBuilder();
                    for (int i = 1; i < dataElements.length; i++) {
                        desc.append(dataElements[i].getText());
                    }
                    return desc.toString().trim();
                }
            }
        }
        return "";
    }
    
    /**
     * 获取文件路径
     */
    private String getFilePath(@NotNull PsiClass psiClass) {
        PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile != null) {
            return containingFile.getVirtualFile().getPath();
        }
        return "";
    }
    
    /**
     * 获取行号
     */
    private int getLineNumber(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null) {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            com.intellij.openapi.editor.Document document = documentManager.getDocument(containingFile);
            if (document != null) {
                int offset = element.getTextOffset();
                return document.getLineNumber(offset) + 1; // 行号从 1 开始
            }
        }
        return 0;
    }
}

