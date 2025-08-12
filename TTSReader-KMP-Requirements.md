# TTSReader - Kotlin KMP跨平台需求文档

## 项目概述
基于最新的Kotlin Multiplatform (KMP)框架，开发支持Android和iOS的文本转语音阅读器应用。

## 核心功能需求

### 1. 相机扫描仪 (Camera Scanner)
- **功能描述**: 使用移动设备摄像头扫描实体书籍和笔记，将物理文本转换为音频
- **技术要求**:
  - 使用CameraX (Android) / AVFoundation (iOS) 实现高质量图像捕获
  - 集成ML Kit (Android) / Vision Framework (iOS) 进行OCR文本识别
  - 支持多语言OCR识别
  - 图像预处理：自动裁剪、增强、旋转校正
  - 批量扫描处理，支持多页文档

### 2. 自然语音引擎 (Natural Voices)
- **功能描述**: 基于插件化架构的通用TTS服务，支持MiniMax及未来扩展
- **技术要求**:
  - **抽象接口设计**: 定义通用TTS Provider接口，支持多平台无缝切换
  - **当前实现**: MiniMax Provider (speech-2.5-hd-preview模型)
  - **通用参数模型**: 标准化语音参数，支持speed、volume、pitch、emotion
  - **音频格式**: 支持MP3、WAV、AAC等格式自适应
  - **缓存策略**: 统一音频缓存层，支持LRU和自定义过期策略
  - **容错机制**: 自动降级策略，支持多Provider备份
  - **配置驱动**: 通过配置文件切换TTS服务提供商

### 3. AI文本过滤 (AI Text Filtering)
- **功能描述**: 基于大语言模型的智能文本过滤和优化系统
- **技术要求**:
  - **通用AI Provider架构**: 支持DeepSeek/OpenAI API的插件化集成
  - **当前实现**: DeepSeek Chat API (deepseek-chat模型) 或 OpenAI GPT-4o
  - **智能过滤**: URL移除、括号内容清理、页眉页脚识别
  - **文本优化**: 语义分段、长句拆分、专有名词处理
  - **用户规则**: 支持自定义过滤规则和例外列表
  - **实时预览**: 过滤前后文本对比显示
  - **批量处理**: 支持大文本的流式处理
  - **容错机制**: 网络失败后本地规则降级处理

### 4. 可定制体验 (Customizable Experience)
- **功能描述**: 提供个性化阅读体验
- **技术要求**:
  - **播放控制**: 播放/暂停、快进/快退、章节跳转
  - **语音设置**: 语速调节(0.5x-3.0x)、音调调节、音量控制
  - **视觉设置**: 深色/浅色主题、字体大小、单词高亮
  - **字幕显示**: 实时字幕、滚动字幕、全屏字幕模式
  - **发音编辑器**: 自定义词汇发音、词典管理
  - **书签功能**: 添加/管理书签、阅读进度保存

### 5. 跨设备兼容 (Cross-Device Compatibility)
- **功能描述**: 支持多设备同步
- **技术要求**:
  - 用户账户系统（邮箱/社交登录）
  - 云端同步阅读进度、书签、设置
  - 支持Web端和Chrome扩展
  - 数据加密传输和存储

## 支持的文件格式
- **文档**: PDF, DOC, DOCX, PPT, PPTX, RTF, TXT
- **电子书**: EPUB (无DRM)
- **图像**: PNG, JPG, JPEG, BMP, TIFF
- **网页**: HTML内容解析

## Kotlin KMP架构设计

### 核心模块架构
```
ttsreader/
├── shared/                    # 共享代码模块
│   ├── commonMain/           # 通用业务逻辑
│   │   ├── data/            # 数据层
│   │   │   ├── model/       # 数据模型
│   │   │   ├── repository/  # 数据仓库
│   │   │   └── api/         # API接口
│   │   ├── domain/          # 领域层
│   │   │   ├── usecase/     # 用例
│   │   │   └── entity/      # 实体
│   │   ├── presentation/    # 表示层
│   │   │   ├── viewmodel/   # 共享ViewModel
│   │   │   └── state/       # UI状态
│   │   └── utils/           # 工具类
│   ├── androidMain/         # Android特定实现
│   └── iosMain/             # iOS特定实现
├── androidApp/              # Android应用
└── iosApp/                  # iOS应用
```

### 技术栈
- **共享层**: Kotlin Coroutines, Flow, Serialization, Ktor, SQLDelight
- **网络**: Ktor Client with OkHttp (Android) / NSURLSession (iOS)
- **音频**: ExoPlayer (Android) / AVPlayer (iOS) 用于MiniMax音频播放
- **Android**: Jetpack Compose, CameraX, ML Kit
- **iOS**: SwiftUI, Combine, AVFoundation, Vision Framework

