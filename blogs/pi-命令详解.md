---
title: "PI 命令详解"
date: "2026-09-26"
tags: "AI,工具,技术"
---

本文主要关注pi的 /commad ，即斜杠命令。但这不是[命令速查表](https://pistudy.com.cn/cheatsheet/)，或者[文档](https://pi-doc.com/docs/latest/slash-commands)。而是从一些常用命令入手聊一聊PI的设计和一些使用心得。

# tree

Pi将对话作为session，管理交给模型的上下文。值得一提的是，Pi会话以树状组织消息列表。使用/tree 进入会话树。选中任意一条消息可以跳转到历史记录，并且可以选择是否总结旧分支内容。如果从这里发起对话，session树上会同步创建一个新分支。创建新分支后，旧分支的记录依然存在可以随时导航回来。

实际我很少遇到要回到某处新建分支的情况，/tree命令更多浏览历史记录的导航。

并且，分支跳转并不会影响工作区的文件（no git）。

另外两个操作session的命令：fork，顾名思义，是从分支处复制创建新的session。clone 在当前位置复制当前会话到新session

# compact

自动压缩

压缩参数配置在 `~/.pi/agent/settings.json` 或项目级 `.pi/settings.json` 

上下文健康

中文token的偏差

# 让屏幕更干净

Ctrl+o 关闭变动

Ctrl+t 关闭思考过程