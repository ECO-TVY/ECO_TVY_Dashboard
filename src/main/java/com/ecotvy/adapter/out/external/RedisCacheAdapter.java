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

            // put()은 항상 JSON 문자열로 저장하므로 문자열은 먼저 JSON으로 푼다.
            // (isInstance를 먼저 검사하면 String 타입 값이 '"v1.0.0"'처럼 따옴표가 붙은 채 반환됨)
            if (value instanceof String json) {
                return objectMapper.readValue(json, type);
            }

            if (type.isInstance(value)) {
                return (T) value;
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