### 通用TTS架构设计
```kotlin
// 核心抽象接口
typealias TTSAudioData = ByteArray

interface TTSProvider {
    val providerName: String
    suspend fun synthesize(request: TTSRequest): Result<TTSAudioData>
    suspend fun getAvailableVoices(): Result<List<TTSVoice>>
    fun isConfigured(): Boolean
}

// 通用请求模型
@Serializable
data class TTSRequest(
    val text: String,
    val voice: TTSVoice,
    val parameters: TTSParameters = TTSParameters(),
    val format: TTSFormat = TTSFormat.MP3_32KHZ
)

@Serializable
data class TTSVoice(
    val id: String,
    val name: String,
    val language: String,
    val gender: TTSSpeakerGender,
    val provider: String
)

@Serializable
data class TTSParameters(
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Float = 0.0f,
    val emotion: String? = null,
    val pronunciationDict: Map<String, String>? = null
)

enum class TTSFormat {
    MP3_32KHZ, MP3_48KHZ, WAV_16KHZ, AAC_44KHZ
}

enum class TTSSpeakerGender { MALE, FEMALE, NEUTRAL }

// MiniMax Provider实现
class MiniMaxTTSProvider(
    private val apiKey: String,
    private val groupId: String
) : TTSProvider {
    override val providerName: String = "minimax"
    
    override suspend fun synthesize(request: TTSRequest): Result<TTSAudioData> {
        // MiniMax specific implementation
    }
    
    override suspend fun getAvailableVoices(): Result<List<TTSVoice>> {
        // Return MiniMax voice catalog
    }
    
    override fun isConfigured(): Boolean = apiKey.isNotBlank()
}

// Provider工厂和配置
object TTSProviderFactory {
    fun create(config: TTSConfig): TTSProvider = when (config.provider) {
        "minimax" -> MiniMaxTTSProvider(config.apiKey, config.groupId)
        "google" -> GoogleTTSProvider(config.apiKey)
        "amazon" -> AmazonPollyProvider(config.apiKey, config.secretKey)
        else -> throw IllegalArgumentException("Unsupported provider: ${config.provider}")
    }
}

// 配置驱动的Provider管理
@Serializable
data class TTSConfig(
    val provider: String,
    val apiKey: String,
    val groupId: String? = null,
    val fallbackProviders: List<String> = emptyList()
)

// 统一音频缓存层
interface TTSCache {
    suspend fun get(key: String): TTSAudioData?
    suspend fun put(key: String, audio: TTSAudioData)
    suspend fun clear()
    suspend fun getCacheSize(): Long
}

### 通用AI文本处理架构
```kotlin
// 核心抽象接口
interface TextProcessor {
    val processorName: String
    suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult>
    suspend fun getCapabilities(): Set<TextProcessingCapability>
}

// 通用请求模型
@Serializable
data class TextProcessingRequest(
    val text: String,
    val operations: List<TextOperation>,
    val context: ProcessingContext = ProcessingContext()
)

@Serializable
data class TextProcessingResult(
    val processedText: String,
    val changes: List<TextChange>,
    val metadata: ProcessingMetadata
)

@Serializable
sealed class TextOperation {
    @Serializable
    data class FilterUrls(val remove: Boolean = true) : TextOperation()
    @Serializable
    data class FilterBrackets(val types: List<BracketType>) : TextOperation()
    @Serializable
    data class RemoveHeaders(val patterns: List<String>) : TextOperation()
    @Serializable
    data class SemanticSegment(val maxLength: Int) : TextOperation()
    @Serializable
    data class OptimizePronunciation(val customDict: Map<String, String>) : TextOperation()
}

enum class BracketType { ROUND, SQUARE, CURLY, ANGLE }

// DeepSeek Provider实现
class DeepSeekTextProcessor(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com"
) : TextProcessor {
    override val processorName: String = "deepseek"
    
    override suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult> {
        val prompt = buildProcessingPrompt(request)
        return callDeepSeekAPI(prompt, request.text)
    }
    
    override suspend fun getCapabilities(): Set<TextProcessingCapability> = setOf(
        TextProcessingCapability.FILTERING,
        TextProcessingCapability.SEMANTIC_ANALYSIS,
        TextProcessingCapability.CONTENT_OPTIMIZATION
    )
}

