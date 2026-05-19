# 闲聊 AI

基于 [OpenCode Go API](https://opencode.ai) 的 Android 聊天应用。

## 功能

- 流式 AI 对话（Server-Sent Events）
- 多轮对话管理（侧边栏切换/删除）
- 思考过程展示与展开/收起
- Markdown 渲染
- 系统提示词自定义
- AI 自动生成对话标题
- 思考模式与推理强度可调
- 暗色主题
- 对话日志与导出

## 技术栈

- Java + Android SDK (minSdk 24, targetSdk 34)
- AppCompat / Material Design
- Markwon (Markdown 渲染)

## 构建

用 Android Studio 打开项目根目录，Gradle Sync 后直接 Run。

或命令行：

```bash
./gradlew assembleDebug
```

输出 APK 路径：`app/build/outputs/apk/debug/app-debug.apk`
