---
title: "apt源修复-Ubuntu24"
date: "2026-08-06"
tags: "Linux"
---

# 一、起因

## 背景故事

通过dpkg安装的deb包会注册到dpkg索引中。apt基于dpkg，因此自己安装的deb包和通过apt安装的包（本质也是deb包）都会被 `apt list —installed` 收录。其中无任何标记的 [已安装] 说明这是我们手动用 `apt install xx` 从sources源中下载的；带[本地]标记说明这是我们用dpkg直接安装的deb包 ；带 [自动] 标志 说明这是某个包的依赖，被连带着下载安装的。 自解压的二进制程序不被apt管理。 

## 故障描述

我本来只是想看看自己安装了哪些软件，执行 `apt list --installed | grep 本地` 时，我发现大量系统包（coreutils、acl、dbus、grub等）有 `[已安装，本地]`标记；这很不正常，也很危险：apt 源中没有这些包意味着也就无法更新和获取安全补丁。

找一个具体的包执行 `apt-cache policy coreutils` 看看怎么回事：本地补丁版本 `9.4-3ubuntu6.1`，软件源仅能读取基础版 `9.4-3ubuntu6`，无匹配补丁版本，APT判定版本不在仓库内，打上local标记。

# 问题分析

我的源之前肯定是好的并且获取到了新版的更新补丁，但后面大概出了什么问题导致apt里只有旧版本的索引list，匹配不上新版本的当前包。所以当前只要修复sources list即可。

## 问题定位

查看source list

```bash
$ cat /etc/apt/sources.list.d/ubuntu.sources
Types: deb
URIs: http://cn.archive.ubuntu.com/ubuntu/
Suites: noble
Components: main restricted universe multiverse
Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg

Types: deb
URIs: http://security.ubuntu.com/ubuntu/
Suites: noble-security
Components: multiverse main universe restricted
Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg
```

果然发现Suites这里只写了 noble，缺少 noble-updates。存放所有小补丁版本，如9.4-3ubuntu6.1 这类带 .1 的更新。

APT 找不到 noble-updates 索引，读不到 .1 补丁版本 → 所有打过补丁的系统包全部判定为本地包。

## 修复

1. 重写完整标准源配置。为主源补充 `noble-updates noble-backports` 即可

  ```bash
Suites: noble noble-updates noble-backports
```

1. 清理APT缓存，拉取全新完整索引

  ```bash
sudo apt clean
sudo rm -rf /var/lib/apt/lists/*
sudo apt update
```

重建索引后再次 `apt list --installed | grep 本地`  能正确过滤出本地包；

具体包 如`apt-cache policy coreutils` 也恢复正常（并且提示有更新）

# 原因~~猜测~~分析

我记得之前配置了清华源而且是写在 `/etc/apt/souces.list` 文件。先现在去看发现这个文件已经失效注释说明 

# Ubuntu sources have moved to /etc/apt/sources.list.d/ubuntu.sources

大概是Ubuntu某次系统更新采用了新的sources list标准 `/etc/apt/sources.list.d/ubuntu.sources`，把原来的 `souces.list` 文件直接覆。但新标准把这些包分得更细但文件里没写全。

也可能是我某次无意间改动了什么设置….

# 后续

我明明没有配置清华源，为何每次apt update 都会命中清华源呢？莫非Ubuntu官方做了重定向？

```bash
命中:4 http://mirrors.tuna.tsinghua.edu.cn/ubuntu noble InRelease        
命中:6 http://mirrors.tuna.tsinghua.edu.cn/ubuntu noble-updates InRelease
命中:7 http://mirrors.tuna.tsinghua.edu.cn/ubuntu noble-backports InRelease

```