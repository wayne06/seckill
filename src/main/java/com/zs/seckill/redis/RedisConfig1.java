package com.zs.seckill.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Method;

@Configuration
@EnableCaching
public class RedisConfig1 extends CachingConfigurerSupport {

    //@Bean
    //@Override
    //public KeyGenerator keyGenerator() {
    //    return new KeyGenerator() {
    //        @Override
    //        public Object generate(Object o, Method method, Object... objects) {
    //            StringBuilder sb = new StringBuilder();
    //            sb.append(o.getClass().getName());
    //            sb.append(method.getName());
    //            for (Object obj : objects) {
    //                sb.append(obj.toString());
    //            }
    //            return sb.toString();
    //        }
    //    };
    //}
    //
    //@Bean
    //public CacheManager cacheManager(RedisConnectionFactory factory) {
    //    RedisSerializer<String> redisSerializer = new StringRedisSerializer();
    //    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
    //
    //    // 配置序列化
    //    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
    //    RedisCacheConfiguration redisCacheConfiguration =
    //            config.serializeKeysWith(RedisSerializationContext
    //                    .SerializationPair
    //                    .fromSerializer(redisSerializer))
    //                    .serializeValuesWith(RedisSerializationContext
    //                            .SerializationPair
    //                            .fromSerializer(jackson2JsonRedisSerializer));
    //
    //    RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
    //            .cacheDefaults(redisCacheConfiguration)
    //            .build();
    //    return cacheManager;
    //}
    //
    //@Bean(name = "redisTemplate")
    //public RedisTemplate redisTemplate(RedisConnectionFactory factory){
    //    RedisTemplate template = new RedisTemplate();
    //    template.setConnectionFactory(factory);
    //
    //    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
    //    ObjectMapper om = new ObjectMapper();
    //    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    //    om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    //
    //    jackson2JsonRedisSerializer.setObjectMapper(om);
    //
    //    template.setValueSerializer(jackson2JsonRedisSerializer);
    //    template.setKeySerializer(new StringRedisSerializer());
    //    template.afterPropertiesSet();
    //    return template;
    //}

}
