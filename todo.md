1. ~~排查问题：根据claude.md和docs/index.md中的文档了解项目。排查问题：server在运行一段时间后响应速度会变得很慢，需要重启才能解决。~~

**已修复**：根本原因是 `RedisCookieStorage` 为每个用户会话创建独立的 `RedisClient`（含独立 Netty event loop），导致：
- 资源累积：N 个用户 = N 个 Redis 客户端 + N 组 Netty 线程，持续消耗内存
- 空闲连接断开：30 分钟不活跃后 Redis 连接超时，清理时触发 "Connection is already closed" 警告
- 一次性客户端：`clearSubject()` 每次创建临时 `RedisClient` 再立即销毁

修复措施：
- `RedisCookieStorageFactory` 管理共享的单一 `RedisClient` + `StatefulRedisConnection`
- 所有 `RedisCookieStorage` 实例共用工厂提供的 `RedisCommands`，不再各自创建连接
- `clearSubject()` 使用共享连接代替创建一次性客户端
- `SessionManager.close()` 现在也正确关闭 `CookieStorageFactory` 的共享连接
- `ManagedCookieStorageFactory` 接口新增 `close()` 默认方法

2. ~~完成修改后同步更新文档。~~

**已完成**：更新了以下文档：
- `docs/architecture-server.md`：会话持久化层描述新增 `RedisCookieStorageFactory` 及共享连接说明
- `docs/data-models-server.md`：`RedisCookieStorage` 章节新增连接管理说明
- `REDIS_MIGRATION.md`：更新 `RedisCookieStorage` 主要特性描述