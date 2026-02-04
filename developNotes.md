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
            userPKId 42
            timestamp 1630000000
            type "text" 
            userPKId 100001
            content '{***}' `
  总的来说就是**可变KV**采用JSON格式存储，**不可变KV**采用扁平化存储
  * 在发送消息时记录Redis返回的id，用来更新游标
* 查看消息：只加载50条(temporary set)
  * enter room的时候`XREVRANGE`出50条(e.p.)历史消息（最新往前）
  * 追踪新消息：WebSocket
  * 定位历史查看位置：`XRANGE` if 游标消息不存在了 返回无法定位已被删除并更新游标为最新消息
  * 返回最新消息：`XREAD`出最新以前的50条消息

### 构建ApiResponse(更好规范返回值)
- SpringBoot 中定义一个通用的ApiResponse类实现标准化响应

      public record ApiResponse<T>(
              String status,
              String message,
              T data,
              Object metaData
      ) {
      }
- 处理成功的反馈

### 全局异常处理——为什么异常基类必须继承RuntimeException
- 首先要回归到Java异常类体系：`Checked Exception`受检异常和`Unchecked Exception`非受检异常
  - `Checked Exception` 继承 Exception 但不继承 RuntimeException 都是受检异常，必须进行try-catch处理或者throws声明
  - `Unchecked Exception` 继承 RuntimeException 都是非受检异常，不需要进行try-catch处理，但可以进行throws声明
- 为什么全局异常处理器只适用于 RuntimeException
  - Spring MVC 的 `@ExceptionHandler` 和 `@RestControllerAdvice` 是运行时机制，只有Unchecked Exception才能在不处理时通过编译

### Questions Wait For Answer
- Servlet 在 Spring 中的应用
- Mybatis 中 Mapper 与 xml 的映射
  - Mapper 方法名一定要对应 Mybatis 的 id 吗？为什么
- 注入的目的是什么？
- @Service为什么要标在实现类上
- 小型项目架构设计理念（各个层的职责“怎么高内聚低耦合”）\ 抽象依赖与业务耦合的区别？
- SecurityChain的http过滤链中的一些常用配置
  - CSRF（跨站请求伪造防护）：默认开启，为每个客户端分配随机token(类比通行证)
    - 客户端如何携带 token：
      1.放在cookie中：在前端发送请求时从 Cookie 中取出 token 放到请求头/请求体中交给服务器认证。
      *万一 Cookie 中的 token 被挟持怎么办？* 现代 CSRF 防护中还会检查请求头的Origin字段确认域名
- Spring Security 的完整流程
  1. 用户登录信息 -> 服务器验证凭证合法性 -> （验证成功）生成JWT返回客户端 -> 客户端保存JWT并在后续需要权限验证的请求中携带token
- 理解Servlet
  - 生命周期
  - 线程模型