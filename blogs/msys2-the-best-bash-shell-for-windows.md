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

MSYS 是Windows上的类Unix环境，提供包括了：pacman 包管理器 、 C/C++ 编译器、Bash解释器、 Linux 命令如[grep、ls] 等一套Linux的工具链。底层基于**Cygwin**但更加现代。我称为the Best shell for Windows。

### 不同的启动程序

可以看多有很多版本的运行环境，其中仅推荐ucrt64。虽然不会直接使用这些exe启动终端，但还是说明一下。

这些exe的底层都是一样的，.每个exe 是一个 “一键切换环境” 的快捷方式，启动时会自动配置好对应的 PATH 和环境变量。

每个exe还有一个配套的ini文件：

ini中设置了两个重要的环境变量，其中CHERE_INVOKING=1是保留工作路径，MSYS2_PATH_TYPEMSYS2_PATH_TYPE=inherit 是让bash启动时自动继承Windows的环境变量（尤其是PATH）

```json
#CHERE_INVOKING=1 
#MSYS2_PATH_TYPE=inherit
```

注意这些配置文件仅对对应的exe文件生效。

### 启动脚本

我们的主角是 跟目录下的 msys2_shell.cmd ，这也是官方推荐的启动器。双击即可用终端打开msys2

Win11 自带的Windows Terminal已经足够好用，Win10 可以从从微软商店或者手动安装： [https://github.com/microsoft/terminal](https://github.com/microsoft/terminal) 

该启动脚本搭配以下参数使用：

`-here` 从此处启动，保留父进程的工作路径

`-use-full-path`  合并Windows的 PATH

# 在Windows终端中配置MYSYS2

[MSYS2如何使用windows环境变量(亲测可用)_msys2 环境变量-CSDN博客](https://blog.csdn.net/qq_45662588/article/details/136605190)

添加新的空配置文件

命令输入`"C:\Program Files\msys2\msys2_shell.cmd" -defterm -here -no-start -ucrt64 -use-full-path` ，注意参数

可以将msys2设置为默认终端

这样打开Windows Terminal 就能直接进入msys2

## Win+R 启动 msys2

如何能像cmd一样，win+r 直接启动？输入 wt 命令即可。因为wt 会打开Windows Terminal终端，而我们的终端默认环境是msys2

# 必要的配置

## bashrc

msys2中，默认的home目录是 安装/home/name。 bash的默认行为是登录时加载这里的.bashrc，但作为Windows，我的home目录明显不是这里，因此我们在.bashrc中添加  `export HOME=

## Home 目录

## 启动命令

不要直接使用exe启动shell。安装目录下的msys2_shell.cmd 是更好的启动器。双击即可在终端打开默认的MSYS2.

# VSCode 集成

在setting.json中添加：


```json
 "terminal.integrated.profiles.windows": {
    "MSYS2": {
      "path": "c:\\Program Files\\msys2\\usr\\bin\\bash.exe",
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