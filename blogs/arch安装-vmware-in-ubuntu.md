---
title: "Arch安装 （VMWare in Ubuntu）"
date: "2025-11-20"
tags: "技术,Linux,避坑"
---

前言：折腾了一下午总算成功了，装系统确实不是什么难事（只要愿意花时间）。相关的教程很多（Arch的一大特色），但不同的环境、已经受限于个人经验，还是会踩坑。这也是个人记录的意义所在，总有人和你曾经的处境相同。

# 准备

假设：你能从vim退出，会运行bash脚本，网络很棒。

## WMWare

安装环境：虚拟机 VMWare，实体机有变砖风险，小白勿试、老鸟无视。

附一个WMWare下载地址：[https://github.com/skrik2/VM-download](https://github.com/skrik2/VM-download)。 我用的Linux（Ubuntu）版，下载后执行bundle文件即可。Win和Linux的操作逻辑完全相同，遗憾的时Linux下的WMWare似乎无法设置中文（我不会，安装win下相同的操作该配置文件无效）。

唯一的环境就是一台VMWare

## ISO

下载Arch的安装映像，这是一个用来安装操作系统的操作系统，表现为一个ISO文件。~~ 如果你有一个专门的启动U盘，放一个Arch的ISO就可以做运维 ~~ 从archwiki下载：[https://archlinux.org/download/](https://archlinux.org/download/) →

[https://mirrors.aliyun.com/archlinux/iso/2025.11.01/archlinux-x86_64.iso](https://mirrors.aliyun.com/archlinux/iso/2025.11.01/archlinux-x86_64.iso)

下一个iso即可，sig是验证文件我们用不到。

# 创建虚拟机

类VMM中创建虚拟机，选custom自己配置参数。指定iso的位置。VMW不认识Arch，选择其他64位Linux即可，不影响。

然后起一个名字，设置虚拟机文件的存储位置。记好这个位置。VMW的虚拟机文件可以无障碍迁移。

然后就是配置硬件，常见的小型web服务区，4核2G用不完。对于我们研究学习更是绰绰有余。但本着宽松原则，我设置4x2(4个2核)，内存4G。

网络模式选择NAT，理解为宿主机开热点，虚拟机连宿主wifi。

IO控制器类型随便，按推荐用LSI logic（似乎没影响）。

**重要**:磁盘类型用**SATA**！！！因为VM的固件EFI下Arch用不了SCSI，开机直接panic。BISO可以识别SCSI，不建议用BIOS（BIOS过时淘汰倒是其次，主要是我不会，现在的电脑几乎见也不到BIOS）。

选择创建新的虚拟硬盘。硬盘大小20G（够用）。下面保持默认：“将硬盘拆分为多个文件”，多个适度大小的文件对操作系统和物理硬盘更友好。

最终配置

但注意把上面的立刻开机关掉，创建完先设置→选项→高级中，将固件类型设置为UEFI，默认是BIOS。save后开机。

# Arch 启动！

## 开机

开机后直接点击确定（安装Arch UEFI）。加载完后进入终端，live环境就准备好了。注意现在我们已经在一个操作系统里的，但是在U盘（ISO文件）的系统中，我们的目标是将一个操作系统和引导程序写入到磁盘里。

屏幕太小，在控制bar的view视图中将autosize设置为拉伸strech，全屏幕后勉强能看清。

第一件事是设置root用户密码，然后看一下网络。

```bash
passwd
## 输入两次密码
## 测试网络
ping baidu.com
## 看一下ip
ip a 
## 记住ip
```

设置密码和看ip都是为了能连接ssh。这远古终端，不能复制粘贴就算了，盯着这分辨率眼睛早瞎了。

正常情况下网络没问题，因为NAT模式是直接有线连接宿主机，ip应该显示两个网络接口lo127是本地回环，第二个ens就是有线网卡的三级子网地址，用这个和主机通信，我的是176.16.155.131。

然后就不同管机器了，让它在后台跑着吧。来到我们的终端 `ssh root@172.16.155.131` 注意换成你的ip。输入yes信任该主机，输入密码。

## **确认启动方式**

```bash
cat /sys/firmware/efi/fw_platform_size
```

正常情况输出应该是 `64` ，说明是64位UEFI。如果是32说明是32位EFI，取决于的CPU。没有这个目录说明是BIOS，VMWare没设置好。

## **更新系统时钟**

```bash
timedatectl set-ntp true # 将系统时间与网络时间进行同步
timedatectl status # 查看系统时间
```

## **更换国内软件仓库镜像源**

```bash
vim /etc/pacman.d/mirrorlist
```

在开头写入

```plain text
Server = https://mirrors.tuna.tsinghua.edu.cn/archlinux/$repo/os/$arch
Server = https://mirrors.bfsu.edu.cn/archlinux/$repo/os/$arch
Server = https://mirrors.ustc.edu.cn/archlinux/$repo/os/$arch
Server = https://mirrors.zju.edu.cn/archlinux/$repo/os/$arch
```

更新pacman索引，要不然下的是旧软件

```bash
pacman -Sy
```

## **磁盘分区**

我们使用**fdisk命令为磁盘分区分区。 **UEFI引导需要一个efi系统分区，放在磁盘的开头第一个分区。Linux系统需要至少一个分区；我们再来一个swap，用磁盘作为备用内存，大概设置为物理内存的50%，多少一点都无所谓，但不要太多会加重CPU负担，我们设置2G。我们这一块磁盘分三个区：

第一个区是EFI，512MB。第二个是swap交互分区，2G。剩下大约18G作为系统分区（Linux文件系统）。

使用 `lsblk` 可以看到我们的磁盘设备。他们都在/dev目录下。

我们用SATA介入的磁盘就是sd+字母，只有一块磁盘是sda。 sr0是用CD/ROM介入的光盘映像，不用动它。loop0是我们内存中的操作系统。

```bash
lsblk 
##
#NAME  MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS
#loop0   7:0    0 961.5M  1 loop /run/archiso/airootfs
#sda     8:0    0    20G  0 disk 
#sr0    11:0    1   1.4G  0 rom  /run/archiso/bootmnt
```

进入fdisk `fdisk /dev/sda` 。

具体操作方法前人说的很详细，看文献：[https://lingxi9374.github.io/posts/教程/archinst/#八-检查磁盘分区情况](https://lingxi9374.github.io/posts/%E6%95%99%E7%A8%8B/archinst/#%E5%85%AB-%E6%A3%80%E6%9F%A5%E7%A3%81%E7%9B%98%E5%88%86%E5%8C%BA%E6%83%85%E5%86%B5)

结论是依次按下：

```bash
g # 创建GPT分区表
n # 创建一块新的分区
enter # 分区号 默认1
enter# 开始扇区位置 默认2048
+512MB # 结束扇区位置
t # 分区类型 
1 # 1 是 EFI 系统分区
---
n
enter
enter
+2G
t
enter # 分区编号 默认2（第一个默认编号的分区） 
82 # 82 是交换区
---
n
enter
enter
enter
t
enter
83 # 83是更文件系统
--
p
w
```

格式化三个分区：

```bash
mkfs.vfat -F 32 -n boot /dev/sda1 # EFI分区格式化为FAT32
mkswap -L swap /dev/sda2 # 格式化为交换区
mkfs.ext4 -L Arch /dev/sda3 ## 格式化为ext4文件系统
```

挂载分区

```bash
mount /dev/sda3 /mnt
mkdir /mnt/efi
mount /dev/sda1 /mnt/efi
swapon /dev/sda2
```

## 安装系统

现在你有了一个纯净，开垦好的的磁盘。里面要装什么系统和软件全取决与你。

```bash
pacstrap /mnt base base-devel linux linux-firmware linux-zen linux-headers linux-zen-headers vim sudo grub efibootmgr networkmanager intel-ucode
```

挂载信息

```bash
 genfstab -U /mnt >> /mnt/etc/fstab
```

## 切换到Arch

切换到新系统

```bash
arch-chroot /mnt /bin/bash
```

## **新系统的基本配置**

```bash
timedatectl set-ntp true # 启用网络时间同步
ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime # 设置时区为上海
hwclock --systohc # 将系统时间写入硬件时钟
```

```bash
 echo 'ArchM' > /etc/hostname # 设置主机名
```

编辑hosts

```bash
vim /etc/hosts
127.0.0.1   localhost
::1         localhost
127.0.1.1   ArchM.localdomain ArchM
```

```bash
 vim /etc/locale.gen
 
locale-gen
echo LANG=en_US.UTF-8 > /etc/locale.conf
```

创建用户，设置密码

```bash
useradd -m -g users -G wheel name
passwd name
passwd root
```

sudo权限

```bash
EDITOR=vim visudo
```

```bash
systemctl enable NetworkManager.service
```

CPU

```bash
pacman -S intel-ucode
```

## 安装引导

```bash
pacman -S grub efibootmgr os-prober
grub-install --target=x86_64-efi --efi-directory=/efi --bootloader-id=GRUB --removable
```

说明：

-target 指定目标平台为x86_64-efi

-efi-directory 指定EFI分区的路径

-bootloader-id 指定引导程序的ID

-removable 选项表示安装到可移动设备上，可不选

```bash
vim /etc/default/grub

##将"GRUB_TIMEOUT=5"修改为"GRUB_TIMEOUT=30"；
##将"GRUB_CMDLINE_LINUX_DEFAULT="后面的参数修改为"loglevel=5 nowatchdog"。

grub-mkconfig -o /boot/grub/grub.cfg
```

## **重启系统**

```bash
exit # 或者键入Ctrl+D
swapoff /dev/sda2
umount -R /mnt
reboot
```

开机后进入Arch！

走一个fastfetch吧：

`sudo pacman -S fastfetch && fastfetch`

# 参考文献

[https://lingxi9374.github.io/posts/%E6%95%99%E7%A8%8B/archinst/#%E4%BA%8C-%E7%A6%81%E7%94%A8%E9%9D%9E%E5%BF%85%E8%A6%81%E7%9A%84%E6%9C%8D%E5%8A%A1%E5%92%8Cmodule](https://lingxi9374.github.io/posts/%E6%95%99%E7%A8%8B/archinst/#%E4%BA%8C-%E7%A6%81%E7%94%A8%E9%9D%9E%E5%BF%85%E8%A6%81%E7%9A%84%E6%9C%8D%E5%8A%A1%E5%92%8Cmodule)

[https://archlinux.org](https://archlinux.org/)