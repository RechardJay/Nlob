---
title: "OneDrive 客户端 for Linux"
date: "2026-08-03"
tags: "Linux,避坑,工具"
---

# 起因

我常用的网盘如onedrive、夸克 在Linux上并没有客户端，而且由于我的onedrive使用教育教育邮箱（白嫖2$），一些Ubuntu常见的方案如onedrive、onedriver都会遇到权限问题。因此尝试第三客户端。

# onedirve

OneDrive由于开放API，因此有大量第三方客户端可用。但大部分都遇到权限问题，显示需要管理员授权；在一番调查之后，我发现onedrive触发管理员授权的原因是这些第三方客户端申请了 微软云Azure的敏感权限；但实际上个人使用的话仅需要访问自己的文件即可，并不会触发鉴权；

rclone可以自定义权限范围。使用rclone配置onedrive，只需要设置 `access_scopes = Files.ReadWrite offline_access` 就可成功获取token，不会触发管理员鉴权。

rclone能实现win下的的FUSE效果，虽然不能和原生媲美，但也NB。

注意不需要去Azure注册什么应用、客户端密钥！用rclone配置全留空，设置`access_scopes` 就能连接上；如果用个人账户，没有权限问题一路留空就行。

目前不知道云厂商的组织应用有什么用，总之个人用户不要跟着alist瞎搞。（谁懂我登陆edu账户后看到组织里一堆alist应用的救赎感，不止我浪费了一下午）

# Quark

Quark 不开放API但是开放Web Cookie，有cookie也很容易获取直链。我使用alist作为三方客户端；能实现一些简单的浏览和下载；而且alist转WebDAV能再接入rclone。

夸克会员用alist直链下载速度在200KB，一般文件勉强能用；（之前用其他客户端分片并发甚至跑到4MB，但不可复现）

遗憾的是cookie一两天就会过期，要登陆网页刷新；

# 碎碎念

注意所有第三方客户端并不具备对网盘加速下载或大小扩容的能力，只是对接口（或公开或逆向）的二次封装。云厂商精着呢。

云厂商什么时候能当人啊？