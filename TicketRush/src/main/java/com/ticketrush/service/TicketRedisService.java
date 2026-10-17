package com.ticketrush.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    // Lets several apps share one Redis; empty by default.
    @Value("${ticketrush.redis.key-prefix:}")
    private String keyPrefix;

    private final String DECREMENT_STOCK_LUA =
            "local stock = redis.call('get', KEYS[1]) " +
                    "if stock and tonumber(stock) > 0 then " +
                    "    redis.call('decr', KEYS[1]) " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end";

    public void setInitialStock(Long eventId, int stock) {
        String key = keyPrefix + "event_stock:" + eventId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
    }

    public boolean tryDecreaseStock(Long eventId) {
        String key = keyPrefix + "event_stock:" + eventId;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(DECREMENT_STOCK_LUA);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(redisScript, List.of(key));

        return result != null && result == 1;
    }

    // Demo: per-visitor stock. SET NX seeds the key on first use, then the same atomic script decrements it.
    private static final String VISITOR_DECREMENT_LUA =
            "redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) " +
                    "redis.call('expire', KEYS[1], ARGV[2]) " +
                    "local stock = tonumber(redis.call('get', KEYS[1])) " +
                    "if stock > 0 then redis.call('decr', KEYS[1]) return 1 end " +
                    "return 0";

    private static final String VISITOR_READ_LUA =
            "redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) " +
                    "redis.call('expire', KEYS[1], ARGV[2]) " +
                    "return tonumber(redis.call('get', KEYS[1]))";

    @Value("${ticketrush.demo.seed-stock:10}")
    private int seedStock;

    @Value("${ticketrush.demo.visitor-ttl-hours:24}")
    private long visitorTtlHours;

    private String visitorKey(Long eventId, String visitorId) {
        return keyPrefix + "event_stock:" + eventId + ":" + visitorId;
    }

    private Long runVisitorScript(String script, Long eventId, String visitorId) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return stringRedisTemplate.execute(redisScript, List.of(visitorKey(eventId, visitorId)),
                String.valueOf(seedStock), String.valueOf(visitorTtlHours * 3600));
    }

    public boolean tryDecreaseVisitorStock(Long eventId, String visitorId) {
        Long result = runVisitorScript(VISITOR_DECREMENT_LUA, eventId, visitorId);
        return result != null && result == 1;
    }

    /** Remaining stock for this visitor; creates the key with the seed stock on first call. */
    public int getVisitorStock(Long eventId, String visitorId) {
        Long result = runVisitorScript(VISITOR_READ_LUA, eventId, visitorId);
        return result == null ? 0 : result.intValue();
    }
}