// OpenAI Provider实现
class OpenAITextProcessor(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : TextProcessor {
    override val processorName: String = "openai"
    
    override suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult> {
        val messages = buildOpenAIMessages(request)
        return callOpenAIAPI(messages)
    }
    
    override suspend fun getCapabilities(): Set<TextProcessingCapability> = setOf(
        TextProcessingCapability.FILTERING,
        TextProcessingCapability.SEMANTIC_ANALYSIS,
        TextProcessingCapability.CONTENT_OPTIMIZATION
    )
}

// 配置驱动的文本处理器管理
object TextProcessorFactory {
    fun create(config: AIConfig): TextProcessor = when (config.provider) {
        "deepseek" -> DeepSeekTextProcessor(config.apiKey)
        "openai" -> OpenAITextProcessor(config.apiKey, config.model ?: "gpt-4o-mini")
        "local" -> LocalTextProcessor() // 本地规则降级
        else -> throw IllegalArgumentException("Unsupported AI provider: ${config.provider}")
    }
}

@Serializable
data class AIConfig(
    val provider: String,
    val apiKey: String,
    val model: String? = null,
    val fallbackRules: Boolean = true
)

// 统一文本处理服务
class TextProcessingService(
    private val processor: TextProcessor,
    private val fallbackProcessor: TextProcessor? = null
) {
    suspend fun processText(
        text: String,
        operations: List<TextOperation>
    ): Result<TextProcessingResult> {
        return processor.process(TextProcessingRequest(text, operations))
            .recoverCatching { error ->
                if (fallbackProcessor != null && error is NetworkError) {
                    fallbackProcessor.process(TextProcessingRequest(text, operations)).getOrThrow()
                } else {
                    throw error
                }
            }
    }
}
```

### 数据流架构
### 完整系统架构
```
UI Layer (Compose/SwiftUI)
    ↓
ViewModel (shared)
    ├─ TTSViewModel
    ├─ TextProcessingViewModel
    └─ SettingsViewModel
    ↓
Service Layer (shared)
    ├─ TTSProviderManager
    │   ├─ MiniMaxTTSProvider
    │   ├─ GoogleTTSProvider (未来)
    │   └─ AmazonPollyProvider (未来)
    ├─ TextProcessingService
    │   ├─ DeepSeekTextProcessor
    │   ├─ OpenAITextProcessor
    │   └─ LocalTextProcessor (降级)
    ├─ Cache Manager (TTSCache + TextCache)
    ├─ File Manager
    └─ Settings Manager
    ↓
Data Layer (shared)
    ├─ Repository (TTSRepo, TextRepo, SettingsRepo)
    ├─ Local Data (SQLDelight)
    ├─ Remote Data (Ktor Client)
    └─ Platform Adapters
    ↓
Platform Layer (Android/iOS)
    ├─ Camera/OCR (CameraX/AVFoundation)
    ├─ Audio System (ExoPlayer/AVPlayer)
    └─ File System
```

### Provider配置管理
```kotlin
// 应用级配置
interface TTSConfigurationManager {
    suspend fun getCurrentProvider(): String
    suspend fun switchProvider(provider: String): Result<Unit>
    suspend fun getAvailableProviders(): List<String>
    suspend fun validateConfiguration(provider: String, config: Map<String, String>): Boolean
}

// 运行时Provider管理
class TTSProviderManager(
    private val cache: TTSCache,
    private val config: TTSConfigurationManager
) {
    private val providers = mutableMapOf<String, TTSProvider>()
    
    suspend fun synthesize(text: String, voice: TTSVoice): Result<TTSAudioData> {
        val cacheKey = generateCacheKey(text, voice)
        
        // 检查缓存
        cache.get(cacheKey)?.let { return Result.success(it) }
        
        // 获取当前Provider
        val provider = getCurrentProvider()
        
        // 尝试合成，失败后回退
        return trySynthesizeWithFallback(provider, text, voice)
            .onSuccess { audio -> cache.put(cacheKey, audio) }
    }
}

## 开发优先级

### 第一阶段 (MVP)
1. 基础项目架构搭建
2. 文本转语音核心功能
3. 基础文件支持 (TXT, PDF)
4. 基础播放控制

### 第二阶段
1. 相机扫描OCR功能
2. 多语言语音支持
3. 用户设置和个性化
4. 书签和进度保存

### 第三阶段
1. AI文本过滤
2. 高级语音功能 (Plus语音)
3. 跨设备同步
4. 高级文件格式支持

## 性能要求
- 冷启动时间 < 2秒
- 语音响应延迟 < 500ms
- OCR处理时间 < 3秒/页
- 支持后台播放
- 内存占用 < 200MB

## 安全和隐私
- 用户数据本地加密存储
- 网络传输使用HTTPS/TLS
- OCR图像处理后立即删除
- 遵守GDPR隐私规范

## 测试策略
- 单元测试覆盖率 > 80%
- 集成测试关键用户路径
- UI自动化测试
- 性能基准测试
- 设备兼容性测试

## 部署和发布
- CI/CD使用GitHub Actions
- 自动化测试和构建
- Google Play Console集成
- App Store Connect集成
- 灰度发布策略

## 后续扩展
- Wear OS和watchOS支持
- 桌面端应用 (Windows/macOS)
- 语音转文字功能
- 多人协作朗读
- AI内容摘要