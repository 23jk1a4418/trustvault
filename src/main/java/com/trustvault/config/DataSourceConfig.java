package com.trustvault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/** Accepts both Spring JDBC URLs and Render's native PostgreSQL connection string. */
@Configuration
public class DataSourceConfig {

    @Bean
    DataSource dataSource(
            @Value("${spring.datasource.url}") String configuredUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        String jdbcUrl = configuredUrl.startsWith("postgresql://")
                ? "jdbc:" + configuredUrl
                : configuredUrl;
        String driver = jdbcUrl.startsWith("jdbc:postgresql:")
                ? "org.postgresql.Driver"
                : "org.h2.Driver";

        return DataSourceBuilder.create()
                .driverClassName(driver)
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }
}
