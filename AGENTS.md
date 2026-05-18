# 闲聊AI - Android 开发 Agent 必读

## 项目概况

- **包名**: `net.archie.ai`
- **最低安卓版本**: 7 (API 24)
- **架构**: armv7 兼容 (纯 Java, 无 native 依赖)
- **目标仓库**: https://github.com/NaHCO3-Firefly/simple-ai-app
- **分支**: main

## 已完成

- [x] Android 项目骨架 (Gradle 8.4, AGP 8.1.4)
- [x] Message 模型 (user/ai 双类型, 含 thinkingContent/thinkingExpanded)
- [x] AiResponse 模型 (content + thinking)
- [x] OpenCodeApi — 纯 HttpURLConnection 网络层, 无第三方依赖
  - [x] 拉取模型列表 `GET /zen/go/v1/models`
  - [x] 发送消息 `POST /zen/go/v1/chat/completions`
  - [x] 支持 reasoning_effort 参数 (low/medium/high)
  - [x] 解析 reasoning_content 思考过程
- [x] ChatAdapter — RecyclerView 适配器, AI消息可折叠展示思考过程
- [x] Conversation 模型 + ConversationStore (SharedPreferences JSON 持久化)
- [x] 多会话侧栏 (DrawerLayout + ListView)
  - [x] 新建/切换/删除对话
  - [x] 自动保存当前对话
  - [x] 对话标题自动从首条用户消息生成
- [x] MainActivity — 主聊天界面
  - [x] 小顶栏 (44dp) + 汉堡菜单打开侧栏
  - [x] 模型选择按钮 (弹出对话框: 模型下拉+刷新, 思考开关, 思考程度)
  - [x] 设置按钮 → SettingsActivity
  - [x] 小输入框初始高度 (minHeight=36dp)
- [x] SettingsActivity — 精简设置页
  - [x] API Key 输入 + 保存
  - [x] 对话日志查看 (Logger: SharedPreferences 存储)
  - [x] 清除日志
- [x] Prefs — SharedPreferences 封装 (API Key, Model, Thinking, Effort, CachedModels)
- [x] Logger — 简易日志工具
- [x] 资源文件 (colors, strings, themes, drawables, menus, layouts)

## 待完成

- [ ] 思考模式的 icon 需要换成更合适的 (目前用 ic_chat_add 临时替代)
- [ ] 对话删除时添加确认提示改进
- [ ] 输入框多行自动增高优化
- [ ] RecyclerView 平滑滚动到底部优化
- [ ] 错误处理增强 (网络超时、API 错误提示)
- [ ] 可选: 导出对话为文件
- [ ] 可选: 暗色主题

## 文件结构

```
app/src/main/java/net/archie/ai/
├── AiResponse.java          # AI 响应模型 (content + thinking)
├── ChatAdapter.java         # RecyclerView 适配器
├── Conversation.java        # 对话模型 + JSON 序列化
├── ConversationStore.java   # 对话本地持久化
├── Logger.java              # 日志工具
├── MainActivity.java        # 主界面 (DrawerLayout + 多会话)
├── Message.java             # 消息模型
├── OpenCodeApi.java         # API 网络请求
├── Prefs.java               # SharedPreferences 封装
└── SettingsActivity.java    # 设置页 (API Key + 日志)

app/src/main/res/
├── drawable/                # 矢量图标和背景
├── layout/
│   ├── activity_main.xml     # DrawerLayout + Toolbar + RecyclerView + Input
│   ├── activity_settings.xml # API Key + 日志查看
│   ├── drawer_item_conversation.xml
│   ├── item_message_ai.xml   # AI 消息 (含思考折叠区)
│   └── item_message_user.xml # 用户消息
├── menu/main_menu.xml       # 顶栏菜单 (模型按钮 + 设置按钮)
└── values/                  # colors, strings, themes
```

## 关键设计决策

1. **零第三方网络库** — 纯 `HttpURLConnection`, 避免 armv7 native 兼容问题
2. **对话存储** — SharedPreferences + JSON, 无需 Room 数据库, 无权限申请
3. **异步处理** — Executors.newSingleThreadExecutor() + Handler(Looper.getMainLooper())
4. **思考模式** — reasoning_effort 参数发送, reasoning_content 解析, UI 可折叠

## 每次提交后必读

- 确保 `git push origin main` 在每次代码改动后执行
- 更新此文件的状态标记
