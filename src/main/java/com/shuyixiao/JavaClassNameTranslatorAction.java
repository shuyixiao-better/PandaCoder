package com.shuyixiao;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.shuyixiao.setting.PluginSettings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class JavaClassNameTranslatorAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 检查API配置是否已设置
        if (!com.shuyixiao.util.TranslationUtil.checkApiConfiguration()) {
            return; // 未配置API，无法继续
        }

        Project project = e.getProject();
        PsiElement selectedElement = LangDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        PsiDirectory selectedDirectory = (selectedElement instanceof PsiDirectory) ? (PsiDirectory) selectedElement : null;

        if (project != null && selectedDirectory != null) {
            // Step 1: 选择文件类型的对话框
            String[] fileTypeOptions = {"类", "接口", "记录", "枚举", "注解", "异常"};
            String fileType = Messages.showEditableChooseDialog("请选择要生成的文件类型：", "文件类型选择", null, fileTypeOptions, fileTypeOptions[0], null);

            if (fileType != null) {
                // Step 2: 弹出输入框，获取用户输入的中文类名
                String chineseClassName = Messages.showInputDialog(project, "请输入中文类名：", "类名转换", Messages.getQuestionIcon());

                if (chineseClassName != null && !chineseClassName.trim().isEmpty()) {
                    // Step 3: 调用翻译 API，将中文类名转换为英文
                    String translatedName = null;
                    try {
                        translatedName = translateChineseToEnglish(chineseClassName);
                        if (translatedName == null || translatedName.trim().isEmpty()) {
                            Messages.showErrorDialog(project, "翻译结果为空，无法创建文件。", "翻译错误");
                            return;
                        }
                    } catch (UnsupportedEncodingException ex) {
                        Messages.showErrorDialog(project, "翻译API配置错误: " + ex.getMessage(), "API错误");
                        return;
                    } catch (Exception ex) {
                        Messages.showErrorDialog(project, "翻译过程中发生错误: " + ex.getMessage(), "翻译错误");
                        return;
                    }

                    // Step 4: 将翻译后的英文名转换为大驼峰命名法
                    String className = toCamelCase(translatedName);

                    // 根据选择的文件类型生成相应的文件
                    switch (fileType) {
                        case "类":
                            createJavaClass(project, className, selectedDirectory);
                            break;
                        case "接口":
                            createJavaInterface(project, className, selectedDirectory);
                            break;
                        case "记录":
                            createJavaRecord(project, className, selectedDirectory);
                            break;
                        case "枚举":
                            createJavaEnum(project, className, selectedDirectory);
                            break;
                        case "注解":
                            createJavaAnnotation(project, className, selectedDirectory);
                            break;
                        case "异常":
                            createJavaException(project, className, selectedDirectory);
                            break;
                        default:
                            Messages.showErrorDialog(project, "未知文件类型", "错误");
                            break;
                    }
                } else {
                    Messages.showErrorDialog(project, "未输入有效的类名", "错误");
                }
            }
        } else {
            Messages.showErrorDialog(project, "未选择目标文件夹或项目未加载", "错误");
        }
    }

    // 模拟百度翻译 API 调用
    private String translateChineseToEnglish(String chinese) throws UnsupportedEncodingException {
        // 假设这里调用了实际的百度翻译 API
        return BaiduAPI.translate(chinese);  // 示例：将"中文类名"翻译为 "TranslatedName"
    }

    // 将英文名转换为大驼峰命名法
    private String toCamelCase(String input) {
        String[] words = input.split(" ");
        StringBuilder camelCaseName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                camelCaseName.append(Character.toUpperCase(word.charAt(0)));
                camelCaseName.append(word.substring(1).toLowerCase());
            }
        }
        return camelCaseName.toString();
    }
    /**
     * 创建Java类
     *
     * @param project 当前项目，用于获取项目上下文
     * @param className 新建类的名称，用于定义类的标识
     * @param targetDirectory 目标目录，指定新建类存放的位置
     */
    private void createJavaClass(Project project, String className, PsiDirectory targetDirectory) {
        // 获取文件模板管理器
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 获取类模板
        FileTemplate classTemplate = templateManager.getInternalTemplate("Class");

        // 创建用于替换模板中变量的属性集合
        Properties properties = new Properties(templateManager.getDefaultProperties());
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, className);

        try {
            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(className);

            // 使用类模板创建类并附加用户定义的文件头
            String classContent = classTemplate.getText(properties);
            String finalContent = fileHeader + "\n" + classContent;
            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(className + ".java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录
            WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> targetDirectory.add(psiFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取自定义文件头模板
     *
     * @param className 类的名称
     * @return 文件头模板内容
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


    // 创建 Java 接口文件
    private void createJavaInterface(Project project, String interfaceName, PsiDirectory targetDirectory) {
        // 获取文件模板管理器
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 获取接口模板
        FileTemplate interfaceTemplate = templateManager.getInternalTemplate("Interface");

        // 创建用于替换模板中变量的属性集合
        Properties properties = new Properties(templateManager.getDefaultProperties());
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, interfaceName);

        try {
            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(interfaceName);

            // 使用接口模板创建接口并附加用户定义的文件头
            String interfaceContent = interfaceTemplate.getText(properties);
            String finalContent = fileHeader + "\n" + interfaceContent;

            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(interfaceName + ".java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录
            WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> targetDirectory.add(psiFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 创建 Java 记录文件
    private void createJavaRecord(Project project, String recordName, PsiDirectory targetDirectory) {
        // 获取文件模板管理器
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 获取记录模板
        FileTemplate recordTemplate = templateManager.getInternalTemplate("Record");

        // 创建用于替换模板中变量的属性集合
        Properties properties = new Properties(templateManager.getDefaultProperties());
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, recordName);

        try {
            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(recordName);

            // 使用记录模板创建记录并附加用户定义的文件头
            String recordContent = recordTemplate.getText(properties);
            String finalContent = fileHeader + "\n" + recordContent;

            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(recordName + ".java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录
            WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> targetDirectory.add(psiFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 创建 Java 枚举文件
    private void createJavaEnum(Project project, String enumName, PsiDirectory targetDirectory) {
        // 获取文件模板管理器
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 获取枚举模板
        FileTemplate enumTemplate = templateManager.getInternalTemplate("Enum");

        // 创建用于替换模板中变量的属性集合
        Properties properties = new Properties(templateManager.getDefaultProperties());
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, enumName);

        try {
            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(enumName);

            // 使用枚举模板创建枚举并附加用户定义的文件头
            String enumContent = enumTemplate.getText(properties);
            String finalContent = fileHeader + "\n" + enumContent;

            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(enumName + ".java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录
            WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> targetDirectory.add(psiFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 创建 Java 注解文件
    private void createJavaAnnotation(Project project, String annotationName, PsiDirectory targetDirectory) {
        // 获取文件模板管理器
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 获取注解模板
        FileTemplate annotationTemplate = templateManager.getInternalTemplate("AnnotationType");

        // 创建用于替换模板中变量的属性集合
        Properties properties = new Properties(templateManager.getDefaultProperties());
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, annotationName);

        try {
            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(annotationName);

            // 使用注解模板创建注解并附加用户定义的文件头
            String annotationContent = annotationTemplate.getText(properties);
            String finalContent = fileHeader + "\n" + annotationContent;

            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(annotationName + ".java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录
            WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> targetDirectory.add(psiFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 创建 Java 异常文件
    private void createJavaException(Project project, String exceptionName, PsiDirectory targetDirectory) {
        // 获取文件模板管理器
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 获取类模板
        FileTemplate exceptionTemplate = templateManager.getInternalTemplate("Class");

        // 创建用于替换模板中变量的属性集合
        Properties properties = new Properties(templateManager.getDefaultProperties());
        properties.setProperty(FileTemplate.ATTRIBUTE_NAME, exceptionName + "Exception");

        try {
            // 获取用户定义的模板注释
            String fileHeader = getCustomFileHeader(exceptionName + "Exception");

            // 使用异常模板创建异常类并附加用户定义的文件头
            String exceptionContent = exceptionTemplate.getText(properties);
            String finalContent = fileHeader + "\n" + exceptionContent;

            // 创建 PsiFile
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(exceptionName + "Exception.java", JavaFileType.INSTANCE, finalContent);

            // 写入到目标目录
            WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> targetDirectory.add(psiFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
