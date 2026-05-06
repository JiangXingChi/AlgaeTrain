# 🌿 识藻训练 · AlgaeTrain

淡水藻类闪卡记忆训练 Android APP。基于 Kotlin + Jetpack Compose 构建，支持中英双语拉丁学名对照。

A freshwater algae flashcard training Android app built with Kotlin + Jetpack Compose. Supports bilingual Chinese–Latin scientific name lookup.

---

## 📊 数据集 / Dataset

| | 中文 | English |
|---|---|---|
| 门 | 8 (硅藻/绿藻/蓝藻/裸藻/甲藻/金藻/隐藻/黄藻) | 8 phyla |
| 属 | 124 | 124 genera |
| 图片 | 629 张显微照片 | 629 micrographs |
| 来源 | 《中国内陆水域常见藻类图谱》《澳门常见淡水藻类图谱》 | Published atlases |

每张卡片翻面后同时显示中文名称和拉丁学名（如 *Bacillariophyta* · *Navicula*）。

Each card flips to reveal both the Chinese name and Latin scientific name (e.g. *Bacillariophyta* · *Navicula*).

---

## 🎮 功能 / Features

- **闪卡训练** — 看图识藻，点击翻面查看名称 / Flashcard drill: identify algae from micrographs
- **双指缩放** — 拖拽+缩放观察细节 / Pinch-to-zoom for microscopic detail
- **错题集** — 不认识的项目自动收集，支持逐张回顾 / Auto-collected error book with image review
- **撤销** — 点错了？一键回退重新标记 / One-tap undo for misclicks
- **断点续练** — 退出自动保存进度，下次打开继续 / Progress auto-saved via SharedPreferences
- **门筛选** — 按 8 个门分别训练 / Filter by phylum
- **中英双语** — 翻面显示中文名 + 拉丁学名 / Bilingual Chinese + Latin display

---

## 🏗️ 技术栈 / Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Coil** / BitmapFactory 图片加载
- **SharedPreferences** 本地持久化
- **Gradle 8.9** + **AGP 8.7**

---

## 📁 项目结构 / Project Structure

```
AlgaeTrain/
├── app/src/main/
│   ├── java/com/rhodes/algae/
│   │   ├── MainActivity.kt              # 入口 / Entry point
│   │   ├── data/AlgaeItem.kt            # 数据模型 + 常量 / Data model
│   │   ├── viewmodel/TrainViewModel.kt  # 核心逻辑 + 持久化 / Core logic
│   │   └── ui/
│   │       ├── screens/MainScreen.kt    # 闪卡 / 错题集 / 关于
│   │       └── theme/Theme.kt          # Material3 主题
│   ├── assets/
│   │   ├── algae_data.json             # 629 条数据（含拉丁名）
│   │   └── images/                      # 图片资源
│   └── AndroidManifest.xml
├── app/build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 构建 / Build

用 Android Studio（Flatpak 安装）：

```bash
flatpak run com.google.AndroidStudio ~/Desktop/Esperanta/AlgaeTrain
```

或命令行构建：

```bash
export JAVA_HOME=/path/to/android-studio/jbr
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

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
