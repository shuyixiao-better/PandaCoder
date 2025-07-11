package com.shuyixiao.jenkins.highlight;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Jenkins Pipeline 颜色设置页面
 * 允许用户自定义Jenkins Pipeline语法高亮的颜色
 */
public class JenkinsColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("关键字//Keyword", JenkinsSyntaxHighlighter.JENKINS_KEYWORD),
            new AttributesDescriptor("Pipeline块//Pipeline Block", JenkinsSyntaxHighlighter.JENKINS_PIPELINE_BLOCK),
            new AttributesDescriptor("Stage块//Stage Block", JenkinsSyntaxHighlighter.JENKINS_STAGE_BLOCK),
            new AttributesDescriptor("步骤方法//Step Method", JenkinsSyntaxHighlighter.JENKINS_STEP_METHOD),
            new AttributesDescriptor("变量//Variable", JenkinsSyntaxHighlighter.JENKINS_VARIABLE),
            new AttributesDescriptor("字符串//String", JenkinsSyntaxHighlighter.JENKINS_STRING),
            new AttributesDescriptor("注释//Comment", JenkinsSyntaxHighlighter.JENKINS_COMMENT),
            new AttributesDescriptor("数字//Number", JenkinsSyntaxHighlighter.JENKINS_NUMBER),
            new AttributesDescriptor("方括号//Bracket", JenkinsSyntaxHighlighter.JENKINS_BRACKET),
            new AttributesDescriptor("大括号//Brace", JenkinsSyntaxHighlighter.JENKINS_BRACE),
            new AttributesDescriptor("操作符//Operator", JenkinsSyntaxHighlighter.JENKINS_OPERATOR),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/jenkinsfile.svg", JenkinsColorSettingsPage.class);
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new JenkinsSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return """
                // Jenkins Pipeline Example
                pipeline {
                    agent any
                    
                    environment {
                        MAVEN_OPTS = '-Xmx1024m'
                        BUILD_NUMBER = "${BUILD_NUMBER}"
                    }
                    
                    parameters {
                        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Git branch to build')
                        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip unit tests')
                        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Target environment')
                    }
                    
                    stages {
                        stage('Checkout') {
                            steps {
                                checkout scm
                                echo "Checked out branch: ${params.BRANCH_NAME}"
                            }
                        }
                        
                        stage('Build') {
                            steps {
                                sh 'mvn clean compile'
                                echo "Build completed with Maven options: ${env.MAVEN_OPTS}"
                            }
                        }
                        
                        stage('Test') {
                            when {
                                not { params.SKIP_TESTS }
                            }
                            steps {
                                sh 'mvn test'
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                        
                        stage('Deploy') {
                            when {
                                branch 'main'
                            }
                            steps {
                                script {
                                    def deployEnv = params.ENVIRONMENT
                                    if (deployEnv == 'prod') {
                                        input message: 'Deploy to production?', ok: 'Deploy'
                                    }
                                    
                                    sh "deploy.sh ${deployEnv}"
                                    echo "Deployed to ${deployEnv} environment"
                                }
                            }
                        }
                    }
                    
                    post {
                        always {
                            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                            deleteDir()
                        }
                        success {
                            echo 'Pipeline succeeded!'
                            emailext subject: 'Build Success', body: 'The build was successful.', to: 'team@example.com'
                        }
                        failure {
                            echo 'Pipeline failed!'
                            emailext subject: 'Build Failed', body: 'The build failed. Please check the logs.', to: 'team@example.com'
                        }
                    }
                }
                """;
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Jenkins Pipeline";
    }
}
