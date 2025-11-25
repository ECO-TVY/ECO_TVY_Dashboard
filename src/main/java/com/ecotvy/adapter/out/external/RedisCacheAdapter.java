package com.ecotvy.adapter.out.external;

import com.ecotvy.application.port.out.CachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }

            if (type.isInstance(value)) {
                return (T) value;
            }

            // JSON 역직렬화
            if (value instanceof String) {
                return objectMapper.readValue((String) value, type);
            }

            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.error("Failed to get value from cache: {}", key, e);
            return null;
        }
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to put value to cache: {}", key, e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to evict key from cache: {}", key, e);
        }
    }

    @Override
    public void evictPattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
        } catch (Exception e) {
            log.error("Failed to evict pattern from cache: {}", pattern, e);
        }
    }
}

