package nlob;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NotionClient {
    private final String apiToken;
    private final String databaseId;
    private final HttpClient httpClient;

    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final DateTimeFormatter NOTION_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public NotionClient(String apiToken, String databaseId) {
        this.apiToken = apiToken;
        this.databaseId = databaseId;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * 查询Notion数据库获取所有博客文章
     */
    public List<BlogPost> fetchBlogPosts() throws Exception {
        System.out.println("开始从Notion获取博客文章...");
        System.out.println("Database ID: " + databaseId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NOTION_API_BASE + "/databases/" + databaseId + "/query"))
                .header("Authorization", "Bearer " + apiToken)
                .header("Notion-Version", "2022-06-28")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("API响应状态: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.err.println("API响应内容: " + response.body());
            throw new RuntimeException("Notion API请求失败: " + response.statusCode() + " - " + response.body());
        }

        JSONObject root = JSON.parseObject(response.body());
        JSONArray results = root.getJSONArray("results");

        List<BlogPost> posts = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject page = results.getJSONObject(i);
            BlogPost post = parsePage(page);
            if (post != null) {
                posts.add(post);
            }
        }

        System.out.println("成功获取 " + posts.size() + " 篇文章");
        return posts;
    }

    /**
     * 解析Notion页面数据
     */
    private BlogPost parsePage(JSONObject page) {
        try {
            BlogPost post = new BlogPost();

            // 设置基本属性
            post.setId(page.getString("id"));
            post.setCreatedTime(parseDateTime(page.getString("created_time")));
            post.setLastEditedTime(parseDateTime(page.getString("last_edited_time")));

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

            // 你的数据库没有状态属性，默认所有文章都发布
            post.setPublished(true);
            System.out.println("发布状态: 已发布");

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
     * 获取页面内容并转换为Markdown
     */
    private String fetchPageContent(String pageId) throws Exception {
        System.out.println("获取页面内容: " + pageId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NOTION_API_BASE + "/blocks/" + pageId + "/children?page_size=100"))
                .header("Authorization", "Bearer " + apiToken)
                .header("Notion-Version", "2022-06-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("获取页面内容失败: " + response.statusCode() + " - " + response.body());
            return "";
        }

        JSONObject root = JSON.parseObject(response.body());
        JSONArray blocks = root.getJSONArray("results");

        MarkdownConverter converter = new MarkdownConverter();
        String content = converter.convertBlocksToMarkdown(blocks);

        // 如果页面没有内容，添加默认内容
        if (content == null || content.trim().isEmpty()) {
            content = "这篇文章还没有内容，请在Notion中添加内容。";
        }

        return content;
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, NOTION_DATE_FORMATTER);
        } catch (Exception e) {
            System.err.println("日期解析失败: " + dateTimeStr);
            return LocalDateTime.now();
        }
    }
}