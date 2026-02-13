## 2025/12/11
### aim：房间管理设计
1. 确定 Redis 存储模型
   1. 房间元数据 `HASH` 
   
            `chat:room:{roomId}
                  roomName
                  ownerId    (存储时为userPKId)
                  description
                  createdAt
                  muted    (禁言)
                  locked    (是否公共开放)
                  ttlMillis`   
   2. 房间成员列表 `SET`

      `chat:room:{roomId}:members `
   3. 用户加入的房间列表 `SET`
   
      `chat:user:{userPKId}:rooms `
2. 接口设计（写数据时注意原子性）
   1. RoomRepository
      1. createRoom `params: RoomInfoDTO`    创建房间
      2. getRoomInfo `params: roomId` `return: RoomInfoDTO`    获取房间信息
      3. addUserToRoom `params: roomId, userPKId`    用户加入房间
      4. removeUserFromRoom `params: roomId, userPKId`   用户退出房间
      5. getRoomMembers `params: roomId` `return: Set<String>`    房间成员表(获取id)
      6. joinedRooms `params: userPKId` `return: Set<String>`    房间信息(获取id)
      7. deleteRoomAtomic `params: roomId`    删除房间（同时删除
                                          `room:{roomId} (hash) `
                                          `room:{roomId}:members`
                                          `chat:room:{roomId}:msg (redis stream)` 
                                          `chat:room:{roomId}:user:{userPKId}:cursor`
                                          `user:{userPKId}:rooms`
         因为要操作多个数据，所以要保证原子性：Lua / pipeline
      8. updateRoomInfo `params: roomId, RoomInfoDTO`    更新房间信息
      9. authorizeRoomAccess `params: roomId, userPKId` `return: boolean`    房间级别用户鉴权（是否能够进入房间）
      10. roomExists `params: roomId` `return: boolean`    房间存在校验(读校验)
   2. RoomService
      1. createRoom `params: RoomInfoDTO`
      2. getRoomInfo `params: roomId` `return: RoomInfoDTO`    获取房间信息
      3. addUserToRoom `params: roomId, userPKId`    用户加入房间
      4. removeUserFromRoom `params: roomId, userPKId`    用户退出房间
      5. getRoomMembers `params: roomId` `return: List<UserInfoDTO>`    房间成员表(聚合具体信息)
      6. joinedRooms `params: userPKId` `return: RoomInfoDTO[]`    用户加入的房间列表
      7. deleteRoom `params: roomId`    删除房间
      8. updateRoomInfo `params: roomId, RoomInfoDTO`
      9. authorizeRoomAccess `params: roomId, userPKId` `return: boolean`
      10. roomExists `params: roomId` `return: boolean`    房间存在校验

## 2025/12/16 - 2025/12/19
### 完成房间管理模块
Notes
1. ~~com.chatroom.room.dao：RoomCacheRepository中的addUserToRoom需要二次开发，缺少**一致性**与**原子性**维护~~
2. addUserToRoom 的行为语义是**建立关系**的操作，所以逻辑层面应当是**要么都没有，要么都存在**，否则就是存在不一致状态，进入修复功能
3. 编写接口逻辑功能时需要注意**幂等性**

## 2025/12/20
### 用户管理设计
1. 存储模型
   1. Mysql作为持久层存储用户信息，只包含基本信息（id, 用户名，头像，密码， // 关联邮箱）
   2. UserInfoDTO 

            userPKId:
            userName:
            avatarUrl:
   
   3. UserEntity
   
            id:(PK, auto_incr)
            userPKId:(uuid)    对外使用
            userName:
            avatarUrl:
            password:(不可逆哈希)
            email:
            status(ACTIVE/ DELETED/ BANNED)    尽量不物理删除用户

   4. UserRegisterDTO

            userName:
            password:
            email:
            avatarUrl:(optional)

2. 接口设计
   1. UserRepository
      1. saveUser `params: UserEntity`
      2. getUserById `params: userPKId` `return: UserEntity`
      3. getUserByName `params: userName` `return: List<UserEntity>`
      4. updateUserStatus `params: String userPKId, UserStatus status`    （修改数据库中用户状态）
      5. updateUserInfo `params: userPKId, UserEntity`    更新用户信息
   2. UserService
      1. registerUser `params: UserRegisterDTO`    注册用户(留意并发问题)(邮箱注册用户，最终实现版本)
      2. getUserById `params: userPKId` `return: UserInfoDTO`
      3. SearchUserByName `params: userName` `return: List<UserInfoDTO>`
      4. cancelUser `params: targetUserId, operatorUserId`    注销用户(仅允许用户自行注销和管理员注销)，需要做防御性检测确定是否为用户本人或管理员角色
         - operatorUserId由认证模块得到表示进行这个操作的用户，具体在实现时的逻辑为先验证是否为本人，如果为本人直接进行操作，
            否则进入管理员身份鉴权，鉴权为管理员时允许进一步操作，否则警告无权限
      5. updateUserInfo `params: targetUserId, operatorUserId, UserInfoDTO`    更新用户信息
      6. bannedUser `params: targetUserId, operatorUserId`    封禁用户(仅允许管理员操作)
### thoughts
1. 房间如何添加用户? 通过用户查询添加，还是使用好友功能；除了被邀请加入外，还应当有用户自主搜索房间并加入(通过roomId)    Controller层实现？
    - 添加用户是房间模块功能；好友需要新增模块 ; 防止Room模块,User模块,Friend模块的耦合

