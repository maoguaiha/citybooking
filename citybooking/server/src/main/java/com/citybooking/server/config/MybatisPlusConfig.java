package com.citybooking.server.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            @Value("${spring.datasource.url:}") String dsUrl) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Only enable MP's built-in pagination on MySQL.
        // H2 embedded (dev/test) triggers SQLFeatureNotSupportedException in
        // PaginationInnerInterceptor.prepare() even with H2 dialect — caused by
        // H2's ProxyResultSet.getTimestamp() failing on TIMESTAMP columns.
        // ServiceService uses manual selectList+slice as fallback.
        boolean h2 = dsUrl.contains("h2:");
        if (!h2) {
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(
                    com.baomidou.mybatisplus.annotation.DbType.MYSQL));
        }
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
