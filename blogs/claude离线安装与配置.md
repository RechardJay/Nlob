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

# 安装Claude DeskTop App

从该仓库下载离线镜像

[https://github.com/Wangnov/claude-app-mirror/releases](https://github.com/Wangnov/claude-app-mirror/releases)，安装后

**重点：**

参考文献

## Desktop和CLI

Desktop其实集成了三个不同的功能/产品：Chat、Cowork、Code。

3p模式下无法使用Chat，不做讨论。

桌面版的Cowork、Code以及CLI版本的Claude Code其实是三个不同的产品、数据不互通。

只有桌面版Code和Claude Code的Skill/Plugin是共享的，（~/.claude目录），但二者的会话也是独立存储。

真是反直觉的，共享同一引擎确不互通数据。Coworke和Code不互通可以理解，但CLI和Desktop Code共享技能不共享会话实在愚蠢。虽然官方在CLI提供可/desktop 可以把CLI会话导入桌面版，也只是对这个失败设计亡羊补牢，桌面版