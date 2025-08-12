# TTSReader - Kotlin KMP跨平台文本转语音阅读器

基于最新Kotlin Multiplatform框架开发的跨平台TTS应用，支持Android和iOS设备。

## 🚀 核心功能

- **通用TTS架构**: 插件化设计，支持MiniMax及未来扩展
- **智能文本处理**: DeepSeek/OpenAI大模型驱动的文本优化
- **相机扫描**: OCR文本识别，支持多语言
- **本地缓存**: 统一音频和文本缓存系统
- **跨平台UI**: 原生Compose/ SwiftUI界面
- **配置驱动**: 运行时切换服务提供商

## 🏗️ 技术架构

### 项目结构
```
TTSReader/
├── shared/                    # 共享核心模块
│   ├── commonMain/           # KMP共享代码
│   ├── androidMain/          # Android特定实现
│   └── iosMain/              # iOS特定实现
├── androidApp/              # Android应用
└── iosApp/                  # iOS应用
```

### 核心模块
- **TTSProvider**: 通用TTS接口（MiniMax实现）
- **TextProcessor**: AI文本处理（DeepSeek/OpenAI/本地）
- **Cache**: 统一缓存管理
- **Config**: 配置管理系统
- **Database**: SQLDelight数据持久化

### 技术栈
- **共享层**: Kotlin Coroutines, Flow, Ktor, Serialization
- **Android**: Jetpack Compose, CameraX, ML Kit
- **iOS**: SwiftUI, Combine, AVFoundation, Vision
- **存储**: SQLDelight跨平台数据库

## 🎯 核心特性

### TTS提供商支持
```kotlin
// 当前支持MiniMax，可扩展其他提供商
MiniMaxTTSProvider(apiKey, groupId)
```

### AI文本处理
```kotlin
// 支持DeepSeek和OpenAI，带本地回退
DeepSeekTextProcessor(apiKey)
OpenAITextProcessor(apiKey, model = "gpt-4o-mini")
LocalTextProcessor() // 本地规则处理
```

### 缓存策略
- **内存缓存**: LRU策略，50MB限制
- **文件缓存**: 持久化存储，100MB限制
- **统一接口**: 跨平台缓存实现

## 🚀 快速开始

### 环境要求
- **Kotlin**: 2.0.20
- **Android**: API 24+
- **iOS**: iOS 14.1+

### 配置API密钥

#### Android
在`androidApp/src/main/java/com/ttsreader/android/di/AppModule.kt`中配置：

```kotlin
// 设置MiniMax配置
val ttsConfig = mapOf(
    "apiKey" to "your-minimax-api-key",
    "groupId" to "your-group-id"
)

// 设置DeepSeek配置
val aiConfig = mapOf(
    "apiKey" to "your-deepseek-api-key"
)
```

#### iOS
在iOS应用中配置：
```swift
// 通过ConfigurationManager设置
```

### 运行项目

#### Android
```bash
./gradlew androidApp:installDebug
```

#### iOS
```bash
open iosApp/iosApp.xcodeproj
# 在Xcode中构建运行
```

## 📋 开发计划

### ✅ 已完成
- [x] Kotlin KMP项目结构
- [x] 核心接口设计
- [x] MiniMax TTS Provider
- [x] AI文本处理服务
- [x] 缓存管理系统
- [x] Android基础UI
- [x] iOS基础UI
- [x] 配置管理
- [x] 数据持久化

### 🚧 待开发
- [ ] 相机扫描OCR功能
- [ ] 音频播放器集成
- [ ] 文件导入支持
- [ ] 书签系统
- [ ] 设置界面
- [ ] 主题切换
- [ ] 性能优化

## 🔧 构建配置

### Gradle构建
```bash
# 构建所有平台
./gradlew build

# 构建Android应用
./gradlew androidApp:assembleDebug

# 构建iOS框架
./gradlew shared:packForXcode
```

### 依赖关系
- **Ktor**: HTTP客户端
- **SQLDelight**: 数据库
- **Coroutines**: 异步编程
- **Serialization**: JSON序列化

## 📝 API使用示例

### 文本转语音
```kotlin
val ttsManager = TTSProviderManager(configManager, cache, httpClient)
val result = ttsManager.synthesize(
    text = "Hello, world!",
    voice = TTSVoice("male-qn-qingse", "青瑟男声", "zh-CN", TTSSpeakerGender.MALE, "minimax")
)
```

### 文本处理
```kotlin
val textService = TextProcessingService(configManager, httpClient)
val result = textService.processText(
    text = "This is a test [with brackets] and http://example.com",
    operations = listOf(
        TextOperation.FilterUrls(true),
        TextOperation.FilterBrackets(listOf(BracketType.SQUARE))
    )
)
```

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交Pull Request
4. 通过代码审查

## 📄 许可证

MIT License - 详见LICENSE文件

## 🔗 相关链接

- [MiniMax文档](https://platform.minimaxi.com/document/guides/TTS)
- [DeepSeek文档](https://platform.deepseek.com/docs)
- [Kotlin KMP文档](https://kotlinlang.org/docs/multiplatform.html)