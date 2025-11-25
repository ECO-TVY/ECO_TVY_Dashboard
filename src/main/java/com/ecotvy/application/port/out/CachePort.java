package com.ecotvy.application.port.out;

public interface CachePort {
    <T> T get(String key, Class<T> type);
    void put(String key, Object value, long ttlSeconds);
    void evict(String key);
    void evictPattern(String pattern);
}

