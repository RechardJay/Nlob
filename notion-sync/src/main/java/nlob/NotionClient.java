package nlob;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import nlob.entity.BlogPostDO;
import nlob.utils.TimeUtil;
import okhttp3.*;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
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
     * 查询Notion数据库获取所有博客文章
     */
    public List<BlogPostDO> fetchBlogPosts() throws Exception {
        System.out.println("开始从Notion获取博客文章...");
        System.out.println("Database ID: " + databaseId);

        //构建查询条件
        String timeString = TimeUtil.getUTCBeforeDays(10);
        String filterCondition = String.format("""
                {
                    "filter": {
                        "timestamp": "last_edited_time",
                        "last_edited_time": {
                            "on_or_after": "%s"
                        }
                    }
                }
                """, timeString);
        Request request = new Request.Builder()
                .url(NOTION_API_BASE + "/databases/" + databaseId + "/query")
                .header("Authorization", "Bearer " + apiToken)
                .header("Notion-Version", "2022-06-28")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(filterCondition, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute();) {
            System.out.println("API响应状态: " + response.code());
            if (response.code() != 200) {
                System.err.println("API响应内容: " + response.body());
                throw new RuntimeException("Notion API请求失败: " + response.code() + " - " + response.body());
            }
            if (!response.isSuccessful()) {
                System.out.println("请求失败，响应头: " + response.headers());
            }
            String responseBody = response.body().string();
            JSONObject root = JSON.parseObject(responseBody);
            JSONArray results = root.getJSONArray("results");

            List<BlogPostDO> posts = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                JSONObject page = results.getJSONObject(i);
                BlogPostDO post = parsePage(page);
                if (post != null) {
                    posts.add(post);
                }
            }

            System.out.printf("成功获取,从 %s 到现在，共有%d篇文章", timeString, posts.size());
            return posts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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