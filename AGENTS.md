# Codex 协作说明

本文档为 Codex 在本仓库协作时的工作指引。

## 开发环境

- 使用 **JDK 21**。`androidApp`、`composeApp`、`shared`、`server` 的 Gradle 配置都已统一到 Java/Kotlin 21。
- 本地开发前先复制环境变量模板：
  - `cp .env.sample .env`
- `shared/build.gradle.kts` 会在**构建时**把 `API_ENDPOINT` 写入 `BuildKonfig.API_ENDPOINT`：
  - 优先读取根目录 `.env`
  - 其次读取系统环境变量 `API_ENDPOINT`
  - 两者都没有时，回退到 `https://ubaa.mofrp.top`
- 修改 `API_ENDPOINT` 后，需要重新构建客户端模块，`composeApp` / `androidApp` / iOS Framework / Web 产物不会在运行时热切换后端地址。
- 当前服务端默认会话持久化路径为 **Redis**：
  - `SessionManager` 默认使用 `RedisSessionStore` 与 `RedisCookieStorageFactory`
  - `REDIS_URI` 通过运行时环境变量读取，未设置时默认 `redis://localhost:6379`
  - 注意：当前 `.env.sample` **没有**列出 `REDIS_URI`，如需连接非默认 Redis，请自行在运行环境中提供

## 常用命令

### 本地运行

- 启动后端：
  - `./gradlew :server:run`
- 以 Ktor 开发模式启动后端：
  - `./gradlew :server:run -Pdevelopment`
- 启动桌面客户端：
  - `./gradlew :composeApp:run`
