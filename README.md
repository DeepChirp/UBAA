# UBAA (智慧北航 Remake)

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-blue.svg?style=flat&logo=kotlin)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.10.3-blueviolet.svg?style=flat&logo=jetpack-compose)
![Ktor](https://img.shields.io/badge/Ktor-3.4.1-orange.svg?style=flat&logo=ktor)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-lightgrey.svg?style=flat)

**UBAA** 是一款面向北京航空航天大学学生的跨平台客户端，基于 **Kotlin Multiplatform**、**Compose Multiplatform** 和 **Ktor** 构建，覆盖 Android、iOS、Desktop 与 Web。

它不仅是客户端，也是一套统一的校园服务聚合方案：服务端负责适配和清洗校内系统数据，客户端在多端提供一致、现代的使用体验。

## 入口

- 下载发布版：[GitHub Releases](https://github.com/BUAASubnet/UBAA/releases)
- 在线使用：[网页版](https://app.buaa.team)
- 开发文档：[GitHub Wiki](https://github.com/BUAASubnet/UBAA/wiki)

### Arch Linux (AUR)

```bash
yay -S ubaa
```

## 核心能力

- 多端统一：Android、iOS、Desktop、Web 共用核心能力与大部分业务代码。
- 校园服务聚合：统一认证、课表、考试、空闲教室、博雅、签到、评教等能力集中接入。
- 全栈同仓：`shared` 统一前后端契约，`server` 负责网关与会话管理，`composeApp` 负责跨平台 UI。
- 现代体验：基于 Material Design 3，支持系统主题适配与持续更新。

## 开发文档

仓库首页只保留概览信息，详细开发说明统一放在 GitHub Wiki：

- 总览入口：[Wiki 首页](https://github.com/BUAASubnet/UBAA/wiki)
- 起步与配置：[快速开始](https://github.com/BUAASubnet/UBAA/wiki/Quick-Start) / [配置说明](https://github.com/BUAASubnet/UBAA/wiki/Configuration)
- 架构与模块：[架构总览](https://github.com/BUAASubnet/UBAA/wiki/Architecture-Overview) / [shared](https://github.com/BUAASubnet/UBAA/wiki/Module-Shared) / [composeApp](https://github.com/BUAASubnet/UBAA/wiki/Module-ComposeApp) / [server](https://github.com/BUAASubnet/UBAA/wiki/Module-Server)
- 接口与质量：[API 契约](https://github.com/BUAASubnet/UBAA/wiki/API-Contracts) / [测试与质量](https://github.com/BUAASubnet/UBAA/wiki/Testing-and-Quality) / [编码规范](https://github.com/BUAASubnet/UBAA/wiki/Coding-Standards)
- 发布与排障：[发布与部署](https://github.com/BUAASubnet/UBAA/wiki/Release-and-Deployment) / [常见问题](https://github.com/BUAASubnet/UBAA/wiki/Troubleshooting)

## 仓库概览

```text
UBAA/
├── composeApp/   # 跨平台客户端 UI
├── shared/       # 前后端共享契约与通用逻辑
├── server/       # Ktor 后端网关
├── androidApp/   # Android 壳工程
└── iosApp/       # iOS 壳工程
```
