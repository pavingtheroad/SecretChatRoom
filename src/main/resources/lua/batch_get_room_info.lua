-- KEYS[i] = chat:room:{room_id}

local result = {}
for i = 1, #KEYS do
    if redis.call('EXISTS', KEYS[i]) == 1 then
        local data = redis.call('HGETALL', KEYS[i])
        local roomId = string.match(KEYS[i], "chat:room:(.+)")
        if roomId then
            table.insert(data,"roomId")
            table.insert(data, roomId)
        end
        table.insert(result, data)
    end
end

return result