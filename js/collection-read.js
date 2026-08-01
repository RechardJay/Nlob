/* ============================================================
   Collection Read — 合集阅读页逻辑
   ============================================================ */
(function () {
  'use strict';

  let allBlogs = [];
  let currentCollection = '';
  let currentPostId = '';

  document.addEventListener('DOMContentLoaded', init);

  async function init() {
    const params = new URLSearchParams(window.location.search);
    currentCollection = params.get('collection') || '';
    currentPostId = params.get('post') || '';

    if (!currentCollection) {
      showError('未指定合集');
      return;
    }

    try {
      const res = await fetch('blogs/index.json');
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const list = await res.json();

      // 去重
      const seen = new Set();
      allBlogs = (list || [])
        .filter(it => it && it.id)
        .filter(it => (seen.has(it.id) ? false : (seen.add(it.id), true)))
        .sort((a, b) => {
          const da = new Date(a.update || a.date || 0);
          const db = new Date(b.update || b.date || 0);
          return da - db;
        });

      // 过滤当前合集
      const collectionBlogs = allBlogs.filter(b =>
        b.collection && b.collection.trim() === currentCollection
      );

      if (!collectionBlogs.length) {
        showError('该合集暂无文章');
        return;
      }

      // 设置合集名
      document.getElementById('cr-collection-name').textContent = currentCollection;

      // 渲染文章列表
      renderArticleList(collectionBlogs);

      // 加载第一篇文章或指定文章
      const target = currentPostId
        ? collectionBlogs.find(b => b.id === currentPostId)
        : collectionBlogs[0];

      if (target) {
        loadArticle(target);
      }
    } catch (err) {
      console.error('加载合集数据失败:', err);
      showError('加载失败，请稍后重试');
    }
  }

  // —— 渲染左侧文章列表 ——
  function renderArticleList(blogs) {
    const list = document.getElementById('cr-article-list');
    if (!list) return;

    list.innerHTML = blogs.map((blog, i) => {
      const num = String(i + 1).padStart(2, '0');
      const date = blog.update || blog.date || '';
      const isActive = blog.id === currentPostId || (!currentPostId && i === 0);
      return `<li>
        <a class="cr-article-link${isActive ? ' active' : ''}"
           data-post-id="${escapeHtml(blog.id)}"
           data-filename="${escapeHtml(blog.filename)}">
          ${num}. ${escapeHtml(blog.title)}
          <span class="cr-article-date">${escapeHtml(date)}</span>
        </a>
      </li>`;
    }).join('');

    // 点击切换文章
    list.querySelectorAll('.cr-article-link').forEach(link => {
      link.addEventListener('click', function () {
        const postId = this.dataset.postId;
        const blog = allBlogs.find(b => b.id === postId);
        if (blog) {
          // 更新 URL
          const url = new URL(window.location);
          url.searchParams.set('post', postId);
          window.history.replaceState({}, '', url);

          // 更新高亮
          list.querySelectorAll('.cr-article-link').forEach(l => l.classList.remove('active'));
          this.classList.add('active');

          loadArticle(blog);
        }
      });
    });
  }

  // —— 加载并渲染文章 ——
  async function loadArticle(blog) {
    const container = document.getElementById('cr-content-container');
    if (!container) return;

    if (!blog.filename) {
      container.innerHTML = '<div class="error"><h2>加载失败</h2><p>文章文件不存在。</p></div>';
      return;
    }

    container.innerHTML = '<div class="loading">Fetching the manuscript</div>';

    try {
      const loader = new MarkdownLoader();
      const { metadata, htmlContent } = await loader.loadBlogContent(blog.filename);

      // 设置标题
      const title = blog.title || metadata.title || '无标题';
      document.title = `${title} · ${currentCollection} · Jay's Blog`;

      const date = blog.update || blog.date || '—';
      const tags = parseTags(blog.tags || '');
      const tagsHtml = tags.length
        ? tags.map(t => `<span class="blog-tag">${escapeHtml(t)}</span>`).join(' ')
        : '';

      container.innerHTML = `
        <article class="blog-post reveal is-visible">
          <header class="blog-header">
            <span class="post-eyebrow">${escapeHtml(currentCollection)}</span>
            <h1 class="blog-title">${escapeHtml(title)}</h1>
            <div class="blog-meta-full">
              <span>${escapeHtml(date)}</span>
              ${tagsHtml ? '<span class="meta-divider">·</span>' + tagsHtml : ''}
            </div>
          </header>
          <div class="blog-content">
            ${htmlContent}
          </div>
        </article>
      `;

      // 为 pre 加上语言 data 属性
      container.querySelectorAll('pre code').forEach((codeEl) => {
        const m = /language-([\w+-]+)/.exec(codeEl.className || '');
        const pre = codeEl.closest('pre');
        if (pre && m) pre.setAttribute('data-lang', m[1]);
      });

      // 外链处理
      container.querySelectorAll('a').forEach((a) => {
        const href = a.getAttribute('href') || '';
        if (/^https?:/.test(href)) {
          a.target = '_blank';
          a.rel = 'noopener noreferrer';
        }
      });

      // 构建 TOC
      buildToc();
    } catch (err) {
      console.error('加载文章失败:', err);
      container.innerHTML = '<div class="error"><h2>加载失败</h2><p>请稍后重试。</p></div>';
    }
  }

  // —— 构建当前文章 TOC ——
  function buildToc() {
    const tocList = document.getElementById('cr-toc-list');
    const content = document.querySelector('.cr-content .blog-content');
    if (!tocList || !content) return;

    const headings = content.querySelectorAll('h1, h2, h3');
    if (!headings.length) {
      tocList.parentElement.style.display = 'none';
      return;
    }

    tocList.parentElement.style.display = '';
    let html = '';
    headings.forEach((h) => {
      const level = parseInt(h.tagName.charAt(1));
      if (!h.id) {
        h.id = h.textContent
          .toLowerCase()
          .replace(/[^\w\u4e00-\u9fff]+/g, '-')
          .replace(/^-+|-+$/g, '');
      }
      const cls = level > 1 ? ` toc-h${level}` : '';
      html += `<li><a href="#${h.id}" class="${cls}">${escapeHtml(h.textContent)}</a></li>`;
    });
    tocList.innerHTML = html;

    // 点击平滑滚动
    tocList.querySelectorAll('a').forEach((a) => {
      a.addEventListener('click', (e) => {
        e.preventDefault();
        const target = document.getElementById(a.getAttribute('href').slice(1));
        if (target) {
          const top = target.getBoundingClientRect().top + window.scrollY - 100;
          window.scrollTo({ top, behavior: 'smooth' });
        }
      });
    });

    // 滚动高亮
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

  // —— Helpers ——
  function showError(msg) {
    const container = document.getElementById('cr-content-container');
    if (container) {
      container.innerHTML = `<div class="error"><h2>${escapeHtml(msg)}</h2><p>请返回合集页面选择其他合集。</p></div>`;
    }
  }

  function parseTags(tagsStr) {
    if (!tagsStr) return [];
    return String(tagsStr).split(/[,，\s、;；]+/).map(s => s.trim()).filter(Boolean);
  }

  function escapeHtml(s) {
    if (!s) return '';
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
  }
})();