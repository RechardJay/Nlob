// 初始化应用
document.addEventListener('DOMContentLoaded', function() {
    // 等待所有资源加载完成
    if (typeof marked === 'undefined') {
        console.error('marked 库未加载，请检查CDN链接');
        showError('必要的库加载失败，请刷新页面重试。');
        return;
    }

    const markdownLoader = new MarkdownLoader();
    
    // 延迟初始化，确保所有资源就绪
    setTimeout(() => {
        initializeApp(markdownLoader);
    }, 100);
});

function initializeApp(markdownLoader) {
    // 检查当前页面类型并执行相应逻辑
    if (document.getElementById('blog-list-container')) {
        // 首页 - 渲染博客列表
        markdownLoader.renderBlogList('blog-list-container');
    } else if (document.getElementById('blog-content-container')) {
        // 博客详情页 - 渲染博客内容
        const postId = markdownLoader.getBlogFromUrl();
        if (postId) {
            markdownLoader.renderBlogContent('blog-content-container', postId);
        } else {
            document.getElementById('blog-content-container').innerHTML = 
                '<div class="error">未找到指定的文章</div>';
        }
    }
}

function showError(message) {
    const container = document.getElementById('blog-list-container') || 
                     document.getElementById('blog-content-container');
    if (container) {
        container.innerHTML = `<div class="error">${message}</div>`;
    }
}