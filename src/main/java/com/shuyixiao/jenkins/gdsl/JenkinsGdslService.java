package com.shuyixiao.jenkins.gdsl;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.shuyixiao.jenkins.model.Descriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jenkins GDSL服务
 * 管理Jenkins Pipeline的GDSL描述符，提供智能补全数据源
 */
@Service(Service.Level.PROJECT)
public final class JenkinsGdslService {
    private static final Logger LOG = Logger.getInstance(JenkinsGdslService.class);
    private static final String DESCRIPTORS_FILE = "/descriptors/jenkinsPipeline.xml";
    
    private final Project project;
    private final ConcurrentHashMap<String, Map<String, Descriptor>> descriptorsCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public JenkinsGdslService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 获取指定的描述符
     * 
     * @param gdslId GDSL标识符（通常为文件类型或上下文）
     * @param definitionId 定义标识符（方法或指令名称）
     * @return 描述符对象，如果未找到返回null
     */
    @Nullable
    public Descriptor getDescriptor(@NotNull String gdslId, @NotNull String definitionId) {
        ensureInitialized();
        
        Map<String, Descriptor> descriptors = descriptorsCache.get(gdslId);
        if (descriptors == null) {
            // 如果没有找到指定的GDSL ID，尝试使用默认的"jenkins"
            descriptors = descriptorsCache.get("jenkins");
        }
        
        return descriptors != null ? descriptors.get(definitionId) : null;
    }

    /**
     * 获取所有描述符
     * 
     * @param gdslId GDSL标识符
     * @return 描述符映射
     */
    @NotNull
    public Map<String, Descriptor> getAllDescriptors(@NotNull String gdslId) {
        ensureInitialized();
        
        Map<String, Descriptor> descriptors = descriptorsCache.get(gdslId);
        if (descriptors == null) {
            descriptors = descriptorsCache.get("jenkins");
        }
        
        return descriptors != null ? Map.copyOf(descriptors) : Map.of();
    }

    /**
     * 检查是否包含指定的定义
     */
    public boolean hasDefinition(@NotNull String gdslId, @NotNull String definitionId) {
        return getDescriptor(gdslId, definitionId) != null;
    }

    /**
     * 获取所有Jenkins Pipeline定义的名称
     */
    @NotNull
    public List<String> getAllDefinitionNames() {
        ensureInitialized();
        
        return descriptorsCache.values().stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 确保服务已初始化
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    loadDescriptors();
                    initialized = true;
                }
            }
        }
    }

    /**
     * 从XML文件加载描述符
     */
    private void loadDescriptors() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(DESCRIPTORS_FILE);
            if (inputStream == null) {
                LOG.warn("Jenkins Pipeline descriptors file not found: " + DESCRIPTORS_FILE);
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            Map<String, Descriptor> jenkinsDescriptors = parseDescriptors(document);
            
            // 将描述符存储在缓存中，使用"jenkins"作为默认GDSL ID
            descriptorsCache.put("jenkins", jenkinsDescriptors);
            descriptorsCache.put("jenkinsfile", jenkinsDescriptors); // 兼容不同的命名
            
            LOG.info("Loaded " + jenkinsDescriptors.size() + " Jenkins Pipeline descriptors");
            
        } catch (Exception e) {
            LOG.error("Failed to load Jenkins Pipeline descriptors", e);
        }
    }

    /**
     * 解析XML文档中的描述符
     */
    @NotNull
    private Map<String, Descriptor> parseDescriptors(@NotNull Document document) {
        Map<String, Descriptor> descriptors = new ConcurrentHashMap<>();
        
        NodeList definitionNodes = document.getElementsByTagName("definition");
        for (int i = 0; i < definitionNodes.getLength(); i++) {
            Node definitionNode = definitionNodes.item(i);
            if (definitionNode.getNodeType() == Node.ELEMENT_NODE) {
                Descriptor descriptor = parseDefinition((Element) definitionNode);
                if (descriptor != null) {
                    descriptors.put(descriptor.getId(), descriptor);
                }
            }
        }
        
        return descriptors;
    }

    /**
     * 解析单个定义元素
     */
    @Nullable
    private Descriptor parseDefinition(@NotNull Element definitionElement) {
        try {
            String id = definitionElement.getAttribute("id");
            String name = definitionElement.getAttribute("name");
            boolean hasGetter = "true".equals(definitionElement.getAttribute("hasGetter"));
            
            if (id.isEmpty() || name.isEmpty()) {
                LOG.warn("Invalid definition element: missing id or name");
                return null;
            }
            
            // 解析文档
            String documentation = parseDocumentation(definitionElement);
            
            // 解析参数
            List<Descriptor.Parameter> parameters = parseParameters(definitionElement);
            
            return new Descriptor(id, name, hasGetter, documentation, parameters);
            
        } catch (Exception e) {
            LOG.warn("Failed to parse definition element", e);
            return null;
        }
    }

    /**
     * 解析文档内容
     */
    @Nullable
    private String parseDocumentation(@NotNull Element definitionElement) {
        NodeList docNodes = definitionElement.getElementsByTagName("doc");
        if (docNodes.getLength() > 0) {
            Node docNode = docNodes.item(0);
            return docNode.getTextContent().trim();
        }
        return null;
    }

    /**
     * 解析参数列表
     */
    @NotNull
    private List<Descriptor.Parameter> parseParameters(@NotNull Element definitionElement) {
        List<Descriptor.Parameter> parameters = new ArrayList<>();
        
        NodeList parametersNodes = definitionElement.getElementsByTagName("parameters");
        if (parametersNodes.getLength() > 0) {
            Element parametersElement = (Element) parametersNodes.item(0);
            NodeList parameterNodes = parametersElement.getElementsByTagName("parameter");
            
            for (int i = 0; i < parameterNodes.getLength(); i++) {
                Node parameterNode = parameterNodes.item(i);
                if (parameterNode.getNodeType() == Node.ELEMENT_NODE) {
                    Descriptor.Parameter parameter = parseParameter((Element) parameterNode);
                    if (parameter != null) {
                        parameters.add(parameter);
                    }
                }
            }
        }
        
        return parameters;
    }

    /**
     * 解析单个参数
     */
    @Nullable
    private Descriptor.Parameter parseParameter(@NotNull Element parameterElement) {
        try {
            String name = parameterElement.getAttribute("name");
            String type = parameterElement.getAttribute("type");
            boolean required = "true".equals(parameterElement.getAttribute("required"));
            
            if (name.isEmpty() || type.isEmpty()) {
                LOG.warn("Invalid parameter element: missing name or type");
                return null;
            }
            
            // 解析参数文档
            String documentation = null;
            NodeList docNodes = parameterElement.getElementsByTagName("doc");
            if (docNodes.getLength() > 0) {
                documentation = docNodes.item(0).getTextContent().trim();
            }
            
            return new Descriptor.Parameter(name, type, required, documentation);
            
        } catch (Exception e) {
            LOG.warn("Failed to parse parameter element", e);
            return null;
        }
    }

    /**
     * 重新加载描述符（用于开发和测试）
     */
    public void reload() {
        synchronized (this) {
            descriptorsCache.clear();
            initialized = false;
            ensureInitialized();
        }
    }

    /**
     * 获取项目实例
     */
    @NotNull
    public static JenkinsGdslService getInstance(@NotNull Project project) {
        return project.getService(JenkinsGdslService.class);
    }
} 