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

    private final List<OpenAICompatibleChatClient.Message> messages = new ArrayList<>();

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
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("模式："));
        top.add(modeCombo);
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

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
        return panel;
    }

    private void wireEvents() {
        sendButton.addActionListener(this::onSend);
        applyButton.addActionListener(this::onApplyToEditor);
        modeCombo.addActionListener(e -> applyButton.setEnabled("Agent".equals(modeCombo.getSelectedItem().toString())));
    }

    private void onSend(ActionEvent e) {
        String userText = inputArea.getText().trim();
        if (userText.isEmpty()) {
            return;
        }
        inputArea.setText("");
        addMessage("用户", userText);

        messages.add(new OpenAICompatibleChatClient.Message("user", userText));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                PluginSettings s = PluginSettings.getInstance();
                String reply;
                if ("openai".equals(s.getAiProviderType())) {
                    reply = OpenAICompatibleChatClient.chat(s.getAiBaseUrl(), s.getAiApiKey(), s.getAiModel(), new ArrayList<>(messages));
                } else {
                    // 国内模型复用翻译API的分析能力
                    com.shuyixiao.DomesticAITranslationAPI api = new com.shuyixiao.DomesticAITranslationAPI();
                    reply = api.translateTextWithAI(buildAgentAwarePrompt(), userText);
                }
                String r = reply == null ? "<空>" : reply;
                SwingUtilities.invokeLater(() -> {
                    messages.add(new OpenAICompatibleChatClient.Message("assistant", r));
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
        if (messages.isEmpty()) return;
        // 取最后一条助手消息
        String content = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            OpenAICompatibleChatClient.Message m = messages.get(i);
            if ("assistant".equals(m.role)) {
                content = m.content;
                break;
            }
        }
        if (content == null || content.isEmpty()) {
            Messages.showInfoMessage(project, "暂无可应用的助手回复", "AI Agent");
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

    private void addMessage(String who, String md) {
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBorder(new EmptyBorder(8, 8, 8, 8));
        bubble.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());

        JLabel header = new JLabel(who);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        bubble.add(header, BorderLayout.NORTH);

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

        messageListPanel.add(bubble);
        messageListPanel.add(Box.createVerticalStrut(6));
        messageListPanel.revalidate();
        SwingUtilities.invokeLater(() -> {
            messageListPanel.scrollRectToVisible(new Rectangle(0, messageListPanel.getHeight() + 200, 1, 1));
        });
    }
}
