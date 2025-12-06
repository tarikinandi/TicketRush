package com.ticketrush.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    private final String DECREMENT_STOCK_LUA =
            "local stock = redis.call('get', KEYS[1]) " +
                    "if stock and tonumber(stock) > 0 then " +
                    "    redis.call('decr', KEYS[1]) " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end";

    public void setInitialStock(Long eventId, int stock) {
        String key = "event_stock:" + eventId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
    }

    public boolean tryDecreaseStock(Long eventId) {
        String key = "event_stock:" + eventId;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(DECREMENT_STOCK_LUA);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(redisScript, List.of(key));

        return result != null && result == 1;
    }
}