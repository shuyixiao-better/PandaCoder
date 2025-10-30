# PandaCoder UI 重构 - 快速实施指南

## 📋 准备工作检查清单

- [x] 已阅读《PandaCoder_UI重构方案.md》
- [x] 已阅读《PandaCoder_UI重构_用户体验对比.md》
- [ ] 确定选择的方案
- [ ] 备份当前代码（git commit）
- [ ] 准备开发环境

---

## 🚀 阶段一：30分钟快速改进（推荐立即实施）

### 改进 1：右键菜单优化

#### 当前代码
```xml
<!-- src/main/resources/META-INF/plugin.xml -->
<action id="ReportMessage" class="com.shuyixiao.ReportMessage" 
        text="关于PandaCoder"
        description="Show PandaCoder plugin information">
    <add-to-group group-id="EditorPopupMenu" anchor="first"/>  ← 问题：占据首位
</action>
```

#### 修改方案 A：移到底部（最简单）

```xml
<!-- src/main/resources/META-INF/plugin.xml -->
<action id="ReportMessage" class="com.shuyixiao.ReportMessage" 
        text="PandaCoder 助手 🐼"
        description="Open PandaCoder assistant panel">
    <add-to-group group-id="EditorPopupMenu" anchor="last"/>  ← 改为 last
    <keyboard-shortcut keymap="$default" first-keystroke="alt P"/>
</action>
```

#### 修改方案 B：创建子菜单（更优雅）

```xml
<!-- src/main/resources/META-INF/plugin.xml -->

<!-- 创建 PandaCoder 菜单组 -->
<group id="PandaCoderGroup" 
       text="PandaCoder 🐼" 
       description="PandaCoder Tools"
       popup="true">
    <add-to-group group-id="EditorPopupMenu" anchor="last"/>
    
    <!-- 实用功能 -->
    <action id="ConvertToCamelCase" 
            class="com.shuyixiao.ConvertToCamelCaseAction" 
            text="中文转小驼峰" 
            description="Convert to camelCase">
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt C"/>
    </action>
    
    <action id="ConvertToPascalCase" 
            class="com.shuyixiao.ConvertToPascalCaseAction" 
            text="中文转大驼峰" 
            description="Convert to PascalCase">
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt P"/>
    </action>
    
    <action id="ConvertToUpperCase" 
            class="com.shuyixiao.ConvertToUpperCaseAction" 
            text="中文转大写带下划线" 
            description="Convert to UPPER_CASE">
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt U"/>
    </action>
    
    <separator/>
    
    <!-- 关于/帮助 -->
    <action id="ReportMessage" 
            class="com.shuyixiao.ReportMessage" 
            text="关于插件" 
            description="About PandaCoder">
        <keyboard-shortcut keymap="$default" first-keystroke="alt P"/>
    </action>
</group>
```

**同时删除原有的独立 action 注册**（避免重复）：
```xml
<!-- 删除这些独立的 action 定义 -->
<action id="ConvertToCamelCase" ...>  ← 删除
<action id="ConvertToPascalCase" ...>  ← 删除
<action id="ConvertToUpperCase" ...>  ← 删除
```

---

### 改进 2：模态对话框 → 轻量级气泡

#### 当前代码
```java
// src/main/java/com/shuyixiao/ReportMessage.java
public class ReportMessage extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        // 显示模态对话框 ← 问题：阻断工作流
        WelcomeDialog.show(e.getProject());
    }
}
```

#### 推荐修改：创建新的轻量级版本

**步骤 1：创建气泡工具类**

