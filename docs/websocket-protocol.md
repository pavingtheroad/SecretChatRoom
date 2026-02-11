# ChatRoom WebSocket Protocol v1.0（MVP）

**Scope**：单实例 WebSocket 即时聊天室（MVP）
**Transport**：WebSocket (Text / JSON)
**Encoding**：UTF-8
**Message Format**：JSON

> 本协议定义客户端与服务器之间基于 WebSocket 的通信语义、消息格式、状态机与错误处理规则。

---

## 1. 连接生命周期（Connection Lifecycle）

### 1.1 建立连接

* 客户端通过 WebSocket 握手连接服务器
* 握手阶段通过 [JwtHandshakeInterceptor](../src/main/java/com/chatroom/websocket/interceptor/JwtHandshakeInterceptor.java) 完成：

    * JWT 合法性校验
    * 用户身份解析（userId）
* 握手成功后通过[ChatWebSocketHandler](../src/main/java/com/chatroom/websocket/handler) 完成：

    * 服务端创建 `SessionContext`
    * 绑定 `sessionId`
    * **此时尚未加入任何房间**

---

### 1.2 连接关闭

连接可能通过以下方式关闭：

| 场景      | 行为            |
| ------- | ------------- |
| 客户端主动关闭 | 服务端释放 Session |
| 心跳超时    | 服务端强制关闭       |
| 网络异常    | 由定时清理任务回收     |
| 服务器异常   | 服务端关闭连接       |

关闭时：

* 自动执行 `leaveRoom`（若已 JOIN）
* 自动执行 `unbindUser`
* 释放 SessionContext

---

## 2. Session 状态模型（State Model）

### 2.1 [SessionContext](../src/main/java/com/chatroom/websocket/domain/SessionContext.java) 字段语义

| 字段         | 含义               |
| ---------- | ---------------- |
| sessionId  | WebSocket 会话唯一标识 |
| userId     | 认证后的用户标识         |
| roomId     | 当前加入的房间（单房间模型）   |
| activeTime | 最近一次活跃时间         |

---

### 2.2 合法状态转换

```text
CONNECTED
   │
   ├── JOIN(roomId)
   │       │
   │       └── IN_ROOM
   │               │
   │               ├── TEXT
   │               ├── HEARTBEAT
   │               └── LEAVE
   │                       │
   │                       └── CONNECTED
   │
   └── CLOSE
```

**约束：**

* 一个 Session 同时只能在一个房间内
* 未 JOIN 不允许发送 TEXT
* LEAVE 后可再次 JOIN

---

## 3. 消息通用结构

### 3.1 客户端请求（[WsMessageRequest](../src/main/java/com/chatroom/websocket/dto/WsMessageRequest.java)）

```json
{
  "requestId": "uuid-string",
  "type": "JOIN | LEAVE | TEXT | HEARTBEAT",
  "roomId": "string (optional)",
  "content": "string"
}
```

#### 字段说明

| 字段        | 必须  | 说明                      |
|-----------| --- | ------------- |
| requestId | 是   | 请求唯一标识，用于 ACK / ERROR   |
| type      | 是   | 消息类型                    |
| roomId    | 视类型 | JOIN / LEAVE / TEXT 时需要 |
| content   | 否   | 扩展数据                    |

---

### 3.2 服务端响应（[WsMessageResponse](../src/main/java/com/chatroom/websocket/dto/WsMessageResponse.java)）

```json
{
  "responseType": "ACK | ERROR | TEXT",
  "requestId": "uuid-string",
  "roomId": "string",
  "userId": "string",
  "code": "string",
  "content": "string",
  "timestamp": 16000000
}
```

---

## 4. 消息类型定义

---

## 4.1 JOIN —— 加入房间

### 请求

```json
{
  "requestId": "xxx",
  "type": "JOIN",
  "roomId": "room-1"
}
```

### 处理规则

1. Session 尚未加入任何房间
2. roomId 合法且存在
3. 用户具备房间访问权限

---

### 成功响应（ACK）

```json
{
  "responseType": "ACK",
  "requestId": "xxx",
  "roomId": "room-1",
  "code": null,
  "message": "Join into room"
}
```

---

### 失败响应（ERROR）

| 错误码             | 场景    |
| --------------- | ----- |
| ALREADY_JOINED  | 已在房间中 |
| ROOM_NOT_FOUND  | 房间不存在 |
| NOT_ROOM_MEMBER | 无访问权限 |
| INVALID_CONTEXT | 请求非法  |

---

## 4.2 LEAVE —— 离开房间

### 请求

```json
{
  "requestId": "xxx",
  "type": "LEAVE",
  "roomId": "room-1"
}
```

### 语义说明

* LEAVE 是 **幂等操作**
* 即使当前未在房间中，也返回成功 ACK

---

### 成功响应（ACK）

```json
{
  "responseType": "ACK",
  "requestId": "xxx",
  "roomId": "room-1",
  "code": "LEAVE_ROOM_SUCCESS",
  "message": "OK"
}
```

---

## 4.3 TEXT —— 房间消息

### 请求

```json
{
  "requestId": "xxx",
  "type": "TEXT",
  "roomId": "room-1",
  "payload": {
    "content": "hello"
  }
}
```

### 处理规则

* 必须已 JOIN 对应房间
* 消息广播至房间内其他 Session

---

### 广播消息（TEXT）

```json
{
  "responseType": "TEXT",
  "roomId": "room-1",
  "userId": "user-123",
  "message": "hello",
  "timestamp": 1700000000000
}
```

---

## 4.4 HEARTBEAT —— 心跳保活

### 请求

```json
{
  "requestId": "xxx",
  "type": "HEARTBEAT"
}
```

### 语义

* 用于保持连接活跃
* 更新 `activeTime`
* **不返回 ACK**

---

## 5. ACK / ERROR 语义说明

### 5.1 ACK

* ACK 表示：

  > **服务端已成功处理该请求**
* 不表示：

    * 对方已收到消息
    * 消息已被持久化

---

### 5.2 ERROR

* ERROR 表示请求被拒绝或非法
* 不会导致连接关闭（除严重协议错误）

---

## 6. 心跳与会话清理机制

* 客户端需定期发送 HEARTBEAT
* 服务端维护 `activeTime`
* 定时任务扫描超时 Session 并关闭连接

---

## 7. 断线重连语义（MVP）

* 断线即 Session 失效
* 不保留房间状态
* 客户端需重新：

    1. 建立连接
    2. JOIN 房间

> MVP 不支持断线状态恢复

---

## 8. 非支持项（Explicit Non-Goals）

当前协议 **不支持**：

* 多房间同时在线
* 消息重发 / 已读回执
* 多节点 WebSocket 集群
* 服务端推送 ACK 给广播接收者

---