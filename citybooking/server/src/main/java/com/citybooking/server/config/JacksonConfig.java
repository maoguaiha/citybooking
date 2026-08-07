package com.citybooking.server.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 Long 包装类型序列化为字符串，避免前端 JSON.parse 对超过 2^53 的大整数（如雪花 ID）发生精度丢失。
 * 实体主键统一使用 Long 包装类型，因此所有 id 会以字符串形式下发，前端可精确持有并按 id 回传。
 * 基本类型 long 不受影响（如分页 total 仍按数值处理，由调用方保证使用基本类型）。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonLongToString() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
