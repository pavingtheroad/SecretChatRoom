-- KEYS[1] = chat:room:{roomId}
-- KEYS[2] = chat:room:{roomId}:members
-- ARGV[1] = userId
-- ARGV[2] = roomId

if redis.call('EXISTS', KEYS[1]) == 0 then
    return 0
end
local members = redis.call('SMEMBERS', KEYS[2])
for i = 1, #members do
    local userPKId = members[i]
    redis.call("SREM", "user:"..userPKId..":rooms", ARGV[2])
    redis.call("DEL", "chat:room:"..ARGV[1]..":user:"..userPKId..":cursor")
end
redis.call("DEL", KEYS[1])
redis.call("DEL", KEYS[2])
redis.call("DEL", "chat:room:"..ARGV[2]..":msg")    -- chat:room:{roomId}:msg (redis stream)
redis.call("DEL", "chat:room:"..ARGV[2]..":state")    -- chat:room:{roomId}:state (redis hashkey)
redis.call("SREM", "chat:room", ARGV[2])    --从房间索引集合中删除

return 1