```java
// src/main/java/com/shuyixiao/ui/PandaCoderBalloon.java
package com.shuyixiao.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

/**
 * PandaCoder 轻量级气泡提示
 * 替代模态对话框，提供更好的用户体验
 */
public class PandaCoderBalloon {
    
    private static final String VERSION = com.shuyixiao.version.VersionInfo.getVersion();
    
    /**
     * 显示欢迎气泡
     */
    public static void showWelcome(Project project, Editor editor) {
        String html = createWelcomeHtml();
        
        Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                html,
                MessageType.INFO,
                createHyperlinkListener(project)
            )
            .setFadeoutTime(7000)  // 7秒自动消失
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setAnimationCycle(200)
            .setCloseButtonEnabled(true)
            .createBalloon();
        
        if (editor != null) {
            // 在编辑器中显示
            balloon.show(
                JBPopupFactory.getInstance().guessBestPopupLocation(editor),
                Balloon.Position.below
            );
        } else {
            // 在屏幕中央显示
            balloon.show(
                RelativePoint.getCenterOf(
                    com.intellij.openapi.wm.WindowManager.getInstance()
                        .getFrame(project).getComponent()
                ),
                Balloon.Position.above
            );
        }
    }
    
    /**
     * 创建欢迎消息 HTML
     */
    private static String createWelcomeHtml() {
        return "<html>" +
               "<div style='padding: 15px; width: 350px; font-family: Arial, sans-serif;'>" +
               "<h2 style='margin: 0 0 10px 0; color: #2C3E50;'>🐼 PandaCoder v" + VERSION + "</h2>" +
               "<p style='margin: 5px 0; color: #34495E; font-size: 13px;'>" +
               "中文开发者的智能编码助手" +
               "</p>" +
               "<hr style='border: none; border-top: 1px solid #E0E0E0; margin: 12px 0;'/>" +
               
               // 快速功能入口
               "<div style='margin: 10px 0;'>" +
               "<p style='margin: 5px 0; font-weight: bold; color: #2C3E50;'>⚡ 快速功能</p>" +
               "<p style='margin: 3px 0; font-size: 12px;'>" +
               "• Git 统计分析 | ES/SQL 监控 | Jenkins 增强" +
               "</p>" +
               "</div>" +
               
               "<hr style='border: none; border-top: 1px solid #E0E0E0; margin: 12px 0;'/>" +
               
               // 操作链接
               "<div style='margin-top: 15px; text-align: center;'>" +
               "<a href='open_toolwindow' style='color: #3498DB; text-decoration: none; margin: 0 10px;'>" +
               "📂 打开功能面板</a> | " +
               "<a href='show_features' style='color: #3498DB; text-decoration: none; margin: 0 10px;'>" +
               "✨ 查看所有功能</a>" +
               "</div>" +
               
               "<div style='margin-top: 10px; text-align: center;'>" +
               "<a href='follow_wechat' style='color: #27AE60; text-decoration: none; margin: 0 10px;'>" +
               "📱 关注公众号</a> | " +
               "<a href='github' style='color: #9B59B6; text-decoration: none; margin: 0 10px;'>" +
               "⭐ GitHub Star</a>" +
               "</div>" +
               
               "<div style='margin-top: 12px; padding-top: 10px; border-top: 1px solid #E0E0E0; " +
               "text-align: center; font-size: 11px; color: #95A5A6;'>" +
               "💡 提示：按 <kbd>Alt+P</kbd> 随时打开助手面板" +
               "</div>" +
               
               "</div>" +
               "</html>";
    }
    
    /**
     * 创建超链接监听器
     */
    private static HyperlinkListener createHyperlinkListener(Project project) {
        return e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                
                switch (desc) {
                    case "open_toolwindow":
                        // TODO: 打开 Tool Window（阶段二实现）
                        WelcomeDialog.show(project);
                        break;
                        
                    case "show_features":
                        // 显示完整功能对话框
                        WelcomeDialog.show(project);
                        break;
                        
                    case "follow_wechat":
                        // 显示公众号二维码
                        QRCodeDialog.showWechatQRCode(project);
                        break;
                        
                    case "github":
                        // 打开 GitHub 仓库
                        try {
                            java.awt.Desktop.getDesktop().browse(
                                new java.net.URI("https://github.com/shuyixiao-better/PandaCoder")
                            );
                        } catch (Exception ex) {
                            // 忽略错误
                        }
                        break;
                }
            }
        };
    }
    
    /**
     * 显示里程碑气泡（用于智能推广）
     */
    public static void showMilestone(Project project, int usageCount) {
        String message = getMilestoneMessage(usageCount);
        
        Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                message,
                MessageType.INFO,
                createHyperlinkListener(project)
            )
            .setFadeoutTime(5000)
            .setHideOnClickOutside(true)
            .createBalloon();
        
        // 在状态栏显示
        balloon.show(
            RelativePoint.getSouthEastOf(
                com.intellij.openapi.wm.WindowManager.getInstance()
                    .getStatusBar(project).getComponent()
            ),
            Balloon.Position.atRight
        );
    }
    
    private static String getMilestoneMessage(int count) {
        String content = "";
        
        switch (count) {
            case 10:
                content = "<h3>🎉 您已使用 PandaCoder 10 次！</h3>" +
                         "<p>觉得有用？<a href='github'>给个 Star</a> 支持作者 😊</p>";
                break;
                
            case 50:
                content = "<h3>🚀 您已使用 PandaCoder 50 次！</h3>" +
                         "<p>成为资深用户啦！<a href='follow_wechat'>关注公众号</a>获取高级技巧</p>";
                break;
                
            case 100:
                content = "<h3>💎 您已使用 PandaCoder 100 次！</h3>" +
                         "<p>感谢一路相伴！<a href='follow_wechat'>关注公众号</a>第一时间获取新功能</p>";
                break;
        }
        
        return "<html><div style='padding: 10px; width: 280px;'>" +
               content +
               "</div></html>";
    }
}
```

