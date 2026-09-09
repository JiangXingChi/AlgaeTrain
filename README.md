# 🦫 识浮游 · AlgaeTrain

浮游生物闪卡记忆训练 Android APP。植物 + 动物两本图谱，内置艾宾浩斯记忆曲线（SRS）与每日打卡。基于 Kotlin + Jetpack Compose 构建，支持中英双语拉丁学名对照。

A freshwater plankton flashcard training Android app built with Kotlin + Jetpack Compose. Two illustrated books (algae & zooplankton), built-in spaced repetition (SRS) and daily check-in. Bilingual Chinese–Latin scientific name lookup.

---

## 📊 数据集 / Dataset

| | 🌿 植物图谱 Algae | 🦠 动物图谱 Zooplankton |
|---|---|---|
| 图片 | 630 张显微照片 | 146 张显微照片 |
| 门 | 8（硅藻/绿藻/蓝藻/裸藻/甲藻/金藻/隐藻/黄藻） | 3（原生动物/节肢动物/轮虫动物） |
| 属 | 125 | 56 |
| 来源 | 《中国内陆水域常见藻类图谱》《澳门常见淡水藻类图谱》等已出版图谱 | Published atlases |

每张卡片翻面后同时显示中文名称和拉丁学名（如 *Bacillariophyta* · *Navicula*）。

Each card flips to reveal both the Chinese name and Latin scientific name (e.g. *Bacillariophyta* · *Navicula*).

---

## 🎮 功能 / Features

- **图谱书架** — 植物/动物两本书独立进度，随时切换 / Two illustrated books with independent progress
- **SRS 记忆曲线** — 认识/不认识自动排期，间隔 1/2/4/7/15/30 天 / Ebbinghaus spaced repetition (1–30 day intervals)
- **每日打卡** — 新卡 + 复习双队列，设定每日任务量，全部认完即打卡 / Daily quota with review & new-card queues; complete to check in
- **加练队列** — 「再学一组」「再复习一组」，不占每日配额 / Optional extra queues that never affect check-in
- **断点续练** — 队列与进度持久化，退出重开无缝衔接 / Queue & progress auto-saved via SharedPreferences
- **双指缩放** — 拖拽+缩放观察显微细节 / Pinch-to-zoom for microscopic detail
- **撤销** — 点错了？一键回退重新标记 / One-tap undo for misclicks
- **打卡月历** — 每日打卡记录一目了然 / Monthly check-in calendar
- **暗色模式 + 字体缩放** — 亮/暗/跟随系统，三档字号（与系统字体缩放相乘）/ Dark mode & app font scaling on top of system settings
- **中英双语** — 翻面显示中文名 + 拉丁学名 / Bilingual Chinese + Latin display

---

## 🏗️ 技术栈 / Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3, BOM 2024.09.00)
- **Coil 2.7.0** 图片加载（`file:///android_asset/...`）
- **SharedPreferences** 本地持久化（按书隔离 + 全局设置）
- 单 Activity + ViewModel 架构
- **Gradle 8.9** + **AGP 8.7** · compileSdk 35 · minSdk 26

---

## 📁 项目结构 / Project Structure

```
AlgaeTrain/
├── app/src/main/
│   ├── java/com/rhodes/algae/
│   │   ├── MainActivity.kt              # 入口 / Entry point
│   │   ├── data/AlgaeItem.kt            # 数据模型 + 常量 / Data model
│   │   ├── viewmodel/TrainViewModel.kt  # 核心逻辑（SRS/队列/打卡）/ Core logic
│   │   └── ui/
│   │       ├── theme/Theme.kt           # 主题状态 + 亮/暗色方案
│   │       └── screens/
│   │           ├── MainScreen.kt        # 训练 / 打卡 / 关于 / 月历
│   │           └── BookShelfScreen.kt   # 图谱书架
│   ├── assets/
│   │   ├── algae_data.json              # 630 条（含拉丁名）
│   │   ├── zooplankton_data.json        # 146 条
│   │   ├── images/                      # 植物图片
│   │   └── zooplankton/                 # 动物图片
│   └── AndroidManifest.xml
├── app/build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 构建 / Build

```bash
# 需要 JDK 21 + Android SDK（local.properties 指定 sdk.dir）
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

也可以直接在 [Releases](../../releases) 下载已构建的 APK，安装即可（debug 签名）。

Prebuilt debug APKs are available in [Releases](../../releases).

---

## 📜 开源许可 / License

本软件采用 **MIT** 开源许可协议。图片版权归原作者及出版社所有，仅用于学术教育目的。

MIT License. Image copyrights belong to the original authors and publishers — for educational use only.

Copyright © 2025 喝茶喵 & Contributors

---

## 👥 团队 / Team

| 成员 | 角色 |
|---|---|
| 喝茶喵 | 项目负责人 / Project Lead |
| 凯尔希 | 架构 & AI 工程 / Architecture & AI |
| 蛋蛋 | 数据 & 标注 / Data & Annotation |
| 进宝 | 测试 & 运维 / QA & Ops |
