package nlob;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayDeque;
import java.util.Deque;

public class MarkdownConverter {

    public String convertBlocksToMarkdown(JSONArray blocks) {
        StringBuilder markdown = new StringBuilder();
        Deque<String> openTags = new ArrayDeque<>(); // 用于跟踪需要关闭的标签

        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            int depth = block.getIntValue("_depth", 0);

            String blockMarkdown = convertBlockToMarkdown(block, depth, openTags);
            if (blockMarkdown != null && !blockMarkdown.isEmpty()) {
                markdown.append(blockMarkdown).append("\n\n");
            }
        }

        // 关闭所有未关闭的标签
        while (!openTags.isEmpty()) {
            String tag = openTags.pop();
            if ("details".equals(tag)) {
                markdown.append("</details>\n\n");
            }
        }

        return markdown.toString().trim();
    }

    private String convertBlockToMarkdown(JSONObject block, int depth, Deque<String> openTags) {
        String type = block.getString("type");
        if (type == null) return "";

        JSONObject content = block.getJSONObject(type);
        if (content == null) return "";

        // 根据深度添加缩进
        String indent = "  ".repeat(depth);

        switch (type) {
            case "toggle":
                return convertToggleBlock(content, depth, openTags);

            case "bulleted_list_item":
                return indent + convertBulletedListItem(content, block.getBoolean("has_children"));

            case "numbered_list_item":
                return indent + convertNumberedListItem(content, block.getBoolean("has_children"));

            case "to_do":
                return indent + convertTodoItem(content);

            case "table":
                return convertTable(content);

            case "table_row":
                return convertTableRow(content);

            case "child_page":
                return convertChildPage(content);

            case "column_list":
            case "column":
                // 列布局本身不生成内容，由子块处理
                return "";

            case "paragraph":
                return indent + convertRichText(content.getJSONArray("rich_text"));

            case "heading_1":
                return indent + "# " + convertRichText(content.getJSONArray("rich_text"));

            case "heading_2":
                return indent + "## " + convertRichText(content.getJSONArray("rich_text"));

            case "heading_3":
                return indent + "### " + convertRichText(content.getJSONArray("rich_text"));

            case "code":
                String language = content.getString("language");
                if (language == null) language = "";
                String code = convertRichText(content.getJSONArray("rich_text"));
                return indent + "```" + language + "\n" + code + "\n```";

            case "quote":
                return indent + "> " + convertRichText(content.getJSONArray("rich_text"));

            case "callout":
                return convertCallout(content, indent);

            case "image":
                return convertImage(content, indent);

            default:
                System.out.println("未处理的块类型: " + type);
                if (content.containsKey("rich_text")) {
                    return indent + convertRichText(content.getJSONArray("rich_text"));
                }
                return "";
        }
    }

    /**
     * 处理折叠块 (toggle)
     */
    private String convertToggleBlock(JSONObject content, int depth, Deque<String> openTags) {
        String toggleText = convertRichText(content.getJSONArray("rich_text"));
        openTags.push("details"); // 标记需要关闭的标签

        return "<details>\n<summary>" + toggleText + "</summary>\n\n";
    }

    /**
     * 处理项目符号列表项
     */
    private String convertBulletedListItem(JSONObject content, boolean hasChildren) {
        String text = convertRichText(content.getJSONArray("rich_text"));
        return "- " + text;
    }

    /**
     * 处理编号列表项
     */
    private String convertNumberedListItem(JSONObject content, boolean hasChildren) {
        String text = convertRichText(content.getJSONArray("rich_text"));
        return "1. " + text;
    }

    /**
     * 处理待办事项
     */
    private String convertTodoItem(JSONObject content) {
        boolean checked = content.getBooleanValue("checked");
        String text = convertRichText(content.getJSONArray("rich_text"));
        return (checked ? "- [x] " : "- [ ] ") + text;
    }

    /**
     * 处理标注块 (callout)
     */
    private String convertCallout(JSONObject content, String indent) {
        String text = convertRichText(content.getJSONArray("rich_text"));
        // 使用引用格式表示标注
        return indent + "> 💡 " + text;
    }

    /**
     * 处理图片
     */
    private String convertImage(JSONObject content, String indent) {
        JSONObject image = content.getJSONObject("image");
        if (image != null) {
            String url = image.getString("url");
            if (url != null) {
                return indent + "![](" + url + ")";
            }
        }
        return "";
    }

    /**
     * 处理子页面
     */
    private String convertChildPage(JSONObject content) {
        String title = content.getString("title");
        return "**子页面: " + title + "**";
    }

    /**
     * 处理表格
     */
    private String convertTable(JSONObject content) {
        // 表格需要特殊处理，这里返回空，由表格行处理具体内容
        return "";
    }

    /**
     * 处理表格行
     */
    private String convertTableRow(JSONObject content) {
        JSONArray cells = content.getJSONArray("cells");
        StringBuilder row = new StringBuilder("|");

        for (int i = 0; i < cells.size(); i++) {
            JSONArray cellText = cells.getJSONArray(i);
            String cellContent = convertRichText(cellText);
            row.append(" ").append(cellContent).append(" |");
        }

        return row.toString();
    }

    /**
     * 转换富文本（保持不变）
     */
    private String convertRichText(JSONArray richText) {
        if (richText == null || richText.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < richText.size(); i++) {
            JSONObject text = richText.getJSONObject(i);
            String plainText = text.getString("plain_text");
            if (plainText == null) continue;

            JSONObject annotations = text.getJSONObject("annotations");
            String formattedText = applyTextFormatting(plainText, annotations);

            String href = text.getString("href");
            if (href != null && !href.isEmpty()) {
                formattedText = "[" + formattedText + "](" + href + ")";
            }

            result.append(formattedText);
        }

        return result.toString();
    }

    private String applyTextFormatting(String text, JSONObject annotations) {
        if (annotations == null) return text;

        boolean bold = annotations.getBooleanValue("bold");
        boolean italic = annotations.getBooleanValue("italic");
        boolean code = annotations.getBooleanValue("code");
        boolean strikethrough = annotations.getBooleanValue("strikethrough");

        if (code) {
            return "`" + text + "`";
        }

        String result = text;
        if (bold) result = "**" + result + "**";
        if (italic) result = "*" + result + "*";
        if (strikethrough) result = "~~" + result + "~~";

        return result;
    }
}