**步骤 2：修改 ReportMessage.java**

```java
// src/main/java/com/shuyixiao/ReportMessage.java
package com.shuyixiao;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.shuyixiao.ui.PandaCoderBalloon;
import com.shuyixiao.ui.WelcomeDialog;

/**
 * PandaCoder 助手面板入口
 * 优化后使用轻量级气泡，而非模态对话框
 */
public class ReportMessage extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        // 优先使用轻量级气泡
        if (editor != null) {
            PandaCoderBalloon.showWelcome(e.getProject(), editor);
        } else {
            // 降级方案：如果没有编辑器，显示完整对话框
            WelcomeDialog.show(e.getProject());
        }
    }
}
```

---

### 改进 3：首次安装欢迎提示

**步骤 1：创建配置服务**

```java
// src/main/java/com/shuyixiao/service/PandaCoderSettings.java
package com.shuyixiao.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PandaCoder 插件设置
 * 用于跟踪首次安装、使用次数等
 */
@Service(Service.Level.PROJECT)
@State(
    name = "PandaCoderSettings",
    storages = @Storage("pandacoder.xml")
)
public final class PandaCoderSettings implements PersistentStateComponent<PandaCoderSettings.State> {
    
    private State state = new State();
    
    public static PandaCoderSettings getInstance(Project project) {
        return project.getService(PandaCoderSettings.class);
    }
    
    @Nullable
    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
    
    // 设置方法
    public boolean isFirstInstall() {
        return state.firstInstall;
    }
    
    public void setFirstInstallComplete() {
        state.firstInstall = false;
    }
    
    public int getUsageCount() {
        return state.usageCount;
    }
    
    public void incrementUsageCount() {
        state.usageCount++;
    }
    
    public boolean shouldShowMilestone() {
        int count = state.usageCount;
        return count == 10 || count == 50 || count == 100;
    }
    
    // 状态类
    public static class State {
        public boolean firstInstall = true;
        public int usageCount = 0;
        public long lastWelcomeTime = 0;
    }
}
```

**步骤 2：创建启动监听器**

```java
// src/main/java/com/shuyixiao/listener/PandaCoderStartupActivity.java
package com.shuyixiao.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.shuyixiao.service.PandaCoderSettings;
import com.shuyixiao.ui.PandaCoderBalloon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 插件启动活动
 * 处理首次安装欢迎提示
 */
public class PandaCoderStartupActivity implements StartupActivity.DumbAware {
    
    @Override
    public void runActivity(@NotNull Project project) {
        PandaCoderSettings settings = PandaCoderSettings.getInstance(project);
        
        if (settings.isFirstInstall()) {
            // 延迟 2 秒显示欢迎提示（避免启动时过于拥挤）
            Timer timer = new Timer(2000, e -> {
                PandaCoderBalloon.showWelcome(project, null);
                settings.setFirstInstallComplete();
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
}
```

**步骤 3：注册服务和监听器**

```xml
<!-- src/main/resources/META-INF/plugin.xml -->

<extensions defaultExtensionNs="com.intellij">
    <!-- 添加设置服务 -->
    <projectService serviceImplementation="com.shuyixiao.service.PandaCoderSettings"/>
</extensions>

<!-- 添加启动活动 -->
<extensions defaultExtensionNs="com.intellij">
    <postStartupActivity implementation="com.shuyixiao.listener.PandaCoderStartupActivity"/>
</extensions>
```

---

### ✅ 阶段一完成检查

完成以上步骤后，你应该看到：

1. ✅ 右键菜单中"关于PandaCoder"已移到底部或变成子菜单
2. ✅ 点击"关于"显示轻量级气泡，而非模态对话框
3. ✅ 首次安装时自动显示欢迎气泡
4. ✅ 7秒后气泡自动消失或用户点击外部关闭

