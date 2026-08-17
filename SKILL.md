# 识浮游 — Android 项目

浮游生物识别训练 App（翻卡学习 + 记忆曲线 + 每日打卡）。Kotlin + Jetpack Compose + Material3。

---

## 快速构建

```bash
cd /home/teacat/Agent/Temp/App开发/识浮游

# Debug APK（构建 0 warning 是硬要求）
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 产物路径
# app/build/outputs/apk/debug/app-debug.apk

# 注意：gradlew 里 JAVA_HOME 已去掉硬编码，使用系统 JDK21；
# 若 ~/.gradle/gradle.properties 配了代理而 Clash 未开，构建会失败，注释 proxy 行即可
```

---

## 架构概览

```
MainActivity                 ← 入口，AlgaeTheme + TrainViewModel
  └─ MainScreen              ← Scaffold + 3 标签页 + 1 个全屏 overlay
       ├─ TrainTab           ← 训练页：闪卡（图谱书卡片在打卡页）
       ├─ CheckInTab         ← 打卡页：图谱书卡片(总进度/换书) + 今日任务双进度条 + 月历 + 设置（配额10/20+自定义/比例/重置）
       ├─ AboutTab           ← 关于：玩法说明 + 版本信息 + 版权
       └─ BookShelfScreen    ← 图谱书架 overlay（像选词书一样换书）
```

### 数据流

```
assets/algae_data.json       ← 静态数据（浮游植物 630 图 / 8 门）
assets/zooplankton_data.json ← 静态数据（浮游动物 146 图 / 3 门）
assets/images/*.jpg          ← 浮游植物图片
assets/zooplankton/*.jpg     ← 浮游动物图片

TrainViewModel               ← 状态管理器（唯一 ViewModel）
  ├── loadData()             ← 从 assets 加载两本图谱（含旧数据迁移）
  ├── parseItems(json, mode) ← 解析 + 旧版 known 迁移，mode 决定 prefs 归属
  ├── rebuildDailyQueue()    ← 每日调度：到期复习（先）+ 新卡配额（后）
  ├── mark(known)            ← 2 档评价，更新 SRS 等级/日期 + 打卡判定
  ├── addMoreCards()         ← 学完后加练：剩余新卡再取一组（不影响打卡，0=学完）
  ├── undo()                 ← 撤销上一步（同时回滚 SRS 状态）
  ├── restart()              ← 重置两本图谱进度（保留打卡记录）
  ├── setQuota/setRatio      ← 打卡设置，改后立即重建今日队列
  └── isCheckedIn()/streakDays() ← 打卡判定 / 连续天数
```

---

## 核心约定

### 包名与命名

- 包名 `com.rhodes.algae`（未改为 `com.rhodes.plankton`，若重构需同步 `build.gradle.kts`）
- 数据模型 `AlgaeItem` 同时服务植物/动物两本图谱
- UI 文件：MainScreen.kt（训练/打卡/关于）+ BookShelfScreen.kt（书架），拆 UI 文件时保持既有约定

### 图谱书（V0.4.0 引入）

- 两本：**浮游植物图谱**（630 图 · 8 门）、**浮游动物图谱**（146 图 · 3 门）
- 书架入口：**打卡页顶部「我的图谱书」卡片**（含总进度条，点击换书）；未选过书首次启动自动进书架
- `currentMode` = "algae"/"zooplankton"，即"当前书"；选书写入 `app_settings.selected_book`
- 每本书进度独立（两套 SharedPreferences：`algae_train` / `zoo_train`），切书互不影响

### SRS 记忆曲线（V0.4.0 引入）

- **间隔表**（艾宾浩斯遗忘曲线节点）：等级 0→1 天，1→2 天，2→4 天，3→7 天，4→15 天，5→30 天
- 每卡：`s_<id>` 熟练等级 0-5，`d_<id>` 下次复习日（epochDay），按书隔离
- **回答 2 档**：
  - 认识：等级 +1（封顶 5 级），按间隔表排下次复习
  - 不认识：等级归 0，`d_` = 明天，卡回今日队列尾部当日重练
- 每日队列 = 到期复习卡（`d_` ≤ 今天，按到期先后）+ 新卡（`d_` == 0，配额内先复习后新学）
- 队列日期 `queue_date`：当天已建队列则断点续练（跨天自动重建）

### 每日打卡（V0.4.0 引入）

- 设置存 `app_settings`：`daily_quota`（默认 20，快捷档 10/20 + **自定义 1-100** 输入）、`review_ratio`（1:1/1:2/1:3，默认 1:2）
- 复习上限 = 配额 × 比例，超出的到期卡顺延明天
- **打卡条件**：今日新卡配额完成即打卡（复习卡不阻塞）
- 打卡记录：`app_settings.checkin_dates`（JSONArray<epochDay>），连续天数从今天/昨天往回数
- 漏签不补卡；打卡页面在**独立 tab**（今日任务双进度条 + 打卡月历 + 每日新卡量 / 复习比例设置 + 重置），不占训练页空间

### 主题

