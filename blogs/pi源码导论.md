---
title: "PI源码导论"
date: "2026-09-24"
tags: "技术,工具,AI"
---

[pi仓库](https://github.com/earendil-works/pi)采用经典的monorepo（**单仓库多包）**结构。在一个Git 仓库中维护多个相互依赖、可独立发布到 npm 的 TypeScript 包 **packages**。Pi现在的产品全名是 Pi Agent Harness，目前仍处于0.xx的开发阶段（截至9/24 最新版本是0.87.1），项目结构变动较大。因此本文只聚焦核心的几个包：

- `pi-coding-agent`。接收输入、处理业务逻辑，同时也是官方的SDK。该包直接依赖`pi-agent-core`*、*`pi-ai`* 。共同组成*本文所聚焦的重点。

- `pi-agent-core` 内核负责管理 Agent 循环、消息状态和工具调用。

- `pi-ai`将统一的请求转换成 OpenAI、Anthropic、Google 等 Provider 各自的协议

# pi-agent-code 之Agent Loop

agent区别与workflow本质，在核心agent-core包中提供的Agent Loop

```typescript
 Reason（模型思考）→ Act（执行工具）→ Observe（观察结果）→ Reason（再次思考）的循环。
```

模型的输出内容决定”该调什么工具”。当**模型不再输出工具调用时，就认为本轮结束。**这种方式成为**ReAct**循环。

正常启动时，在main函数中做一些检查并初始化runtime，然后pi会进入交互模式。交互模式之做一件事，获取用户输入，并调用 session

输入的promot会依次经过

1. session.prompt(input) 将用户输入构建 messages消息列表 →_runAgentPrompt

1. _runAgentPrompt(messages)→agent.prompt(messages) 

  agent返回后执行后置处理

1. agent.prompt(messages) 将用户输入的图片放入messages→runPromptMessages

1. runPromptMessages(messages)→runWithLifecycle 并发送对应信号

1. runAgentLoop() 根据上下文和用户输入，构造本次新上下文，调用runLoop 得到newMessages

1. runLoop() agentLoop核心逻辑

## runLoop

runLoop是两层循环，每次循环称为一轮

第一轮turn会发送信号；

对于每一轮turn

# pi-code-agent

pi-code-agent是里用户最进的一层，名为main的本源函数定义在这里 `packages/coding-agent/src/main.ts`。main函数负责

- 解析参数并进入对应的模式执行，默认为 `interactiveMode` 交互模式

- 分析环境变量和配置参数、目录，初始化runtime、session

- 信任当前目录

- 错误处理和提示

# pi-ai

这一层负责处理各种协议。主流的LLM API协议是 O\的 [OpenAI Chat Completion](https://developers.openai.com/api/reference/resources/chat)  和A\的 [Anthropic Messages API](https://platform.claude.com/docs/en/api/messages)。

各家大模型提供商大都基于此，大致相似却有在细节上有出入，所谓**兼容。**因此agent工具单独抽出一层来处理不同的调用细节。