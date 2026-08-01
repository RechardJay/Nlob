/* ============================================================
   MarkdownLoader — 博客列表与内容加载
   ============================================================ */
class MarkdownLoader {
  constructor() {
    // this.rawBaseUrl = 'https://raw.githubusercontent.com/rechardjay/Nlob/master/blogs/';
    // 本地调试备用：
    this.rawBaseUrl = 'blogs/';
    this.initMarked();
  }

  initMarked() {
    if (typeof marked === 'undefined') {
      console.error('marked 库未加载！');
      return;
    }

    marked.setOptions({
      highlight: (code, lang) => {
        if (typeof hljs !== 'undefined') {
          const language = hljs.getLanguage(lang) ? lang : 'plaintext';
          try {
            return hljs.highlight(code, { language }).value;
          } catch (err) {
            console.warn('代码高亮错误:', err);
          }
        }
        return code;
      },
      langPrefix: 'hljs language-',
      pedantic: false,
      gfm: true,
      breaks: false,
      sanitize: false,
      smartLists: true,
      smartypants: false,
    });
  }

  // ============== Markdown 解析 ==============
  parseMarkdown(markdown) {
    if (typeof marked === 'undefined') {
      return { metadata: {}, htmlContent: this.getErrorContent('Markdown 解析器未正确加载。') };
    }

    const { metadata, content } = this.extractFrontMatter(markdown);
    const htmlContent = marked.parse(content.trim());
    return { metadata, htmlContent };
  }

