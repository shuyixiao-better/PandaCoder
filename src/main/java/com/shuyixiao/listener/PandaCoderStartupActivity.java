package com.shuyixiao.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.shuyixiao.service.PandaCoderSettings;
import com.shuyixiao.ui.PandaCoderBalloon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * PandaCoder 插件启动活动
 * 处理首次安装欢迎提示和初始化工作
 * 
 * @author 舒一笑不秃头
 * @version 2.2.0
 */
public class PandaCoderStartupActivity implements StartupActivity.DumbAware {
    
    private static final int WELCOME_DELAY_MS = 2000; // 延迟2秒显示，避免启动时过于拥挤
    
    @Override
    public void runActivity(@NotNull Project project) {
        PandaCoderSettings settings = PandaCoderSettings.getInstance(project);
        
        // 如果是首次安装，延迟显示欢迎提示
        if (settings.isFirstInstall()) {
            scheduleWelcomeMessage(project, settings);
        }
    }
    
    /**
     * 安排显示欢迎消息
     */
    private void scheduleWelcomeMessage(Project project, PandaCoderSettings settings) {
        // 使用 Timer 延迟显示，避免与 IDE 启动冲突
        Timer timer = new Timer(WELCOME_DELAY_MS, e -> {
            try {
                // 显示欢迎气泡
                PandaCoderBalloon.showWelcome(project, null);
                
                // 标记首次安装已完成
                settings.setFirstInstallComplete();
                settings.updateLastWelcomeTime();
                
            } catch (Exception ex) {
                // 忽略异常，不影响 IDE 启动
            }
        });
        
        timer.setRepeats(false);
        timer.start();
    }
}

