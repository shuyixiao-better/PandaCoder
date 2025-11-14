package com.shuyixiao.chat.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.shuyixiao.ai.chat.OpenAICompatibleChatClient;
import com.shuyixiao.chat.CodeLocator;
import com.shuyixiao.setting.PluginSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AiChatToolWindowPanel {

    private static final String INPUT_PLACEHOLDER = "向 PandaCoder 提问，支持 @类 / 文件路径 绑定上下文 (Enter 发送，Shift+Enter 换行)";

    private final Project project;
    private final JPanel root;
    private final JPanel messageListPanel = new JPanel();
    private final JTextArea inputArea = new JTextArea(5, 60);
    private final JButton sendButton = new JButton("发送");
    private final JButton applyButton = new JButton("应用到编辑器");
    private final JToggleButton chatModeButton = new JToggleButton("Chat 模式");
    private final JToggleButton agentModeButton = new JToggleButton("Agent 模式");
    private final JCheckBox applyToTargetCheck = new JCheckBox("写入 @目标");
    private final JLabel targetLabel = new JLabel("未选择目标");
    private final JComboBox<String> sessionCombo = new JComboBox<>();
    private final JButton newSessionButton = new JButton("新建会话");
    private final JButton deleteSessionButton = new JButton("删除会话");
    private final JButton clearSessionButton = new JButton("清空会话");

    private final JPanel contextChipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final JTextField contextTargetField = new JTextField();
    private final JButton attachManualButton = new JButton("添加");
    private final JButton attachCurrentFileButton = new JButton("当前文件");
    private final JButton attachSelectionButton = new JButton("选中代码");
    private final JButton clearContextButton = new JButton("清空上下文");
    private final JLabel contextEmptyLabel = new JLabel("未选择上下文，可使用下方按钮或 @引用添加");

    private PsiElement applyTargetPsi = null;
    private final Map<String, List<OpenAICompatibleChatClient.Message>> sessionMessages = new LinkedHashMap<>();
    private final Map<String, List<ContextAttachment>> sessionContexts = new LinkedHashMap<>();
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
        panel.setBorder(JBUI.Borders.empty(12));
        panel.add(buildHeaderPanel(), BorderLayout.NORTH);
        panel.add(buildTranscriptPanel(), BorderLayout.CENTER);
        panel.add(buildComposerPanel(), BorderLayout.SOUTH);
        targetLabel.setForeground(JBColor.GRAY);
        contextEmptyLabel.setForeground(JBColor.GRAY);
        applyToTargetCheck.setEnabled(false);
        chatModeButton.setSelected(true);
        styleModeButtons();
        createNewSession();
        renderContextChips();
        updateModeState();
        return panel;
    }

    private JComponent buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(JBUI.Borders.emptyBottom(12));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("PandaCoder 智能助手");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize() + 2f));
        JLabel subtitle = new JLabel("支持 Chat / Agent 模式，结合项目上下文精准问答");
        subtitle.setForeground(JBColor.GRAY);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel controlPanel = new JPanel();
        controlPanel.setOpaque(false);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(buildModePanel());
        controlPanel.add(Box.createVerticalStrut(6));
        controlPanel.add(buildSessionPanel());
        header.add(controlPanel, BorderLayout.EAST);
        return header;
    }

    private JComponent buildModePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        configureCompactButton(chatModeButton);
        configureCompactButton(agentModeButton);
        ButtonGroup group = new ButtonGroup();
        group.add(chatModeButton);
        group.add(agentModeButton);
        panel.add(chatModeButton);
        panel.add(agentModeButton);
        return panel;
    }

    private JComponent buildSessionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setOpaque(false);
        sessionCombo.setPreferredSize(new Dimension(180, 28));
        configureCompactButton(newSessionButton);
        configureCompactButton(clearSessionButton);
        configureCompactButton(deleteSessionButton);
        panel.add(new JLabel("会话："));
        panel.add(sessionCombo);
        panel.add(newSessionButton);
        panel.add(clearSessionButton);
        panel.add(deleteSessionButton);
        return panel;
    }

    private JComponent buildTranscriptPanel() {
        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
        messageListPanel.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        JScrollPane scrollPane = new JScrollPane(messageListPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        return scrollPane;
    }

    private JComponent buildComposerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.add(buildContextPanel(), BorderLayout.NORTH);
        panel.add(buildInputPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildContextPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border(), 1), JBUI.Borders.empty(10)));
        container.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("上下文片段（可选）");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        header.add(title, BorderLayout.WEST);
        header.add(targetLabel, BorderLayout.EAST);
        container.add(header, BorderLayout.NORTH);

        contextChipsPanel.setOpaque(false);
        container.add(contextChipsPanel, BorderLayout.CENTER);

        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hintRow.setOpaque(false);
        hintRow.add(new JLabel("快捷添加："));
        configureCompactButton(attachCurrentFileButton);
        configureCompactButton(attachSelectionButton);
        configureCompactButton(clearContextButton);
        hintRow.add(attachCurrentFileButton);
        hintRow.add(attachSelectionButton);
        hintRow.add(clearContextButton);

        JPanel manualRow = new JPanel(new BorderLayout(6, 0));
        manualRow.setOpaque(false);
        JLabel manualLabel = new JLabel("指定文件/类：");
        manualRow.add(manualLabel, BorderLayout.WEST);
        contextTargetField.setColumns(24);
        manualRow.add(contextTargetField, BorderLayout.CENTER);
        configureCompactButton(attachManualButton);
        manualRow.add(attachManualButton, BorderLayout.EAST);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(hintRow);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(manualRow);
        container.add(bottom, BorderLayout.SOUTH);
        return container;
    }

    private JComponent buildInputPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border(), 1), JBUI.Borders.empty(10)));
        configureInputArea();
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(JBUI.Borders.empty());
        container.add(inputScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(applyToTargetCheck);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        configurePrimaryButton(applyButton);
        configurePrimaryButton(sendButton);
        right.add(applyButton);
        right.add(sendButton);
        actions.add(left, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);
        container.add(actions, BorderLayout.SOUTH);
        return container;
    }

    private void configureInputArea() {
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(inputArea.getFont().deriveFont(inputArea.getFont().getSize() + 1f));
        inputArea.setForeground(JBColor.GRAY);
        inputArea.setText(INPUT_PLACEHOLDER);
        inputArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (INPUT_PLACEHOLDER.equals(inputArea.getText())) {
                    inputArea.setText("");
                    inputArea.setForeground(JBColor.foreground());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputArea.getText().trim().isEmpty()) {
                    inputArea.setForeground(JBColor.GRAY);
                    inputArea.setText(INPUT_PLACEHOLDER);
                }
            }
        });
        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputArea.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
        am.put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendButton.doClick();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");
    }

    private void configureCompactButton(AbstractButton button) {
        button.setFocusPainted(false);
        button.setBorder(JBUI.Borders.empty(4, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configurePrimaryButton(AbstractButton button) {
        configureCompactButton(button);
        button.setBackground(new JBColor(new Color(0x4C8BF5), new Color(0x3D6DD6)));
        button.setForeground(JBColor.WHITE);
        button.setOpaque(true);
    }

    private void styleModeButtons() {
        styleToggle(chatModeButton);
        styleToggle(agentModeButton);
    }

    private void styleToggle(JToggleButton button) {
        button.setOpaque(true);
        button.setBorder(JBUI.Borders.empty(6, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(button.isSelected()
                ? new JBColor(new Color(0xE3ECFF), new Color(0x3B4A63))
                : new JBColor(new Color(0xF6F7FB), new Color(0x2B2D30)));
        button.setForeground(button.isSelected() ? JBColor.foreground() : JBColor.GRAY);
    }

    private void wireEvents() {
        sendButton.addActionListener(this::onSend);
        applyButton.addActionListener(this::onApplyToEditor);
        chatModeButton.addActionListener(e -> updateModeState());
        agentModeButton.addActionListener(e -> updateModeState());
        sessionCombo.addActionListener(ev -> switchSession((String) sessionCombo.getSelectedItem()));
        newSessionButton.addActionListener(ev -> createNewSession());
        clearSessionButton.addActionListener(ev -> clearCurrentSession());
        deleteSessionButton.addActionListener(ev -> deleteCurrentSession());
        attachCurrentFileButton.addActionListener(ev -> attachCurrentFileContext());
        attachSelectionButton.addActionListener(ev -> attachSelectionContext());
        clearContextButton.addActionListener(ev -> clearPinnedContexts());
        attachManualButton.addActionListener(ev -> attachTargetFromField());
        contextTargetField.addActionListener(ev -> attachTargetFromField());
    }

    private void updateModeState() {
        styleModeButtons();
        boolean agent = isAgentMode();
        applyButton.setEnabled(agent);
        if (!agent) {
            applyToTargetCheck.setSelected(false);
        }
        applyToTargetCheck.setEnabled(agent && applyTargetPsi != null);
    }

    private boolean isAgentMode() {
        return agentModeButton.isSelected();
    }

    private void onSend(ActionEvent e) {
        if (currentSessionId == null) {
            createNewSession();
        }
        String rawText = inputArea.getText();
        if (INPUT_PLACEHOLDER.equals(rawText)) {
            rawText = "";
        }
        String userText = rawText == null ? "" : rawText.trim();
        if (userText.isEmpty()) {
            return;
        }
        inputArea.setText("");
        String contextMd = buildContextMarkdown(userText);
        if (!contextMd.isEmpty()) {
            addMessage("上下文", contextMd);
        }
        addMessage("用户", userText);

        getCurrentMessages().add(new OpenAICompatibleChatClient.Message("user", userText));

        final String finalUserText = userText;
        final String finalContextMd = contextMd;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                PluginSettings s = PluginSettings.getInstance();
                if (!s.isEnableAiChatAssistant()) {
                    SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "未启用智能助手，请在设置中开启并配置。", "AI Chat"));
                    return;
                }
                String provider = s.getAiProviderType();
                if ("openai".equals(provider) || "ollama".equals(provider)) {
                    if (s.getAiBaseUrl() == null || s.getAiBaseUrl().trim().isEmpty() || s.getAiModel() == null || s.getAiModel().trim().isEmpty()) {
                        SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "AI 服务未配置完整，请填写 Base URL 和模型名称。", "AI Chat"));
                        return;
                    }
                } else {
                    if (!PluginSettings.getInstance().isEnableDomesticAI() || PluginSettings.getInstance().getDomesticAIApiKey().isEmpty()) {
                        SwingUtilities.invokeLater(() -> Messages.showWarningDialog(project, "国内模型未配置或未启用，请在“翻译引擎”页设置。", "AI Chat"));
                        return;
                    }
                }
                String reply;
                if ("openai".equals(provider)) {
                    boolean useOllama = s.getAiBaseUrl() != null && (s.getAiBaseUrl().contains("11434") || s.getAiBaseUrl().toLowerCase().contains("ollama") || s.getAiBaseUrl().endsWith("/api") || s.getAiBaseUrl().endsWith("/api/"));
                    if (useOllama) {
                        List<com.shuyixiao.ai.chat.OllamaChatClient.Message> ms = new ArrayList<>();
                        for (OpenAICompatibleChatClient.Message m : getCurrentMessages()) {
                            ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message(m.role, m.content));
                        }
                        if (!finalContextMd.isEmpty()) {
                            ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message("system", "项目上下文:\n\n" + finalContextMd));
                        }
                        reply = com.shuyixiao.ai.chat.OllamaChatClient.chat(s.getAiBaseUrl(), s.getAiModel(), ms);
                    } else {
                        List<OpenAICompatibleChatClient.Message> ms = new ArrayList<>(getCurrentMessages());
                        if (!finalContextMd.isEmpty()) {
                            ms.add(new OpenAICompatibleChatClient.Message("system", "项目上下文:\n\n" + finalContextMd));
                        }
                        reply = OpenAICompatibleChatClient.chat(s.getAiBaseUrl(), s.getAiApiKey(), s.getAiModel(), ms);
                    }
                } else if ("ollama".equals(provider)) {
                    List<com.shuyixiao.ai.chat.OllamaChatClient.Message> ms = new ArrayList<>();
                    for (OpenAICompatibleChatClient.Message m : getCurrentMessages()) {
                        ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message(m.role, m.content));
                    }
                    if (!finalContextMd.isEmpty()) {
                        ms.add(new com.shuyixiao.ai.chat.OllamaChatClient.Message("system", "项目上下文:\n\n" + finalContextMd));
                    }
                    reply = com.shuyixiao.ai.chat.OllamaChatClient.chat(s.getAiBaseUrl(), s.getAiModel(), ms);
                } else {
                    com.shuyixiao.DomesticAITranslationAPI api = new com.shuyixiao.DomesticAITranslationAPI();
                    reply = api.translateTextWithAI(buildAgentAwarePrompt(), finalUserText);
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
        if (!isAgentMode()) {
            return "请以开发助手风格回答问题";
        }
        return "你是IDE智能Agent，针对用户需求给出代码修改建议与片段，输出清晰的步骤与代码。";
    }

    private void onApplyToEditor(ActionEvent e) {
        if (!isAgentMode()) {
            return;
        }
        if (currentSessionId == null) {
            Messages.showInfoMessage(project, "请先新建会话", "AI Agent");
            return;
        }
        if (getCurrentMessages().isEmpty()) return;
        String content = null;
        List<OpenAICompatibleChatClient.Message> list = getCurrentMessages();
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
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
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
        } else if (applyTargetPsi instanceof PsiMethod) {
            targetFile = ((PsiMethod) applyTargetPsi).getContainingFile();
        }
        if (targetFile == null || targetFile.getVirtualFile() == null) {
            Messages.showInfoMessage(project, "无法定位 @目标 文件", "AI Agent");
            return;
        }
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, targetFile.getVirtualFile());
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
        Document doc = PsiDocumentManager.getInstance(project).getDocument(targetFile);
        if (doc == null) {
            Messages.showInfoMessage(project, "无法写入目标文件", "AI Agent");
            return;
        }
        final Document fdoc = doc;
        final String finalContent = content;
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                fdoc.insertString(fdoc.getTextLength(), "\n" + finalContent + "\n");
            }
        });
        PsiDocumentManager.getInstance(project).commitDocument(fdoc);
        Messages.showInfoMessage(project, "已将助手内容追加到 @目标文件 末尾", "AI Agent");
    }

    private void addMessage(String who, String md) {
        JPanel bubbleWrapper = new JPanel(new BorderLayout());
        bubbleWrapper.setBorder(JBUI.Borders.empty(6));
        bubbleWrapper.setOpaque(false);

        JPanel bubble = new JPanel(new BorderLayout(0, 6));
        bubble.setOpaque(true);
        Color userColor = new Color(0xEAF6FF);
        Color assistantColor = new Color(0xF4F5F7);
        if ("用户".equals(who)) {
            bubble.setBackground(new JBColor(userColor, new Color(0x334155)));
        } else if ("助手".equals(who)) {
            bubble.setBackground(new JBColor(assistantColor, new Color(0x2A2B30)));
        } else {
            bubble.setBackground(new JBColor(new Color(0xFFF6E9), new Color(0x3A3325)));
        }
        bubble.setBorder(JBUI.Borders.empty(10));

        JLabel header = new JLabel(who);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        bubble.add(header, BorderLayout.NORTH);

        String html = MarkdownUtil.renderCompositeHtml(md);
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        htmlPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlPane.setText(html);
        htmlPane.setOpaque(false);
        htmlPane.setBorder(null);
        bubble.add(htmlPane, BorderLayout.CENTER);

        if ("上下文".equals(who)) {
            JButton openBtn = new JButton("打开上下文");
            configureCompactButton(openBtn);
            openBtn.addActionListener(e -> openLocatedTarget(md));
            bubble.add(openBtn, BorderLayout.SOUTH);
        }

        bubbleWrapper.add(bubble, BorderLayout.WEST);
        messageListPanel.add(bubbleWrapper);
        messageListPanel.add(Box.createVerticalStrut(6));
        messageListPanel.revalidate();
        SwingUtilities.invokeLater(() -> messageListPanel.scrollRectToVisible(new Rectangle(0, messageListPanel.getHeight() + 200, 1, 1)));
    }

    private String buildContextMarkdown(String userText) {
        applyTargetPsi = null;
        StringBuilder md = new StringBuilder();
        for (ContextAttachment att : getPinnedContexts()) {
            md.append(att.markdown);
            if (applyTargetPsi == null && att.psi != null) {
                applyTargetPsi = att.psi;
            }
        }
        String auto = attachProjectContext(userText);
        if (!auto.isEmpty()) {
            if (md.length() > 0) {
                md.append("\n");
            }
            md.append(auto);
        }
        updateTargetIndicator();
        return md.toString();
    }

    private void updateTargetIndicator() {
        if (applyTargetPsi == null) {
            targetLabel.setText("未选择目标");
            applyToTargetCheck.setEnabled(false);
            applyToTargetCheck.setSelected(false);
        } else {
            targetLabel.setText("目标：" + describePsi(applyTargetPsi));
            applyToTargetCheck.setEnabled(isAgentMode());
        }
    }

    private String describePsi(PsiElement element) {
        if (element instanceof PsiMethod) {
            PsiMethod m = (PsiMethod) element;
            return (m.getContainingClass() != null ? m.getContainingClass().getQualifiedName() : "") + "#" + m.getName();
        }
        if (element instanceof PsiClass) {
            PsiClass cls = (PsiClass) element;
            return cls.getQualifiedName() != null ? cls.getQualifiedName() : cls.getName();
        }
        if (element instanceof PsiFile) {
            PsiFile file = (PsiFile) element;
            return file.getVirtualFile() != null ? file.getVirtualFile().getPath() : file.getName();
        }
        return element.toString();
    }

    private String attachProjectContext(String userText) {
        List<String> targets = extractTargets(userText);
        if (targets.isEmpty()) return "";
        StringBuilder md = new StringBuilder();
        int totalLen = 0;
        Set<String> seen = new HashSet<>();
        for (String t : targets) {
            ContextAttachment att = resolveContextAttachment(t);
            if (att == null || !seen.add(att.key)) {
                continue;
            }
            md.append(att.markdown);
            totalLen += att.markdown.length();
            if (applyTargetPsi == null && att.psi != null) {
                applyTargetPsi = att.psi;
            }
            if (totalLen > 12000) break;
        }
        return md.toString();
    }

    private List<String> extractTargets(String text) {
        List<String> list = new ArrayList<>();
        java.util.regex.Pattern mentionFqn = java.util.regex.Pattern.compile("@((?:[a-zA-Z_]\\w*\\.)+[A-Z]\\w+(?:#[a-zA-Z_]\\w+)?)");
        java.util.regex.Matcher mmf = mentionFqn.matcher(text);
        while (mmf.find()) list.add(mmf.group(1));
        java.util.regex.Pattern mentionPath = java.util.regex.Pattern.compile("@([\\w./-]+\\.java(?::\\d+(?:-\\d+)?)?)");
        java.util.regex.Matcher mmp = mentionPath.matcher(text);
        while (mmp.find()) list.add(mmp.group(1));
        java.util.regex.Pattern fqn = java.util.regex.Pattern.compile("(?:[a-zA-Z_]\\w*\\.)+[A-Z]\\w+");
        java.util.regex.Matcher mf = fqn.matcher(text);
        while (mf.find()) list.add(mf.group());
        java.util.regex.Pattern jfn = java.util.regex.Pattern.compile("[A-Z]\\w+\\.java");
        java.util.regex.Matcher mj = jfn.matcher(text);
        while (mj.find()) list.add(mj.group());
        return list;
    }

    private void openLocatedTarget(String md) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("###\\s+([^\\n]+)");
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

    private List<OpenAICompatibleChatClient.Message> getCurrentMessages() {
        return sessionMessages.get(currentSessionId);
    }

    private void createNewSession() {
        String id = "会话 " + (sessionCounter++);
        sessionMessages.put(id, new ArrayList<>());
        sessionContexts.put(id, new ArrayList<>());
        sessionCombo.addItem(id);
        sessionCombo.setSelectedItem(id);
        currentSessionId = id;
        messageListPanel.removeAll();
        messageListPanel.revalidate();
        messageListPanel.repaint();
        applyTargetPsi = null;
        applyToTargetCheck.setEnabled(false);
        targetLabel.setText("未选择目标");
        renderContextChips();
    }

    private void switchSession(String id) {
        if (id == null || !sessionMessages.containsKey(id)) return;
        currentSessionId = id;
        messageListPanel.removeAll();
        for (OpenAICompatibleChatClient.Message m : sessionMessages.get(id)) {
            addMessage("assistant".equals(m.role) ? "助手" : "用户", m.content);
        }
        messageListPanel.revalidate();
        messageListPanel.repaint();
        applyTargetPsi = null;
        applyToTargetCheck.setEnabled(false);
        targetLabel.setText("未选择目标");
        renderContextChips();
    }

    private void clearCurrentSession() {
        if (currentSessionId == null) return;
        sessionMessages.put(currentSessionId, new ArrayList<>());
        sessionContexts.put(currentSessionId, new ArrayList<>());
        messageListPanel.removeAll();
        messageListPanel.revalidate();
        messageListPanel.repaint();
        applyTargetPsi = null;
        applyToTargetCheck.setEnabled(false);
        targetLabel.setText("未选择目标");
        renderContextChips();
    }

    private void deleteCurrentSession() {
        if (currentSessionId == null) return;
        int idx = sessionCombo.getSelectedIndex();
        sessionMessages.remove(currentSessionId);
        sessionContexts.remove(currentSessionId);
        sessionCombo.removeItem(currentSessionId);
        if (sessionCombo.getItemCount() == 0) {
            currentSessionId = null;
            messageListPanel.removeAll();
            messageListPanel.revalidate();
            messageListPanel.repaint();
            applyTargetPsi = null;
            applyToTargetCheck.setEnabled(false);
            targetLabel.setText("未选择目标");
            contextChipsPanel.removeAll();
            contextChipsPanel.revalidate();
            contextChipsPanel.repaint();
        } else {
            int sel = Math.max(0, idx - 1);
            sessionCombo.setSelectedIndex(sel);
            switchSession((String) sessionCombo.getSelectedItem());
        }
    }

    private void renderContextChips() {
        contextChipsPanel.removeAll();
        List<ContextAttachment> contexts = getPinnedContexts();
        if (contexts == null || contexts.isEmpty()) {
            contextChipsPanel.add(contextEmptyLabel);
        } else {
            for (ContextAttachment att : contexts) {
                JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                chip.setBorder(JBUI.Borders.empty(2, 8));
                chip.setBackground(new JBColor(new Color(0xE2E8F0), new Color(0x253043)));
                chip.setOpaque(true);
                JLabel text = new JLabel(att.label);
                chip.add(text);
                JButton close = new JButton("×");
                close.setMargin(new Insets(0, 4, 0, 4));
                close.setBorder(null);
                close.setFocusPainted(false);
                close.setOpaque(false);
                close.addActionListener(e -> removePinnedContext(att.id));
                chip.add(close);
                contextChipsPanel.add(chip);
            }
        }
        contextChipsPanel.revalidate();
        contextChipsPanel.repaint();
    }

    private void addPinnedContext(ContextAttachment attachment) {
        if (attachment == null || currentSessionId == null) {
            return;
        }
        List<ContextAttachment> contexts = sessionContexts.computeIfAbsent(currentSessionId, k -> new ArrayList<>());
        contexts.removeIf(existing -> existing.key.equals(attachment.key));
        contexts.add(attachment);
        renderContextChips();
    }

    private void removePinnedContext(String id) {
        if (currentSessionId == null) return;
        List<ContextAttachment> contexts = sessionContexts.get(currentSessionId);
        if (contexts == null) return;
        contexts.removeIf(att -> att.id.equals(id));
        renderContextChips();
    }

    private void clearPinnedContexts() {
        if (currentSessionId == null) return;
        sessionContexts.put(currentSessionId, new ArrayList<>());
        renderContextChips();
    }

    private List<ContextAttachment> getPinnedContexts() {
        if (currentSessionId == null) {
            return new ArrayList<>();
        }
        return sessionContexts.computeIfAbsent(currentSessionId, k -> new ArrayList<>());
    }

    private void attachCurrentFileContext() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            Messages.showInfoMessage(project, "未检测到打开的编辑器", "AI Chat");
            return;
        }
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            Messages.showInfoMessage(project, "无法读取当前文件", "AI Chat");
            return;
        }
        int totalLines = editor.getDocument().getLineCount();
        int endLine = Math.min(200, totalLines);
        String snippet = CodeLocator.snippet(psiFile, 1, endLine);
        String path = psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : psiFile.getName();
        ContextAttachment attachment = buildAttachment(psiFile.getName(), path, snippet, psiFile, "file:" + path, guessLanguage(psiFile));
        addPinnedContext(attachment);
    }

    private void attachSelectionContext() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            Messages.showInfoMessage(project, "请先打开文件并选中代码", "AI Chat");
            return;
        }
        String selection = editor.getSelectionModel().getSelectedText();
        if (selection == null || selection.trim().isEmpty()) {
            Messages.showInfoMessage(project, "请选中需要作为上下文的代码片段", "AI Chat");
            return;
        }
        if (selection.length() > 4000) {
            selection = selection.substring(0, 4000);
        }
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        String name = psiFile != null ? psiFile.getName() + " 选中片段" : "选中片段";
        String path = psiFile != null && psiFile.getVirtualFile() != null ? psiFile.getVirtualFile().getPath() : "";
        ContextAttachment attachment = buildAttachment(name, path, selection, psiFile, "selection:" + path + selection.hashCode(), guessLanguage(psiFile));
        addPinnedContext(attachment);
    }

    private void attachTargetFromField() {
        String text = contextTargetField.getText().trim();
        if (text.isEmpty()) {
            Messages.showInfoMessage(project, "请输入要定位的文件路径、类名或 @表达式", "AI Chat");
            return;
        }
        ContextAttachment attachment = resolveContextAttachment(text);
        if (attachment == null) {
            Messages.showWarningDialog(project, "未找到匹配的文件/类：" + text, "AI Chat");
            return;
        }
        addPinnedContext(attachment);
        contextTargetField.setText("");
    }

    private ContextAttachment resolveContextAttachment(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String raw = token.trim();
        if (raw.startsWith("@")) {
            raw = raw.substring(1);
        }
        try {
            if (raw.matches("^(?:[a-zA-Z_]\\w*\\.)+[A-Z]\\w+#[a-zA-Z_]\\w+$")) {
                String className = raw.substring(0, raw.indexOf('#'));
                String methodName = raw.substring(raw.indexOf('#') + 1);
                PsiClass cls = CodeLocator.findClass(project, className);
                if (cls != null) {
                    PsiMethod method = CodeLocator.findMethod(cls, methodName);
                    if (method != null) {
                        String file = filePathOf(cls) + ":" + CodeLocator.line(project, method);
                        String snippet = CodeLocator.snippet(method, 6000);
                        return buildAttachment(className + "#" + methodName, file, snippet, method, raw, "java");
                    }
                }
            }
            if (raw.contains(":") && raw.endsWith(".java")) {
                String path = raw.substring(0, raw.lastIndexOf(':'));
                String range = raw.substring(raw.lastIndexOf(':') + 1);
                PsiFile file = CodeLocator.findFileByPath(project, path);
                if (file != null) {
                    String[] nums = range.split("-");
                    int s = Integer.parseInt(nums[0]);
                    int e = nums.length > 1 ? Integer.parseInt(nums[1]) : s + 50;
                    String chunk = CodeLocator.snippet(file, s, e);
                    return buildAttachment(raw, filePathOf(file), chunk, file, raw, "java");
                }
            }
            PsiClass cls = CodeLocator.findClass(project, raw);
            if (cls != null) {
                String file = filePathOf(cls) + ":" + CodeLocator.line(project, cls);
                String chunk = CodeLocator.snippet(cls, 6000);
                return buildAttachment(raw, file, chunk, cls, raw, "java");
            }
            if (raw.endsWith(".java")) {
                PsiFile file = CodeLocator.findFile(project, raw);
                if (file == null) {
                    file = CodeLocator.findFileByPath(project, raw);
                }
                if (file != null) {
                    String chunk = CodeLocator.snippet(file, 1, 200);
                    return buildAttachment(raw, filePathOf(file), chunk, file, raw, "java");
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private ContextAttachment buildAttachment(String title, String path, String snippet, PsiElement psi, String key, String lang) {
        StringBuilder md = new StringBuilder();
        md.append("### ").append(title).append("\n");
        if (path != null && !path.isEmpty()) {
            md.append("文件: `").append(path).append("`\n\n");
        }
        md.append("```").append(lang == null ? "" : lang).append("\n");
        md.append(snippet).append("\n```\n\n");
        return new ContextAttachment(title, md.toString(), psi, key);
    }

    private String filePathOf(PsiElement element) {
        PsiFile file = element instanceof PsiFile ? (PsiFile) element : element.getContainingFile();
        if (file != null && file.getVirtualFile() != null) {
            return file.getVirtualFile().getPath();
        }
        return file != null ? file.getName() : "";
    }

    private String guessLanguage(PsiElement element) {
        if (element instanceof PsiFile) {
            return ((PsiFile) element).getLanguage().getID().toLowerCase();
        }
        return "java";
    }

    private static class ContextAttachment {
        private final String id = UUID.randomUUID().toString();
        private final String label;
        private final String markdown;
        private final PsiElement psi;
        private final String key;

        private ContextAttachment(String label, String markdown, PsiElement psi, String key) {
            this.label = label;
            this.markdown = markdown;
            this.psi = psi;
            this.key = key == null ? label : key;
        }
    }
}
