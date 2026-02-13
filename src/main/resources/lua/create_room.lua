-- KEYS[1] = chat:room:{roomId}

-- ARGV[1] = roomName
-- ARGV[2] = ownerId
-- ARGV[3] = description
-- ARGV[4] = createdAt
-- ARGV[5] = muted
-- ARGV[6] = locked
-- ARGV[7] = ttlMillis
-- ARGV[8] = roomId
if redis.call("EXISTS", KEYS[1]) == 1 then    -- 房间存在则返回 0 创建失败
    return 0
else
    redis.call("HSET", KEYS[1], "roomName", ARGV[1], "ownerId", ARGV[2], "description", ARGV[3],
            "createdAt", ARGV[4], "muted", ARGV[5], "locked", ARGV[6], "ttlMillis", ARGV[7])
    redis.call("HSET", KEYS[1]..":state", "init", "")
    redis.call("SADD", "chat:room:"..ARGV[8]..":members", ARGV[2])
    redis.call("SADD", "user:"..ARGV[2]..":rooms", ARGV[8])
    return 1
end