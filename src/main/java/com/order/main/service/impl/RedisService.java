package com.order.main.service.impl;

import com.pdd.pop.ext.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 基本的set方法
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 带过期时间的set方法
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    // 优化get方法，移除重试机制（在大多数场景下是过度设计）
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null; // 快速失败，不重试
        }
    }


    // 新增：使用JSON序列化的设置或追加方法
    public void listSetOrAppendAsJson(String key, Object value) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 检查key是否存在
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                // key存在，检查是否为列表类型
                String type = String.valueOf(redisTemplate.type(key));
                if ("list".equalsIgnoreCase(type)) {
                    // 是列表类型，向后追加
                    String jsonValue = objectMapper.writeValueAsString(value);
                    redisTemplate.opsForList().rightPush(key, jsonValue);
                } else {
                    // 不是列表类型，创建新列表
                    List<String> newList = new ArrayList<>();
                    String jsonValue = objectMapper.writeValueAsString(value);
                    newList.add(jsonValue);

                    // 删除原有数据
                    redisTemplate.delete(key);
                    // 逐条添加JSON字符串到列表
                    for (String item : newList) {
                        redisTemplate.opsForList().rightPush(key, item);
                    }
                }
            } else {
                // key不存在，创建新列表
                String jsonValue = objectMapper.writeValueAsString(value);
                redisTemplate.opsForList().rightPush(key, jsonValue);
            }
        } catch (Exception e) {
            // 静默处理
            e.printStackTrace();
        }
    }
    // 异步设置缓存
    public void setAsync(String key, Object value, long timeout, TimeUnit unit) {
        asyncExecutor.submit(() -> {
            try {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
            } catch (Exception e) {
                // 静默处理，不影响主流程
            }
        });
    }

    // 新增：自增方法
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            return null;
        }
    }

    // 新增：设置过期时间
    public Boolean expire(String key, long timeout) {
        try {
            return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    // 新增：带时间单位的设置过期时间
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            return false;
        }
    }
}