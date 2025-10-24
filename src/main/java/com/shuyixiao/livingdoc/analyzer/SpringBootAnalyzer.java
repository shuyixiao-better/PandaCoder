package com.shuyixiao.livingdoc.analyzer;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.shuyixiao.livingdoc.analyzer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Spring Boot 项目代码分析器
 * 
 * <p>从Spring Boot项目中提取API端点信息
 * 
 * @author PandaCoder Team
 * @since 2.3.0
 */
public class SpringBootAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(SpringBootAnalyzer.class);
    
    /**
     * 分析项目
     * 
     * @param project IntelliJ项目
     * @return 项目文档
     */
    public ProjectDocumentation analyzeProject(Project project) {
        log.info("Starting to analyze project: {}", project.getName());
        
        ProjectDocumentation doc = new ProjectDocumentation();
        doc.setProjectName(project.getName());
        doc.setGeneratedAt(LocalDateTime.now());
        
        // 1. 查找所有Controller
        Collection<PsiClass> controllers = findControllers(project);
        log.info("Found {} controllers", controllers.size());
        
        // 2. 遍历分析每个Controller
        for (PsiClass controller : controllers) {
            try {
                List<ApiEndpoint> apis = extractApis(controller);
                doc.addApis(apis);
                log.debug("Extracted {} APIs from {}", apis.size(), controller.getName());
            } catch (Exception e) {
                log.error("Failed to extract APIs from controller: {}", controller.getName(), e);
            }
        }
        
        log.info("Analysis complete. Total APIs: {}", doc.getApis().size());
        return doc;
    }
    
    /**
     * 查找所有Controller类
     */
    private Collection<PsiClass> findControllers(Project project) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        PsiJavaFacade javaFacade = PsiJavaFacade.getInstance(project);
        
        // 查找 @RestController 和 @Controller
        return Stream.of(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller"
        )
        .flatMap(annotation -> {
            PsiClass annotationClass = javaFacade.findClass(annotation, scope);
            if (annotationClass != null) {
                return AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).stream();
            }
            return Stream.empty();
        })
        .toList();
    }
    
    /**
     * 从Controller中提取API信息
     */
    private List<ApiEndpoint> extractApis(PsiClass controller) {
        List<ApiEndpoint> apis = new ArrayList<>();
        
        // 获取类级别的@RequestMapping
        String basePath = extractBasePath(controller);
        
        // 遍历方法
        for (PsiMethod method : controller.getMethods()) {
            if (isApiMethod(method)) {
                ApiEndpoint api = extractApiFromMethod(method, basePath, controller);
                if (api != null) {
                    apis.add(api);
                }
            }
        }
        
        return apis;
    }
    
    /**
     * 提取类级别的基础路径
     */
    private String extractBasePath(PsiClass controller) {
        PsiAnnotation requestMapping = controller.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
        if (requestMapping != null) {
            PsiAnnotationMemberValue value = requestMapping.findAttributeValue("value");
            if (value != null) {
                return cleanPath(value.getText());
            }
        }
        return "";
    }
    
    /**
     * 判断方法是否是API方法
     */
    private boolean isApiMethod(PsiMethod method) {
        // 检查是否有请求映射注解
        String[] mappingAnnotations = {
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping"
        };
        
        for (String annotation : mappingAnnotations) {
            if (method.getAnnotation(annotation) != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 从方法中提取API信息
     */
    private ApiEndpoint extractApiFromMethod(PsiMethod method, String basePath, PsiClass controller) {
        ApiEndpoint api = new ApiEndpoint();
        
        // 基本信息
        api.setController(controller.getName());
        api.setMethodName(method.getName());
        api.setFilePath(method.getContainingFile().getVirtualFile().getPath());
        api.setLineNumber(getLineNumber(method));
        
        // HTTP方法和路径
        String[] httpInfo = extractHttpMethodAndPath(method);
        api.setHttpMethod(httpInfo[0]);
        api.setPath(combinePath(basePath, httpInfo[1]));
        
        // 描述（从JavaDoc提取）
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            api.setDescription(extractDescription(docComment));
            api.setJavadoc(docComment.getText());
            api.setAuthor(extractAuthor(docComment));
        }
        
        // 参数
        api.setParameters(extractParameters(method));
        
        // 响应
        api.setResponse(extractResponse(method));
        
        return api;
    }
    
    /**
     * 提取HTTP方法和路径
     */
    private String[] extractHttpMethodAndPath(PsiMethod method) {
        String httpMethod = "GET";  // 默认
        String path = "";
        
        // 检查各种映射注解
        PsiAnnotation annotation = null;
        
        if ((annotation = method.getAnnotation("org.springframework.web.bind.annotation.GetMapping")) != null) {
            httpMethod = "GET";
        } else if ((annotation = method.getAnnotation("org.springframework.web.bind.annotation.PostMapping")) != null) {
            httpMethod = "POST";
        } else if ((annotation = method.getAnnotation("org.springframework.web.bind.annotation.PutMapping")) != null) {
            httpMethod = "PUT";
        } else if ((annotation = method.getAnnotation("org.springframework.web.bind.annotation.DeleteMapping")) != null) {
            httpMethod = "DELETE";
        } else if ((annotation = method.getAnnotation("org.springframework.web.bind.annotation.PatchMapping")) != null) {
            httpMethod = "PATCH";
        } else if ((annotation = method.getAnnotation("org.springframework.web.bind.annotation.RequestMapping")) != null) {
            // 从RequestMapping中提取method
            PsiAnnotationMemberValue methodValue = annotation.findAttributeValue("method");
            if (methodValue != null) {
                String methodText = methodValue.getText();
                if (methodText.contains("POST")) httpMethod = "POST";
                else if (methodText.contains("PUT")) httpMethod = "PUT";
                else if (methodText.contains("DELETE")) httpMethod = "DELETE";
                else if (methodText.contains("PATCH")) httpMethod = "PATCH";
            }
        }
        
        // 提取路径
        if (annotation != null) {
            PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            if (value != null) {
                path = cleanPath(value.getText());
            }
        }
        
        return new String[]{httpMethod, path};
    }
    
    /**
     * 提取方法参数
     */
    private List<Parameter> extractParameters(PsiMethod method) {
        List<Parameter> parameters = new ArrayList<>();
        
        for (PsiParameter psiParam : method.getParameterList().getParameters()) {
            Parameter param = new Parameter();
            param.setName(psiParam.getName());
            param.setType(psiParam.getType().getPresentableText());
            
            // 判断参数来源
            if (psiParam.getAnnotation("org.springframework.web.bind.annotation.RequestParam") != null) {
                param.setSource("query");
                extractParamAnnotationInfo(psiParam, param, "RequestParam");
            } else if (psiParam.getAnnotation("org.springframework.web.bind.annotation.PathVariable") != null) {
                param.setSource("path");
                extractParamAnnotationInfo(psiParam, param, "PathVariable");
            } else if (psiParam.getAnnotation("org.springframework.web.bind.annotation.RequestBody") != null) {
                param.setSource("body");
            } else if (psiParam.getAnnotation("org.springframework.web.bind.annotation.RequestHeader") != null) {
                param.setSource("header");
            }
            
            parameters.add(param);
        }
        
        return parameters;
    }
    
    /**
     * 提取参数注解信息
     */
    private void extractParamAnnotationInfo(PsiParameter psiParam, Parameter param, String annotationType) {
        PsiAnnotation annotation = psiParam.getAnnotation("org.springframework.web.bind.annotation." + annotationType);
        if (annotation != null) {
            // required
            PsiAnnotationMemberValue required = annotation.findAttributeValue("required");
            if (required != null) {
                param.setRequired("true".equals(required.getText()));
            } else {
                param.setRequired(true);  // 默认必填
            }
            
            // defaultValue
            PsiAnnotationMemberValue defaultValue = annotation.findAttributeValue("defaultValue");
            if (defaultValue != null) {
                param.setDefaultValue(cleanString(defaultValue.getText()));
            }
        }
    }
    
    /**
     * 提取响应信息
     */
    private ResponseModel extractResponse(PsiMethod method) {
        ResponseModel response = new ResponseModel();
        
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            response.setType(returnType.getPresentableText());
        }
        
        return response;
    }
    
    /**
     * 提取描述
     */
    private String extractDescription(PsiDocComment docComment) {
        String[] lines = docComment.getDescriptionElements()[0].getText().split("\n");
        StringBuilder description = new StringBuilder();
        
        for (String line : lines) {
            String cleaned = line.trim()
                .replaceAll("^\\*+\\s*", "")
                .replaceAll("^/\\*+\\s*", "");
            if (!cleaned.isEmpty()) {
                description.append(cleaned).append(" ");
            }
        }
        
        return description.toString().trim();
    }
    
    /**
     * 提取作者
     */
    private String extractAuthor(PsiDocComment docComment) {
        PsiDocTag[] authorTags = docComment.findTagsByName("author");
        if (authorTags.length > 0) {
            PsiElement[] elements = authorTags[0].getDataElements();
            if (elements.length > 0) {
                return elements[0].getText().trim();
            }
        }
        return null;
    }
    
    /**
     * 获取行号
     */
    private int getLineNumber(PsiMethod method) {
        PsiFile file = method.getContainingFile();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(method.getProject());
        com.intellij.openapi.editor.Document document = documentManager.getDocument(file);
        
        if (document != null) {
            int offset = method.getTextRange().getStartOffset();
            return document.getLineNumber(offset) + 1;
        }
        
        return 0;
    }
    
    /**
     * 组合路径
     */
    private String combinePath(String basePath, String methodPath) {
        if (basePath.isEmpty()) {
            return methodPath;
        }
        if (methodPath.isEmpty()) {
            return basePath;
        }
        
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        if (!methodPath.startsWith("/")) {
            methodPath = "/" + methodPath;
        }
        
        return basePath + methodPath;
    }
    
    /**
     * 清理路径
     */
    private String cleanPath(String path) {
        return path.replaceAll("[\"{\\[\\]}]", "").trim();
    }
    
    /**
     * 清理字符串
     */
    private String cleanString(String text) {
        return text.replaceAll("\"", "").trim();
    }
}