- 启动 Web（Wasm）开发服务器：
  - `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- 安装 Android Debug 包到设备/模拟器：
  - `./gradlew :androidApp:installDebug`
- iOS：
  - 使用 Xcode 打开 `iosApp/iosApp.xcodeproj`

### 构建

- 构建后端：
  - `./gradlew :server:build`
- 构建共享模块：
  - `./gradlew :shared:build`
- 构建 Compose Multiplatform UI 模块：
  - `./gradlew :composeApp:build`
- 构建 Android Debug APK：
  - `./gradlew :androidApp:assembleDebug`

说明：
- 本地开发优先使用模块级任务，避免不必要的全仓构建。
- `androidApp` 的 Release 签名读取 `local.properties` 或环境变量，根级构建可能带出你当前并不需要的 Android Release 流程。
- 如需打包当前发布服务器产物，可使用 `./gradlew :server:buildFatJar`。

### 测试

优先使用显式模块任务，而不是笼统依赖根级 `test`。

- 后端测试：
  - `./gradlew :server:test`
- `shared` 的 JVM 测试：
  - `./gradlew :shared:jvmTest`
- `composeApp` 的 JVM 测试：
  - `./gradlew :composeApp:jvmTest`
- 本地较完整的一组验证：
  - `./gradlew :server:test :shared:jvmTest :composeApp:jvmTest`

单测示例：

- 单个后端测试类：
  - `./gradlew :server:test --tests "cn.edu.ubaa.ApplicationTest"`
- 单个后端测试方法：
  - `./gradlew :server:test --tests "cn.edu.ubaa.ApplicationTest.testRoot"`
- 单个 `shared` 测试类：
  - `./gradlew :shared:jvmTest --tests "cn.edu.ubaa.api.AuthServiceTest"`
- 单个 `shared` 测试方法：
  - `./gradlew :shared:jvmTest --tests "cn.edu.ubaa.api.AuthServiceTest.shouldReturnLoginResponseWhenLoginSuccess"`
- 单个 `composeApp` 测试类：
  - `./gradlew :composeApp:jvmTest --tests "cn.edu.ubaa.ui.AuthViewModelTest"`
- 单个 `composeApp` 测试方法：
  - `./gradlew :composeApp:jvmTest --tests "cn.edu.ubaa.ui.AuthViewModelTest.testInitialState"`

说明：
- `shared` 与 `composeApp` 的测试代码主要位于 `commonTest`，但本地最稳定的入口仍然是 JVM 测试任务。
- 当前认证/会话主路径依赖 Redis，会话相关本地运行与测试如需完整验证，应确保默认 Redis 可用或正确提供 `REDIS_URI`。

### 覆盖率与 Lint

- 覆盖率报告：
  - `./gradlew koverHtmlReport`
- Lint：
  - `./gradlew lint`

说明：
- `koverHtmlReport` 是根项目聚合任务。
- 当前 `lint` 实际对应 `:androidApp:lint`，仓库里没有统一启用的 ktlint / detekt。

## 高层架构

本仓库是一个 Kotlin Multiplatform 全栈项目，整体由以下部分构成：

- `shared`：共享 DTO、客户端 API 层、令牌/凭据/`clientId` 存储，以及多平台 HTTP 基础设施
- `composeApp`：真正的跨平台客户端 UI 模块，覆盖 Desktop / Web / iOS / Android Compose
- `androidApp`：围绕 `composeApp` 的 Android 应用壳层
- `iosApp`：嵌入 `ComposeApp` Framework 的 Xcode / SwiftUI 壳工程
- `server`：适配北航系统并统一暴露 `/api/v1/*` 接口的 Ktor 后端网关

依赖方向：

- `composeApp` 依赖 `shared`
- `server` 依赖 `shared`
- `androidApp` 依赖 `composeApp`
- `iosApp` 通过 `composeApp` 生成的 Framework 嵌入客户端 UI

### `shared`

`shared` 是**共享契约层与客户端基础设施层**，不是后端业务实现层。

它包含：
- 客户端与服务端共用 DTO
- 基于 Ktor Client 的 API 封装
- `TokenStore` / `CredentialStore` / `ClientIdStore`
- 平台特定 HTTP 引擎绑定
- `BuildKonfig.API_ENDPOINT` 与版本常量

当你调整 DTO、API 路径或认证流程时，需要同时核对 `shared`、`server` 以及消费这些接口的 `composeApp`。

### `composeApp`

`composeApp` 是主 UI 模块。

关键结构：
- `App.kt`：顶层状态机，负责启动页、会话恢复、登录/主界面切换和更新检查
- `ui/navigation/MainAppScreen.kt`：应用主壳层，负责导航栈、顶栏/底栏/侧边栏和各功能 ViewModel 的组织
- 绝大多数跨平台 UI 变更都应落在这里，而不是放进 `androidApp` 或 `iosApp`

当前主要功能域包括：
- 认证
- 课表
- 考试
- BYKC
- 签到
- 空教室
- 自动评教
- 菜单与导航

### `androidApp` 与 `iosApp`

二者都是平台壳层：

- `androidApp/src/main/java/cn/edu/ubaa/MainActivity.kt` 负责承载 `App()`
- `iosApp/iosApp/ContentView.swift` 负责嵌入 `ComposeApp.MainViewController()`

如果是跨平台 UI 行为或业务体验的变更，通常应修改 `composeApp`，而不是平台壳层。

### `server`

`server` 是一个 **Ktor + Netty 网关**，不是围绕本地数据库的传统 CRUD 应用。

核心职责：
- CAS / SSO 登录编排
- JWT 鉴权
- 服务端会话与上游 Cookie 管理
- 将北航多个系统适配为统一 API
- 暴露 `/metrics` 指标

`server/src/main/kotlin/cn/edu/ubaa/Application.kt` 负责：
- 启动 Netty
- 读取 `SERVER_PORT` 与 `SERVER_BIND_HOST`
- 安装 Micrometer、CallLogging、CORS、JSON、JWT 等插件
- 注册各业务路由

路由边界：
- 匿名接口：`/api/v1/auth/*`
- JWT 保护接口：`/api/v1/user`、`/api/v1/schedule`、`/api/v1/exam`、`/api/v1/bykc`、`/api/v1/signin`、`/api/v1/classroom`、`/api/v1/evaluation`

需要注意的业务边界：
- 认证模型不是“纯 JWT 无状态认证”
- 服务端虽然会签发 JWT，但仍然需要维护每个用户对应的上游会话、Cookie 和 `HttpClient`
- `SessionManager` 是预登录会话、正式会话提升、JWT 到会话映射、TTL 管理、会话恢复与清理的中心
- 当前主路径使用 Redis 进行会话与 Cookie 持久化；仓库中仍保留 SQLite 相关类作为历史/备选实现
- `authRouting()` 中的 `/status` 与 `/logout` 依然需要 `Authorization` 头，只是没有被放进统一的 `authenticate {}` 路由块中

### 外部系统适配

后端主要围绕外部北航系统适配，而不是维护庞大的本地领域模型。

当前主要域包括：
- auth / user
- schedule
- exam
- BYKC
- signin
- classroom
- evaluation

适配层特点：
- `BYKC` 具有独立的下游协议与加解密逻辑
- `Signin` 受主认证保护，但其下游交互由独立签到客户端处理
- `Evaluation` 通过专门的下游客户端执行评教流程
- `Classroom`、`Schedule`、`Exam` 等模块围绕统一后端会话与上游系统解析展开

## 运行与配置说明

`.env.sample` 当前包含：
- `API_ENDPOINT`
- `SERVER_PORT`
- `SERVER_BIND_HOST`
- `JWT_SECRET`
- `USE_VPN`

额外运行时配置：
- `REDIS_URI`：当前未写入 `.env.sample`，但服务端代码会从环境变量读取，缺省值为 `redis://localhost:6379`
- `HTTP_PROXY` / `HTTPS_PROXY`：后端上游请求代理
- `TRUST_ALL_CERTS`：仅用于开发或调试场景，允许宽松证书校验

`USE_VPN` 与服务端访问北航内网系统有关：
- `server/src/main/kotlin/cn/edu/ubaa/utils/VpnCipher.kt` 在启用时会把部分内网 URL 转写为 WebVPN 地址
- 代码中虽然存在 `autoDetectEnvironment()` 辅助方法，但当前启动流程没有接入该方法，不应默认认为未设置 `USE_VPN` 时会自动探测环境

## 测试目录布局

- `server/src/test/kotlin`：后端测试
- `shared/src/commonTest/kotlin`：共享层 / 客户端 SDK 测试
- `composeApp/src/commonTest/kotlin`：Compose UI / 状态测试

CI 中的 `.github/workflows/test.yml` 当前更准确地说是“多平台编译/装配检查”，而不是一个会跑完全部测试任务的总入口。