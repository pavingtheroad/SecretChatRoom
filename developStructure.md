# Development Structure
构思该聊天室为高保密版Telegram
## Logic
1. 在握手时从ws请求拿到各项确认握手前需要的参数(roomId,Token,etc.)[JwtHandshakeInterceptor](src/main/java/com/chatroom/websocket/JwtHandshakeInterceptor.java)
~~2. 转发消息时通过发送方session中的roomId将消息广播给其他在同一房间的session~~
## Room Management    
- ~~只关注session层~~
- Already set up the frame (include the rudiment of room division) hoping to expand a JWT authentication system.
  `-- until 2025/10/27`
- 房间改用中间件存储（避免后端服务宕机而丢失房间数据）
  - `HashSet room:{roomId}
        ttlMillis
        createdAt
        owner
        description`
- 房间成员
  - `Set room:{roomId}:users user1 user2 user3`

## Message Service
-- Life Cycle Management

- [MessageService](src/main/java/com/chatroom/message/service/MessageService.java)
## Message Cache
- Message的Redis持久化`-- 2025/10/28`
  - 定义MessageDTO
  - 先使用Sorted Set完成阅后即焚的功能（原生支持TTL消息）**后期可过渡到Redis Stream，实现类似微信的平滑消息流**
  - 存储结构：
    - Sorted Set：key:chatroom:{roomId} value:messageId  score:timestamp
    - SETEX: key:message:messageId value:messageJSON TTL
  - 具体方法：
    - 保存消息并设置TTL
    - 消息绑定至chatRoom
    - 获取房间内的消息（获取messageId)
    - 获取消息
    - 删除消息(目前逻辑为直接删除对应键值对， 完善状态我希望是添加为回收机制，类似Wechat撤回功能)


### 2025/11/14 消息存储的数据结构
* TTL实现：
  * 为每个房间设置元数据 `HSET room:123:config retentionMs 3600` 123房间消息保留1小时
  * 定时任务遍历房间 `O(N) N为房间数` `for(String roomId : rooms) 尊重retentionMs` @Scheduled
    * 剪枝 .xtrim:创建房间时创建一个时刻变量标记时间，每次遍历对比时间戳与当前时间戳，超过时更新房间时间戳并剪枝
* 未读游标：
  * 用户建立自己的游标 `HSET`
  * 用户游标如何更新？传递前端渲染位置。前端请求时机？用户滚动到底部、用户停留1秒（节流throttle)、发送消息后。 
`预防极限并发请求——pipeline传送命令`
* 发送消息：
  * 使用 `XADD room:123:msg * 
            userId 42
            timestamp 1630000000
            type "text" 
            userId 100001
            content '{***}' `
  总的来说就是**可变KV**采用JSON格式存储，**不可变KV**采用扁平化存储
  * 在发送消息时记录Redis返回的id，用来更新游标
* 查看消息：只加载50条(temporary set)
  * enter room的时候`XREVRANGE`出50条(e.p.)历史消息（最新往前）
  * 追踪新消息：WebSocket
  * 定位历史查看位置：`XRANGE` if 游标消息不存在了 返回无法定位已被删除并更新游标为最新消息
  * 返回最新消息：`XREAD`出最新以前的50条消息
