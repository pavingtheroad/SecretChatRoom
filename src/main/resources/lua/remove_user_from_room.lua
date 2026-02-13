-- KEYS[1] = chatroom:{roomId}
-- KEYS[2] = chatroom:{roomId}:members
-- KEYS[3] = user:{userId}:rooms
-- ARGV[1] = userId
-- ARGV[2] = roomId

if redis.call('EXISTS', KEYS[1]) == 0 then
    return 0
end

local inRoom = redis.call('SISMEMBER', KEYS[2], ARGV[1])
local inUser = redis.call('SISMEMBER', KEYS[3], ARGV[2])

if inRoom == 0 and inUser == 0 then
    return -1    -- 关系不存在
end

-- 可以扩展一个数据损坏的返回机制
-- inRoom == 0 and inUser == 1 return -2
-- inRoom == 1 and inUser == 0 return -3
-- Java侧拿到该返回值再进行数据修复

-- 断开关系（允许修复不一致）
redis.call('SREM', KEYS[2], ARGV[1])
redis.call('SREM', KEYS[3], ARGV[2])

return 1