Material3 + lightColorScheme：
- `primary` `#3A7C6B`（墨绿）、`secondary` `#D4A853`（金色）、暖白米色背景

### 进度持久化

SharedPreferences（不用 Room/DataStore）：
- `k_<id>` — 已掌握标记（V0.3.1 遗留，兼容保留）
- `s_<id>` / `d_<id>` — SRS 等级 / 下次复习日
- `queue_ids` + `queue_date` + `queue_*_total/done` — 今日队列与进度
- `app_settings` — 当前书、打卡设置、打卡日期（全局，不分书）

---

## 修改指南

### 添加新数据

1. 创建或更新 `assets/*_data.json`，格式：
   ```json
   {"items": [{"id": "...", "file": "...", "phylum": "...", "phylumLatin": "...", "genus": "...", "genusLatin": "...", "number": 0}]}
   ```
2. 图片放入 `assets/images/`（植物）或 `assets/zooplankton/`（动物）
3. About 页图数动态显示（`allAlgaeItems.size`），无需手动改；新增后同步本文件中的图数说明

### 更新版本号

改 `app/build.gradle.kts` 和 `MainScreen.kt` 中的 `VERSION` 常量（两处同步）：
```kotlin
// app/build.gradle.kts
versionCode = 9
versionName = "V0.5.1"

// MainScreen.kt
private const val VERSION = "V0.5.1"
```

### 添加新的图谱书

1. `assets/` 新增 `xxx_data.json` + 图片目录
2. `TrainViewModel` 增加 `allXxxItems`、`prefsXxx`、`parseItems` 加载、`switchMode` 分支
3. `BookShelfScreen` 增加一张 `BookCard`
4. About 页统计更新

### 过滤器（预留未使用）

- `filter`（"all"|"known"|"unknown"）与 `phylumFilter` 已接入 `rebuildDailyQueue` 的范围过滤
- UI 层未调用；激活时在训练页加 FilterChip 行并调用 `selectPhylum()` / 设置 `filter`

---

## 坑与注意事项

### 编译相关

- **`Context` import 不可删** — `getSharedPreferences()` 需要
- **JDK**：gradlew 已去硬编码 JAVA_HOME，用系统 JDK21；若 build 报代理错误，检查 `~/.gradle/gradle.properties` 的 proxy 行（Clash 未开时注释掉）
- **版本号双源同步** — `MainScreen.kt` 的 `VERSION` 与 `build.gradle.kts` 的 `versionName` 必须一致
- 梯度：AGP 8.7.0 + Kotlin 2.0.21 + Compose BOM 2024.09.00
- 图标 deprecation：`Icons.Filled.KeyboardArrowRight/Left` 用 AutoMirrored 版本，否则 warning

### SRS 细节（别再踩）

- **新卡判定 = `d_` == 0**（从未排期）。忘记归零的卡 `d_` = 明天，不会占新卡配额
- **打卡不回滚**：undo 撤销卡不撤销打卡记录（今日确实完成过）
- 修改配额/比例会**立即重建当日队列**（当天生效）；已学的卡等级已保存不受影响
- 打卡计数（newDone/reviewDone）持久化在 `queue_*_done`，进程重启后断点续练计数不丢
- `checkCheckIn()` 幂等：已打卡当天不再重复写

### 图片与性能

- **无内存缓存** — 按需从 assets 解码，630+146 张图按需加载策略合理
- `rebuildDailyQueue()` 遍历全量 items 查 prefs，仅在启动/切书/改设置时调用，勿放重组热路径
- `checkedInDays()` 有内存缓存（打卡写时更新），UI 重组不再反复解析 JSONArray

### 安全

- `allowBackup="false"` **不要改回 true** — SharedPreferences 含学习进度，防云端备份泄露
- JSON 解析外层 try-catch，内部字段用 getString/getInt

---

## 文件清单

```
识浮游/
├── SKILL.md                                    ← 本文件
├── docs/V0.4.0-开发计划.md                    ← V0.4.0 计划（已定稿）
├── app/build.gradle.kts                        ← 依赖与版本
├── gradlew                                    ← 已去硬编码 JAVA_HOME
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/rhodes/algae/
    │   ├── MainActivity.kt
    │   ├── data/AlgaeItem.kt
    │   ├── viewmodel/TrainViewModel.kt         ← 全部业务逻辑（含 SRS + 打卡）
    │   └── ui/
    │       ├── theme/Theme.kt
    │       └── screens/
    │           ├── MainScreen.kt               ← 训练/打卡/关于/月历
    │           └── BookShelfScreen.kt          ← 图谱书架
    ├── assets/
    │   ├── algae_data.json                     ← 浮游植物 630 图
    │   ├── zooplankton_data.json               ← 浮游动物 146 图
    │   ├── images/                             ← 浮游植物图片
    │   └── zooplankton/                        ← 浮游动物图片
    └── res/
        ├── values/themes.xml                   ← 亮色启动主题
        ├── values-night/themes.xml             ← 暗色启动主题（防闪白）
        └── mipmap-*/                           ← 图标
```