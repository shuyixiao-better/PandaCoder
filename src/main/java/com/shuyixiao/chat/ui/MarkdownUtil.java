package com.shuyixiao.chat.ui;

public class MarkdownUtil {

    public static String renderCompositeHtml(String md) {
        String safe = md == null ? "" : md;
        String htmlBody = toHtml(safe);
        return wrapHtml(htmlBody);
    }

    private static String wrapHtml(String body) {
        String css = "body{font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; font-size:13px;}"+
                "pre{background:#f6f8fa;border:1px solid #e1e4e8;border-radius:6px;padding:8px;overflow:auto;}"+
                "code{font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace; background:#f6f8fa; padding:2px 4px; border-radius:4px;}"+
                "h1,h2,h3{margin:8px 0;}"+
                "ul{margin:4px 0 4px 16px;}"+
                "p{margin:4px 0;}";
        return "<html><head><meta charset='utf-8'><style>" + css + "</style></head><body>" + body + "</body></html>";
    }

    // 简易 Markdown 转换器（标题/列表/代码块/行内代码/粗体/斜体）
    private static String toHtml(String md) {
        String normalized = md.replace("\r\n", "\n").replace("\r", "\n");

        // 处理代码块
        normalized = replaceFencedCodeBlocks(normalized);

        // 行级处理：标题/列表
        StringBuilder html = new StringBuilder();
        boolean inList = false;
        for (String line : normalized.split("\n")) {
            String t = line.trim();
            if (t.startsWith("### ")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h3>").append(t.substring(4)).append("</h3>");
            } else if (t.startsWith("## ")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h2>").append(t.substring(3)).append("</h2>");
            } else if (t.startsWith("# ")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h1>").append(t.substring(2)).append("</h1>");
            } else if (t.startsWith("- ")) {
                if (!inList) { html.append("<ul>"); inList = true; }
                html.append("<li>").append(applyInline(t.substring(2))).append("</li>");
            } else if (t.startsWith("<pre>")) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append(line);
            } else if (t.isEmpty()) {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<p></p>");
            } else {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<p>").append(applyInline(t)).append("</p>");
            }
        }
        if (inList) html.append("</ul>");
        return html.toString();
    }

    private static String applyInline(String s) {
        String r = s;
        // 粗体 **text**
        r = r.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        // 斜体 *text*
        r = r.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)\\*(?<!\\*)", "<i>$1</i>");
        // 行内代码 `code`
        r = replaceInlineCode(r);
        return r;
    }

    private static String replaceInlineCode(String s) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("`([^`]+)`");
        java.util.regex.Matcher m = p.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String code = escapeHtml(m.group(1));
            String rep = "<code>" + code + "</code>";
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String replaceFencedCodeBlocks(String s) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("```([a-zA-Z0-9_-]+)?\\n([\\s\\S]*?)```", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher m = p.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String lang = m.group(1) == null ? "" : m.group(1);
            String code = escapeHtml(m.group(2));
            String rep = "<pre><code class='language-" + lang + "'>" + code + "</code></pre>";
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
