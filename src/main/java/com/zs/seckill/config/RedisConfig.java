package com.zs.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zs.seckill.config.serializer.JodaDateTimeJsonDeserializer;
import com.zs.seckill.config.serializer.JodaDateTimeJsonSerializer;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;

@Component
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
        //1. 生成redisTemplate
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(factory);

        //2. 解决key的序列化方式：直接使用string的序列化方式，不使用java的序列化方式
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        //3. 解决vale的序列化方式：使用json的序列化方式
        //3.1 生成jackson2JsonRedisSerializer
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        //3.2 对jodaDateTime做定制化的serializer
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(DateTime.class, new JodaDateTimeJsonSerializer());
        simpleModule.addDeserializer(DateTime.class, new JodaDateTimeJsonDeserializer());

        ObjectMapper objectMapper = new ObjectMapper();
        // 使序列化后包含类的信息，反序列化时强转类型不会报错
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        // 注册simpleModule
        objectMapper.registerModule(simpleModule);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);

        return redisTemplate;
    }

}
