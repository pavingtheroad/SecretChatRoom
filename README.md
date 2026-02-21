# 隐私聊天室
> 一个基于 Spring Boot + Redis + WebSocket + JWT 的实时聊天室后端项目，支持 Redis Stream 消息持久化与端到端加密（E2EE）架构。
# ChatRoom - Spring Boot + Redis + WebSocket

一个基于 **Spring Boot + Redis Stream + WebSocket + JWT** 的实时聊天室后端项目。
当前版本支持：

* 用户注册 / 登录 / 权限管理
* 房间创建与成员管理
* WebSocket 实时消息
* Redis Stream 持久化
* 游标分页模型
* 端到端加密（E2EE）

---

# 技术栈

* Spring Boot
* Spring Security + JWT
* Redis (Stream + Hash)
* MySQL
* WebSocket
* MyBatis
* Maven

---

# 项目结构

```text
chatroom/
├── auth            权限模块(登录)
├── component       组件模块
├── user            用户模块
├── room            房间模块
├── message         消息模块
├── websocket       协议与连接管理
├── security        JWT 与权限模型
├── util            工具类
├── config          配置文件
└── exceptionHandler 错误处理
```

---

# 模块说明

---

## User 模块

负责：

* 用户注册 / 注销
* 角色管理
* 用户生命周期
* 与 Spring Security 对接
* JWT 主体身份来源

详细设计文档：

[User 模块设计](./docs/user-module.md)

---

## Room 模块

负责：

* 房间创建
* 房间成员管理
* 房间访问权限校验
* 房间级授权模型

详细设计文档：

[Room 模块设计](./docs/room-module.md)

权限模型：

* owner
* member
* 非成员不可访问

---

## Message 模块

负责：

* WebSocket 消息写入
* Redis Stream 持久化
* 分页历史查询
* 阅读游标管理

详细设计文档：

[Message 模块设计](./docs/message-module.md)

---

### 分页模型

采用：

> 页面边界 ID 分页模型

支持：

* init 初始化
* earlier 向前翻页
* later 向后翻页

---

## WebSocket 协议

通信基于：

* JSON
* 单房间模型

协议文档：

[WebSocket 协议文档](./docs/websocket-protocol.md)

---

支持消息类型：

* JOIN
* LEAVE
* TEXT
* HEARTBEAT

状态机模型：

```text
CONNECTED
  └── JOIN
        └── IN_ROOM
              ├── TEXT
              ├── HEARTBEAT
              └── LEAVE
```

---

# 安全模型

---

## 身份认证

* 登录成功后生成 JWT
* WebSocket 握手阶段校验 JWT
* JWT subject = userPKId

---

## 授权模型

* 系统级权限 → 角色控制
* 房间级权限 → Room 模块控制
* 消息接口 → 必须校验房间访问权限

---

## E2EE 端到端加密模型

当前版本支持前端端到端加密设计：

### 密钥模型

* 每个用户生成：
    * 公钥
    * 私钥（仅客户端保存）

* 每个房间生成：
    * roomKey（对称密钥）

### 分发流程

1. 房主生成 roomKey
2. 使用成员公钥加密 roomKey
3. 成员使用私钥解密得到 roomKey
4. 房间内消息使用 roomKey 加密

### 重要安全原则

* 服务端不保存私钥
* 服务端不解密消息
* Redis 中存储的是密文
* 服务端仅负责转发与持久化

---

# 消息流转流程

### 写入链路

```
Client
  ↓
WebSocket
  ↓
MessageProcessor
  ↓
Redis Stream
  ↓
广播
```

---

### 查询链路

```
HTTP
  ↓
MessageQueryService
  ↓
Redis Stream
  ↓
DTO
  ↓
返回客户端
```

---

# 当前 MVP 特性范围

已实现：

* 单实例 WebSocket
* Redis Stream 持久化
* 游标分页
* 房间级授权
* JWT 认证
* E2EE 设计支持

未实现：

* 多节点 WebSocket 集群
* 消息撤回
* 未读计数
* 消息搜索
* 断线恢复
* 消息重发机制

---

# 并发语义说明

当前游标模型为：

> 最后写覆盖语义

并发翻页时不保证严格单调递增。

---

# 开发文档索引

* [User 模块](./docs/user-module.md)
* [Room 模块](./docs/room-module.md)
* [Message 模块](./docs/message-module.md)
* [WebSocket 协议](./docs/websocket-protocol.md)