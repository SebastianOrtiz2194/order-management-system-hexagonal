package com.oms.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.domain.model.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Registramos el JSON Mapper y RedisTemplate de Spring
 */
@Configuration
public class RedisConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Soporte crítico para Serializar Clases Record y LocalDateTimes
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Order> orderRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper mapper) {
        RedisTemplate<String, Order> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Serializadores en crudo JSON (nos permite depurar la DB usando redis-cli 'GET key')
        Jackson2JsonRedisSerializer<Order> serializer = new Jackson2JsonRedisSerializer<>(mapper, Order.class);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }
}
