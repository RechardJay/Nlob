package nlob;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import nlob.entity.BlogPostDO;
import nlob.utils.TimeUtil;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NotionClient {
    public static final MediaType json = MediaType.parse("application/json; charset=utf-8");
    private final String apiToken;
    private final String databaseId;
    private final OkHttpClient httpClient;

    private static final String NOTION_API_BASE = "https://api.notion.com/v1";

    public NotionClient(String apiToken, String databaseId) {
        this.apiToken = apiToken;
        this.databaseId = databaseId;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true) // 自动重试连接失败
                .build();
    }

    /**
     * 查询Notion数据库获取所有博客文章（全量，支持分页）
     */
    public List<BlogPostDO> fetchAllPosts() throws Exception {
        System.out.println("开始从Notion获取所有博客文章（全量同步）...");
        System.out.println("Database ID: " + databaseId);
        return fetchPostsWithFilter(null);
    }

    /**
     * 查询Notion数据库获取最近 N 天编辑的博客文章（增量，支持分页）
     */
    public List<BlogPostDO> fetchRecentPosts(int daysBack) throws Exception {
        String since = TimeUtil.getUTCBeforeDays(daysBack);
        System.out.println("开始从Notion获取最近 " + daysBack + " 天的博客文章（增量同步）...");
        System.out.println("Database ID: " + databaseId + "，起始时间: " + since);

        String filterBody = String.format(
                "{\"filter\":{\"timestamp\":\"last_edited_time\",\"last_edited_time\":{\"on_or_after\":\"%s\"}}}",
                since
        );
        return fetchPostsWithFilter(filterBody);
    }

    /**
     * 通用的分页查询方法
     * @param filterBody 可选的过滤器 JSON，为 null 时表示无过滤（全量）
     */
    private List<BlogPostDO> fetchPostsWithFilter(String filterBody) throws Exception {
        List<BlogPostDO> allPosts = new ArrayList<>();
        String startCursor = null;
        boolean hasMore = true;
        int pageNum = 0;

        while (hasMore) {
            pageNum++;
            String requestBody = buildQueryBody(startCursor, filterBody);
            Request request = new Request.Builder()
                    .url(NOTION_API_BASE + "/databases/" + databaseId + "/query")
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Notion-Version", "2022-06-28")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() != 200) {
                    System.err.println("API响应内容: " + response.body());
                    throw new RuntimeException("Notion API请求失败: " + response.code() + " - " + response.body());
                }
                String responseBody = response.body().string();
                JSONObject root = JSON.parseObject(responseBody);
                JSONArray results = root.getJSONArray("results");

                System.out.println("分页 " + pageNum + " 获取到 " + results.size() + " 条记录");

                for (int i = 0; i < results.size(); i++) {
                    JSONObject page = results.getJSONObject(i);
                    BlogPostDO post = parsePage(page);
                    if (post != null) {
                        allPosts.add(post);
                    }
                }

                startCursor = root.getString("next_cursor");
                hasMore = root.getBooleanValue("has_more");
            }
        }

        System.out.println("成功获取文章，共 " + allPosts.size() + " 篇（分页 " + pageNum + " 次）");
        return allPosts;
    }

    /**
     * 构建查询请求体（支持过滤器和分页游标）
     */
    private String buildQueryBody(String startCursor, String filterBody) {
        StringBuilder body = new StringBuilder("{");
        if (filterBody != null) {
            // 移除外层花括号后嵌入
            String inner = filterBody.trim();
            if (inner.startsWith("{") && inner.endsWith("}")) {
                inner = inner.substring(1, inner.length() - 1).trim();
            }
            body.append(inner).append(",");
        }
        body.append("\"page_size\": 100");
        if (startCursor != null && !startCursor.isEmpty()) {
            body.append(",\"start_cursor\": \"").append(startCursor).append("\"");
        }
        body.append("}");
        return body.toString();
    }

    /**
     * 解析Notion页面数据
     */
    private BlogPostDO parsePage(JSONObject page) {
        try {
            BlogPostDO post = new BlogPostDO();

            // 设置基本属性
            post.setId(page.getString("id"));
            post.setCreatedTime(TimeUtil.parseUTCDateTime2Beijing(page.getString("created_time")));
            post.setLastEditedTime(TimeUtil.parseUTCDateTime2Beijing(page.getString("last_edited_time")));

            // 解析属性
            JSONObject properties = page.getJSONObject("properties");

            System.out.println("=== 解析页面: " + page.getString("id") + " ===");
            System.out.println("可用属性: " + properties.keySet());

            // 提取标题 - 直接使用"名称"属性
            String title = extractTitle(properties);
            if (title == null || title.trim().isEmpty()) {
                System.out.println("跳过无标题页面");
                return null;
            }
            post.setTitle(title);
            System.out.println("文章标题: " + title);

            // 提取标签 - 使用"多选"属性
            List<String> tags = extractTags(properties);
            post.setTags(tags);
            System.out.println("文章标签: " + tags);
            //提取合集归属（可能为null）- 使用 "合集属性"
            String collection = extractCollectionName(properties);
            post.setCollection(collection);

            // 获取页面内容
            String content = fetchPageContent(post.getId());
            post.setContent(content);
            System.out.println("内容长度: " + (content != null ? content.length() : 0));
            System.out.println("=== 页面解析完成 ===\n");

            return post;

        } catch (Exception e) {
            System.err.println("解析页面失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 提取标题 - 直接使用"名称"属性
     */
    private String extractTitle(JSONObject properties) {
        // 优先使用"名称"属性
        String[] titlePropertyNames = {"名称", "Name", "Title", "标题", "name", "title"};

        for (String propName : titlePropertyNames) {
            if (properties.containsKey(propName)) {
                JSONObject titleProp = properties.getJSONObject(propName);

                // 处理title类型
                if ("title".equals(titleProp.getString("type"))) {
                    JSONArray titleArray = titleProp.getJSONArray("title");
                    if (titleArray != null && titleArray.size() > 0) {
                        JSONObject firstTitle = titleArray.getJSONObject(0);
                        String text = firstTitle.getString("plain_text");
                        if (text != null && !text.trim().isEmpty()) {
                            return text.trim();
                        }
                    }
                }
            }
        }

        System.out.println("未找到有效的标题属性");
        return null;
    }

    /**
     * 提取标签 - 直接使用"多选"属性
     */
    private List<String> extractTags(JSONObject properties) {
        List<String> tags = new ArrayList<>();

        // 优先使用"多选"属性
        String[] tagPropertyNames = {"多选", "Tags", "标签", "Tag", "tag", "tags"};

        for (String propName : tagPropertyNames) {
            if (properties.containsKey(propName)) {
                JSONObject tagsProp = properties.getJSONObject(propName);

                // 处理multi_select类型
                if ("multi_select".equals(tagsProp.getString("type"))) {
                    JSONArray multiSelect = tagsProp.getJSONArray("multi_select");
                    if (multiSelect != null) {
                        for (int i = 0; i < multiSelect.size(); i++) {
                            JSONObject tag = multiSelect.getJSONObject(i);
                            String tagName = tag.getString("name");
                            if (tagName != null && !tagName.trim().isEmpty()) {
                                tags.add(tagName.trim());
                            }
                        }
                    }
                }
            }
        }

        return tags;
    }
    /**
     * 提取合集归属（可能为null）- 使用 "合集属性"
     */
    private String extractCollectionName(JSONObject properties){
        String result = null;
        JSONObject collectionJsonObject = properties.getJSONObject("合集").getJSONObject("select");
        if(collectionJsonObject!=null){
            Object selectName = collectionJsonObject.getOrDefault("name","?");
            result = String.valueOf(selectName);
        }
        return result;
    }
    /**
     * 递归获取页面所有块内容（包括所有嵌套子块）
     */
    private String fetchPageContent(String pageId) throws Exception {
        System.out.println("开始递归获取页面内容: " + pageId);

        JSONArray allBlocks = new JSONArray();
        fetchBlocksRecursive(pageId, allBlocks, 0);

        System.out.println("共获取 " + allBlocks.size() + " 个块（包含嵌套块）");

        MarkdownConverter converter = new MarkdownConverter();
        return converter.convertBlocksToMarkdown(allBlocks);
    }

    /**
     * 递归获取块及其所有子块
     */
    private void fetchBlocksRecursive(String blockId, JSONArray resultBlocks, int depth) throws Exception {
        if (depth > 10) { // 防止无限递归
            System.out.println("警告：达到最大递归深度: " + blockId);
            return;
        }

        JSONArray children = fetchBlockChildren(blockId);

        for (int i = 0; i < children.size(); i++) {
            JSONObject block = children.getJSONObject(i);

            // 标记块的层级信息（用于调试和格式化）
            block.put("_depth", depth);
            block.put("_parent_id", blockId);

            resultBlocks.add(block);

            // 检查是否需要获取子块
            if (shouldFetchChildren(block)) {
                String childBlockId = block.getString("id");
                String blockType = block.getString("type");
                System.out.println("深度 " + depth + " - 获取 " + blockType + " 块的子块: " + childBlockId);
                fetchBlocksRecursive(childBlockId, resultBlocks, depth + 1);
            }
        }
    }

    /**
     * 判断是否需要获取子块
     */
    private boolean shouldFetchChildren(JSONObject block) {
        boolean hasChildren = block.getBoolean("has_children");
        String type = block.getString("type");

        // 这些类型的块通常有子内容
        return hasChildren && (
                "toggle".equals(type) ||
                        "column_list".equals(type) ||
                        "column".equals(type) ||
                        "table".equals(type) ||
                        "bulleted_list_item".equals(type) ||
                        "numbered_list_item".equals(type) ||
                        "to_do".equals(type) ||
                        "quote".equals(type) ||
                        "callout".equals(type) ||
                        "child_page".equals(type)  // 注意：child_page 需要特殊处理
        );
    }

    /**
     * 获取指定块的子块
     */
    private JSONArray fetchBlockChildren(String blockId) throws Exception {
        String url = NOTION_API_BASE + "/blocks/" + blockId + "/children?page_size=100";
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiToken)
                .header("Notion-Version", "2022-06-28")
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                System.err.println("获取块内容失败: " + response.code() + " - " + response.body());
                return new JSONArray();
            }
            JSONObject root = JSON.parseObject(response.body().string());
            return root.getJSONArray("results");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}