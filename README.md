# AI 键增强 (MI-AIKey-Pro)

一个 LSPosed 模块，用来自定义小米 HyperOS 上 **AI 键**的短按 / 长按动作，并把更多动作直接注入系统的「AI 键」设置页。

![icon](docs/icon-512.png)

## 功能

- **短按 / 长按独立配置**，每个键位都能单独选择动作
- **复用系统已实现的动作**，在设置页直接可选：
  - 手电筒、相机、拍照、相机拍照/录像模式
  - 截屏、区域截屏
  - 计算器
  - 支付宝扫码 / 付款码 / 乘车码，微信扫码 / 付款码 / 乘车码
  - 最近任务、回到桌面、睡眠
  - 控制中心、通知中心
  - 屏幕录制、录音机、小米创作
  - 小爱识屏、智能家居、全局搜索、AI 快捷功能
  - 系统原生的：超级小爱记忆、小爱同学、语音笔记、窥屏防护、静音、勿扰、无
- **启动任意应用**：模块内提供应用选择器
- **完全自定义**：打开网址 / 打开页面 / 发送广播 / 执行 Shell（system uid）
- 设置页 UI 注入，选项直接出现在系统「设置 → AI 键」中
- 配置界面使用 [miuix](https://github.com/compose-miuix-ui/miuix) 风格

## 环境

- 已在 **Xiaomi 18 Pro (M610BB) / HyperOS 4.0.29 / Android 17** 上验证
- LSPosed 2.x（libxposed API 101+）
- 理论上适用于其他 HyperOS 4 且带 `KEYCODE_MI_AI` 的机型，未验证

## 安装

1. 安装 APK
2. 在 LSPosed 中启用本模块，作用域包含：
   - `系统框架`（system server）
   - `com.miui.securitycore`
3. 重启设备（或软重启）
4. 打开系统 **设置 → AI 键**，选择动作
5. 如需「启动应用」或「完全自定义」，先在「AI 键增强」App 内配置

## 构建

环境：JDK 17+、Android SDK 37、Gradle 9.8

```bash
./gradlew assembleDebug
```

Release 签名通过 Gradle 属性传入，不要把密钥提交到仓库：

```bash
./gradlew assembleRelease \
  -PAIKEY_STORE_FILE=/path/to/key.jks \
  -PAIKEY_STORE_PASSWORD=****** \
  -PAIKEY_KEY_ALIAS=****** \
  -PAIKEY_KEY_PASSWORD=******
```

> 本项目使用 KGP（非 AGP 内置 Kotlin），因此 `gradle.properties` 中需要：
> `android.builtInKotlin=false` 与 `android.newDsl=false`。

## 注意

- Shell 动作运行在 **system uid（uid 1000）**，不是 root，只能执行系统权限内的命令
- 「启动应用」「完全自定义」的配置通过 LSPosed remote prefs 从模块 App 传给 system server
- 卸载模块前建议先把系统 AI 键动作改回系统原生选项

## 链接

- 酷安：[BlueEve](https://www.coolapk.com/u/27414574)
- GitHub：[BlueEve04](https://github.com/BlueEve04)
