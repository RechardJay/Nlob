/* ============================================================
   Nlob App — 全局交互与初始化
   ============================================================ */

(function () {
  // ============== 主题系统（立即执行，避免闪屏） ==============
  function getPreferredTheme() {
    const saved = localStorage.getItem('theme');
    if (saved === 'light' || saved === 'dark') return saved;
    // 自动检测：6:00-18:00 为日间，其余为夜间
    const hour = new Date().getHours();
    return (hour >= 6 && hour < 18) ? 'light' : 'dark';
  }

  function setTheme(theme, skipStorage) {
    document.documentElement.setAttribute('data-theme', theme);
    if (!skipStorage) {
      localStorage.setItem('theme', theme);
    }
    // 更新按钮图标
    const btn = document.getElementById('theme-toggle');
    if (btn) btn.textContent = theme === 'dark' ? '☾' : '☀';
    // 切换 highlight.js 主题
    const hljs = document.getElementById('highlight-theme');
    if (hljs) {
      hljs.href = theme === 'dark'
        ? 'https://cdn.jsdelivr.net/npm/highlight.js@11.7.0/styles/github-dark.min.css'
        : 'https://cdn.jsdelivr.net/npm/highlight.js@11.7.0/styles/github.min.css';
    }
  }

  // 立即应用主题（DOMContentLoaded 前）
  const initialTheme = getPreferredTheme();
  setTheme(initialTheme, true);

  // ============== 页面加载动画 ==============
  window.addEventListener('load', function () {
    const loader = document.getElementById('page-loader');
    if (loader) {
      // 延迟一小段时间让动画有表现
      setTimeout(() => {
        loader.classList.add('hidden');
        // 动画结束后移除，防止遮挡
        setTimeout(() => {
          loader.remove();
        }, 900);
      }, 500);
    }
  });

  // ============== DOM 就绪后初始化 ==============
  document.addEventListener('DOMContentLoaded', function () {
    initScrollReveal();
    initHeaderScroll();
    initReadingProgress();
    initBackToTop();
    initNavActive();
    initBlogApplication();
    initStats();
    initThemeToggle();
    initHeroCollapse();
  });

  // ============== 1. 滚动显示动画（Intersection Observer） ==============
  function initScrollReveal() {
    const reveals = document.querySelectorAll('.reveal');
    if (!reveals.length || !('IntersectionObserver' in window)) {
      // 不支持时直接显示所有
      reveals.forEach(el => el.classList.add('is-visible'));
      return;
    }

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry, i) => {
          if (entry.isIntersecting) {
            // 添加渐入类；若元素本身无 delay，按出现顺序做细微 stagger
            const el = entry.target;
            if (!el.style.transitionDelay) {
              el.style.transitionDelay = (i * 0.04) + 's';
            }
            el.classList.add('is-visible');
            io.unobserve(el);
          }
        });
      },
      {
        threshold: 0.08,
        rootMargin: '0px 0px -40px 0px'
      }
    );

    reveals.forEach(el => io.observe(el));
  }

  // ============== 2. 导航栏滚动阴影 ==============
  function initHeaderScroll() {
    const header = document.getElementById('site-header');
    if (!header) return;

    const onScroll = () => {
      if (window.scrollY > 20) {
        header.classList.add('scrolled');
      } else {
        header.classList.remove('scrolled');
      }
    };

    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
  }

  // ============== 3. 阅读进度条 ==============
  function initReadingProgress() {
    const bar = document.getElementById('reading-progress');
    if (!bar) return;

    const update = () => {
      const scrollTop = window.scrollY || document.documentElement.scrollTop;
      const docHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
      const pct = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
      bar.style.width = pct + '%';
    };

    update();
    window.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
  }

  // ============== 4. 回到顶部 ==============
  function initBackToTop() {
    const btn = document.getElementById('back-to-top');
    if (!btn) return;

    const toggle = () => {
      if (window.scrollY > 400) {
        btn.classList.add('show');
      } else {
        btn.classList.remove('show');
      }
    };

    btn.addEventListener('click', () => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    toggle();
    window.addEventListener('scroll', toggle, { passive: true });
  }

  // ============== 5. 导航高亮（按文件名自动匹配） ==============
  function initNavActive() {
    const links = document.querySelectorAll('.nav a[data-page]');
    if (!links.length) return;

    const pageMap = {
      home: ['index.html', ''],
      collection: ['collection.html'],
      resource: ['resource.html'],
      blog: ['blog.html']
    };

    const path = window.location.pathname;
    const filename = path.substring(path.lastIndexOf('/') + 1);

    links.forEach(link => {
      const key = link.getAttribute('data-page');
      const targets = pageMap[key] || [];
      if (targets.includes(filename)) {
        link.classList.add('active');
      }
    });
  }

  // ============== 6. 博客内容加载 & 渲染 ==============
  function initBlogApplication() {
    if (typeof marked === 'undefined') {
      console.error('marked 库未加载');
      showInitError('Markdown 解析器加载失败，请检查网络。');
      return;
    }

    const markdownLoader = (typeof MarkdownLoader !== 'undefined') ? new MarkdownLoader() : null;

    // 延迟等待所有脚本就绪
    setTimeout(() => {
      if (document.getElementById('blog-list-container') && markdownLoader) {
        markdownLoader.renderBlogList('blog-list-container');
      } else if (document.getElementById('blog-content-container') && markdownLoader) {
        markdownLoader.renderBlogContent('blog-content-container');
      }
    }, 50);
  }

  // ============== 7. 首页统计数据 ==============
  function initStats() {
    const articlesEl = document.getElementById('count-articles');
    const collectionsEl = document.getElementById('count-collections');
    const tagsEl = document.getElementById('count-tags');
    const lastUpdateEl = document.getElementById('last-update');

    // 任何一个不存在则跳过
    if (!articlesEl && !collectionsEl && !tagsEl && !lastUpdateEl) return;

    fetch('blogs/index.json')
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(list => {
        if (!Array.isArray(list)) return;

        // 去重（index.json 里有重复数据）
        const seen = new Set();
        const unique = list.filter(it => {
          if (seen.has(it.id)) return false;
          seen.add(it.id);
          return true;
        });

        if (articlesEl) {
          animateNumber(articlesEl, unique.length, '篇');
        }

        // 合集统计
        if (collectionsEl) {
          const collections = new Set();
          unique.forEach(it => {
            if (it.collection && it.collection.trim()) collections.add(it.collection.trim());
          });
          animateNumber(collectionsEl, collections.size, '组');
        }

        // 标签统计
        if (tagsEl) {
          const tags = new Set();
          unique.forEach(it => {
            if (!it.tags) return;
            String(it.tags).split(/[,，\s]+/).forEach(t => t && tags.add(t));
          });
          animateNumber(tagsEl, tags.size, '个');
        }

        // 最后更新日期
        if (lastUpdateEl) {
          const dates = unique.map(it => it.update || it.date).filter(Boolean);
          if (dates.length) {
            const latest = dates.sort().reverse()[0];
            lastUpdateEl.textContent = latest;
          }
        }
      })
      .catch(() => {
        // 失败时不显示错误，保留占位符即可
      });
  }

  // 数字简单动画
  function animateNumber(el, target, unitText) {
    const duration = 900;
    const start = performance.now();
    const from = 0;

    function tick(now) {
      const p = Math.min(1, (now - start) / duration);
      // ease out cubic
      const eased = 1 - Math.pow(1 - p, 3);
      const val = Math.round(from + (target - from) * eased);
      el.innerHTML = val + '<span class="unit">' + unitText + '</span>';
      if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  // 初始化错误显示
  function showInitError(msg) {
    const el = document.getElementById('blog-list-container')
      || document.getElementById('blog-content-container');
    if (el) {
      el.innerHTML = '<div class="error"><h2>加载失败</h2><p>' + msg + '</p></div>';
    }
  }

  // ============== 8. 主题切换 ==============
  function initThemeToggle() {
    const btn = document.getElementById('theme-toggle');
    if (!btn) return;

    btn.addEventListener('click', function () {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      setTheme(next);
    });
  }

  // ============== 9. 首页 Hero 鼠标悬停展开/收起 ==============
  function initHeroCollapse() {
    const hero = document.querySelector('.hero');
    if (!hero) return;

    // 鼠标进入 hero 区域 → 展开
    hero.addEventListener('mouseenter', function () {
      hero.classList.remove('collapsed');
    });

    // 鼠标离开 hero 区域 → 收起
    hero.addEventListener('mouseleave', function () {
      hero.classList.add('collapsed');
    });
  }
})();
