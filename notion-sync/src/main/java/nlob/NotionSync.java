package nlob;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import nlob.entity.BlogPostDO;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class NotionSync {
    private final String apiToken;
    private final String databaseId;
    private final String outputDir;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public NotionSync(String apiToken, String databaseId, String outputDir) {
        this.apiToken = apiToken;
        this.databaseId = databaseId;
        this.outputDir = outputDir;
    }

    /**
     * 同步入口
     * @param fullSync true = 全量同步所有文章, false = 仅同步最近 3 天
     */
    public void sync(boolean fullSync) {
        try {
            // 创建输出目录
            Path blogsDir = Paths.get(outputDir, "blogs");
            Files.createDirectories(blogsDir);

            // 获取Notion数据
            NotionClient client = new NotionClient(apiToken, databaseId);
            List<BlogPostDO> posts;
            if (fullSync) {
                posts = client.fetchAllPosts();
            } else {
                posts = client.fetchRecentPosts(3);
            }

            // 生成Markdown文件
            for (BlogPostDO post : posts) {
                if (post.getTitle() != null) {
                    String markdown = generateMarkdownFile(post);
                    Path filePath = blogsDir.resolve(post.getFilename());
                    Files.writeString(filePath, markdown);
                    System.out.println("生成文件: " + post.getFilename());
                }
            }

            // 更新索引
            updateIndex(posts);

            System.out.println("同步完成！" + (fullSync ? "全量" : "增量") + "同步 " + posts.size() + " 篇文章");

        } catch (Exception e) {
            System.err.println("同步失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 更新索引文件：用 Notion 全量数据重建索引，消除覆盖和重复问题。
     * <p>
     * 修复说明：
     * - 旧代码用 id→index 的 Map 逐条删除，但 remove 后后续元素下标偏移，导致错删和重复。
     * - 改用 removeIf + ID 集合批量删除，消除下标偏移 bug。
     */
    private void updateIndex(List<BlogPostDO> posts) {
        try {
            List<Map<String, Object>> index = loadExistingIndex();
            Set<String> fetchedIds = posts.stream()
                    .map(BlogPostDO::getId)
                    .collect(Collectors.toSet());

            // 批量移除已获取文章（防止下标偏移 bug）
            int before = index.size();
            index.removeIf(entry -> fetchedIds.contains(entry.get("id").toString()));
            int removed = before - index.size();
            System.out.println("索引移除 " + removed + " 条旧记录");

            // 添加新条目
            int added = 0;
            for (BlogPostDO post : posts) {
                if (post.getTitle() == null) continue;
                Map<String, Object> entry = buildIndexEntry(post);
                index.add(entry);
                added++;
            }
            System.out.println("索引新增 " + added + " 条记录");

            // 按日期排序（最新的在前）
            index.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date")));

            String indexJson = JSON.toJSONString(index, JSONWriter.Feature.PrettyFormat);
            Files.writeString(Paths.get(outputDir, "blogs", "index.json"), indexJson);
            System.out.println("索引更新完成，共 " + index.size() + " 条");

        } catch (Exception e) {
            throw new RuntimeException("更新索引失败", e);
        }
    }

    /**
     * 构建单条索引条目
     */
    private Map<String, Object> buildIndexEntry(BlogPostDO post) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", post.getId());
        entry.put("filename", post.getFilename());
        entry.put("title", post.getTitle());
        entry.put("date", post.getCreatedTime().format(DATE_FORMATTER));
        entry.put("update", post.getLastEditedTime().format(DATE_FORMATTER));

        String collection = post.getCollection();
        if (collection != null) {
            entry.put("collection", collection);
        }

        // 处理摘要
        String excerpt = post.getExcerpt();
        if (excerpt == null || excerpt.trim().isEmpty()) {
            if (post.getContent() != null && post.getContent().length() > 50) {
                excerpt = post.getContent().substring(0, 50) + "...";
            } else if (post.getContent() != null) {
                excerpt = post.getContent();
            } else {
                excerpt = "暂无摘要";
            }
        }
        entry.put("excerpt", excerpt);

        if (!post.getTags().isEmpty()) {
            entry.put("tags", String.join(",", post.getTags()));
        }

        return entry;
    }

    /**
     * 从实体类创建Markdown文件
     */
    private String generateMarkdownFile(BlogPostDO post) {
        StringBuilder markdown = new StringBuilder();

        // 头部元数据
        markdown.append("---\n");
        markdown.append("title: \"").append(escapeYaml(post.getTitle())).append("\"\n");
        markdown.append("date: \"").append(post.getCreatedTime().format(DATE_FORMATTER)).append("\"\n");
        if (!post.getTags().isEmpty()) {
            markdown.append("tags: \"").append(String.join(",", post.getTags())).append("\"\n");
        }
        markdown.append("---\n\n");

        // 内容
        if (post.getContent() != null) {
            markdown.append(post.getContent());
        }

        return markdown.toString();
    }

    /**
     * 加载现有索引文件
     */
    private List<Map<String, Object>> loadExistingIndex() {
        try {
            Path indexFile = Paths.get(outputDir, "blogs", "index.json");
            if (Files.exists(indexFile)) {
                String content = Files.readString(indexFile);
                if (content != null && !content.trim().isEmpty()) {
                    List<Map<String, Object>> index = JSON.parseArray(content, (Type) Map.class);
                    System.out.println("成功加载现有索引文件");
                    return index != null ? index : new ArrayList<>();
                }
            }
        } catch (Exception e) {
            System.out.println("无法加载现有索引文件，将创建新索引: " + e.getMessage());
        }
        System.out.println("创建新的索引文件");
        return new ArrayList<>();
    }

    private String escapeYaml(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"");
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("用法: java com.blog.NotionSync <NOTION_TOKEN> <DATABASE_ID> <OUTPUT_DIR> [--full]");
            System.out.println("  --full    全量同步（默认仅同步最近3天）");
            System.out.println("环境变量: NOTION_TOKEN, NOTION_DATABASE_ID");
            System.exit(1);
        }

        String apiToken = args[0];
        String databaseId = args[1];
        String outputDir = args[2];

        // 优先使用环境变量
        if (apiToken.isEmpty()) {
            apiToken = System.getenv("NOTION_TOKEN");
        }
        if (databaseId.isEmpty()) {
            databaseId = System.getenv("NOTION_DATABASE_ID");
        }

        if (apiToken == null || apiToken.isEmpty()) {
            System.err.println("错误: 必须提供Notion API Token");
            System.exit(1);
        }
        if (databaseId == null || databaseId.isEmpty()) {
            System.err.println("错误: 必须提供Notion Database ID");
            System.exit(1);
        }

        NotionSync sync = new NotionSync(apiToken, databaseId, outputDir);
        sync.sync(false);
    }
}