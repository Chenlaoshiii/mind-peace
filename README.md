# Mind Peace

一个温和的「打开前停一停」应用。它**不会锁住整部手机**，只在你打开已守护的应用（例如视频流）时立刻盖住一层确认，问你是不是真的要打开；确定后再选择时长。倒计时在后台进行，时间到会回到桌面并提醒。每个应用可以设置每日总额度。

- 应用名：Mind Peace
- applicationId：`com.mindpeace.app`
- 界面语言：简体中文
- minSdk 26 / targetSdk 35 / compileSdk 35
- Kotlin + Jetpack Compose + Material 3（Android 12+ 跟随壁纸的动态取色）
- 个性主题：浅色 / 深色 / 跟随系统；风格：谷歌 Material You / 橙色风格 / 苹果风格（系统蓝 + RenderEffect 玻璃回退）
- 版本：1.2.1（versionCode 4）
- 作者：陈老实Chenlaoshi（https://space.bilibili.com/3546678682454822）

## 在 Android Studio 中打开

1. 安装 [Android Studio](https://developer.android.com/studio)（建议 Ladybug / Koala 或更新，需 JDK 17）。
2. **File → Open**，选择本目录 `mind-peace`。
3. 等待 Gradle Sync。若提示缺少 SDK 35，在 SDK Manager 安装 **Android 15 (API 35)** 与对应 Build-Tools。
4. 用真机调试（无障碍与后台保活在模拟器上往往不完整）。

也可命令行构建：

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

首次在 Linux/macOS 上若 `gradlew` 不可执行：`chmod +x ./gradlew`。

## 无障碍是如何工作的

系统不会把「当前前台应用」交给普通 App。Mind Peace 使用 **AccessibilityService** 作为主要感知方式：

- 监听窗口变化，并定时读取当前应用窗口的包名。
- 若该包名在守护名单中，且当前没有仍有效的限时会话，立刻用 `TYPE_ACCESSIBILITY_OVERLAY` 盖住全屏确认层（目标应用还在下面，但不能操作）。
- 选「确定」后挑选时长，遮罩消失，前台服务通知里显示剩余时间。
- 倒计时**只在目标应用位于前台时走**；切走会暂停，回来接着走。
- 时间为 0 时执行返回桌面，并给出「时间到了」提醒。再次打开需重新确认（还受每日剩余额度约束）。
- **不会拦截 Mind Peace 自己**，并对系统桌面 / System UI 做了忽略与去抖，减少闪烁。

无障碍配置见 `app/src/main/res/xml/accessibility_service_config.xml`。服务说明里写明：不读取聊天、密码或屏幕文字细节，只关心是哪一个应用来到前台。

首次启动必须走完引导。最后一步的「我完成」只有在**无障碍确实已开启**（读取系统设置，而不是只点「继续」），并且你勾选了后台锁定确认之后才会点亮。可在设置里重新查看引导。若之后关掉无障碍，应用内会全屏提示，而不是默默失效。

## 1.2

- B 站主页：作者卡片与关于页署名打开 https://space.bilibili.com/3546678682454822
- 写给自己的话：三个预设 +「自定义」（清空后自填）
- 用词：拦截「真的确定要打开…吗？」；小结「一共用了 / 少用了」
- 7 天趋势：使用统计页 Compose Canvas 折线（最近最多 7 个日历日）；每日运行 / 会话 / 夜间小结会补当天记录
- 关于页：设置 → 关于；大标题连点 5 下打开隐藏通知实验室
- 苹果风格未接入 Kyant backdrop（2.0.1 需 Kotlin 2.4；1.0.x 需 Kotlin 2.2），改用 iOS 系统蓝与分组灰底、RenderEffect 玻璃回退（半透明描边胶囊按钮；API < 31 半透明+边框）。橙色风格为暖纸底与橙色强调（兼容旧版已保存的风格值）。

## 1.1 新能力

- **更完整的应用列表**：读取所有可启动应用（不再用 `MATCH_DEFAULT_ONLY`），搜索同时匹配名称和包名。
- **写给自己的话**：引导里写一句提醒，每次拦截确认都会看见；设置里可改。
- **毛玻璃拦截层**：半透明 + 高斯模糊（系统支持时），确认卡片浮在上面。
- **坚持通知**：一段时间没打开被守护应用会鼓励你（4 小时 / 1 天 / 3 天），夜间 22:00–08:00 不打扰。
- **晚间小结**：约 21:00 推送今天 vs 昨天；点开进入使用统计页。用量会保留至少 14 天。
- **通知权限**：引导里会明确请求；拒绝仍可完成引导，首页会继续提醒。

## 每日额度

按设备本地时区的自然日统计。会话里实际走过的时间计入「今日已用」。`剩余 = 每日上限 − 今日已用`。若你选的时长大于剩余，会先警告，再把本次会话截成剩余时间（例如每日 15 分钟、已用 10 分钟、再选 10 分钟 → 本次只有 5 分钟）。剩余为 0 时只能退出。上限为 0 表示不限制每日总量，但每次打开仍会询问。

## 保活与厂商限制

拦截依赖进程还活着。项目里已经做了：

- 无障碍服务（系统会尽量维持）
- 会话期间的前台服务 + 通知渠道
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `RECEIVE_BOOT_COMPLETED`（引导完成后重启可恢复会话状态；无障碍若仍开启会由系统拉起）

**厂商电池与后台清理无法在应用内彻底解决**，尤其是小米 / 华为 / OPPO / vivo：

| 设备 | 建议 |
| --- | --- |
| 小米 / 红米 | 最近任务下拉卡片锁定；关闭神隐模式；允许自启动与后台运行 |
| 华为 / 荣耀 | 最近任务锁定；电池「允许后台活动」；关闭自动管理 |
| OPPO / 一加 | 最近任务锁定；自启动管理允许 |
| vivo / iQOO | 最近任务锁定；后台高耗电允许 |
| Pixel / 原生 | 一般关闭电池优化即可 |

即使忽略了电池优化，部分 ROM 仍会在「锁屏清理」时杀掉无障碍进程。若拦截突然不出现，打开 Mind Peace 看是否提示无障碍已关闭，并重新锁定后台任务。

## 权限

| 权限 | 用途 |
| --- | --- |
| 无障碍 | 感知前台应用、盖住确认层、回到桌面 |
| 忽略电池优化 | 倒计时不被doze杀掉 |
| 通知 | 会话剩余时间、时间到提醒 |
| 悬浮窗 | 备用；主路径是无障碍遮罩 |
| 开机广播 | 引导完成后恢复监控状态 |

## 工程结构（关键路径）

```
mind-peace/
  app/src/main/java/com/mindpeace/app/
    MainActivity.kt
    data/                 DataStore 名单与每日用量
    session/              拦截状态机 + 无障碍遮罩
    service/              AccessibilityService / 前台计时 / 开机接收
    ui/onboarding|home|picker|settings|overlay|stats
    work/                 WorkManager 坚持通知与晚间小结
  app/src/main/res/xml/accessibility_service_config.xml
```

本仓库若在没有 Android SDK 的环境生成，可能无法在该环境执行 `assembleDebug`；用 Android Studio 打开后即可同步并编译。