**测试步骤：**
```bash
# 1. 编译插件
./gradlew build

# 2. 运行 IDE
./gradlew runIde

# 3. 测试场景
- 打开任意文件，右键查看菜单位置
- 点击"PandaCoder 助手"，查看是否显示气泡
- 删除 .idea/pandacoder.xml，重启 IDE 测试首次安装
```

---

## 🏗️ 阶段二：Tool Window 开发（2-3天）

### 文件结构规划

```
src/main/java/com/shuyixiao/toolwindow/
├── PandaCoderToolWindowFactory.java      [核心工厂类]
├── PandaCoderToolWindow.java             [主窗口]
├── panels/
│   ├── DashboardPanel.java               [仪表盘]
│   ├── FunctionCardsPanel.java           [功能卡片]
│   ├── PromotionPanel.java               [推广面板]
│   └── QuickLinksPanel.java              [快速链接]
└── actions/
    ├── OpenGitStatAction.java            [打开Git统计]
    ├── OpenEsMonitorAction.java          [打开ES监控]
    └── OpenSqlMonitorAction.java         [打开SQL监控]

src/main/resources/
├── icons/
│   ├── toolwindow_panda.svg              [Tool Window 图标]
│   └── toolwindow_panda@2x.svg           [高分辨率图标]
└── META-INF/
    └── plugin.xml                        [注册 Tool Window]
```

### 步骤 1：创建 Tool Window 图标

**toolwindow_panda.svg** (13x13)
```svg
<!-- src/main/resources/icons/toolwindow_panda.svg -->
<svg width="13" height="13" viewBox="0 0 13 13" xmlns="http://www.w3.org/2000/svg">
  <!-- 简化的熊猫头像 -->
  <circle cx="6.5" cy="6.5" r="6" fill="#FFFFFF" stroke="#000000" stroke-width="0.5"/>
  <circle cx="4" cy="4" r="1.5" fill="#000000"/>
  <circle cx="9" cy="4" r="1.5" fill="#000000"/>
  <circle cx="4.5" cy="6" r="0.8" fill="#000000"/>
  <circle cx="8.5" cy="6" r="0.8" fill="#000000"/>
  <ellipse cx="6.5" cy="8.5" rx="2" ry="1.2" fill="#000000"/>
</svg>
```

### 步骤 2：创建 Tool Window Factory

```java
// src/main/java/com/shuyixiao/toolwindow/PandaCoderToolWindowFactory.java
package com.shuyixiao.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.shuyixiao.service.PandaCoderSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * PandaCoder Tool Window 工厂
 */
public class PandaCoderToolWindowFactory implements ToolWindowFactory, DumbAware {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, 
                                       @NotNull ToolWindow toolWindow) {
        // 创建 Tool Window 内容
        PandaCoderToolWindow window = new PandaCoderToolWindow(project, toolWindow);
        
        Content content = ContentFactory.getInstance()
            .createContent(window.getContent(), "", false);
        
        toolWindow.getContentManager().addContent(content);
        
        // 首次安装：自动展开 3 秒后自动折叠
        handleFirstInstall(project, toolWindow);
    }
    
    private void handleFirstInstall(Project project, ToolWindow toolWindow) {
        PandaCoderSettings settings = PandaCoderSettings.getInstance(project);
        
        if (settings.isFirstInstall()) {
            // 自动展开
            toolWindow.show(() -> {
                // 3 秒后自动折叠
                Timer timer = new Timer(3000, e -> {
                    toolWindow.hide(null);
                });
                timer.setRepeats(false);
                timer.start();
            });
        }
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;  // 对所有项目可用
    }
}
```

### 步骤 3：创建主窗口

