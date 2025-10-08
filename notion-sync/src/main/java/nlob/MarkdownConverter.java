package nlob;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class MarkdownConverter {

    public String convertBlocksToMarkdown(JSONArray blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();

        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            String blockMarkdown = convertBlockToMarkdown(block);
            if (blockMarkdown != null && !blockMarkdown.isEmpty()) {
                markdown.append(blockMarkdown).append("\n\n");
            }
        }

        return markdown.toString().trim();
    }

    private String convertBlockToMarkdown(JSONObject block) {
        String type = block.getString("type");
        if (type == null) {
            return "";
        }

        JSONObject content = block.getJSONObject(type);
        if (content == null) {
            return "";
        }

        switch (type) {
            case "paragraph":
                return convertRichText(content.getJSONArray("rich_text"));

            case "heading_1":
                return "# " + convertRichText(content.getJSONArray("rich_text"));

            case "heading_2":
                return "## " + convertRichText(content.getJSONArray("rich_text"));

            case "heading_3":
                return "### " + convertRichText(content.getJSONArray("rich_text"));

            case "bulleted_list_item":
                return "- " + convertRichText(content.getJSONArray("rich_text"));

            case "numbered_list_item":
                return "1. " + convertRichText(content.getJSONArray("rich_text"));

            case "code":
                String language = content.getString("language");
                if (language == null) language = "";
                String code = convertRichText(content.getJSONArray("rich_text"));
                return "```" + language + "\n" + code + "\n```";

            case "quote":
                return "> " + convertRichText(content.getJSONArray("rich_text"));

            case "divider":
                return "---";

            case "image":
                // 处理外部图片
                JSONObject external = content.getJSONObject("external");
                if (external != null) {
                    String url = external.getString("url");
                    if (url != null) {
                        return "![](" + url + ")";
                    }
                }
                // 处理内部文件
                JSONObject file = content.getJSONObject("file");
                if (file != null) {
                    String url = file.getString("url");
                    if (url != null) {
                        return "![](" + url + ")";
                    }
                }
                return "";

            case "to_do":
                boolean checked = content.getBooleanValue("checked");
                String todoText = convertRichText(content.getJSONArray("rich_text"));
                return (checked ? "- [x] " : "- [ ] ") + todoText;

            case "toggle":
                return convertRichText(content.getJSONArray("rich_text")) + " ▶";

            default:
                System.out.println("未处理的块类型: " + type);
                // 对于未处理的块类型，尝试提取文本内容
                if (content.containsKey("rich_text")) {
                    return convertRichText(content.getJSONArray("rich_text"));
                }
                return "";
        }
    }

    private String convertRichText(JSONArray richText) {
        if (richText == null || richText.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < richText.size(); i++) {
            JSONObject text = richText.getJSONObject(i);
            String plainText = text.getString("plain_text");
            if (plainText == null) {
                continue;
            }

            JSONObject annotations = text.getJSONObject("annotations");
            String formattedText = plainText;

            if (annotations != null) {
                boolean bold = annotations.getBooleanValue("bold");
                boolean italic = annotations.getBooleanValue("italic");
                boolean code = annotations.getBooleanValue("code");
                boolean strikethrough = annotations.getBooleanValue("strikethrough");
                boolean underline = annotations.getBooleanValue("underline");

                if (code) {
                    formattedText = "`" + plainText + "`";
                } else {
                    if (bold) formattedText = "**" + formattedText + "**";
                    if (italic) formattedText = "*" + formattedText + "*";
                    if (strikethrough) formattedText = "~~" + formattedText + "~~";
                    if (underline) formattedText = "__" + formattedText + "__";
                }
            }

            // 处理链接
            String href = text.getString("href");
            if (href != null && !href.isEmpty()) {
                formattedText = "[" + formattedText + "](" + href + ")";
            }

            result.append(formattedText);
        }

        return result.toString();
    }
}