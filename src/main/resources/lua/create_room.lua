-- KEYS[1] = room:{roomId}

-- ARGS[1] = roomName
-- ARGS[2] = owner
-- ARGS[3] = description
-- ARGS[4] = createdAt
-- ARGS[5] = muted
-- ARGS[6] = locked
-- ARGS[7] = ttlMillis
if redis.call("EXISTS", KEYS[1]) == 1 then    -- 房间存在则返回 0 创建失败
    return 0
else
    redis.call("HSET", KEYS[1], "roomName", ARGV[1], "owner", ARGV[2], "description", ARGV[3],
            "createdAt", ARGV[4], "muted", ARGV[5], "locked", ARGV[6], "ttlMillis", ARGV[7])
    return 1
end