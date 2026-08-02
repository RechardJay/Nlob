---
title: "GitAction + GitPages"
date: "2026-08-01"
tags: "博客,避坑"
---

GitAction 和GitPages 用github部署静态站点的一套流程。其中GitPage负责托管静态网页，GitAction则可以执行流程（服务器操作）将仓库变成站点资源。

# MD不见了？

在Nlob中，我将md文件直接放在仓库中。网页html直接请求此地址的md，如如`https://rechardjay.github.io/Nlob/blogs/xxx.md` 

本地服务器测试时没问题，可以正常fetch到md。但部署到gitpage时发现无法获取到md。

`https://rechardjay.github.io/Nlob/blogs/xxx.md`  返回404

# 问题排查

起初我以为是跨域问题，但实际gitpage都部署在`name.github.io`  这个顶级域名下，不存在跨域问题。

也并非文件转移，服务器和浏览器会自动转换中文、非法字符（但要注意分区大小写）。

直到我打开了部署产物，才发现 所有的md文件都被编译成了html，再请求.md 自然404，因为托管服务上并没有对于的md文件。

查看Action日志：发现罪魁祸首是 `Jekyll` ！。

## Pages的默认行为

在仓库的Pages设置中有两个souces:

默认是Deploy from a branch。

该工作流除了执行 仓库下配置的 workflow之外，还会前置执行内置的build和后置的deploy

其中的的build有一步便是build with Jekyll。Jekyll会把仓库中的md渲染成html。

在网上搜索方案说是添加 .noJekyll 文件可以关闭。但实测并没有用，似乎没有手段能管理这两个内置过程。

# 解决方案

对于已经编译好的纯静态站点，内置的build明显是多此一举。并且我确实需要原始文件。为了能获取到仓库原始内容，有两种方式：

## raw.githubusercontent.com

这个站点暴露了github所有仓库的原文件。只要把获取md的地址改成

[`raw.githubusercontent.com/RechardJay/Nlob/refs/heads/master/blogs/xxx.md`](http://raw.githubusercontent.com/RechardJay/Nlob/refs/heads/master/blogs/xxx.md) 即可拿到原始md。

## 自定义Action

要使用默认的Deploy from branch，而是用GithubAction。

在 `/.github/workflows` 自己写workflow即可，github会执行所有workflow file。已经编译好的项目，可以直接用自动生成的static.yml。 任何需要的过程自己补充即可。

Action是完全可控的，没有额外行为。