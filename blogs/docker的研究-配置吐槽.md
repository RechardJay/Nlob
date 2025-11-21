---
title: "Docker的研究（配置吐槽）"
date: "2025-11-19"
tags: "技术"
---

个人用户在日常桌面环境（Ubuntu24.04 LTS）使用Docker指南

# 在Ubuntu安装Docker（国内魔法版）

按照官网的教程简直不要太简单：[https://docs.docker.com/engine/install/ubuntu/#installation-methods](https://docs.docker.com/engine/install/ubuntu/#installation-methods)

1. 添加docker官方的apt仓库

  ```bash
# Add Docker's official GPG key:
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
```

1. 安装docker组件

  `sudo apt install docker-ce docker-ce-cli `[`containerd.io`](http://containerd.io/)` docker-buildx-plugin docker-compose-plugin`

  ps: docker-compose 已经转正成为官方内置的模块了，虽然该包是要单独安装，但可以直接用 `docker compose` 命令。

  装完检查：

  将当前用户添加到 docker 组（避免每次使用 sudo）

  `sudo usermod -aG docker $USER`

1. 运行

  走一个hello-world `sudo docker run hello-world` 

然而由于不可名状之物，需要一些额外的配置。如果你能流畅的走完上面的一切，就不用看了。

## apt仓库和GPG 密钥

Ubuntu 默认源没有 Docker CE以及许多组件（高情商：稳定），需要从 Docker 官方获取最新版本，因此要添加一个apt仓库。该完`etc/apt/sources.list.d`  记得 `sudo update` 。GPG 密钥则是处于安全考虑，验证我们的下载的包没问题（对个人开发者无感）。

## Docker组件

下载完不出意外我们已经拥有了：

```bash
docker-ce         - Docker 社区版引擎（守护进程）
docker-ce-cli     - Docker 命令行工具（客户端）
containerd.io     - 容器运行时（实际运行容器的组件）
docker-buildx-plugin    - 构建工具
docker-compose-plugin   - 编排工具
```

docker的架构如下：

```bash
┌─────────────────┐    UNIX Socket    ┌──────────────────┐
│  Docker CLI     │ ────────────────► │  Docker Daemon   │
│  (docker命令)    │                   │   (dockerd进程)  │
└─────────────────┘                   └──────────────────┘
                                             │
                                     ┌───────┼───────┐
                                     ▼       ▼       ▼
                               ┌─────────┐ ┌──────┐ ┌─────┐
                               │镜像管理  │ │容器管理│ │网络 │
                               └─────────┘ └──────┘ └─────┘
```

CLI即（终端）客户端负责和我们打交道，真正执行命令干活的是一个守护进程Demon ，这是引擎的核心。

用systemctl 命令检查一下，就说明docker安装好了。

## 拉镜像

安装好了，但里能用还差一步：网络。Dockerhub在国内是用不了的。两种方法：

镜像源，配置放在这里： `/etc/docker/daemon.json` 。

```bash
# 创建 Docker 配置目录（如果不存在）
sudo mkdir -p /etc/docker

# 编辑 daemon.json 配置文件
sudo tee /etc/docker/daemon.json > /dev/null <<EOF
{
  "registry-mirrors": [
    "https://docker.xuanyuan.me",
    "https://docker.1ms.run"
  ],,
  "dns": ["8.8.8.8", "8.8.4.4"]
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2"
}
EOF
```

镜像没问题，使用就没问题。

否则就上代理。但注意，Docker天然**不支持代理**。其并没有代理相关的配置。桌面环境的系统代理和终端的环境变量都不起作用，负责网络是守护进程，因此要用systemd为守护进程单独设置代理。

代理配置在这里： `/etc/systemd/system/docker.service.d/proxy.conf`

```plain text
[Service]
Environment="HTTP_PROXY=http://127.0.0.1:7890"
Environment="HTTPS_PROXY=http://127.0.0.1:7890"
```

docker守护进程拉镜像时，先找镜像仓库，在（用代理）连官方仓库。

## 桌面客户端

在apt里找不到，直接戳官方下载deb：[https://desktop.docker.com/linux/main/amd64/docker-desktop-amd64.deb?utm_source=docker&utm_medium=webreferral&utm_campaign=docs-driven-download-linux-amd64](https://desktop.docker.com/linux/main/amd64/docker-desktop-amd64.deb?utm_source=docker&utm_medium=webreferral&utm_campaign=docs-driven-download-linux-amd64)。

网络问题

Docker Desktop和系统 Docker 服务是在 Linux 上的两个选择，而且它们不互通！

桌面环境为开发者而设计，图形界面友好。它本质是一个Linux虚拟机，里面运行着一套Docker架构（核心是守护进程）。你还会发现自己多了一个docker0虚拟网卡。它是应用级别的服务：目录在`~/.docker/desktop/`

而之前安装的Docker是一个真正的实体docker，他的一切都放在系统文件中。如`/var/lib/docker/` 

如果你安装了多个docker，在终端中必须使用命令管理你的docker环境（上下文），他们是不互通的。 `docker context` 

前期建议用桌面版，更友好易于使用。用多了就让docker系统常驻，毕竟虚拟机有开销。容器和镜像也可以相互迁移。

### 切换到桌面版

用 `docker context ls` 可以看到当前的环境（目前有两个，默认是系统主机，docker-linxu是桌面版，描述栏有写）。 `docker version` 显示当前的环境。

停止系统docker的服务

切换上下文：`docker context use desktop-linux`

点开docker桌面版，OK。

### 切换到主机版

把上面那一套反过来走一边就行了。切换的主要操作集中在对主机版的启动、停止，必须通过systemd。

但操作桌面版，只要用鼠标点击就行了。desktop版的守护进程是用户级别的，其所有操作和UI同步，UI退出进程也停止，反之也同步（你守护了什么啊）。要用systemd操作时记得加 -user，他运行在在用户级。

```bash
# 启动
systemctl --user start docker-desktop
# 桌面版自启动
systemctl --user enable docker-desktop
```

简而言之：只要把系统docker停止，禁止自启动。再把cli上下文切换到桌面版。就完全迁移到桌面docker了把。就好像没有安装系统docker，但熟悉操作之后再反过来（或者直接把桌面docker卸载），就完全迁移到系统docker（移动镜像）

# 碎碎念

构造的docker,开发的时候就没考虑网络环境，人家本来就是给服务器用的。想走个代理还得用systemd改配置文件。搞个桌面版把，还拖家带口虚拟机。

Docker源于2010前后，已经是旧时代的残党了，确实有很多设计上的槽点，而且对个人用户/开发不友好。但现在虚拟/容器化的需求早已经不局限与服务器。个人（我甚至算不算上开发者），也会经常用，已然晋升为开发的基础设施。小鲸鱼,有点落后了。但好在多年来建立了庞大生态，该用还是能用。

尤其对于个人/用户，尽量Podman吧。对于生产环境，**Kubernetes + Containerd** 也是上位替代。