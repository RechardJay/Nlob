package nlob;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    public void sync() {
        try {
            // 创建输出目录
            Path blogsDir = Paths.get(outputDir, "blogs");
            if(!Files.exists(blogsDir)) {
                Files.createDirectories(blogsDir);
            }

            // 获取Notion数据
            NotionClient client = new NotionClient(apiToken, databaseId);
            List<BlogPost> posts = client.fetchBlogPosts();

            // 生成索引和Markdown文件
            List<Map<String, Object>> index =loadExistingIndex();

            for (BlogPost post : posts) {
                if (post.isPublished() && post.getTitle() != null) {
                    // 生成Markdown文件
                    String markdown = generateMarkdownFile(post);
                    Path filePath = blogsDir.resolve(post.getFilename());
                    Files.writeString(filePath, markdown);
                    System.out.println("生成文件: " + post.getFilename());

                    // 添加到索引
                    Map<String, Object> indexEntry = new LinkedHashMap<>();
                    indexEntry.put("id", post.getPostId());
                    indexEntry.put("filename", post.getFilename());
                    indexEntry.put("title", post.getTitle());
                    indexEntry.put("date", post.getCreatedTime().format(DATE_FORMATTER));

                    // 处理摘要
                    String excerpt = post.getExcerpt();
                    if (excerpt == null || excerpt.trim().isEmpty()) {
                        if (post.getContent() != null && post.getContent().length() > 100) {
                            excerpt = post.getContent().substring(0, 100) + "...";
                        } else if (post.getContent() != null) {
                            excerpt = post.getContent();
                        } else {
                            excerpt = "暂无摘要";
                        }
                    }
                    indexEntry.put("excerpt", excerpt);

                    if (!post.getTags().isEmpty()) {
                        indexEntry.put("tags", String.join(",", post.getTags()));
                    }

                    index.add(indexEntry);
                }
            }

            // 按日期排序（最新的在前）
            index.sort((a, b) -> ((String)b.get("date")).compareTo((String)a.get("date")));

            String indexJson = JSON.toJSONString(index, JSONWriter.Feature.PrettyFormat);
            Files.writeString(Paths.get(outputDir, "blogs", "index.json"), indexJson);

            System.out.println("同步完成！生成 " + index.size() + " 篇文章");

        } catch (Exception e) {
            System.err.println("同步失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 从实体类创建Markdown文件
     */
    private String generateMarkdownFile(BlogPost post) {
        StringBuilder markdown = new StringBuilder();

        // Front Matter
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
            System.out.println("用法: java com.blog.NotionSync <NOTION_TOKEN> <DATABASE_ID> <OUTPUT_DIR>");
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
        sync.sync();
    }
}