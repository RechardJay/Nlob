---
title: "PI-Package和Extension"
date: "2026-08-22"
---

package 本质是一个普通 npm 包。专门用来打包分发PI的skills、prompts、themes、extension等资源。

Package 最常见的内容就是安装第三方的Extension。

扩展Extension是PI定制化和强大的根源。Package则让快速获得这些能力。如果你决定现有package不够强大、不够定制化，考虑自己（让PI）开发，独属于你工作流的扩展。

# 安装扩展

从官方的官方搜索有用的包 [https://pi.dev/packages](https://pi.dev/packages)

使用CLI管理，直接从npm 或者 github下载 package。

对应下载到目录 ~/.pi/agent/npm ~/.pi/agent/git

卸载使用 `pi uninstall`

# 推荐一些扩展

[计划模式](https://pi.dev/packages/@narumitw/pi-plan-mode?name=plan)

[网络搜索](https://pi.dev/packages/pi-web-access)