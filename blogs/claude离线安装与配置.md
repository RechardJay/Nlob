---
title: "Claude离线安装与配置"
date: "2026-06-23"
tags: "工具,技术,避坑"
---

# Claude Code+CC Switch

claude 已经不再推荐使用node安装。

Windows下直接是使用pwsh脚本：

```typescript
// 官方
irm [https://claude.ai/install.ps1](https://claude.ai/install.ps1) | iex

// 国内镜像
irm https://daheiai.com/cc.ps1 | iex
```

# 安装Claude App

从该仓库下载离线镜像

[https://github.com/Wangnov/claude-app-mirror/releases](https://github.com/Wangnov/claude-app-mirror/releases)，安装后

**重点：**

这样仅能运行code模式，cowork仍然无法使用