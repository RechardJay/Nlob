/* ============================================================
   Collection Page — 合集页逻辑
   ============================================================ */
(function () {
  let allBlogs = [];
  let allTags = new Set();
  let activeTag = 'all';

  document.addEventListener('DOMContentLoaded', init);

  async function init() {
    try {
      const res = await fetch('blogs/index.json');
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const list = await res.json();

      // 去重 & 排序
      const seen = new Set();
      allBlogs = (list || [])
        .filter(it => it && it.id)
        .filter(it => (seen.has(it.id) ? false : (seen.add(it.id), true)))
        .sort((a, b) => {
          const da = new Date(a.update || a.date || 0);
          const db = new Date(b.update || b.date || 0);
          return da - db;
        });

      // 收集所有标签
      allBlogs.forEach((blog) => {
        parseTags(blog.tags).forEach(t => allTags.add(t));
      });

      renderTagButtons();
      renderCollections(activeTag);
    } catch (err) {
      console.error('加载博客数据失败:', err);
      const el = document.getElementById('collection-container');
      if (el) el.innerHTML = '<div class="empty-state"><h3>加载失败</h3><p>请稍后重试。</p></div>';
    }
  }

  const TAG_OTHER = 'other';

  // —— 渲染标签按钮 ——
  function renderTagButtons() {
    const container = document.getElementById('tag-buttons-container');
    if (!container) return;
    container.innerHTML = '';

    const tags = Array.from(allTags).sort((a, b) => a.localeCompare(b, 'zh'));
    tags.forEach((tag) => {
      const btn = document.createElement('button');
      btn.className = 'tag-btn';
      btn.textContent = tag;
      btn.dataset.tag = tag;
      btn.addEventListener('click', () => setActiveTag(tag));
      container.appendChild(btn);
    });

    // 追加"其他"按钮（无合集的文章）
    const otherBtn = document.createElement('button');
    otherBtn.className = 'tag-btn';
    otherBtn.textContent = '其他';
    otherBtn.dataset.tag = TAG_OTHER;
    otherBtn.addEventListener('click', () => setActiveTag(TAG_OTHER));
    container.appendChild(otherBtn);

    // "全部"按钮绑定
    const wrap = document.getElementById('tag-filter-wrap');
    if (wrap) {
      wrap.querySelectorAll('.tag-btn').forEach(btn => {
        // 防止重复绑定
        if (btn.dataset.bound) return;
        btn.dataset.bound = '1';
        if (btn.dataset.tag === 'all') {
          btn.addEventListener('click', () => setActiveTag('all'));
        }
      });
    }
  }

  function setActiveTag(tag) {
    activeTag = tag;
    document.querySelectorAll('.tag-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tag === tag);
    });
    renderCollections(tag);
  }

  // —— 渲染合集（按 collection 分组） ——
  function renderCollections(selectedTag) {
    const container = document.getElementById('collection-container');
    if (!container) return;

    // 先按标签过滤
    let list = allBlogs;
    if (selectedTag === TAG_OTHER) {
      list = allBlogs.filter((b) => !(b.collection && b.collection.trim()));
    } else if (selectedTag !== 'all') {
      list = allBlogs.filter((b) => parseTags(b.tags).includes(selectedTag));
    }

    if (list.length === 0) {
      container.innerHTML = '<div class="empty-state reveal is-visible"><h3>暂无文章</h3><p>该分类下还没有文章。</p></div>';
      return;
    }

    // 按 collection 分组
    const groups = new Map();
    const MISC_KEY = '其他';
    list.forEach((blog) => {
      const key = (blog.collection && blog.collection.trim()) || MISC_KEY;
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(blog);
    });

    // 组排序（按字母/拼音），"其他"固定排最后
    const sortedKeys = Array.from(groups.keys())
      .filter(k => k !== MISC_KEY)
      .sort((a, b) => a.localeCompare(b, 'zh'));
    if (groups.has(MISC_KEY)) sortedKeys.push(MISC_KEY);

    let html = '';
    sortedKeys.forEach((grpKey, gIdx) => {
      const items = groups.get(grpKey);
      const isMisc = grpKey === MISC_KEY;
      html += `<div class="collection-group reveal" style="transition-delay:${(gIdx * 0.06).toFixed(2)}s">
        <h3 class="collection-group-title">
          ${isMisc
            ? `<span class="collection-group-link">${escapeHtml(grpKey)}</span>`
            : `<a href="collection-read.html?collection=${encodeURIComponent(grpKey)}" class="collection-group-link">${escapeHtml(grpKey)}</a>`
          }
          <span class="count">${items.length}</span>
        </h3>`;

      items.forEach((blog, idx) => {
        const num = String(idx + 1).padStart(2, '0');
        const date = blog.update || blog.date || '—';
        const tags = parseTags(blog.tags);
        const tagsHtml = tags.slice(0, 3).map(t =>
          `<span class="tag-badge">${escapeHtml(t)}</span>`
        ).join('');

        html += `
          <article class="collection-item">
            <div class="collection-item-num">${num}</div>
            <div class="collection-item-main">
              <h4><a href="blog.html?post=${encodeURIComponent(blog.id)}">${escapeHtml(blog.title)}</a></h4>
              <div class="collection-item-meta">${escapeHtml(date)}${blog.collection && blog.collection !== grpKey ? ' · ' + escapeHtml(blog.collection) : ''}</div>
              <div class="collection-item-excerpt">${escapeHtml(cleanExcerpt(blog.excerpt || ''))}</div>
              ${tagsHtml ? `<div class="collection-item-tags">${tagsHtml}</div>` : ''}
            </div>
            <a class="blog-card-arrow" href="blog.html?post=${encodeURIComponent(blog.id)}" aria-label="Read article">→</a>
          </article>`;
      });

      html += '</div>';
    });

    container.innerHTML = html;

    // 触发 reveal
    if ('IntersectionObserver' in window) {
      const io = new IntersectionObserver((entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add('is-visible');
            io.unobserve(e.target);
          }
        });
      }, { threshold: 0.05 });
      container.querySelectorAll('.reveal').forEach(el => io.observe(el));
    } else {
      container.querySelectorAll('.reveal').forEach(el => el.classList.add('is-visible'));
    }
  }

  // —— Helpers ——
  function parseTags(tagsStr) {
    if (!tagsStr) return [];
    return String(tagsStr)
      .split(/[,，\s、;；]+/)
      .map(s => s.trim())
      .filter(Boolean);
  }

  function cleanExcerpt(text) {
    const plain = String(text || '').replace(/<[^>]*>/g, ' ');
    return plain.replace(/\s+/g, ' ').trim().slice(0, 120);
  }

  function escapeHtml(s) {
    if (!s) return '';
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
  }
})();
