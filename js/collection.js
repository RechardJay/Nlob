let allBlogs = [];
let allTags = new Set();

// 加载博客数据
async function loadBlogs() {
  try {
    const response = await fetch('blogs/index.json');
    allBlogs = await response.json();
    
    // 收集所有标签，并去重
    allBlogs.forEach(blog => {
      if (blog.tags) {
        blog.tags.split(',').forEach(tag => {
          allTags.add(tag.trim());
        });
      }
    });
    
    renderTagButtons();
    renderCollections('all');
  } catch (error) {
    console.error('加载博客数据失败:', error);
    document.getElementById('collection-container').innerHTML = 
      '<div class="empty-state"><h3>加载失败</h3><p>无法加载博客数据</p></div>';
  }
}

// 渲染标签按钮
function renderTagButtons() {
  const container = document.getElementById('tag-buttons-container');
  const tags = Array.from(allTags).sort();
  
  tags.forEach(tag => {
    const button = document.createElement('button');
    button.className = 'tag-btn';
    button.textContent = tag;
    button.dataset.tag = tag;
    button.addEventListener('click', () => {
      document.querySelectorAll('.tag-btn').forEach(btn => btn.classList.remove('active'));
      button.classList.add('active');
      renderCollections(tag);
    });
    container.appendChild(button);
  });
}

// 渲染合集内容
function renderCollections(selectedTag) {
  const container = document.getElementById('collection-container');
  
  let filteredBlogs = allBlogs;
  if (selectedTag !== 'all') {
    filteredBlogs = allBlogs.filter(blog => 
      blog.tags && blog.tags.split(',').map(t => t.trim()).includes(selectedTag)
    );
  }
  
  if (filteredBlogs.length === 0) {
    container.innerHTML = '<div class="empty-state"><h3>暂无文章</h3><p>该分类下还没有文章</p></div>';
    return;
  }
  
  // 按标签分组
  const grouped = {};
  filteredBlogs.forEach(blog => {
    const tags = blog.tags ? blog.tags.split(',').map(t => t.trim()) : [];
    const mainTag = selectedTag === 'all' ? (tags[0] || '其他') : selectedTag;
    
    if (!grouped[mainTag]) {
      grouped[mainTag] = [];
    }
    grouped[mainTag].push(blog);
  });
  
  // 渲染分组内容
  let html = '';
  Object.keys(grouped).sort().forEach(tag => {
    html += `<div class="collection-group">
      <h3 class="collection-group-title">${tag}</h3>`;
    
    grouped[tag].forEach(blog => {
      const tags = blog.tags ? blog.tags.split(',').map(t => t.trim()) : [];
      const tagsHtml = tags.map(t => `<span class="tag-badge">${t}</span>`).join('');
      
      html += `<div class="collection-item">
        <h4><a href="blog.html?post=${blog.id}">${blog.title}</a></h4>
        <div class="collection-item-meta">
          <span class="publish-date">${blog.date}</span>
        </div>
        <p class="collection-item-excerpt">${blog.excerpt}</p>
        <div class="collection-item-tags">${tagsHtml}</div>
      </div>`;
    });
    
    html += '</div>';
  });
  
  container.innerHTML = html;
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', loadBlogs);
