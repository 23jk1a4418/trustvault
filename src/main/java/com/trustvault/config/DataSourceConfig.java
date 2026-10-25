package com.trustvault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import javax.sql.DataSource;

/** Accepts both Spring JDBC URLs and Render's native PostgreSQL connection string. */
@Configuration
public class DataSourceConfig {

    @Bean
    DataSource dataSource(
            @Value("${spring.datasource.url}") String configuredUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        String jdbcUrl = toJdbcUrl(configuredUrl);
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

    private static String toJdbcUrl(String configuredUrl) {
        if (configuredUrl.startsWith("jdbc:")) {
            return configuredUrl;
        }

        if (configuredUrl.startsWith("postgresql://") || configuredUrl.startsWith("postgres://")) {
            URI uri = URI.create(configuredUrl);
            if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().isBlank()) {
                throw new IllegalArgumentException("Invalid PostgreSQL connection URL");
            }

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
            if (uri.getPort() >= 0) {
                jdbcUrl.append(':').append(uri.getPort());
            }
            jdbcUrl.append(uri.getRawPath());
            if (uri.getRawQuery() != null) {
                jdbcUrl.append('?').append(uri.getRawQuery());
            }
            return jdbcUrl.toString();
        }

        return configuredUrl;
    }
}
