package com.shuyixiao;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.CreateFromTemplateAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import com.shuyixiao.setting.PluginSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 中文类创建Action
 * 在IntelliJ IDEA的「New」菜单中添加创建中文命名类的功能
 * 支持在原生输入框中输入中文直接创建英文类名
 */
public class ChineseClassCreationAction extends CreateFromTemplateAction<PsiFile> implements DumbAware {

    // 中文字符匹配模式
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    public ChineseClassCreationAction() {
        super("智能中文类", "创建中文命名的类，自动转换为英文类名", PlatformIcons.CLASS_ICON);
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle("创建Java文件");

        // 添加不同类型的Java文件模板
        builder.addKind("类", PlatformIcons.CLASS_ICON, "Class");
        builder.addKind("接口", PlatformIcons.INTERFACE_ICON, "Interface");
        builder.addKind("枚举", PlatformIcons.ENUM_ICON, "Enum");
        builder.addKind("注解", PlatformIcons.ANNOTATION_TYPE_ICON, "AnnotationType");
        builder.addKind("记录", PlatformIcons.RECORD_ICON, "Record");
        builder.addKind("异常", PlatformIcons.EXCEPTION_CLASS_ICON, "Class");

        // 文件名输入验证器
        builder.setValidator(new InputValidatorEx() {
            @Override
            public boolean checkInput(String inputString) {
                return inputString.length() > 0;
            }

            @Override
            public boolean canClose(String inputString) {
                return !inputString.trim().isEmpty();
            }

            @Nullable
            @Override
            public String getErrorText(String inputString) {
                if (inputString.trim().isEmpty()) {
                    return "文件名不能为空";
                }
                return null;
            }
        });

        // 注意：在较新版本的API中，我们无法直接监听模板类型选择
        // 在createFile方法中，我们将使用传入的templateName参数
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return "创建Java文件 " + newName;
    }

    @Override
    protected PsiFile createFile(String fileName, String templateName, PsiDirectory dir) {
        Project project = dir.getProject();

        // 检查文件名是否包含中文，如果包含则进行翻译
        String className = fileName;
        if (containsChinese(fileName)) {
            try {
                // 翻译中文为英文
                String translatedName = BaiduAPI.translate(fileName);
                // 转换为大驼峰命名
                className = toCamelCase(translatedName);

                // 如果是异常类且不以Exception结尾，加上Exception后缀
                if (templateName.equals("Class") && fileName.contains("异常") 
                        && !className.endsWith("Exception")) {
                    className += "Exception";
                }

            } catch (UnsupportedEncodingException ex) {
                throw new IncorrectOperationException("翻译失败: " + ex.getMessage());
            }
        }

        try {
            // 获取文件模板管理器
            FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
            FileTemplate template = templateManager.getInternalTemplate(templateName);

            // 设置模板属性
            Properties properties = new Properties(templateManager.getDefaultProperties());
            properties.setProperty(FileTemplate.ATTRIBUTE_NAME, className);

            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(className);

            // 使用模板创建内容并附加用户定义的文件头
            String content = template.getText(properties);
            String finalContent = fileHeader + "\n" + content;

            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    className + ".java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录并返回
            return (PsiFile) dir.add(psiFile);

        } catch (IOException e) {
            throw new IncorrectOperationException("创建文件失败: " + e.getMessage());
        }
    }

    @Override
    protected void postProcess(PsiFile createdElement, String templateName, java.util.Map<String, String> customProperties) {
        // 文件创建后的处理，可以在这里添加额外的文件处理逻辑
        super.postProcess(createdElement, templateName, customProperties);
    }

    /**
     * 检查字符串是否包含中文字符
     */
    private boolean containsChinese(String str) {
        return CHINESE_PATTERN.matcher(str).find();
    }

    /**
     * 将英文转换为大驼峰命名
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // 移除特殊字符
        input = input.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();

        String[] words = input.split("\\s+");
        StringBuilder camelCaseName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                camelCaseName.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    camelCaseName.append(word.substring(1).toLowerCase());
                }
            }
        }
        return camelCaseName.toString();
    }

    /**
     * 获取自定义文件头模板
     */
    private String getCustomFileHeader(String className) {
        // 从设置中获取模板
        String template = PluginSettings.getInstance().getTemplate();

        // 替换模板中的占位符
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        String currentTime = dateFormat.format(new Date());

        return template.replace("${NAME}", className)
                .replace("${YEAR}", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
                .replace("${TIME}", currentTime);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只在有IdeView的情况下启用此Action
        IdeView view = e.getData(LangDataKeys.IDE_VIEW);
        e.getPresentation().setEnabledAndVisible(view != null && view.getDirectories().length > 0);
    }
}
