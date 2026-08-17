# 识浮游 — Android 项目

浮游生物识别训练 App（翻卡学习）。Kotlin + Jetpack Compose + Material3。

---

## 快速构建

```bash
cd /home/teacat/Desktop/RhodesIsland/Skadi/识浮游

# Debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 清除构建缓存
./gradlew clean

# 产物路径
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 架构概览

```
MainActivity                 ← 入口，AlgaeTheme + TrainViewModel
  └─ MainScreen              ← Scaffold + 3 标签页
       ├─ TrainContent       ← 训练页：模式切换 + 统计栏 + 进度条 + 闪卡
       ├─ ErrorBookContent   ← 错题集：模式切换 + 列表 + 详情查看
       └─ AboutContent       ← 关于：玩法说明 + 版本信息 + 版权
```

### 数据流

```
assets/algae_data.json       ← 静态数据（浮游植物 629 图）
assets/zooplankton_data.json ← 静态数据（浮游动物 146 图）
assets/images/*.jpg          ← 浮游植物图片
assets/zooplankton/*.jpg     ← 浮游动物图片

TrainViewModel               ← 状态管理器（唯一 ViewModel）
  ├── loadData()             ← 从 assets 加载 JSON
  ├── parseItems()           ← 解析 + 回填 isKnown()
  ├── SharedPreferences      ← 训练进度持久化
  │   ├── k_<id>             ← 是否已掌握（Boolean）
  │   ├── e_<id>             ← 错误次数（Int）
  │   └── remaining_ids      ← 当前剩余卡组（JSONArray）
  ├── buildPool()            ← 按过滤器构建卡池
  ├── mark(known)            ← 标记答案 + 更新剩余
  ├── undo()                 ← 撤销上一步
  ├── restart()              ← 重置所有进度
  └── errorBookEntries()     ← 错题列表（按错误次数倒序）
```

---

## 核心约定

### 包名与命名

- 包名 `com.rhodes.algae`（暂未改为 `com.rhodes.plankton`，若重构需同步 `build.gradle.kts` 的 `namespace` 和 `applicationId`）
- 类名：`AlgaeItem`（虽名 algae，实际同时用于浮游动物 zooplankton，是同一数据模型）
- ViewModel：`TrainViewModel` 管理全部状态，单一实例，不拆分

### 模式机制

`currentMode` 两个值：
- `"algae"` —— 浮游植物，图片前缀 `images/`，SharedPreferences 键 `algae_train`
- `"zooplankton"` —— 浮游动物，图片前缀 `zooplankton/`，SharedPreferences 键 `zoo_train`

### 主题

Material3 + lightColorScheme：
- `primary` `#3A7C6B`（墨绿）— TopAppBar、进度条、属名
- `secondary` `#D4A853`（金色）— 强调色
- `surface` `#FFFCF5` / `background` `#F5F0E8`（暖白米色）

### 进度持久化

SharedPreferences（不用 Room/DataStore，避免引入依赖）：
- `k_<id>` — 已掌握标记
- `e_<id>` — 错误计数
- `remaining_ids` — 持久化剩余卡组，实现断点续练

---

## 修改指南

### 添加新数据

1. 创建或更新 `assets/*_data.json`，格式：
   ```json
   {"items": [{"id": "...", "file": "...", "phylum": "...", "phylumLatin": "...", "genus": "...", "genusLatin": "...", "number": 0}]}
   ```
2. 把图片放入 `assets/images/`（浮游植物）或 `assets/zooplankton/`（浮游动物）
3. 更新 AboutContent 中的统计数字（`"8 门 · 124 属 · 629 图"`）

### 更新版本号

改 `app/build.gradle.kts` 和 `MainScreen.kt` 中的 `APP_VERSION`：
```kotlin
// app/build.gradle.kts
versionCode = 7
versionName = "V0.4.0"

// MainScreen.kt
private const val APP_VERSION = "V0.4.0"
```

### 添加新的 tab

1. `MainScreen` 的 `NavigationBar` 加一项（Material Icons 已有 `Info`/`List`/`PlayArrow`）
2. `selectedTab when` 分支加新 Composable
3. TopAppBar title 的 when 加对应文字

### 添加过滤器（预留未使用）

ViewModel 已预留：
- `filter` 字段（"all"|"known"|"unknown"）
- `phylumFilter` 字段（String?）
- `selectPhylum()` 方法

UI 层目前没有调用它们。若需激活过滤功能，在 `TrainContent` 中添加 FilterChip 行并调用对应方法。

---

## 坑与注意事项

### 编译相关

- **`Context` import 不可删** — `getSharedPreferences()` 需要 `Context.MODE_PRIVATE`，已在 ViewModel 第 24-25 行使用
- **版本号双源同步** — `MainScreen.kt` 的 `APP_VERSION` 常量需要与 `app/build.gradle.kts` 的 `versionName` 保持一致。更新版本时两处都要改。
- **梯度版本**：AGP 8.7.0 + Kotlin 2.0.21 + Compose BOM 2024.09.00

### 性能注意

- **图片无内存缓存** — FlashcardView 和 ErrorBookContent 每次进入都从 assets 重新解码位图。若用户频繁切换标签（训练→错题集→训练），图片会反复加载。可考虑用 `remember` 加弱引用缓存，但 629 张图会导致 OOM，当前按需加载策略合理。
- **`poolTotal()` 惰性缓存** — 调用 `buildPool()` 后缓存结果，在所有修改池状态的操作（`mark`/`undo`/`restart`/`selectPhylum`/`initPool`）中通过 `updateStats()` 自动失效。不要直接调用 `buildPool()` 进行统计。
- **`knownCount` O(n) 但可接受** — `updateStats()` 遍历全部 items 查询 SharedPreferences，775 items 在 UI 线程上单次约几毫秒，非瓶颈。
- **`buildPool()` 不宜频繁调用** — 它是过滤 + 遍历，不要在 Recompose 热路径上调用。当前仅在用户操作（标记、切换、重启）时触发。
- **`random()` 选择下一张** — `remaining.random()` 每次随机取一张，不是真正的间隔重复（SRS）。如果要做智能排卡，替换 `nextCard()` 逻辑即可。

### 安全

- `allowBackup="false"` **不要改回 true** — 否则 SharedPreferences 会被 Google Drive 备份，可能泄露学习数据
- JSON 解析外层 `try-catch` 包裹，内部字段用 `getString()`/`getInt()`（非 `opt` 系列），JSON 格式错误时会抛异常，但会被外层捕获

### 死代码（已清理）

- ~~`knownDefault`~~ — 已删
- ~~`modeTitle`/`modeIcon`/`modeOther`~~ — 已删
- ~~`fullReset()`/`selectFilter()`~~ — 已删
- ~~`PHYLUM_ORDER`/`PHYLUM_LATIN`/`PHYLUM_EMOJI`~~ — 已删
- ~~`Dialog` import / `APP_VERSION` 常量~~ — 已删

### 视图与 ViewModel 通信

ViewModel 用 `mutableStateOf` / `mutableIntStateOf` 暴露可观察状态，Compose 自动订阅重组。
关键原则：**不要在 ViewModel 中引用 `Context`（除了 `Application`）**。当前通过 `AndroidViewModel(application)` 获取 `Application` 来访问 assets 和 SharedPreferences，这是正确做法。
`loadData()` 在 `MainActivity.onCreate()` 中调用，不在 Compose 内，避免 LaunchedEffect 重复触发。

### 数据文件

数据 JSON 使用 `org.json`（Android 内置，无额外依赖）而非 Gson/Moshi。
JSON 结构全部以 `{"items": [...]}` 为根，内层字段名固定：`id`/`file`/`phylum`/`phylumLatin`/`genus`/`genusLatin`/`number`。`phylumLatin`/`genusLatin`/`number` 可选（带 `opt` 前缀），其余字段缺失会抛异常。

---

## 文件清单

```
识浮游/
├── SKILL.md                                    ← 本文件
├── app/build.gradle.kts                        ← 依赖与版本
├── settings.gradle.kts                         ← 项目名 "识浮游"
├── build.gradle.kts                            ← 根级插件
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/rhodes/algae/
    │   ├── MainActivity.kt
    │   ├── data/AlgaeItem.kt
    │   ├── viewmodel/TrainViewModel.kt
    │   └── ui/
    │       ├── theme/Theme.kt
    │       └── screens/MainScreen.kt
    ├── assets/
    │   ├── algae_data.json
    │   ├── zooplankton_data.json
    │   ├── images/        ← 浮游植物图片
    │   └── zooplankton/   ← 浮游动物图片
    └── res/drawable/
        ├── ic_launcher_foreground.xml
        └── ic_launcher_background.xml
```
