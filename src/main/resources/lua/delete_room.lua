-- KEYS[1] = room:{roomId}
-- KEYS[2] = room:{roomId}:members
-- ARGV[1] = userId
-- ARGV[2] = roomId

if redis.call('EXISTS', KEYS[1]) == 0 then
    return 0
end
local members = redis.call('SMEMBERS', KEYS[2])
for i = 1, #members do
    local userPKId = members[i]
    redis.call("SREM", "user:"..userId..":rooms", ARGV[1])
    redis.call("DEL", "chat:room:"..ARGV[1]..":user:"..userPKId..":cursor")
end
redis.call("DEL", KEYS[1])
redis.call("DEL", KEYS[2])
redis.call("DEL", "chat:room:"..ARGV[1]..":msg")    -- chat:room:{roomId}:msg (redis stream)

return 1