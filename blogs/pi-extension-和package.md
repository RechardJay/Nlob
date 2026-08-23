---
title: "PI-Extension 和Package"
date: "2026-08-22"
---

package 本质是一个普通 npm 包，增加了`pi`元数据字段。专门用来打包分发PI的skills、prompts、themes、extension等资源。

Package 最常见的内容就是安装第三方的Extension。

pi提供了CLI管理package

# 安装扩展

直接从npm 或者 github下载 package。

对应下载到目录 ~/.pi/agent/npm ~/.pi/agent/git