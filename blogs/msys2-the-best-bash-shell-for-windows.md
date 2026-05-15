---
title: "MSYS2 — the Best Bash/Shell for Windows"
date: "2026-05-14"
tags: "msys2,shell"
---

用MSYS2 在Windows创建一个完美的终端环境，拒绝cmd、powershell、虚拟机、WSL，让Windows也能拥有类Unix终端操作。

# 安装

官网：[https://www.msys2.org/](https://www.msys2.org/)

一路下一步，完成后如图

双击msys2.exe、urct.exe 将运行在内置的mintty，该终端比较简陋。并不推荐直接使用。

## 介绍

MSYS 是Windows上的类Unix环境，提供包括了：包管理器 、 C/C++ 编译器、Bash解释器、 Linux 命令如[grep、ls] 等一套Linux的工具链。底层基于**Cygwin**但更加现代。我称为the Best shell for Windows。

# 必要的配置

## bashrc

## Home 目录

## 启动命令

不要直接使用exe启动shell。安装目录下的msys2_shell.cmd 是更好的启动器。双击即可在终端打开默认的MSYS2.

# 在Windows终端中配置MYSYS2

[MSYS2如何使用windows环境变量(亲测可用)_msys2 环境变量-CSDN博客](https://blog.csdn.net/qq_45662588/article/details/136605190)

# VSCode 集成

在setting.json中添加：


```json
 "terminal.integrated.profiles.windows": {
    "MSYS2": {
      "path": "D:\\Program Files\\msys2\\usr\\bin\\bash.exe",
      "args": [
        "-i",
        "-l"
      ],
      "icon": "terminal-bash",
    },
  },
  "terminal.integrated.env.windows": {
    "MSYSTEM": "UCRT64",
    "CHERE_INVOKING": "1",
    "MSYS2_PATH_TYPE": "inherit"
  },
```

如何想将bash 设置为默认终端