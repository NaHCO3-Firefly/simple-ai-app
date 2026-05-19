# WebSearch / WebFetch 测试结果

基于 OpenCode 源码 (webfetch.ts, websearch.ts, mcp-websearch.ts) 的实现分析。

## 测试结果

### Parallel Search ✅ 可用
- 端点: `https://search.parallel.ai/mcp`
- 协议: MCP JSON-RPC 2.0
- 认证: 无需 API Key
- 免费: 是
- 中文搜索: 支持 ("科洛缪 剧本杀" 返回了百度贴吧、知乎、搜狐等中文结果)
- 速率限制: 未遇到 (返回 HTTP 200, ~19KB)
- 结论: **推荐使用，作为默认搜索引擎**

### Exa AI Search ⚠️ 有限制
- 端点: `https://mcp.exa.ai/mcp`
- 协议: MCP JSON-RPC 2.0
- 认证: 无 Key 可用但频率限制严格 (HTTP 429)
- 免费 Key: https://dashboard.exa.ai/api-keys (注册免费获取)
- 带 Key 端点: `https://mcp.exa.ai/mcp?exaApiKey=YOUR_KEY`
- 中国可用: 待验证
- 结论: **作为备选，建议申请免费 Key 提升可用性**

### WebFetch (直接 HTTP GET) ✅ 可用
- 协议: 标准 HTTP GET
- baidu.com: HTTP 200, 78KB, 0.24s ✅
- 支持: 任意网站
- HTML 转纯文本: 正则去标签 (opencode 源码中用 htmlparser2, 我们用 Java 正则实现)

## 文件说明

- `WebTools.java` - Android 可直接使用的搜索/抓取工具类
  - `searchParallel(query, callback)` - Parallel 搜索
  - `searchExa(query, numResults, apiKey, callback)` - Exa 搜索
  - `fetchUrl(urlStr, maxBytes, callback)` - 网页抓取

## 接入方式

将 `WebTools.java` 放入项目 `app/src/main/java/net/archie/ai/`，
然后在 MainActivity 中调用即可。不需要额外依赖。
