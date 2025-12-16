---
title: "Kotlin MultiPlatform(KMP) 跨平台技术"
date: "2025-12-15"
tags: "技术"
---

KMP技术栈中的许多术语是令人迷惑的，本文尝试捋一下。

首先，搞清楚两个大家族的归属：Kotlin，是一门编程语言，由JetBrain公司主导；Android，是一个操作系统+整个平台生态，由google主导。

二者的第一个交叉点：kotlin是安卓开发的首选语言，谷歌在逐渐抛弃Java~~（@Oracle)~~。

为安卓系统写UI，有一套新的工具库：JetPack Compose，替代原来的xml+view。ComposeUI仍是安卓开发体系的一环，谷歌主导，JetBrain有参与其标准制定。

而KMP，Kotlin 跨平台技术，由Jetbrain主导，旨在用一套（或共享部分）kotlin代码，实现多个平台的应用程序。而应用程序的UI，直接复用了JetPack Compose中的部分库和组件，称为Compose MultiPlatform，安卓的文档中对此也有说明[https://developer.android.google.cn/kotlin/multiplatform?hl=zh-cn](https://developer.android.google.cn/kotlin/multiplatform?hl=zh-cn)

必须注意的是，KMP是一套跨平台的方案，写UI需要安卓库的适配版（CMP）的支持，因此KMP的的本意是将安卓生态中的技术迁移到其他平台，因为用kotlin+composeUI 写安卓本来就是原生，而kotlin依赖JVM，天然有跨平台的基因。

但主要强调，KMP的核心仍是Kotlin，不是安卓和JetPack Compose。前者是Jetbrain，后者是google，这是两家公司在做不同的事情。但二者的交叉点是CMP，Compose Multiplatform，视为是JetPack Compose扩展，本来是用于写安卓UI的，为了实现跨平台而被适配到KMP中，但注意CMP成分复杂，如图：

CMP对KMP的适配，有些是原生安卓库，即androidx.xxx，不仅能用于写安卓页面，可以直接桌面（JVM）、IOS页面。但有些是JetBrain分支，即做了调整适配，对应库的前缀是 `org.jetbrains`
如

```kotlin
implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
```

导入依赖的时候要注意，并非所有库都是“通用的”。

JetPackCompose 库的内容非常多，也不是完全跨平台，实际上只有上图中的核心库是通用的，而且要注意包名。

还有一些常用的UI组件没有做KMP适配，可以常用在社区中寻找支持~~（或者自己写），~~比如 约束布局的KMP版本在社区中有提供，如果直接导入 androidx.xxx ，gradle会报不存在。

```kotlin
implementation("tech.annexflow.compose:constraintlayout-compose-multiplatform:0.4.0")

```

# 碎碎念

gradle难学难用是真的，也因为他要处理的问题更复杂，成了kotlin专属。

KMP作为跨平台技术，笼罩在安卓的影子下，一如eletron笼罩在前端的影子下。而且各种教程的重点往往聚焦在安卓和IOS。但私以为，KMP做桌面应用体验也不错，即使没有安卓基础，KMP也可以单独做一门简单的桌面开发技术来使用，对Javaer比较友好。