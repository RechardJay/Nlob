---
title: "pi-agent MCP 连接 GODOT-AI"
date: "2026-08-22"
tags: "工具,AI"
---

# **在GODOT中安装godot-ai插件**

**GODOT-AI 基于UV，确保 uv 已安装**

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
which uv 
```

在项目中安装 godot-ai 插件

在项目设置中启用

配置好后在右侧面板中：

# PI

## **pi-mcp-adapter**

pi没有原生的MCP host

```c++
pi install npm:pi-mcp-adapter
```

## 配置PI MCP

在 ~/.pi/agent/mcp.json 中添加配置：

```bash
{

	"mcpServers": {
	
		"godot-ai": {
			
			"url": "http://127.0.0.1:8000/mcp",
			
			"headers": {
			
				"Accept": "application/json, text/event-stream",
				
				"Content-Type": "application/json"
				
				}
		
		}
		
	}

}

```

重启PI，即可使用