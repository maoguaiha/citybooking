package com.citybooking.server.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class LockConfig {

    @Bean
    @Profile("prod")
    public RedissonClient redissonClient(RedisProperties rp) {
        Config config = new Config();
        String address = "redis://" + rp.getHost() + ":" + rp.getPort();
        var server = config.useSingleServer().setAddress(address);
        if (StringUtils.hasText(rp.getPassword())) {
            server.setPassword(rp.getPassword());
        }
        server.setDatabase(rp.getDatabase());
        return Redisson.create(config);
    }
}
