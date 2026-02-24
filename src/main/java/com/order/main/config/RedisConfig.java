package com.order.main.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String primaryRedisHost;

    @Value("${spring.redis.port}")
    private int primaryRedisPort;

    @Value("${spring.redis.database}")
    private int primaryRedisDatabase;

    @Value("${spring.redis.password}")
    private String primaryRedisPassword;

    /**
     * 主 Redis 连接工厂（用于令牌消费）
     */
    @Primary
    @Bean(name = "primaryRedisConnectionFactory")
    public RedisConnectionFactory primaryRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(primaryRedisHost);
        config.setPort(primaryRedisPort);
        config.setDatabase(primaryRedisDatabase);
        config.setPassword(primaryRedisPassword);
        return new LettuceConnectionFactory(config);
    }


    /**
     * 主 RedisTemplate（用于令牌消费）
     */
    @Primary
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(
            @Qualifier("primaryRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        return createRedisTemplate(connectionFactory);
    }


    /**
     * 创建 RedisTemplate 的通用方法
     */
    private RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器（存储纯字符串，非JSON格式）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // key 和 hash key 使用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 和 hash value 也使用 String 序列化
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        // 对于List操作，设置特定的序列化器
        template.setDefaultSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}