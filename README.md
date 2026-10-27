<p align="center">
  <img src="docs/images/clock-icon.svg" width="88" height="88" alt="静夜时钟图标" />
</p>

<h1 align="center">静夜时钟 · QuietClock</h1>

<p align="center"><strong>简体中文</strong> · <a href="README.en.md">English</a></p>

<p align="center">一个轻量的原生 Android 横屏翻页时钟</p>
<p align="center">纯黑背景 · 翻页数字 · 夜间时段 · 整体位移</p>

<p align="center">
  <a href="#下载安装"><img src="docs/images/badge-android.svg" alt="支持 Android 8.0 及以上" /></a>
  <a href="https://github.com/lucifly/QuietClock/releases/tag/v0.1.3"><img src="docs/images/badge-version.svg" alt="0.1.3 测试版" /></a>
  <a href="LICENSE"><img src="docs/images/badge-license.svg" alt="MIT License" /></a>
</p>

<p align="center">
  <a href="https://github.com/lucifly/QuietClock/releases/download/v0.1.3/QuietClock-0.1.3-debug.apk"><strong>下载 APK</strong></a>
  · <a href="#界面效果">界面效果</a>
  · <a href="#功能特色">功能特色</a>
  · <a href="#开发与构建">自行构建</a>
  · <a href="https://github.com/lucifly/QuietClock/issues">反馈问题</a>
</p>

## 界面效果

<p align="center">
  <img src="docs/images/clock-landscape.png" alt="静夜时钟横屏界面：灰色翻页数字、中央黑色折痕，下方显示日期和电量" />
</p>

效果图由应用实际绘制代码生成，时间、日期和电量为示例。界面会定时整体位移；实际显示亮度取决于设备及亮度设置。

## 下载安装

**当前版本：0.1.3 测试版 · Android 8.0 及以上**