  extractFrontMatter(markdown) {
    const frontMatterRegex = /^---\s*\n([\s\S]*?)\n---\s*\n/;
    const match = markdown.match(frontMatterRegex);
    let content = markdown;
    let metadata = {};

    if (match) {
      content = markdown.slice(match[0].length);
      match[1].split('\n').forEach((line) => {
        const trimmed = line.trim();
        if (!trimmed || !trimmed.includes(':')) return;
        const idx = trimmed.indexOf(':');
        const key = trimmed.slice(0, idx).trim();
        let value = trimmed.slice(idx + 1).trim();
        value = value.replace(/^['"](.*)['"]$/, '$1');
        metadata[key] = value;
      });
    }
    return { metadata, content };
  }

  // ============== 博客详情页 ==============
  async renderBlogContent(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading">Fetching the manuscript</div>';

    try {
      const postId = this.getBlogFromUrl();
      if (!postId) {
        container.innerHTML = this.getNotFoundError();
        return;
      }

      const blogs = await this.getBlogList();
      const blog = blogs.find((b) => b.id === postId);
      if (!blog) {
        container.innerHTML = this.getNotFoundError('文章不存在。');
        return;
      }

      const { metadata, htmlContent } = await this.loadBlogContent(blog.filename);
      container.innerHTML = this.renderBlogPost(metadata, htmlContent, blog);

      // 为 pre 加上语言 data 属性（用于显示 code 角标）
      container.querySelectorAll('pre code').forEach((codeEl) => {
        const m = /language-([\w+-]+)/.exec(codeEl.className || '');
        const pre = codeEl.closest('pre');
        if (pre && m) pre.setAttribute('data-lang', m[1]);
      });

      // 内容区域里的外链
      container.querySelectorAll('a').forEach((a) => {
        const href = a.getAttribute('href') || '';
        if (/^https?:/.test(href)) {
          a.target = '_blank';
          a.rel = 'noopener noreferrer';
        }
      });

      // 生成标题大纲（TOC）
      this.buildToc();
    } catch (err) {
      console.error('渲染博客内容失败:', err);
      container.innerHTML = this.getErrorContent('文章加载失败，请稍后重试。');
    }
  }

  renderBlogPost(metadata, htmlContent, blogInfo) {
    const title = blogInfo.title || metadata.title || '无标题';
    document.title = `${title} · Jay's Blog`;

    const date = blogInfo.update || blogInfo.date || metadata.date || metadata.update || '—';
    const originalDate = blogInfo.date || metadata.date || '';
    const updated = (blogInfo.update && blogInfo.update !== originalDate)
      ? `<span> · 更新于 ${blogInfo.update}</span>`
      : '';

    const tagsArr = this.parseTags(blogInfo.tags || metadata.tags || '');
    const tagsHtml = tagsArr.length
      ? '<span class="meta-divider">·</span>' +
        tagsArr.map(t => `<span class="blog-tag">${this.escapeHtml(t)}</span>`).join(' ')
      : '';

    const collection = blogInfo.collection || metadata.collection || '';
    const collectionHtml = collection
      ? `<span class="meta-divider">·</span><span>系列：${this.escapeHtml(collection)}</span>`
      : '';

    const eyebrow = collection ? collection : 'Essay · Article';

    return `
      <article class="blog-post reveal is-visible">
        <header class="blog-header">
          <span class="post-eyebrow">${this.escapeHtml(eyebrow)}</span>
          <h1 class="blog-title">${this.escapeHtml(title)}</h1>
          <div class="blog-meta-full">
            <span>发布于 ${this.escapeHtml(date)}</span>
            ${updated}
            ${collectionHtml}
            ${tagsHtml}
          </div>
        </header>
        <div class="blog-content">
          ${htmlContent}
        </div>
      </article>
    `;
  }

  // ============== 标题大纲（TOC） ==============
  buildToc() {
    const tocList = document.getElementById('toc-list');
    const content = document.querySelector('.blog-content');
    if (!tocList || !content) return;

    const headings = content.querySelectorAll('h1, h2, h3');
    if (!headings.length) {
      tocList.parentElement.style.display = 'none';
      return;
    }

    let html = '';
    headings.forEach((h) => {
      const tag = h.tagName.toLowerCase(); // h1, h2, h3
      const level = parseInt(tag.charAt(1)); // 1, 2, 3
      // 确保标题有 id 用于锚点跳转
      if (!h.id) {
        h.id = h.textContent
          .toLowerCase()
          .replace(/[^\w\u4e00-\u9fff]+/g, '-')
          .replace(/^-+|-+$/g, '');
      }
      const cls = level > 1 ? ` toc-h${level}` : '';
      html += `<li><a href="#${h.id}" class="${cls}">${this.escapeHtml(h.textContent)}</a></li>`;
    });
    tocList.innerHTML = html;

    // 点击平滑滚动
    tocList.querySelectorAll('a').forEach((a) => {
      a.addEventListener('click', (e) => {
        e.preventDefault();
        const target = document.getElementById(a.getAttribute('href').slice(1));
        if (target) {
          const offset = 100;
          const top = target.getBoundingClientRect().top + window.scrollY - offset;
          window.scrollTo({ top, behavior: 'smooth' });
        }
      });
    });

    // 滚动高亮当前标题
    const links = tocList.querySelectorAll('a');
    if (!links.length || !('IntersectionObserver' in window)) return;

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const id = entry.target.id;
            links.forEach((l) => {
              l.classList.toggle('active', l.getAttribute('href') === '#' + id);
            });
          }
        });
      },
      { rootMargin: '-80px 0px -60% 0px', threshold: 0 }
    );

    headings.forEach((h) => io.observe(h));
  }

  // ============== 博客列表页 ==============
  async renderBlogList(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading">Assembling the journal</div>';

    try {
      const blogs = await this.getBlogList();
      if (blogs.length === 0) {
        container.innerHTML = '<div class="empty-state"><h3>No entries yet.</h3><p>Coming soon.</p></div>';
        return;
      }

      const html = blogs.map((blog, i) => {
        const num = String(i + 1).padStart(2, '0');

        const tagsArr = this.parseTags(blog.tags || '');
        const tagsHtml = tagsArr.slice(0, 3).map(t =>
          `<span class="blog-tag">${this.escapeHtml(t)}</span>`
        ).join('');

        const date = blog.update || blog.date || '—';
        const collection = blog.collection ? ` · ${this.escapeHtml(blog.collection)}` : '';

        return `
          <article class="blog-card reveal" data-id="${this.escapeHtml(blog.id)}" style="transition-delay:${(i * 0.05).toFixed(2)}s">
            <div class="blog-card-inner">
              <div class="blog-card-num">${num}</div>
              <div class="blog-card-body">
                <h4>
                  <a href="blog.html?post=${encodeURIComponent(blog.id)}">${this.escapeHtml(blog.title)}</a>
                </h4>
                <div class="blog-meta">
                  <span>${date}</span>
                  ${blog.collection ? '<span class="dot"></span><span>' + this.escapeHtml(blog.collection) + '</span>' : ''}
                </div>
                <div class="blog-excerpt">${this.escapeHtml(this.cleanExcerpt(blog.excerpt || ''))}</div>
                ${tagsHtml ? `<div style="margin-top:12px;display:flex;gap:6px;flex-wrap:wrap">${tagsHtml}</div>` : ''}
              </div>
              <a class="blog-card-arrow" href="blog.html?post=${encodeURIComponent(blog.id)}" aria-label="Read article">→</a>
            </div>
          </article>
        `;
      }).join('');

      container.innerHTML = html;

      // 重新观察新生成的 reveal 元素
      if ('IntersectionObserver' in window) {
        const io = new IntersectionObserver((entries) => {
          entries.forEach((e) => {
            if (e.isIntersecting) {
              e.target.classList.add('is-visible');
              io.unobserve(e.target);
            }
          });
        }, { threshold: 0.05, rootMargin: '0px 0px -20px 0px' });

        container.querySelectorAll('.reveal').forEach(el => io.observe(el));
      } else {
        container.querySelectorAll('.reveal').forEach(el => el.classList.add('is-visible'));
      }
    } catch (err) {
      console.error('渲染博客列表失败:', err);
      container.innerHTML = '<div class="error"><h2>加载失败</h2><p>请稍后重试。</p></div>';
    }
  }

  // ============== 博客元数据（列表） ==============
  async getBlogList() {
    try {
      const list = await this.loadBlogIndex();
      return this.dedupeAndSort(list);
    } catch (err) {
      console.error('获取博客列表失败:', err);
      return this.dedupeAndSort(this.getFallbackBlogList());
    }
  }

  dedupeAndSort(list) {
    const seen = new Set();
    const uniq = (list || []).filter((it) => {
      if (!it || !it.id) return false;
      if (seen.has(it.id)) return false;
      seen.add(it.id);
      return true;
    });
    return uniq.sort((a, b) => {
      const da = new Date(b.update || b.date || 0);
      const db = new Date(a.update || a.date || 0);
      return da - db;
    });
  }

  async loadBlogIndex() {
    const res = await fetch('blogs/index.json');
    if (!res.ok) throw new Error('HTTP ' + res.status);
    return res.json();
  }

  getFallbackBlogList() {
    return [
      {
        id: 'welcome',
        filename: 'welcome.md',
        title: '欢迎来到我的博客',
        date: '2024-01-15',
        excerpt: '这是我的第一篇博客文章，欢迎阅读！',
      },
      {
        id: 'getting-started',
        filename: 'getting-started.md',
        title: '开始使用 GitHub Pages',
        date: '2024-01-10',
        excerpt: '学习如何使用 GitHub Pages 搭建静态博客网站。',
      },
    ];
  }

  // ============== 单篇内容加载 ==============
  async loadBlogContent(filename) {
    try {
      const enc = encodeURIComponent(filename);
      const res = await fetch(`${this.rawBaseUrl}${enc}`);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const md = await res.text();
      return this.parseMarkdown(md);
    } catch (err) {
      console.error('加载博客内容失败:', err);
      return {
        metadata: {},
        htmlContent: this.getErrorContent('加载文章失败，请稍后重试。'),
      };
    }
  }

  // ============== Helpers ==============
  parseTags(tagsStr) {
    if (!tagsStr) return [];
    return String(tagsStr)
      .split(/[,，\s、;；]+/)
      .map(s => s.trim())
      .filter(Boolean);
  }

  cleanExcerpt(excerpt) {
    // 去掉 HTML 标签和多余换行
    const plain = String(excerpt || '').replace(/<[^>]*>/g, ' ');
    return plain.replace(/\s+/g, ' ').trim().slice(0, 160);
  }

  getBlogFromUrl() {
    return new URLSearchParams(window.location.search).get('post');
  }

  escapeHtml(unsafe) {
    if (!unsafe) return '';
    return String(unsafe)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  getErrorContent(message) {
    return `<div class="error"><h2>加载失败</h2><p>${this.escapeHtml(message)}</p></div>`;
  }

  getNotFoundError(message = '未找到指定的文章') {
    return `<div class="error"><h2>文章不存在</h2><p>${this.escapeHtml(message)}</p></div>`;
  }
}
