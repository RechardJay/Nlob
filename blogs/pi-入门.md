---
title: "PI 入门"
date: "2026-08-22"
tags: "工具,AI"
---

[https://pi.dev/](https://pi.dev/)

PI 是一个极简的agent框架，绝对的开放和自由，高度定制化。PI可以被改造为独属于你的agent工具。

# 安装

bash脚本安装。脚本会自动安装nodejs并通过npm安装程序本体 @earendil-works/pi-coding-agent 

```bash
# 直接执行脚本即可
curl -fsSL https://pi.dev/install.sh | sh

```

# .pi 配置模型/供应商

## 内置供应商

进入pi /login ，选择供应商输入key即可

## 自定义models

编辑   `~/.pi/agent/models.json` 

```bash
{
  "providers": {
    "agens": {
      "baseUrl": "https://apihub.agnes-ai.com/v1",
      "api": "openai-completions",
      "apiKey": "xxx",
      "models": [
        {
          "id": "agnes-2.0-flash",
          "name": "agnes-2.0-flash",
          "reasoning": true,
          "input": ["text"],
          "contextWindow": 512000,
          "maxTokens": 65500,
          "cost": { "input": 0, "output": 0, "cacheRead": 0, "cacheWrite": 0 }
        }
  }
}

```

# pi 交互式对话

完整使用说明：[https://pi.dev/docs/latest/usage](https://pi.dev/docs/latest/usage)

## 常用的快捷键

shift + tab 更改思考等级

/reumse 可以恢复对话（session）。 选中对话后 ctrl-D 删除本条session。