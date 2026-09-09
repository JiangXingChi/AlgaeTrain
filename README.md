# 🦫 识浮游 · AlgaeTrain

浮游生物闪卡记忆训练 Android APP。浮游植物 + 浮游动物两本图谱，按艾宾浩斯记忆曲线安排复习，每日任务打卡。中英双语拉丁学名对照。

> ⚠️ 图片版权归原作者及出版社所有，仅用于学术教育目的，禁止商业用途。

---

## 📖 玩法说明

1. 在「打卡」页的「我的图谱书」卡片选择/切换图谱书（浮游植物 / 浮游动物）
2. 看图片判断是否认识
3. 单击卡牌翻面查看名称
4. 认识点 ✓，不认识点 ✗（不认识会归零重学）
5. 点错可点「↩ 撤销」返回重标
6. 按记忆曲线（1/2/4/7/15/30 天）安排复习
7. 每日新卡学完自动打卡，可翻看打卡月历
8. 学完可点「再学一组」「再复习一组」继续加练（不影响打卡）

---

## 📊 数据集

| | 浮游植物 | 浮游动物 |
|---|---|---|
| 图片 | 630 张显微照片 | 146 张显微照片 |
| 门 | 8（硅藻/绿藻/蓝藻/裸藻/甲藻/金藻/隐藻/黄藻） | 3（原生动物/节肢动物/轮虫动物） |
| 属 | 125 | 56 |

每张卡片翻面后同时显示中文名称和拉丁学名（如 *Bacillariophyta* · *Navicula*）。

**图片来源**（公开出版物及实地采样）：
- 📗 《中国内陆水域常见藻类图谱》
- 📘 《澳门常见淡水藻类图谱》
- 📙 《中国流域常见水生生物图集》

---

## 🎮 特性

- **图谱书架** — 浮游植物/浮游动物两本书，进度独立
- **SRS 记忆曲线** — 间隔 1/2/4/7/15/30 天自动排期，每日复习组 + 新卡组双队列
- **每日打卡** — 新卡全部认完自动打卡，月历回顾
- **加练队列** — 再学一组 / 再复习一组，不占每日配额、不影响打卡
- **断点续练** — 队列与进度本地持久化，退出重开无缝衔接
- **双指缩放** — 拖拽 + 缩放观察显微细节
- **撤销** — 一键回退重新标记
- **暗色模式 + 字体缩放** — 亮/暗/跟随系统，三档字号

## 🏗️ 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Coil 图片加载 · SharedPreferences 本地持久化
- 单 Activity + ViewModel 架构
- Gradle + AGP · compileSdk 35 · **minSdk 26（Android 8.0+）**

## 📁 项目结构

```
app/src/main/
├── java/com/rhodes/algae/
│   ├── MainActivity.kt              # 入口
│   ├── data/AlgaeItem.kt            # 数据模型 + 常量
│   ├── viewmodel/TrainViewModel.kt  # 核心逻辑（SRS / 队列 / 打卡）
│   └── ui/
│       ├── theme/Theme.kt           # 主题状态 + 亮/暗色方案
│       └── screens/
│           ├── MainScreen.kt        # 训练 / 打卡 / 关于 / 月历
│           └── BookShelfScreen.kt   # 图谱书架
└── assets/
    ├── algae_data.json              # 630 条（含拉丁名）
    ├── zooplankton_data.json        # 146 条
    ├── images/                      # 浮游植物图片
    └── zooplankton/                 # 浮游动物图片
```

## 🚀 构建

```bash
# 需要 JDK 21 + Android SDK（local.properties 指定 sdk.dir）
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

也可直接在 [Releases](../../releases) 下载预构建 APK（debug 签名），安装即用。

---

## 👥 作者

喝茶喵 · 蛋蛋 · 进宝

## 📜 许可

代码采用 **MIT** 开源许可协议（见 [LICENSE](LICENSE)）；图片素材**不含**在 MIT 授权内，版权归原作者及出版社所有，仅限学术教育用途，禁止商业用途。

MIT License for the code. Image assets are NOT covered by the MIT license — they belong to the original authors and publishers, for educational use only, no commercial use.
