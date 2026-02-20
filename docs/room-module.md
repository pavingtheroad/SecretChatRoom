# Room 模块 MVP 技术文档

---

# 一、模块目标

Room 模块负责：

* 房间生命周期管理
* 成员管理
* 权限控制
* 成员关系索引维护
* 房间状态维护
* 与消息模块解耦
* 
---

# 二、Redis 存储模型

## 2.1 Key 命名规范

统一前缀：

```
chat:room:
```

---

## 2.2 数据结构总览

| Key                              | 类型     | 说明        |
|----------------------------------|--------|-----------|
| chat:room:{roomId}               | Hash   | 房间主数据     |
| chat:room:{roomId}:members       | Set    | 房间成员集合    |
| user:{userId}:rooms              | Set    | 用户加入的房间集合 |
| chat:room:{roomId}:msg           | Stream | 消息流       |
| chat:room:{roomId}:state         | Hash   | 房间状态      |
 | chat:room:{roomId}:encryptedKeys | Hash   | 用户加密房间密钥  |

---

## 2.3 房间主 Hash 结构

```
HSET chat:room:{roomId}

roomName     string
ownerId      string
description  string
createdAt    long
muted        boolean
locked       boolean
ttlMillis    long
```

---

## 2.4 成员关系模型

采用双向索引：

```
chat:room:{roomId}:members     -> SADD userId
user:{userId}:rooms            -> SADD roomId
```

所有修改必须原子完成。

---

# 三、核心原子操作（Lua）

---

## 3.1 创建房间

### 原子操作：

* 校验不存在
* 创建房间 hash
* 创建 state
* 添加 owner 到成员集合
* 添加 room 到 user 索引

### 返回值：

| 值 | 含义    |
|---|-------|
| 1 | 创建成功  |
| 0 | 房间已存在 |

---

## 3.2 加入房间

### 原子校验：

* 房间存在
* 未加入
* 未锁定

### 原子写入：

* SADD room members
* SADD user rooms

### 返回值：

| 值  | 含义    |
|----|-------|
| 1  | 成功    |
| 0  | 房间不存在 |
| -1 | 已加入   |
| -2 | 房间锁定  |

---

## 3.3 退出房间

### 原子逻辑：

* 房间存在
* 校验关系存在
* 删除双向索引

允许数据修复。

### 返回值：

| 值  | 含义       |
|----|----------|
| 1  | 成功       |
| 0  | 房间不存在    |
| -1 | 用户与房间无关系 |

---

## 3.4 删除房间

### 原子操作：

* 获取成员列表
* 删除所有用户索引
* 删除 cursor
* 删除 room hash
* 删除 members
* 删除 state
* 删除 msg stream

### 返回值：

| 值 | 含义    |
|---|-------|
| 1 | 删除成功  |
| 0 | 房间不存在 |

---

# 四、控制器接口

## 4.1 REST API端点

### 创建房间
```
POST /room
```
**请求体：** RoomInfo对象
**响应：** ApiResponse<Void> - 房间创建成功

### 加入房间
```
POST /room/{roomId}/members/me
```
**路径参数：** roomId - 房间ID
**响应：** ApiResponse<Void> - 成功加入房间

### 获取房间信息
```
GET /room/{roomId}/info
```
**路径参数：** roomId - 房间ID
**响应：** ApiResponse<RoomInfo> - 房间详细信息

### 获取房间成员列表
```
GET /room/{roomId}/members
```
**路径参数：** roomId - 房间ID
**响应：** ApiResponse<Set<String>> - 成员用户ID集合

### 获取用户加入的房间列表
```
GET /room/joined
```
**响应：** ApiResponse<Set<String>> - 房间ID集合

### 退出房间
```
DELETE /room/leave/{roomId}
```
**路径参数：** roomId - 房间ID
**响应：** ApiResponse<Void> - 成功退出房间

### 房主添加成员
```
POST /room/{roomId}/members
```
**路径参数：** roomId - 房间ID
**请求体：** AddUserRequest { userId }
**响应：** ApiResponse<Void> - 成功添加用户

### 更新房间信息
```
PUT /room/{roomId}
```
**路径参数：** roomId - 房间ID
**请求体：** RoomInfoUpdate对象
**响应：** ApiResponse<Void> - 房间信息更新成功

### 删除房间
```
DELETE /room/{roomId}
```
**路径参数：** roomId - 房间ID
**响应：** ApiResponse<Void> - 房间删除成功

## 4.2 请求/响应模型

### AddUserRequest
```java
public record AddUserRequest(
    String userId
){}
```

### 统一响应格式
所有接口均返回 `ApiResponse<T>` 格式：
```JSON
{
    "code": "SUCCESS",
    "message": "操作描述",
    "data": "具体数据",
    "timestamp": "时间戳"
}
```

## 4.3 认证机制
- 所有接口通过JWT Token进行身份认证
- 用户ID从SecurityContext中自动提取
- 房主操作会进行额外的权限验证

## 4.4 权限控制
- **普通用户**：可创建房间、加入/退出房间、查看房间信息
- **房主**：拥有房间的完全控制权，包括添加成员、修改信息、删除房间
- **锁定房间**：仅房主可添加成员

---

# 五、服务接口定义

---

## 5.1 RoomService

### 创建房间

```
void createRoom(RoomInfo roomInfo)
```

---

### 查询房间信息

```
RoomInfo getRoomInfo(String roomId)
```

---

### 加入房间

```
void joinRoom(String roomId, String userId)
```

---

### 退出房间

```
void leaveRoom(String roomId, String userId, String operatorId)
```

---

### 获取房间成员

```
Set<String> getRoomMembersId(String roomId)
```

---

### 查询用户加入的房间

```
Set<String> joinedRoomsId(String userId)
```

---

### 判断房间是否存在

```
Boolean roomExists(String roomId)
```

---

### 判断访问权限

```
Boolean authorizeRoomAccess(String roomId, String userId)
```

---

## 5.2 RoomOwnerService

### 添加成员

```
void addUserToRoom(String roomId, String userId)
```

仅房主可调用。

---

### 更新房间信息

```
void manageRoomInfo(String roomId, RoomInfoUpdate update)
```

---

### 删除房间

```
void deleteRoom(String roomId)
```

---

# 六、权限模型

---

## 6.1 房主权限

* 更新房间信息
* 删除房间
* 踢出成员

---

## 6.2 普通成员权限

* 加入未锁定房间
* 自主退出

---

## 6.3 权限检查组件

```
RoomAuthorization
```

包含：

```
checkRoomOwner()
authorizeLeaveRoom()
```

---

# 七、异常模型

## 7.1 异常类型说明

| 异常                         | 场景    |
|----------------------------|-------|
| RoomNotFoundException      | 房间不存在 |
| RoomAlreadyExistsException | 创建重复  |
| RoomAuthorityException     | 权限不足  |
| UserAlreadyExistsException | 已加入   |
| UserNotFoundException      | 用户不存在 |
| UserNotInRoomException     | 无关系   |

