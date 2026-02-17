# Message 模块设计文档（MVP 1.0）

---

# 一、模块职责（Module Responsibility）

Message 模块负责：

1. 实时消息写入（WebSocket）
2. 消息持久化（Redis Stream）
3. 历史消息分页查询（HTTP）
4. 用户阅读进度记录（Cursor）
5. 消息数据转换与输出

本模块不负责：

* 房间创建与成员管理（由 Room 模块负责）
* 用户身份认证（由 Security 模块负责）
* 消息加密逻辑（前端完成）

---

# 二、整体架构（Architecture Overview）

Message 模块分为两条链路：

### 1️⃣ 写入链路（实时）

客户端 → WebSocket → MessageProcessor → MessageWriteService → Redis Stream → ACK + 广播

### 2️⃣ 查询链路（历史）

客户端 → HTTP Controller → MessageQueryService → Redis Stream → DTO → 返回结果

---

# 三、核心组件结构

## 1. MessageProcessor（WebSocket入口）

职责：

* 解析 TEXT 消息
* 校验 requestId 幂等
* 调用写入服务
* 广播消息
* 返回 ACK

---

## 2. MessageWriteService

职责：

* 构造 Redis Stream 记录
* XADD 写入
* 保证幂等
* 返回生成的 messageId

---

## 3. MessageQueryService

职责：

* 初始化查询
* 分页查询（earlier / later）
* 阅读游标更新
* Redis 记录转换为 MessageDTO

核心方法：

```
initMessageQuery(roomId, userPkId, limit)
getForwardMessages(roomId, userPkId, start, limit)
getBackwardMessages(roomId, userPkId, end, limit)
```

---

## 4. MessageCacheRepository

封装 Redis Stream 操作：

* rangeMessages
* reverseRangeMessages
* getLastMessageId

---

## 5. MessageCursorRepository

负责：

* 获取用户阅读进度
* 更新阅读进度

存储结构：

```
key: message:cursor:{roomId}
field: userPKId
value: lastReadMessageId
```

---

# 四、数据结构设计

## Redis Stream

```
key: chat:room:{roomId}
```

字段：

| 字段名       | 含义    |
| --------- | ----- |
| senderId  | 发送者ID |
| type      | 消息类型  |
| content   | 消息内容  |
| createdAt | 创建时间  |

Stream ID 作为：

```
messageId
```

---

## 阅读游标

```
Hash: message:cursor:{roomId}
```

结构：

```
userPKId -> lastReadMessageId
```

语义：

> 记录用户当前阅读位置

---

# 五、分页语义设计

采用：

> 页面边界 ID 分页模型

### 1️⃣ 初始化

* 查询用户阅读游标
* 若不存在 → 使用最新消息 ID
* 从该位置向前查询 limit 条
* 更新阅读游标

---

### 2️⃣ 查询更早消息（earlier）

输入：

* 当前页面最旧 ID（start）

行为：

* 查询 start 之前的 limit 条
* 更新阅读游标为当前页最旧 ID

---

### 3️⃣ 查询更新消息（later）

输入：

* 当前页面最新 ID（end）

行为：

* 查询 end 之后的 limit 条
* 更新阅读游标为当前页最新 ID

---

# 六、HTTP 接口定义

基路径：

```
/rooms/{roomId}/messages
```

---

## 1️⃣ 初始化查询

```
GET /rooms/{roomId}/messages/init?limit=20
```

返回：

```
List<MessageDTO>
```

---

## 2️⃣ 查询更早

```
GET /rooms/{roomId}/messages/earlier?start={id}&limit=20
```

---

## 3️⃣ 查询更新

```
GET /rooms/{roomId}/messages/later?end={id}&limit=20
```

---

# 七、安全模型

每个 HTTP 接口必须：

1. 从 SecurityContext 获取 userPKId
2. 调用 roomService.authorizeRoomAccess(roomId, userPKId)
3. 校验 Stream ID 合法性

WebSocket 握手阶段已校验：

* JWT
* 房间访问权限

---

# 八、异常处理规范

| 场景           | HTTP状态   |
| ------------ | -------- |
| 非法 Stream ID | 400      |
| 无房间权限        | 403      |
| 房间不存在        | 404      |
| 正常无数据        | 200 + [] |

统一使用：

```
ApiResponse<T>
```

---

# 九、并发语义说明

当前阅读游标采用：

> 最后写覆盖语义

即：

* 并发翻页时
* 后一次请求更新游标为最终阅读位置

MVP 阶段不保证严格单调递增。

---

# 十、当前模块边界

MVP 1.0 不包含：

* 未读计数统计
* 消息撤回
* 消息编辑
* 消息搜索
* 批量已读更新
* hasMore 标志
* 消息总数统计