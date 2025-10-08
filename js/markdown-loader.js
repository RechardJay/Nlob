class MarkdownLoader {
  constructor() {
    this.rawBaseUrl = 'https://raw.githubusercontent.com/rechardjay/Nlob/master/blogs/';
    this.initMarked();
  }

  initMarked() {
    if (typeof marked === "undefined") {
      console.error("marked 库未加载！");
      return;
    }

    // 配置 marked 选项
    marked.setOptions({
      highlight: function (code, lang) {
        // 检查 highlight.js 是否可用
        if (typeof hljs !== "undefined") {
          const language = hljs.getLanguage(lang) ? lang : "plaintext";
          try {
            return hljs.highlight(code, { language }).value;
          } catch (err) {
            console.warn("代码高亮错误:", err);
          }
        }
        return code;
      },
      langPrefix: "hljs language-",
      pedantic: false,
      gfm: true,
      breaks: true,
      sanitize: false,
      smartLists: true,
      smartypants: false,
    });
  }

  // 解析Markdown内容 - 修复元数据显示问题
  parseMarkdown(markdown) {
    if (typeof marked === "undefined") {
      return this.getErrorContent("Markdown 解析器未正确加载。");
    }

    const { metadata, content } = this.extractFrontMatter(markdown);

    // 清理内容，移除可能的多余空白
    const cleanedContent = content.trim();
    const htmlContent = marked.parse(cleanedContent);

    return { metadata, htmlContent };
  }

  // 提取Front Matter - 改进版本
  extractFrontMatter(markdown) {
    const frontMatterRegex = /^---\s*\n([\s\S]*?)\n---\s*\n/;
    const match = markdown.match(frontMatterRegex);

    let content = markdown;
    let metadata = {};

    if (match) {
      content = markdown.slice(match[0].length);
      const frontMatter = match[1];

      // 解析Front Matter
      frontMatter.split("\n").forEach((line) => {
        const trimmedLine = line.trim();
        if (trimmedLine && trimmedLine.includes(":")) {
          const colonIndex = trimmedLine.indexOf(":");
          const key = trimmedLine.slice(0, colonIndex).trim();
          let value = trimmedLine.slice(colonIndex + 1).trim();

          // 移除引号
          value = value.replace(/^['"](.*)['"]$/, "$1");
          metadata[key] = value;
        }
      });
    }

    return { metadata, content };
  }

  // 渲染博客内容 - 修复元数据显示
  async renderBlogContent(containerId) {
    const container = document.getElementById(containerId);
    if (!container) {
      console.error(`容器 #${containerId} 未找到`);
      return;
    }

    container.innerHTML = '<div class="loading">加载文章内容...</div>';

    try {
      const postId = this.getBlogFromUrl();

      if (!postId) {
        container.innerHTML = this.getNotFoundError();
        return;
      }

      const blogs = await this.getBlogList();
      const blog = blogs.find((b) => b.id === postId);

      if (!blog) {
        container.innerHTML = this.getNotFoundError(
          `请求的文章 "${postId}" 不存在`
        );
        return;
      }
      // 使用编码后的文件名加载内容
      const { metadata, htmlContent } = await this.loadBlogContent(
        blog.filename
      );

      // 渲染正确的博客内容结构
      const html = this.renderBlogPost(metadata, htmlContent, blog);
      container.innerHTML = html;
    } catch (error) {
      console.error("渲染博客内容失败:", error);
      container.innerHTML =
        this.getErrorContent("文章加载失败，请刷新页面重试。");
    }
  }

  // 渲染博客文章结构
  renderBlogPost(metadata, htmlContent, blogInfo) {
    // 使用博客信息中的标题，而不是Front Matter的（避免重复）
    const title = blogInfo.title || metadata.title || "无标题";

    // 更新页面标题
    document.title = `${title} - 我的博客`;

    return `
            <article class="blog-post">
                <header class="blog-header">
                    <h1 class="blog-title">${this.escapeHtml(title)}</h1>
                    <div class="blog-meta">
                        <span class="publish-date">发布于 ${
                          blogInfo.date || metadata.date || "未知日期"
                        }</span>
                        ${
                          blogInfo.tags
                            ? `<span class="blog-tags"> | 标签: ${blogInfo.tags}</span>`
                            : ""
                        }
                    </div>
                </header>
                
                <div class="blog-content">
                    ${htmlContent}
                </div>
            </article>
        `;
  }

  // 错误内容模板
  getErrorContent(message) {
    return `
            <div class="error">
                <h2>加载失败</h2>
                <p>${message}</p>
            </div>
        `;
  }

  getNotFoundError(message = "未找到指定的文章") {
    return `
            <div class="error">
                <h2>文章不存在</h2>
                <p>${message}</p>
            </div>
        `;
  }
  // 获取博客列表
  async getBlogList() {
    try {
      // 首先尝试从索引文件加载博客列表
      const blogFiles = await this.loadBlogIndex();
      return blogFiles.sort((a, b) => new Date(b.date) - new Date(a.date));
    } catch (error) {
      console.error("获取博客列表失败:", error);
      return this.getFallbackBlogList();
    }
  }

  // 加载博客索引
  async loadBlogIndex() {
    try {
      const response = await fetch("blogs/index.json");
      if (response.ok) {
        return await response.json();
      }
    } catch (error) {
      console.warn("无法加载博客索引文件，使用默认列表");
    }
    return this.getFallbackBlogList();
  }

  // 备用博客列表
  getFallbackBlogList() {
    return [
      {
        id: "welcome",
        filename: "welcome.md",
        title: "欢迎来到我的博客",
        date: "2024-01-15",
        excerpt: "这是我的第一篇博客文章，欢迎阅读！",
      },
      {
        id: "getting-started",
        filename: "getting-started.md",
        title: "开始使用GitHub Pages",
        date: "2024-01-10",
        excerpt: "学习如何使用GitHub Pages搭建静态博客网站",
      },
    ];
  }

  // 加载单个博客内容
  async loadBlogContent(filename) {
    try {
      // 对文件名进行编码，处理中文和特殊字符
      const encodedFilename = encodeURIComponent(filename);
      const response = await fetch(`${this.rawBaseUrl}${encodedFilename}`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const markdown = await response.text();
      return this.parseMarkdown(markdown);
    } catch (error) {
      console.error("加载博客内容失败:", error);
      return this.getErrorContent("加载文章失败，请稍后重试。");
    }
  }

  // 渲染博客列表
  async renderBlogList(containerId) {
    const container = document.getElementById(containerId);
    if (!container) {
      console.error(`容器 #${containerId} 未找到`);
      return;
    }

    container.innerHTML = '<div class="loading">加载中...</div>';

    try {
      const blogs = await this.getBlogList();

      if (blogs.length === 0) {
        container.innerHTML = '<div class="loading">暂无博客文章</div>';
        return;
      }

      const html = blogs
        .map(
          (blog) => `
                <article class="blog-card">
                    <h4>
                        <a href="blog.html?post=${blog.id}">
                            ${this.escapeHtml(blog.title)}
                        </a>
                    </h4>
                    <div class="blog-meta">发布于 ${blog.date}</div>
                    <div class="blog-excerpt">${this.escapeHtml(
                      blog.excerpt
                    )}</div>
                </article>
            `
        )
        .join("");

      container.innerHTML = html;
    } catch (error) {
      console.error("渲染博客列表失败:", error);
      container.innerHTML = '<div class="error">加载博客列表失败</div>';
    }
  }

  // HTML转义
  escapeHtml(unsafe) {
    if (!unsafe) return "";
    return unsafe
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // 从URL参数获取博客ID
  getBlogFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get("post");
  }
}
