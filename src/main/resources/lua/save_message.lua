-- KEYS[1] = chat:room:<room_id>:msg
-- KEYS[2] = chat:room:<room_id>:req:<request_id>
-- KEYS[3] = chat:room:<room_id>:state
-- ARGV[1] = request_id
-- ARGV[2] = user_id
-- ARGV[3] = type
-- ARGV[4] = content
-- ARGV[5] = createdAt
local req_key = KEYS[2]
local exists = redis.call("SETNX", req_key, "1")
if exists == 0 then
    return nil    -- 重复写入
end
redis.call("PEXPIRE", req_key, 120000)    -- 2 minutes
local stream_key = KEYS[1]
local user_id = ARGV[2]
local msg_type = ARGV[3]
local content = ARGV[4]
local timestamp = ARGV[5]
local message_data = {
    "senderId", user_id,
    "type", msg_type,
    "content", content,
    "createdAt", timestamp
}
local message_id = redis.call("XADD",
        stream_key,
        "MAXLEN", "~", 20000,
        "*",
        unpack(message_data))    -- 消息持久化

redis.call("HSET", KEYS[3], "last_msg_id", message_id)    -- 更新房间最后消息ID
return message_id