-- KEYS[1] = room:{roomId}
-- KEYS[2] = room:{roomId}:members
-- KEYS[3] = user:{userId}:rooms
-- ARGV[1] = userId
-- ARGV[2] = roomId
if redis.call('EXISTS', KEYS[1]) == 0 then    -- 若房间不存在则返回0
    return 0
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then    -- 若用户已加入该房间则返回-1
    return -1
end
if redis.call('SISMEMBER', KEYS[3], ARGV[2]) == 1 then    -- 若用户已加入该房间则返回-1')
    return -1
end
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('SADD', KEYS[3], ARGV[2])
return 1    -- 添加成功