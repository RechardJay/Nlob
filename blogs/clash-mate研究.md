---
title: "clash-mate研究"
date: "2025-10-13"
tags: "技术"
---

clash.Mate来源于大名鼎鼎的clash项目。clash已经删库，但社区中还留下了无数fork分支。clash.Mate就是其中一位，现在已经改名→[mihomo](https://github.com/MetaCubeX/mihomo)，配套文档也十分完善：[虚空终端 Docs](https://wiki.metacubex.one/)

~~看的出来项目组里有不少原~~

实现基本代理工具只需要早期版本即可，Windows版v1.11.2/Clash.Meta-windows-386-v1.11.2.zip

不到10MB。

机缘巧合之下，我得到一个还算干净的meta，但并不是很完善 ~~作者加了不少私货~~。于是我就自己动手用Go写了一个前端。

原材料：clash.mate.exe、机场、配置文件。

clash内核直接去[Releases · MetaCubeX/mihomo](https://github.com/MetaCubeX/mihomo/releases) 找一个早期的。迭代多了太复杂看不懂。机场随便找个免费的，个人使用绰绰有余的。

使用这种工具的的核心其是就是写配置文件：官方的文档大而全，不如直接看范例 [clash-meta懒人配置](https://blog.sephiroth.club/post/clash-meta-lan-ren-pei-zhi.html)。强调几点：

- 分流规则

  - 要先指定规则集 `rule-providers` ，给每配置文件组成规则集，不能直接用配置文件。

  - 在规则中使用规则集如 `RULE-SET,ad-rules,REJECT` 类型，名称，策略

- 启动命令

  - `claash.meta.exe -d .`  -d指定根目录

贴一个仓库地址：[RechardJay/glash](https://github.com/RechardJay/glash)