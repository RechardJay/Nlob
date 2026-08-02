---
title: "Clash Verge 全局规则"
date: "2026-08-01"
tags: "工具,避坑"
---

Verge 中有两个地方可以修改全局规则

前置用yml是覆盖订阅中同名配置。

后者用js脚本动态操作订阅。

新版Verge已经不能自动合并prepend和append；mihomo内核也并不支持这两个字段。

因此要实现合并规则，要么直接修改单个订阅；要么使用全局扩展脚本

```javascript
function main(config, profileName) {
    const customRules = [
        'GEOSITE,category-ads-all,REJECT',
        'DOMAIN-KEYWORD,ads,REJECT',
        'DOMAIN-KEYWORD,tracker,REJECT',
        'DOMAIN,gitlab.com,DIRECT',
        'DOMAIN-KEYWORD,app.notion,DIRECT',
        'DOMAIN-KEYWORD,pixiviz,DIRECT',
        'DOMAIN-KEYWORD,bobopic,DIRECT'
    ];
    // 真正把规则插入数组头部
    config.rules = [...customRules, ...config.rules];
    return config;
}
```