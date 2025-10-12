package nlob;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class MarkdownConverter {

    public String convertBlocksToMarkdown(JSONArray blocks) {
        StringBuilder markdown = new StringBuilder();

        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            String blockMarkdown = convertBlockWithChildren(block, blocks, i);
            if (blockMarkdown != null && !blockMarkdown.isEmpty()) {
                markdown.append(blockMarkdown).append("\n\n");
            }
        }

        return markdown.toString().trim();
    }

    /**
     * 转换块及其子块
     */
    private String convertBlockWithChildren(JSONObject block, JSONArray allBlocks, int currentIndex) {
        String type = block.getString("type");
        if (type == null) return "";

        JSONObject content = block.getJSONObject(type);
        if (content == null) return "";

        int depth = block.getIntValue("_depth", 0);
        String indent = "  ".repeat(depth);

        // 处理折叠块
        if ("toggle".equals(type)) {
            return convertToggleWithChildren(block, allBlocks, currentIndex, indent);
        }
        //处理表格
        if("table".equals(type)) {
            JSONArray tables = new JSONArray();
            JSONObject tableRowObject = allBlocks.getJSONObject(++currentIndex);
            while (tableRowObject.get("type").equals("table_row")){
                tables.add(tableRowObject);
                tableRowObject = allBlocks.getJSONObject(++currentIndex);
            }
            return  convertTableRow(tables);
        }

        // 处理其他块类型
        return convertSingleBlock(block, indent);
    }

    /**
     * 处理折叠块及其子内容
     */
    private String convertToggleWithChildren(JSONObject toggleBlock, JSONArray allBlocks, int currentIndex, String indent) {
        JSONObject content = toggleBlock.getJSONObject("toggle");
        String toggleText = convertRichText(content.getJSONArray("rich_text"));

        StringBuilder toggleMarkdown = new StringBuilder();
        toggleMarkdown.append(indent).append("<details>\n");
        toggleMarkdown.append(indent).append("<summary>").append(toggleText).append("</summary>\n\n");

        // 查找并处理所有子块
        String toggleId = toggleBlock.getString("id");
        int toggleDepth = toggleBlock.getIntValue("_depth", 0);

        // 从下一个块开始，找到所有属于这个折叠块的子块
        int childCount = 0;
        for (int i = currentIndex + 1; i < allBlocks.size(); i++) {
            JSONObject childBlock = allBlocks.getJSONObject(i);
            int childDepth = childBlock.getIntValue("_depth", 0);
            String parentId = childBlock.getString("_parent_id");

            // 如果遇到同级或更浅的块，说明子块结束了
            if (childDepth <= toggleDepth) {
                break;
            }

            // 如果这个子块属于当前折叠块
            if (toggleId.equals(parentId) && childDepth == toggleDepth + 1) {
                String childMarkdown = convertSingleBlock(childBlock, indent + "  ");
                if (childMarkdown != null && !childMarkdown.isEmpty()) {
                    toggleMarkdown.append(childMarkdown).append("\n\n");
                    childCount++;
                }
            }
        }

        toggleMarkdown.append(indent).append("</details>");

        System.out.println("折叠块 '" + toggleText + "' 包含 " + childCount + " 个子块");
        return toggleMarkdown.toString();
    }

    /**
     * 转换单个块（不处理子块）
     */
    private String convertSingleBlock(JSONObject block, String indent) {
        String type = block.getString("type");
        if (type == null) return "";

        JSONObject content = block.getJSONObject(type);
        if (content == null) return "";

        switch (type) {
            case "paragraph":
                return indent + convertRichText(content.getJSONArray("rich_text"));

            case "heading_1":
                return indent + "# " + convertRichText(content.getJSONArray("rich_text"));

            case "heading_2":
                return indent + "## " + convertRichText(content.getJSONArray("rich_text"));

            case "heading_3":
                return indent + "### " + convertRichText(content.getJSONArray("rich_text"));

            case "bulleted_list_item":
                return indent + "- " + convertRichText(content.getJSONArray("rich_text"));

            case "numbered_list_item":
                return indent + "1. " + convertRichText(content.getJSONArray("rich_text"));

            case "to_do":
                boolean checked = content.getBooleanValue("checked");
                String todoText = convertRichText(content.getJSONArray("rich_text"));
                return indent + (checked ? "- [x] " : "- [ ] ") + todoText;

            case "code":
                String language = content.getString("language");
                if (language == null) language = "";
                String code = convertRichText(content.getJSONArray("rich_text"));
                return indent + "```" + language + "\n" + code + "\n```";

            case "quote":
                return indent + "> " + convertRichText(content.getJSONArray("rich_text"));

            case "callout":
                String calloutText = convertRichText(content.getJSONArray("rich_text"));
                return indent + "> 💡 " + calloutText;

            case "child_page":
                String title = content.getString("title");
                return indent + "**子页面: " + title + "**";


            case "image":
                return convertImage(content, indent);

            case "column_list":
            case "column":
                // 列布局本身不生成内容
                return "";

            default:
                System.out.println("未处理的块类型: " + type);
                if (content.containsKey("rich_text")) {
                    return indent + convertRichText(content.getJSONArray("rich_text"));
                }
                return "";
        }
    }

    /**
     * 转换表格
     */
    private String convertTableRow(JSONArray content) {
        StringBuilder ans = new StringBuilder();
        StringBuilder separator = new StringBuilder("|");
        for (int i = 0; i < content.size(); i++) {
            JSONObject tableRow = content.getJSONObject(i).getJSONObject("table_row");
            JSONArray cells = tableRow.getJSONArray("cells");
            StringBuilder row = new StringBuilder("|");

            for (int j = 0; j < cells.size(); j++) {
                JSONArray cellText = cells.getJSONArray(j);
                String cellContent = convertRichText(cellText);
                row.append(" ").append(cellContent).append(" |");
                if(i==0){
                    separator.append("---|");
                }
            }
            if (row.toString().equals("|")) {
                return row.toString();
            }
            ans.append(row).append("\n");
            if (i==0){
                separator.append("\n");
                ans.append(separator);
            }
        }
        return ans.toString();

    }

    /**
     * 转换图片
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
     * 转换富文本
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