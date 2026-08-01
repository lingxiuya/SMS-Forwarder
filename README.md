# 📱 Android 短信自动转发与后台保活工具 (SMS Forwarder)

这是一个轻量、稳定且高效的 Android 短信自动转发工具。能够实时监听手机接收到的短信，并通过 SMTP 协议自动转发至您指定的邮箱，支持深度后台保活与电池优化白名单配置。

> ⚠️ **免责声明**：本软件仅供个人多设备管理（如备用机验证码同步）使用。请勿用于任何非法监听他人隐私或协助电信网络诈骗等违法行为。

---

## ✨ 核心功能

1. **实时短信监听与转发**：精准拦截收到的短信，自动提取发件人、短信内容及时间戳，并通过邮件转发。
2. **自定义 SMTP 邮箱配置**：在 UI 界面上自由配置 SMTP 服务器、端口（支持 465 纯 SSL 加密 / 587 STARTTLS）、发件箱账号、授权码及收件箱。
3. **深度后台保活机制**：
   - 采用 前台服务（Foreground Service） + 驻留通知栏。
   - 提供快捷一键申请“电池优化白名单”（忽略电池优化），防止后台被系统杀死。
4. **简洁全中文界面**：友好易懂的操作面板与状态提示。

---

## 🛠️ 项目架构

```
app/src/main/java/com/example/smsforwarder/
├── email/               # 邮件转发逻辑（JavaMail / SMTPS / 配置存储）
├── receiver/            # 短信广播接收器 (SmsReceiver)
├── service/             # 后台保活服务 (KeepAliveService)
├── util/                # 电池优化与系统设置工具 (BatteryOptimizationUtil)
└── ui/                  # 控制台主界面 (MainActivity)
```

---

## 🚀 快速开始与编译部署

### 准备环境
- Android Studio / Android SDK (API 34+)
- JDK 17+
- Gradle 8.5+

### 编译步骤
```bash
# 克隆仓库
git clone <您的仓库地址>

# 编译 Debug 版 APK
./gradlew assembleDebug
```
编译生成的 APK 路径为：`app/build/outputs/apk/debug/app-debug.apk`

---

## 📧 邮箱配置说明（以 QQ / 163 邮箱为例）

1. **QQ 邮箱配置推荐**：
   - **SMTP 服务器**：`smtp.qq.com`
   - **端口**：`465`（自动开启 SSL 加密）
   - **密码**：前往 QQ 邮箱网页版 -> 设置 -> 账户 -> 开启 POP3/SMTP 服务，生成并填写 **授权码**（并非 QQ 登录密码）。

2. **网易 163 邮箱配置推荐**：
   - **SMTP 服务器**：`smtp.163.com`
   - **端口**：`465`
   - **密码**：填写网易邮箱设置中生成的 **客户端授权密码**。

---

## 📜 许可证

本开源项目基于 [MIT License](LICENSE) 授权许可。
