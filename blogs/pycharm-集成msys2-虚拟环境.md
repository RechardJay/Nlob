---
title: "Pycharm 集成MSYS2 虚拟环境"
date: "2026-05-16"
tags: "避坑,msys2"
---

上次介绍了MYSY2[MSYS2 — the Best Bash/Shell for Windows](https://www.notion.so/360581e74ffc80d289d7db4d17489d50) ，今天在Pycharm中使用集成终端时就遇到了问题。

# 问题复现

一般情况下在Pycharm中打开终端，会自动激活虚拟环境，表现为 提示符前面有虚拟环境的目录名如。

```json
(.venv) E:\ProgramScript\Python Scripts\GameAI>    
```

为了在Pychharm中使用MYSY2，在设置添加自定义shell：


问题是这样打开终端时并不会像cmd或者pwsl一样进入.venv，只是原理的提示符。这时如果手动激活，运行bash激活脚本：  `source .venv/Scripts/activate` 

环境直接崩了


后面啥命令也识别不了

# 解决方案

解决方案是在设置中关闭自动激活虚拟环境

然后在手动激活，运行 `source .venv/Scripts/activate` 即可

# 问题分析

如果不是用Pycharm，用VSCode或者在其他终端打开项目、运行激活脚本，一切都是是正常的。

所以是Pycharm本身对终端做了一些预设置和我们的环境冲突了。罪魁祸首就是这个自动激活虚拟环境的设置。

对于内置的shell，Pycharm表现良好，应该是判断了shell类型并执行了对应的激活脚本。但自定义shell似乎无法被Pycharm识别，因此它做了一个偷懒的方法，直接改了PATH（可能还有其他东西）

导致activate脚本无法正常运行。

其实这个时候，虚拟环境已经添加到PATH较为靠前的位置，也就是说已经激活了。输入py -V可以发现此时的Python就是虚拟环境中的版本而非全局版本。因此如果不介意的话就不用改了，只是少了一个提示符而已。