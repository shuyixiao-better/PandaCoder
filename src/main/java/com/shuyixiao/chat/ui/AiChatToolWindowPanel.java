package com.shuyixiao.chat.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.setting.PluginSettings;
import com.shuyixiao.ai.chat.OpenAICompatibleChatClient;
import com.shuyixiao.chat.CodeLocator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class AiChatToolWindowPanel {
    private final Project project;
    private final JPanel root;
    private final JPanel messageListPanel = new JPanel();
    private final JTextArea inputArea = new JTextArea(4, 60);
    private final JButton sendButton = new JButton("发送");
    private final JButton applyButton = new JButton("应用到当前文件");
    private final JComboBox<String> modeCombo = new JComboBox<>(new String[]{"问答", "Agent"});
    private final JCheckBox applyToTargetCheck = new JCheckBox("应用到 @目标文件");
    private final JLabel targetLabel = new JLabel("");
    private final JComboBox<String> sessionCombo = new JComboBox<>();
    private final JButton newSessionButton = new JButton("新建会话");
    private final JButton deleteSessionButton = new JButton("删除会话");
    private final JButton clearSessionButton = new JButton("清空会话");

    private final List<OpenAICompatibleChatClient.Message> messages = new ArrayList<>();
    private PsiElement applyTargetPsi = null;
    private final java.util.Map<String, java.util.List<OpenAICompatibleChatClient.Message>> sessionMessages = new java.util.LinkedHashMap<>();
    private String currentSessionId;
    private int sessionCounter = 1;

    public AiChatToolWindowPanel(Project project) {
        this.project = project;
        this.root = buildUI();
        wireEvents();
    }

    public JComponent getContent() {
        return root;
    }

    private JPanel buildUI() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftTop.setOpaque(false);
        leftTop.add(new JLabel("模式："));
        leftTop.add(modeCombo);
        leftTop.add(new JLabel("会话："));
        sessionCombo.setPreferredSize(new Dimension(160, 28));
        leftTop.add(sessionCombo);
        leftTop.add(newSessionButton);
        leftTop.add(clearSessionButton);
        leftTop.add(deleteSessionButton);
        top.add(leftTop, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightTop.setOpaque(false);
        targetLabel.setForeground(Color.GRAY);
        rightTop.add(targetLabel);
        rightTop.add(applyToTargetCheck);
        applyToTargetCheck.setEnabled(false);
        top.add(rightTop, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
        messageListPanel.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        JScrollPane transcriptScroll = new JScrollPane(messageListPanel);
        transcriptScroll.setPreferredSize(new Dimension(800, 420));
        panel.add(transcriptScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        bottom.add(inputScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.add(applyButton);
        actions.add(sendButton);
        bottom.add(actions, BorderLayout.SOUTH);

        panel.add(bottom, BorderLayout.SOUTH);
        panel.setBorder(JBUI.Borders.empty(8));

        applyButton.setEnabled(false);
        // 绑定回车发送（Shift+Enter换行）
        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputArea.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
        am.put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendButton.doClick();
            }
        });
        // 保持 Shift+Enter 为换行
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");

        // 输入提示占位
        inputArea.setForeground(new Color(120,120,120));
        inputArea.setText("输入消息，支持 @类名 或 @文件路径 作为上下文。Enter 发送，Shift+Enter 换行。");
        inputArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("输入消息，支持 @类名 或 @文件路径 作为上下文。Enter 发送，Shift+Enter 换行。".equals(inputArea.getText())) {
                    inputArea.setText("");
                    inputArea.setForeground(Color.BLACK);
                }
            }
        });
        // 初始化首次会话
        createNewSession();
        sessionCombo.addActionListener(ev -> switchSession((String) sessionCombo.getSelectedItem()));
        newSessionButton.addActionListener(ev -> createNewSession());
        clearSessionButton.addActionListener(ev -> clearCurrentSession());
        deleteSessionButton.addActionListener(ev -> deleteCurrentSession());
        return panel;
    }

    private void wireEvents() {
        sendButton.addActionListener(this::onSend);
        applyButton.addActionListener(this::onApplyToEditor);
        modeCombo.addActionListener(e -> applyButton.setEnabled("Agent".equals(modeCombo.getSelectedItem().toString())));
    }

    private void onSend(ActionEvent e) {
        if (currentSessionId == null) {
            createNewSession();
        }
        String userText = inputArea.getText().trim();
        if (userText.isEmpty()) {
            return;
        }
        inputArea.setText("");
        String contextMd = attachProjectContext(userText);
        if (!contextMd.isEmpty()) {
            addMessage("上下文", contextMd);
        }
        addMessage("用户", userText);

        getCurrentMessages().add(new OpenAICompatibleChatClient.Message("user", userText));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                PluginSettings s = PluginSettings.getInstance();
                if (!s.isEnableAiChatAssistant()) {
                    SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "未启用智能助手，请在设置中开启并配置。", "AI Chat"));
                    return;
                }
                String provider = s.getAiProviderType();
                if ("openai".equals(provider)) {
                    if (s.getAiBaseUrl() == null || s.getAiBaseUrl().trim().isEmpty() || s.getAiModel() == null || s.getAiModel().trim().isEmpty()) {
                        SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "OpenAI 兼容模式未配置完整，请填写 Base URL 和模型名称。", "AI Chat"));
                        return;
                    }
                } else if ("ollama".equals(provider)) {
                    if (s.getAiBaseUrl() == null || s.getAiBaseUrl().trim().isEmpty() || s.getAiModel() == null || s.getAiModel().trim().isEmpty()) {
                        SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "Ollama 模式未配置完整，请填写 Base URL 和模型名称。", "AI Chat"));
                        return;
                    }
                } else {
                    if (!PluginSettings.getInstance().isEnableDomesticAI() || PluginSettings.getInstance().getDomesticAIApiKey().isEmpty()) {
                        SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "国内模型未配置或未启用，请在“翻译引擎”页设置。", "AI Chat"));
                        return;
                    }
                }
                String reply;
                if ("openai".equals(s.getAiProviderType())) {
                    // 自动识别 Ollama baseUrl
                    boolean useOllama = s.getAiBaseUrl() != null && (s.getAiBaseUrl().contains("11434") || s.getAiBaseUrl().toLowerCase().contains("ollama") || s.getAiBaseUrl().endsWith("/api") || s.getAiBaseUrl().endsWith("/api/"));
                    if (useOllama) {
                        java.util.List<com.shuyixiao.ai.chat.OllamaChatClient.Message> ms = new ArrayList<>();
                        for (OpenAICompatibleChatClient.Message m : getCurrentMessages()) {
                            ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message(m.role, m.content));
                        }
                        if (!contextMd.isEmpty()) {
                            ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message("system", "项目上下文:\n\n" + contextMd));
                        }
                        reply = com.shuyixiao.ai.chat.OllamaChatClient.chat(s.getAiBaseUrl(), s.getAiModel(), ms);
                    } else {
                        List<OpenAICompatibleChatClient.Message> ms = new ArrayList<>(getCurrentMessages());
                        if (!contextMd.isEmpty()) {
                            ms.add(new OpenAICompatibleChatClient.Message("system", "项目上下文:\n\n" + contextMd));
                        }
                        reply = OpenAICompatibleChatClient.chat(s.getAiBaseUrl(), s.getAiApiKey(), s.getAiModel(), ms);
                    }
                } else if ("ollama".equals(s.getAiProviderType())) {
                    java.util.List<com.shuyixiao.ai.chat.OllamaChatClient.Message> ms = new ArrayList<>();
                    for (OpenAICompatibleChatClient.Message m : getCurrentMessages()) {
                        ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message(m.role, m.content));
                    }
                    if (!contextMd.isEmpty()) {
                        ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message("system", "项目上下文:\n\n" + contextMd));
                    }
                    reply = com.shuyixiao.ai.chat.OllamaChatClient.chat(s.getAiBaseUrl(), s.getAiModel(), ms);
                } else {
                    // 国内模型复用翻译API的分析能力
                    com.shuyixiao.DomesticAITranslationAPI api = new com.shuyixiao.DomesticAITranslationAPI();
                    reply = api.translateTextWithAI(buildAgentAwarePrompt(), userText);
                }
                String r = reply == null ? "<空>" : reply;
                SwingUtilities.invokeLater(() -> {
                    getCurrentMessages().add(new OpenAICompatibleChatClient.Message("assistant", r));
                    addMessage("助手", r);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> Messages.showErrorDialog(project, "发送失败：\n" + ex.getMessage(), "AI Chat"));
            }
        });
    }

    private String buildAgentAwarePrompt() {
        boolean agent = "Agent".equals(modeCombo.getSelectedItem().toString());
        if (!agent) return "请以开发助手风格回答问题";
        return "你是IDE智能Agent，针对用户需求给出代码修改建议与片段，输出清晰的步骤与代码。";
    }

    private void onApplyToEditor(ActionEvent e) {
        if (!"Agent".equals(modeCombo.getSelectedItem().toString())) {
            return;
        }
        if (currentSessionId == null) {
            Messages.showInfoMessage(project, "请先新建会话", "AI Agent");
            return;
        }
        if (getCurrentMessages().isEmpty()) return;
        // 取最后一条助手消息
        String content = null;
        java.util.List<OpenAICompatibleChatClient.Message> list = getCurrentMessages();
        for (int i = list.size() - 1; i >= 0; i--) {
            OpenAICompatibleChatClient.Message m = list.get(i);
            if ("assistant".equals(m.role)) {
                content = m.content;
                break;
            }
        }
        if (content == null || content.isEmpty()) {
            Messages.showInfoMessage(project, "暂无可应用的助手回复", "AI Agent");
            return;
        }

        if (applyToTargetCheck.isSelected() && applyTargetPsi != null) {
            openAndApplyToTarget(content);
            return;
        }
        Editor editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            Editor[] editors = EditorFactory.getInstance().getAllEditors();
            if (editors.length == 0) {
                Messages.showInfoMessage(project, "未检测到打开的编辑器", "AI Agent");
                return;
            }
            editor = editors[0];
        }

        final Editor ed = editor;
        final Document doc = ed.getDocument();
        final String finalContent = content;
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                int start = ed.getSelectionModel().hasSelection() ? ed.getSelectionModel().getSelectionStart() : ed.getCaretModel().getOffset();
                int end = ed.getSelectionModel().hasSelection() ? ed.getSelectionModel().getSelectionEnd() : start;
                doc.replaceString(start, end, finalContent);
            }
        });
        Messages.showInfoMessage(project, "已将助手内容写入当前文件（替换选区或插入光标处）", "AI Agent");
    }

    private void openAndApplyToTarget(String content) {
        PsiFile targetFile = null;
        if (applyTargetPsi instanceof PsiFile) {
            targetFile = (PsiFile) applyTargetPsi;
        } else if (applyTargetPsi instanceof PsiClass) {
            targetFile = ((PsiClass) applyTargetPsi).getContainingFile();
        }
        if (targetFile == null || targetFile.getVirtualFile() == null) {
            Messages.showInfoMessage(project, "未找到可写入的目标文件", "AI Agent");
            return;
        }
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, targetFile.getVirtualFile());
        descriptor.navigate(true);
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);

        Document doc = PsiDocumentManager.getInstance(project).getDocument(targetFile);
        if (doc == null) {
            com.intellij.openapi.vfs.VirtualFile vf = targetFile.getVirtualFile();
            if (vf != null) {
                doc = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(vf);
            }
            if (doc == null) {
                Messages.showInfoMessage(project, "无法获取文档对象", "AI Agent");
                return;
            }
        }
        final Document fdoc = doc;
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                fdoc.insertString(fdoc.getTextLength(), "\n\n/* AI Agent 变更 */\n" + content + "\n");
            }
        });
        PsiDocumentManager.getInstance(project).commitDocument(fdoc);
        Messages.showInfoMessage(project, "已将助手内容追加到 @目标文件 末尾", "AI Agent");
    }

    private void addMessage(String who, String md) {
        JPanel lineWrap = new JPanel(new FlowLayout("用户".equals(who) ? FlowLayout.RIGHT : FlowLayout.LEFT));
        lineWrap.setOpaque(false);
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBorder(new EmptyBorder(8, 8, 8, 8));
        bubble.setBackground("用户".equals(who) ? new Color(235, 248, 255) : com.intellij.util.ui.UIUtil.getPanelBackground());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel header = new JLabel(who);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        headerPanel.add(header, BorderLayout.WEST);
        if ("上下文".equals(who)) {
            JButton openBtn = new JButton("打开到编辑器");
            openBtn.addActionListener(e -> openLocatedTarget(md));
            headerPanel.add(openBtn, BorderLayout.EAST);
        }
        bubble.add(headerPanel, BorderLayout.NORTH);

        String html = MarkdownUtil.renderCompositeHtml(md);
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        htmlPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlPane.setText(html);

        JScrollPane inner = new JScrollPane(htmlPane);
        inner.setBorder(null);
        inner.setPreferredSize(new Dimension(760, 200));
        bubble.add(inner, BorderLayout.CENTER);

        lineWrap.add(bubble);
        messageListPanel.add(lineWrap);
        messageListPanel.add(Box.createVerticalStrut(6));
        messageListPanel.revalidate();
        SwingUtilities.invokeLater(() -> {
            messageListPanel.scrollRectToVisible(new Rectangle(0, messageListPanel.getHeight() + 200, 1, 1));
        });
    }

    private String attachProjectContext(String userText) {
        List<String> targets = extractTargets(userText);
        if (targets.isEmpty()) return "";
        StringBuilder md = new StringBuilder();
        applyTargetPsi = null;
        for (String t : targets) {
            PsiClass cls = CodeLocator.findClass(project, t);
            if (cls != null) {
                String file = cls.getContainingFile() != null && cls.getContainingFile().getVirtualFile() != null ? cls.getContainingFile().getVirtualFile().getPath() : "";
                md.append("### ").append(t).append("\n");
                md.append("文件: `").append(file).append(":`").append(CodeLocator.line(project, cls)).append("`\n\n");
                md.append("```java\n").append(CodeLocator.snippet(cls, 4000)).append("\n```\n\n");
                applyTargetPsi = cls;
                continue;
            }
            if (t.endsWith(".java")) {
                PsiFile file = CodeLocator.findFile(project, t);
                if (file != null) {
                    md.append("### ").append(t).append("\n");
                    String path = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : "";
                    md.append("文件: `").append(path).append("`\n\n");
                    md.append("```java\n").append(CodeLocator.snippet(file, 4000)).append("\n```\n\n");
                    applyTargetPsi = file;
                }
            }
        }
        if (applyTargetPsi != null) {
            applyToTargetCheck.setEnabled(true);
            String hint = applyTargetPsi instanceof PsiFile ? ((PsiFile)applyTargetPsi).getVirtualFile().getPath() : (applyTargetPsi instanceof PsiClass ? ((PsiClass)applyTargetPsi).getQualifiedName() : "");
            targetLabel.setText(hint == null ? "" : ("目标：" + hint));
        } else {
            applyToTargetCheck.setEnabled(false);
            targetLabel.setText("");
        }
        return md.toString();
    }

    private List<String> extractTargets(String text) {
        List<String> list = new ArrayList<>();
        // @FQN 类名
        java.util.regex.Pattern mentionFqn = java.util.regex.Pattern.compile("@((?:[a-zA-Z_]\\w*\\.)+[A-Z]\\w+)");
        java.util.regex.Matcher mmf = mentionFqn.matcher(text);
        while (mmf.find()) list.add(mmf.group(1));
        // @路径 文件
        java.util.regex.Pattern mentionPath = java.util.regex.Pattern.compile("@([\\w./-]+\\.java)");
        java.util.regex.Matcher mmp = mentionPath.matcher(text);
        while (mmp.find()) list.add(mmp.group(1));
        // 兜底：非@形式
        java.util.regex.Pattern fqn = java.util.regex.Pattern.compile("(?:[a-zA-Z_]\\w*\\.)+[A-Z]\\w+");
        java.util.regex.Matcher mf = fqn.matcher(text);
        while (mf.find()) list.add(mf.group());
        java.util.regex.Pattern jfn = java.util.regex.Pattern.compile("[A-Z]\\w+\\.java");
        java.util.regex.Matcher mj = jfn.matcher(text);
        while (mj.find()) list.add(mj.group());
        return list;
    }

    private void openLocatedTarget(String md) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("###\\s+([^\n]+)");
        java.util.regex.Matcher m = p.matcher(md);
        if (!m.find()) return;
        String target = m.group(1).trim();
        PsiClass cls = CodeLocator.findClass(project, target);
        if (cls != null) {
            CodeLocator.open(project, cls);
            return;
        }
        if (target.endsWith(".java")) {
            PsiFile file = CodeLocator.findFile(project, target);
            if (file != null) CodeLocator.open(project, file);
        }
    }

    private java.util.List<OpenAICompatibleChatClient.Message> getCurrentMessages() {
        return sessionMessages.get(currentSessionId);
    }

    private void createNewSession() {
        String id = "会话 " + (sessionCounter++);
        sessionMessages.put(id, new java.util.ArrayList<>());
        sessionCombo.addItem(id);
        sessionCombo.setSelectedItem(id);
        currentSessionId = id;
        messageListPanel.removeAll();
        messageListPanel.revalidate();
        messageListPanel.repaint();
        applyTargetPsi = null;
        applyToTargetCheck.setEnabled(false);
        targetLabel.setText("");
    }

    private void switchSession(String id) {
        if (id == null || !sessionMessages.containsKey(id)) return;
        currentSessionId = id;
        messageListPanel.removeAll();
        // 重新渲染当前会话消息历史
        for (OpenAICompatibleChatClient.Message m : sessionMessages.get(id)) {
            addMessage("assistant".equals(m.role) ? "助手" : "用户", m.content);
        }
        messageListPanel.revalidate();
        messageListPanel.repaint();
        applyTargetPsi = null;
        applyToTargetCheck.setEnabled(false);
        targetLabel.setText("");
    }

    private void clearCurrentSession() {
        if (currentSessionId == null) return;
        sessionMessages.put(currentSessionId, new java.util.ArrayList<>());
        messageListPanel.removeAll();
        messageListPanel.revalidate();
        messageListPanel.repaint();
        applyTargetPsi = null;
        applyToTargetCheck.setEnabled(false);
        targetLabel.setText("");
    }

    private void deleteCurrentSession() {
        if (currentSessionId == null) return;
        int idx = sessionCombo.getSelectedIndex();
        sessionMessages.remove(currentSessionId);
        sessionCombo.removeItem(currentSessionId);
        if (sessionCombo.getItemCount() == 0) {
            // 删除最后一个会话后不自动新建，清空界面并要求用户手动新建
            currentSessionId = null;
            messageListPanel.removeAll();
            messageListPanel.revalidate();
            messageListPanel.repaint();
            applyTargetPsi = null;
            applyToTargetCheck.setEnabled(false);
            targetLabel.setText("");
        } else {
            int sel = Math.max(0, idx - 1);
            sessionCombo.setSelectedIndex(sel);
            switchSession((String) sessionCombo.getSelectedItem());
        }
    }
}
