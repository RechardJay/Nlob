package nlob.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
public class BlogPostDO {
    private String id;
    private String title;
    private String content;
    private LocalDateTime createdTime;
    private LocalDateTime lastEditedTime;
    private String excerpt;
    private List<String> tags;

    public BlogPostDO() {
        this.tags = new ArrayList<>();
    }
    public String getFilename() {
        if (title == null || title.trim().isEmpty()) {
            return "untitled-" + id + ".md";
        }
        // 生成安全的文件名
        return sanitizeFilename(title) + ".md";
    }

    private String sanitizeFilename(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @Override
    public String toString() {
        return "BlogPostDO{id='" + id + "', title='" + title + "}";
    }
}