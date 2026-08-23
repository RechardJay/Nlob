---
title: "PI-Skills"
date: "2026-08-22"
tags: "工具,AI"
---

skills，说白了就是一堆提示词。高级一点就是经过别人（或者你自己）验证过的，好用的能明确在某些场景下优化大模型输出减少幻觉的提示词。毕竟每次手动tpye的提示词不可能面面俱到。skills因此是使用和维护成本最低、最通用的harness。

# Skill 结构

skills的原理很简单，把文字内容塞进大模型的上下文，完事。

简单点的skill，一个md文件就够了。

```bash
my-skill/**
└── **SKILL.md # 必需：frontmatter + 指令
```

高级一点会带一些辅助资源

```bash
my-skill/
├── SKILL.md              
├── scripts/              # 辅助脚本
│   └── process.sh
├── references/           # 按需加载的详细文档
│   └── api-reference.md
└── assets/
    └── template.json
```

# 安装Skills

**网络上的的Skill 仓库，下载skill放到 .pi的skill目录 **`~/.pi/agent/skills/`

现在的skills已经泛滥了，很容易找到各种skill市场、仓库，要注意甄别质量。这里是PI官方推荐的仓库

SKill市场也会提供了安装提示词，直接把提示词告诉AI让，让其安装这个skill。

等用的多了，你可以自己编写skill(要符合特定格式)，或者让pi帮你将某次的经验复盘成skill

## 其他skill路径

pi 也会自动扫描全局路径：

项目路径：.pi/skills 和 .agents/skills/

包括package和在设置setting.json中自定义目录中的skills

```bash
 "skills": ["path/to/skills"] 
```

# 推荐一些Skills

web-accsee：零配置让PI拥有联网搜索的能力

# 使用方法

已经发现的skill，会显示在 /skill 指令中。

## 预设的提示/工作流setup

手动 输入 /skill:name 命令，回车直接把整个skill作为用户提示发给大模型，让agent执行一遍该SKILL。

## 给agent的提示/技能

默认，PI将每个已发现的skill的description放入上下文，当每次指令符合某个skill的description时该skill会自动调用。

# 最佳实践

我们自然是希望模型越强，技能越多越好，因此会装大量SKILL。但实际情况是，agents很难判断什么时候该自动调用SKILL，以及调用什么SKILL，即使你的description已经很详细。大量SKILL除了白白占用上下文，平常几乎发挥不了作用。我们暂时不能指望安装某个SKILL，agent就变成了某个领域的专家。目前，SKILL更像是给驾驶员准备的。

为了不成为SKILL屯屯鼠，我建议：

## 减少全局SKILL

请你想清楚哪些SKILL是必要的，否则实在不必放入全局目录。大部分SKILL可能只是为了某些甚至某个特定的任务/项目，或者仅仅是为了尝鲜。此时以项目级安装SKILL是个不错的选择。直到你发现某个SKILL确实高频使用，作为全局安装也不迟。只需要把SKILL目录 从 `.pi/agent/skill/` 移动到 `~/.pi/agent/skill/`

## 关闭自动触发

当skill的formatter指定了：`disable-model-invocation: true`

他不会被自动放入大模型的上下文。你必须使用 /skill目录强制模型读取。

当你觉得某些SKILL暂时用不到时，可以启用这个选项。

记得使用 pi-toggle-skills 扩展。 安装这个包 `pi install npm:pi-toggle-skills` 

PS:关闭自动触发的SKILL仍会出现在你的/skill 命令中供你使用。如果你实现不想看到某些SKILL又不想删除他们，考虑：

## 禁用SKILL

编辑 setting.json 可以高度定制化skill的发现行为

```bash
{
	"skills":[
		"~/.claude/skills" # 加载其他目录中的skill
		"-skills/xxx/SKILL.md", # 禁用默认目录中skill 
	]
}
```

除了发现非默认目录中的skiil，也可以禁用默认目录中的某些skill、甚至某个目录。

被 `-` 禁用SKILL，不会出现在 pi 的skill/指令所。

## Package 附带的Skill

同样可以在setting.json中配置。这部分我们将在Package中详细说明。大部分时候是不用改的。