```java
// src/main/java/com/shuyixiao/toolwindow/PandaCoderToolWindow.java
package com.shuyixiao.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.toolwindow.panels.DashboardPanel;
import com.shuyixiao.toolwindow.panels.FunctionCardsPanel;
import com.shuyixiao.toolwindow.panels.PromotionPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * PandaCoder Tool Window 主窗口
 */
public class PandaCoderToolWindow {
    
    private final Project project;
    private final ToolWindow toolWindow;
    private final JBPanel<?> mainPanel;
    
    public PandaCoderToolWindow(@NotNull Project project, 
                                @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.mainPanel = createMainPanel();
    }
    
    @NotNull
    public JComponent getContent() {
        return new JBScrollPane(mainPanel);
    }
    
    private JBPanel<?> createMainPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 创建垂直布局的内容面板
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // 1. 仪表盘（顶部）
        DashboardPanel dashboard = new DashboardPanel(project);
        contentPanel.add(dashboard);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // 2. 功能卡片（中部）
        FunctionCardsPanel functionCards = new FunctionCardsPanel(project);
        contentPanel.add(functionCards);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // 3. 推广面板（底部，可折叠）
        PromotionPanel promotion = new PromotionPanel(project);
        contentPanel.add(promotion);
        
        // 4. 弹性空间（推到底部）
        contentPanel.add(Box.createVerticalGlue());
        
        panel.add(contentPanel, BorderLayout.NORTH);
        
        return panel;
    }
}
```

### 步骤 4：创建仪表盘面板

```java
// src/main/java/com/shuyixiao/toolwindow/panels/DashboardPanel.java
package com.shuyixiao.toolwindow.panels;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.shuyixiao.service.PandaCoderSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 仪表盘面板 - 显示版本信息和使用统计
 */
public class DashboardPanel extends JBPanel<DashboardPanel> {
    
    private static final String VERSION = com.shuyixiao.version.VersionInfo.getVersion();
    
    public DashboardPanel(@NotNull Project project) {
        super(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));
        setBackground(UIUtil.getPanelBackground());
        
        // 头部区域
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // 左侧：品牌信息
        JBPanel<?> brandPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);
        
        // 图标
        try {
            Icon icon = new ImageIcon(
                getClass().getResource("/icons/pluginIcon.svg")
            );
            JBLabel iconLabel = new JBLabel(icon);
            iconLabel.setBorder(JBUI.Borders.emptyRight(10));
            brandPanel.add(iconLabel);
        } catch (Exception ignored) {}
        
        // 标题和副标题
        JBPanel<?> titlePanel = new JBPanel<>();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JBLabel titleLabel = new JBLabel("PandaCoder");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        
        JBLabel subtitleLabel = new JBLabel("中文开发者的智能编码助手");
        subtitleLabel.setForeground(UIUtil.getContextHelpForeground());
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(11f));
        titlePanel.add(subtitleLabel);
        
        brandPanel.add(titlePanel);
        headerPanel.add(brandPanel, BorderLayout.WEST);
        
        // 右侧：版本信息
        JBPanel<?> versionPanel = new JBPanel<>();
        versionPanel.setLayout(new BoxLayout(versionPanel, BoxLayout.Y_AXIS));
        versionPanel.setOpaque(false);
        
        JBLabel versionLabel = new JBLabel("v" + VERSION);
        versionLabel.setForeground(UIUtil.getContextHelpForeground());
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 10f));
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        versionPanel.add(versionLabel);
        
        // 使用统计
        int usageCount = PandaCoderSettings.getInstance(project).getUsageCount();
        JBLabel usageLabel = new JBLabel("使用 " + usageCount + " 次");
        usageLabel.setForeground(UIUtil.getContextHelpForeground());
        usageLabel.setFont(usageLabel.getFont().deriveFont(Font.PLAIN, 9f));
        usageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        versionPanel.add(usageLabel);
        
        headerPanel.add(versionPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.CENTER);
    }
}
```

### 步骤 5：创建功能卡片面板

