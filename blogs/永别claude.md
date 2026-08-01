---
title: "永别Claude"
date: "2026-07-05"
tags: "技术,避坑,AI"
---

刚入坑没多久Claude就暴雷了，之前以为接入国内大模型不会有影响，后来发现连Agent工具也针对国内用户，包括在请求中随机Hash降低缓存命中率。反华A/不是白叫的。

 [https://www.bilibili.com/video/BV1pXT46TEQq/?share_source=copy_web&vd_source=6ce5aabca1c6953f5a8cfe8ec0c28ea3](https://www.bilibili.com/video/BV1pXT46TEQq/?share_source=copy_web&vd_source=6ce5aabca1c6953f5a8cfe8ec0c28ea3)

[https://www.bilibili.com/video/BV1wpTJ6yEAq/?share_source=copy_web&vd_source=6ce5aabca1c6953f5a8cfe8ec0c28ea3](https://www.bilibili.com/video/BV1wpTJ6yEAq/?share_source=copy_web&vd_source=6ce5aabca1c6953f5a8cfe8ec0c28ea3)

# Windows卸载Claude

原生安装

npm

先保留资产

移除相关目录

```bash
# 删除 Claude 主目录
rm -rf ~/.claude

# 删除本地 bin 中的可执行文件
rm -f ~/.local/bin/claude # 该目录也可以从环境变量中移除
rm -f "~\.local\share\claude"
rm -f "~\.local\state\claude"

```

桌面版

## CC Switch

CCS 这个工具本身没有任何问题，作为配置管理工具十分好用。但由于不再使用Claude及其他由CCS管理的Agent，因此卸载。

配置文件在 ~\.cc-switch 。记得保留skill等资产后移除即可。

# 其他选择

Agnet CLI 我选择PI，注意这不是Claude的平替，而是一种完全不同的选择。

[https://pi.dev/](https://pi.dev/) 

PI 是一个极简的AgentCode，可以按照自己的工作流高度自定义配置。是真正意义上AI时代的软件。非常适合开发者折腾。

开箱即用的桌面Agent，我用了字节的Trae Work，国内的大差不差。有了Claude前车之鉴，国产工具突然眉清目秀

# 碎碎念

AI改变了这个世界，但似乎没有向好的方向。AI的红利没有普惠（反正我没吃到）。

Agent给AI注入了强大的活力，但其高昂的成本将普通人拒之门外。AI产业无疑吸纳的大量资金，但结果是顶模以国界和我划清界限，高额成本将普通人拒之门外。其对经济和基础IT相关从业者造成釜底抽薪式的打击。

AI的变革无疑会到来，但是什么时候，以什么样的方式呢？何时AI能像水电一样普惠所有人而不让人时刻盯着token账单？何时国内的Agent 工具和模型能和世界级持平，让用户不被国界阻拦？新的编程抽象层级-自然语言已经出现，但AI编程范式的边界在哪里？