## 2025/12/21 - 2025/12/22
### 完成用户模块
Notes
- 插入数据时应对并发场景：通过事务+事务隔离，保证数据原子性操作与一致性
- ** 用户权限表结构更新： ** Role-Based Access Control
  
      user: Id (PK)
            userPKId
            userName
            avatarUrl
            password
            email
            status

      permission: id (PK)
                permission

      role: id (PK)
                role

      role_permission: role_id,permission_id (PK)(FK)

      user_role: user_id,role_id (PK)(FK) 
## 2026/01/16
### 控制层设计
- 普通用户
    1. 注册账户
        - 参数为[RegisterDTO](../src/main/java/com/chatroom/user/dto/UserRegisterDTO.java)
        - 返回状态码以及提示信息 ResponseEntity
    2. 用户信息更新
    3. 用户注销
    4. ID查询用户(自己)
- 管理员
    1. 添加其他管理员
        - 新增管理员服务层模块：Role管理方法
    2. 封禁用户
    3. 根据ID查询用户
- 房间管理
    1. 创建房间
    2. 更新房间基本信息
    3. 删除房间
    4. 查询房间信息（基本信息&用户列表）
    5. 邀请用户加入房间
    6. 移除用户
## 2026/01/20
### 用户模块各功能进一步内聚
- 用户功能
    - 用户注册/注销
    - 用户信息操作
    - 修改用户状态
- 角色功能
    - 为用户添加角色身份
    - 为用户删除角色身份
## 2026/01/21
### 房间模块进一步优化

## 2026/01/23
### 消息模块设计
- 写功能
    1. 权限校验
    2. 触发写后事件
- 读功能
    1. 读取操作（初始化读取，向前、后翻页）
    2. 游标维护
        -  游标和房间一起通过chat:room:{roomId}删除
        - 房间剪枝后将游标更新到目前最旧id
    3. 权限校验
- 存储模型

        消息
        chat:room:{roomId}:msg 
                             messageId(Redis Stream生成)
                             userPKId (数据库user表的PKID)
                             type (text/image/file..)
                             content (具体信息)
                             timestamp (时间戳，方便trim剪枝)
        游标
        chat:room:{roomId}:userPKId:{userPKId} 
                             cursor
- 剪枝策略
    1. 确定业务时间
    2. 找到第一条时间戳：进行有limit的顺序读取记录StreamID到cutOffTime
    3. 执行剪枝
    4. cursor兜底机制（退化策略）：剪枝后在 MessageQueryService 里检测：cursor 不存在 → reset 到 earliest / latest
- 房间状态持久层接口：避免反复XREVERSE读取最新消息浪费资源

        chat:room:{roomId}:state
                            lastMessageId
    - updateLastMessageId `params: String roomId, String messageId`
    - getLastMessageId `params: String roomId` `return String`
    - createRoomState `params: String roomId` `return void`
    - deleteRoomState `params: String roomId` `return void`
- 写功能接口
  - saveMessage `params: MessageDTO messageDTO` `return void`

## 2026/01/27
### Security 模块设计
- 构建顺序流程 
    1. UserDetails + UserDetailsService
       - 将UserDetailsService中的userName看作userId进行构建
    2. PasswordEncoder(SecurityConfig)
    3. AuthenticationManager + Login API(LoginService)
    4. JWT generator
    5. JWT Filter
    6. SecurityConfig
    7. IdentityResolver
## 2026/02/02
### Auth 模块设计（登录验证）
- 结构
  - auth
    - controller
      - AuthController
    - service
      - AuthService
    - dto
      - LoginRequest
      - LoginResponse
- AuthService
  - login `params: String userId, String password`
    - 验证客户端的登录凭证 -> 正确则返回JWT
    - 接收客户端的可变userId 和 password, 在凭证验证结束后将不可变的userPKId, Roles保存在JWT中返回给客户端，
    在后续的一系列请求中从token获取的就是不可变的userPKId
## 2026/02/03
### WebSocket 模块设计
- 结构
    - config
    - interceptor
    - handler
    - dto
- handler
    - afterConnectionEstablished（绑定userId、更新 session、绑定roomId）
    - afterConnectionClosed（注销 session、注销user下的session、注销room下的session）
    - handleTransportError（关闭连接）
- session管理
  - sessionContext WebsocketSession源
    * sessionId
    * WebSocketSession
    * userId
    * roomId
  - userSession 用户索引 | 用户可能多端/多连接（用于广播到用户的所有设备）
  - RoomSession 房间索引 | 房间内的在线连接
  - 生命周期
    - 会话、用户|房间 索引创建
    - 会话、用户|房间 索引删除
    - 广播（到房间、用户多端同步）
- Websocket消息协议
    - 请求协议
  
          {
              "type": "JOIN | LEAVE | TEXT | HEARTBEAT",
              "roomId": "string",
              "content": "string",
              "requestId": "uuid"
          }
    - 响应
  
          {
              "type": "ACK | ERROR",
              "requestId": "uuid",
              "roomId": "string",
              “sender": "string",
              "code": "ROOM_MISMATCH | NO_PERMISSION | NOT_IN_ROOM",
              "content": "string"
              "serverTimeStamp": "long"
          }
## 2026/02/06
### WebSocket与Message模块功能划分与重构
- WebSocket
  - MessageDispatcher: 消息投递
- Message
  - MessageWriteService: 消息持久化，接受处理好的WsMessageResponse
## 2026/02/09
### WebSocket逻辑
- MessageHandler -> Router -> Processor -> RoomWriteService | MessageDispatcher
- 协议状态 
    - JOIN

  | 当前状态   | 收到消息  | 结果    |
  | ------ | ----- | ----- |
  | null   | TEXT  | ERROR |
  | null   | JOIN  | OK    |
  | JOINED | JOIN  | ERROR |
  | JOINED | LEAVE | OK    |
  | JOINED | TEXT  | OK    |
## 2026/02/10
### websocket协议文档



