package nlob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class BlogPost {
    private String id;
    private String title;
    private String content;
    private LocalDateTime createdTime;
    private LocalDateTime lastEditedTime;
    private String excerpt;
    private List<String> tags;
    private boolean published;

    public BlogPost() {
        this.tags = new ArrayList<>();
        this.published = true; // 默认发布
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getLastEditedTime() { return lastEditedTime; }
    public void setLastEditedTime(LocalDateTime lastEditedTime) { this.lastEditedTime = lastEditedTime; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public String getFilename() {
        if (title == null || title.trim().isEmpty()) {
            return "untitled-" + id + ".md";
        }
        // 生成安全的文件名
        return sanitizeFilename(title) + ".md";
    }

    public String getPostId() {
        if (title == null || title.trim().isEmpty()) {
            return id;
        }
        return sanitizeFilename(title);
    }

    private String sanitizeFilename(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @Override
    public String toString() {
        return "BlogPost{id='" + id + "', title='" + title + "', published=" + published + "}";
    }
}