```java
// src/main/java/com/shuyixiao/toolwindow/panels/FunctionCardsPanel.java
package com.shuyixiao.toolwindow.panels;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 功能卡片面板 - 快速访问主要功能
 */
public class FunctionCardsPanel extends JBPanel<FunctionCardsPanel> {
    
    public FunctionCardsPanel(@NotNull Project project) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(JBUI.Borders.empty(10, 0));
        setOpaque(false);
        
        // 标题
        JBLabel titleLabel = new JBLabel("⚡ 快速功能");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setBorder(JBUI.Borders.emptyBottom(10));
        add(titleLabel);
        
        // 功能卡片
        add(createFunctionCard(
            "📊 Git 统计",
            "代码提交统计与分析",
            () -> openToolWindow(project, "Git统计")
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "🔍 ES DSL Monitor",
            "Elasticsearch 查询监控",
            () -> openToolWindow(project, "ES DSL Monitor")
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "📝 SQL Monitor",
            "SQL 查询监控与分析",
            () -> openToolWindow(project, "SQL Monitor")
        ));
        
        add(Box.createVerticalStrut(8));
        
        add(createFunctionCard(
            "🚀 Jenkins 增强",
            "Pipeline 语法高亮与补全",
            () -> {
                // 显示提示
                JOptionPane.showMessageDialog(
                    this,
                    "Jenkins 增强功能已自动启用\n" +
                    "在 Jenkinsfile 中自动提供语法高亮和代码补全",
                    "Jenkins 增强",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        ));
    }
    
    private JComponent createFunctionCard(String title, 
                                         String description, 
                                         Runnable action) {
        JBPanel<?> card = new JBPanel<>(new BorderLayout(10, 5));
        card.setBorder(JBUI.Borders.empty(10));
        card.setBackground(UIUtil.getPanelBackground());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // 添加悬停效果
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(UIUtil.getListSelectionBackground(true));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(UIUtil.getPanelBackground());
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        
        // 内容
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        contentPanel.add(titleLabel);
        
        JBLabel descLabel = new JBLabel(description);
        descLabel.setForeground(UIUtil.getContextHelpForeground());
        descLabel.setFont(descLabel.getFont().deriveFont(10f));
        contentPanel.add(descLabel);
        
        card.add(contentPanel, BorderLayout.CENTER);
        
        // 右侧箭头
        JBLabel arrowLabel = new JBLabel("→");
        arrowLabel.setForeground(UIUtil.getContextHelpForeground());
        card.add(arrowLabel, BorderLayout.EAST);
        
        return card;
    }
    
    private void openToolWindow(Project project, String toolWindowId) {
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = manager.getToolWindow(toolWindowId);
        
        if (toolWindow != null) {
            toolWindow.activate(null);
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Tool Window \"" + toolWindowId + "\" 未找到",
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
```

### 步骤 6：创建推广面板（可折叠）

```java
// src/main/java/com/shuyixiao/toolwindow/panels/PromotionPanel.java
package com.shuyixiao.toolwindow.panels;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.shuyixiao.ui.QRCodeDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 推广面板 - 可折叠的作者信息和推广内容
 */
public class PromotionPanel extends JBPanel<PromotionPanel> {
    
    private boolean expanded = false;
    private final JBPanel<?> contentPanel;
    private final JBLabel expandIcon;
    
    public PromotionPanel(@NotNull Project project) {
        super(new BorderLayout());
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 0, 0, 0),
            JBUI.Borders.empty(10)
        ));
        setOpaque(false);
        
        // 头部（可点击展开/折叠）
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JBLabel titleLabel = new JBLabel("🌟 跟随作者成长");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        expandIcon = new JBLabel("▼");
        expandIcon.setForeground(UIUtil.getContextHelpForeground());
        headerPanel.add(expandIcon, BorderLayout.EAST);
        
        // 点击展开/折叠
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
        });
        
        add(headerPanel, BorderLayout.NORTH);
        
        // 内容面板（默认折叠）
        contentPanel = createContentPanel(project);
        contentPanel.setVisible(false);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private void toggleExpanded() {
        expanded = !expanded;
        contentPanel.setVisible(expanded);
        expandIcon.setText(expanded ? "▲" : "▼");
        revalidate();
        repaint();
    }
    
    private JBPanel<?> createContentPanel(Project project) {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.emptyTop(10));
        panel.setOpaque(false);
        
        // 作者信息
        JBLabel authorLabel = new JBLabel(
            "<html>作者：<b>@舒一笑不秃头</b><br/>" +
            "专注于架构与技术分享</html>"
        );
        authorLabel.setFont(authorLabel.getFont().deriveFont(11f));
        panel.add(authorLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        // 公众号按钮
        JButton wechatButton = new JButton("📱 关注公众号");
        wechatButton.putClientProperty("JButton.buttonType", "borderless");
        wechatButton.addActionListener(e -> {
            QRCodeDialog.showWechatQRCode(project);
        });
        panel.add(wechatButton);
        
        panel.add(Box.createVerticalStrut(5));
        
        // 社交链接
        JBPanel<?> linksPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 0));
        linksPanel.setOpaque(false);
        
        linksPanel.add(createLinkLabel("🐙 GitHub", 
            "https://github.com/shuyixiao-better/PandaCoder"));
        linksPanel.add(createLinkLabel("📝 博客", 
            "https://www.poeticcoder.com"));
        linksPanel.add(createLinkLabel("📺 CSDN", 
            "https://blog.csdn.net/yixiaoshu88"));
        
        panel.add(linksPanel);
        
        return panel;
    }
    
    private JComponent createLinkLabel(String text, String url) {
        JBLabel label = new JBLabel(text);
        label.setForeground(UIUtil.getLabelInfoForeground());
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setFont(label.getFont().deriveFont(10f));
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ignored) {}
            }
        });
        
        return label;
    }
}
```