1. 在手机上[下载 APK 安装包](https://github.com/lucifly/QuietClock/releases/download/v0.1.3/QuietClock-0.1.3-debug.apk)。
2. 打开下载的 APK，按系统提示允许该下载来源安装应用。
3. 启动静夜时钟，横放手机；轻触屏幕可打开设置。

[查看 Release 与校验文件](https://github.com/lucifly/QuietClock/releases/tag/v0.1.3)。GitHub 自动生成的 `Source code` 文件为源码，安装应用请选择 `.apk` 文件。当前 APK 使用调试签名，沿用此前测试版签名。

## 功能特色

| 功能 | 显示与操作 |
| :--- | :--- |
| **翻页时钟** | 小时、分钟分组显示，数字中央保留黑色折痕；数字变化时短暂翻页，牌面背景同步出现，结束后恢复纯黑。 |
| **日期与电量** | 显示日期、电量和充电状态，遵循系统的 12/24 小时格式。 |
| **整体位移** | 时间、日期和电量一起移动，每分钟小幅偏移、每 10 分钟轮换较大的位置，并限制在显示区域内。 |
| **夜间时段** | 每日重复，可跨午夜；时段内关闭翻页动画，保留时间更新和整体位移。 |
| **亮度与颜色** | 常规、夜间亮度分别设置；提供灰、暖、绿三种文字颜色。 |
| **摆放方向** | 支持 180° 旋转，保存用户选择；提供自适应应用图标与单色主题图标。 |
| **自动结束** | 可选 30 分钟、1 小时或 2 小时；设置中显示剩余时间和预计结束时刻，也可取消。 |

## 使用与默认设置

轻触时钟界面打开设置，调整后会在本机保存。

| 设置 | 默认值 | 可调整范围 |
| :--- | :--- | :--- |
| 常规亮度 | 5% | 1%–30% |
| 夜间时段 | 开启，23:00–次日 07:00 | 自定义开始与结束时间，支持跨午夜 |
| 夜间亮度 | 4% | 手机最低亮度，或 1%–10% |
| 文字颜色 | 灰色 | 灰 / 暖 / 绿 |
| 自动结束 | 未开启 | 30 分钟 / 1 小时 / 2 小时 |
| 旋转 180° | 关闭 | 按手机摆放方向切换 |

**运行方式：**应用在前台保持屏幕点亮，不接管系统 AOD。按电源键或切换到后台后，手机可以正常熄屏。

**OLED 使用：**位置变化只能分散固定区域的显示时间，不能保证避免烧屏；实际亮度、耗电和温度仍需在具体设备上测试。

## 隐私

- **无需联网：**不声明联网权限，不集成广告、统计或第三方分析 SDK。
- **无需账号：**仅在设备本地读取系统时间、电量和充电状态。
- **设置保存在本机：**亮度、旋转与夜间时间段等使用本地 `SharedPreferences` 保存。
- **无需额外授权：**不需要定位、相机、麦克风、通讯录、无障碍或悬浮窗权限。

## 开发与构建

使用 Java 和 Android 原生绘制实现，应用运行时无第三方库依赖。

<details>
<summary><strong>环境要求与构建步骤</strong></summary>

| 工具 | 版本 |
| :--- | :--- |
| JDK | 17 |
| Android SDK Platform | 35 |
| Android SDK Build Tools | 35.0.0 |
| Android Gradle Plugin | 8.9.2 |
| Gradle Wrapper | 8.11.1，已配置发行包 SHA-256 校验 |

用 Android Studio 打开项目并配置 JDK 和 SDK。也可以配置好 `JAVA_HOME`、`ANDROID_HOME` 后，在项目根目录运行：

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Windows 使用 `gradlew.bat`。首次构建与测试需要下载依赖。

测试 APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` 由开发者在本机配置，不应提交到仓库。本机 SDK 路径、构建缓存、日志、签名私钥和生成的安装包均排除在源码提交之外。

测试构建使用开发者本机的调试签名。仓库不包含私钥；正式发行应使用自行保管的签名。

</details>

<details>
<summary><strong>测试覆盖与实现说明</strong></summary>

- 13 项 Android 行为模拟回归测试，覆盖启动、触控、暂停恢复、弹窗亮度、旋转、倒计时、布局缓存、图标及夜间亮度升级和保存。
- 行为测试基于 Robolectric，在 JVM 中模拟 Android 15（API 35），不能替代真机测试。
- 绘制布局、字体度量和数字拆分结果缓存，在尺寸、字体配置、内容或位置变化时更新。
- 夜间时段按本地时间计算，开始包含、结束不包含；修改系统时间或时区后重新判断。
- 实际最低亮度、系统适配、耗电、温度及长时间运行表现仍需在设备上验证。

</details>

## 更新记录

**[0.1.3](https://github.com/lucifly/QuietClock/releases/tag/v0.1.3)** · 夜间默认亮度由 2% 提高至 4%，兼容旧版设置升级，保留后续手动调整。

<details>
<summary>更早的版本</summary>

- **0.1.2**：增加夜间亮度滑块。
- **0.1.1**：完善自动结束状态、增加应用图标、缓存绘制布局。
- **0.1**：横屏翻页时钟原型。

</details>

## 反馈

遇到问题或有功能建议，欢迎[提交 Issue](https://github.com/lucifly/QuietClock/issues)。描述问题时，请附上手机型号、系统版本和复现步骤。

## 许可证

静夜时钟的原创代码与资源采用 [MIT License](LICENSE)，版权署名为 `Copyright (c) 2026 lucifly`。该许可也适用于本仓库已发布的 0.1.3 APK 中的原创部分。

Gradle Wrapper 及其他第三方组件保留各自的许可证；随仓库分发的 Gradle Wrapper 使用 Apache-2.0，许可证副本见 [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)。

## 🌟 Star History

<a href="https://www.star-history.com/#lucifly/QuietClock&amp;Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=lucifly/QuietClock&amp;type=Date&amp;theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=lucifly/QuietClock&amp;type=Date" />
    <img alt="QuietClock 的 GitHub Star 历史趋势" src="https://api.star-history.com/svg?repos=lucifly/QuietClock&amp;type=Date" />
  </picture>
</a>