### 步骤 7：注册 Tool Window

```xml
<!-- src/main/resources/META-INF/plugin.xml -->

<extensions defaultExtensionNs="com.intellij">
    <!-- ... 其他扩展 ... -->
    
    <!-- 注册 PandaCoder Tool Window -->
    <toolWindow 
        id="PandaCoder" 
        factoryClass="com.shuyixiao.toolwindow.PandaCoderToolWindowFactory"
        anchor="right"
        icon="/icons/toolwindow_panda.svg"
        secondary="false"/>
</extensions>
```

### 步骤 8：更新气泡提示，链接到 Tool Window

```java
// 修改 PandaCoderBalloon.java 中的超链接处理

case "open_toolwindow":
    // 打开 Tool Window
    ToolWindowManager manager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = manager.getToolWindow("PandaCoder");
    if (toolWindow != null) {
        toolWindow.activate(null);
    }
    break;
```

---

## 🎯 阶段三：智能推广系统（3-5天）

### 实现使用次数统计

已在阶段一实现 `PandaCoderSettings.java`，现在需要在功能中调用：

```java
// 在各个功能的 Action 中添加统计

// 示例：Git 统计工具
public class GitStatAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        
        // 增加使用计数
        PandaCoderSettings settings = PandaCoderSettings.getInstance(project);
        settings.incrementUsageCount();
        
        // 检查是否达到里程碑
        if (settings.shouldShowMilestone()) {
            PandaCoderBalloon.showMilestone(project, settings.getUsageCount());
        }
        
        // 原有功能逻辑
        // ...
    }
}
```

---

## 📊 效果对比

### 改进前
- 模态对话框阻断工作流
- 右键菜单首位显示"关于"
- 强制性信息展示

### 改进后（阶段一）
- 轻量级气泡，7秒自动消失
- 右键菜单优化，"关于"移到底部
- 用户可选择查看

### 改进后（阶段二）
- Tool Window 完整集成
- 所有功能集中管理
- 推广内容可折叠

### 改进后（阶段三）
- 智能推广，价值驱动
- 里程碑气泡提示
- 长期复利效应

---

## 🐛 故障排查

### 问题 1：气泡不显示

**原因**：Editor 为 null

**解决**：
```java
if (editor != null) {
    PandaCoderBalloon.showWelcome(project, editor);
} else {
    // 降级方案
    WelcomeDialog.show(project);
}
```

### 问题 2：Tool Window 图标不显示

**原因**：SVG 文件路径错误或格式问题

**解决**：
1. 确认文件路径：`src/main/resources/icons/toolwindow_panda.svg`
2. 使用 PNG 降级：`toolwindow_panda.png` (13x13)

### 问题 3：首次安装不触发

**原因**：配置文件已存在

**解决**：
```bash
# 删除配置文件重新测试
rm -rf .idea/pandacoder.xml
```

---

## ✅ 最终检查清单

### 阶段一
- [ ] 右键菜单已优化
- [ ] 气泡提示正常显示
- [ ] 首次安装欢迎提示工作正常
- [ ] 所有超链接可点击

### 阶段二
- [ ] Tool Window 成功注册
- [ ] 图标正确显示
- [ ] 仪表盘显示版本和统计
- [ ] 功能卡片可点击跳转
- [ ] 推广面板可折叠

### 阶段三
- [ ] 使用次数统计正常
- [ ] 里程碑气泡正常触发
- [ ] 版本更新通知包含推广

---

## 📚 相关文档

- [PandaCoder UI 重构方案](./PandaCoder_UI重构方案.md)
- [用户体验对比分析](./PandaCoder_UI重构_用户体验对比.md)
- [IntelliJ Platform SDK - Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [IntelliJ Platform SDK - Popups](https://plugins.jetbrains.com/docs/intellij/popups.html)

---

**创建时间**：2025-10-24  
**预计实施时间**：
- 阶段一：30 分钟
- 阶段二：2-3 天
- 阶段三：3-5 天

**Good luck! 🐼**

