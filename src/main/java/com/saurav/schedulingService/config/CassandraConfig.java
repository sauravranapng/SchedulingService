package com.saurav.schedulingService.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.io.File;
import java.nio.file.Path;

@Configuration
public class CassandraConfig {

    private static final Logger logger = LoggerFactory.getLogger(CassandraConfig.class);

    @Value("${spring.cassandra.keyspace-name}")
    private String keySpace;

    @Value("${datastax.astra.secure-connect-bundle}")
    private File cloudSecureBundle;

    @Value("${spring.cassandra.username}")
    private String username;

    @Value("${spring.cassandra.password}")
    private String password;

    @Bean
    public CqlSession cqlSession() {
        // Check if the secure bundle exists
        if (cloudSecureBundle == null || !cloudSecureBundle.exists()) {
            throw new IllegalStateException("Secure connect bundle not found at path: " + cloudSecureBundle);
        }

        Path bundlePath = cloudSecureBundle.toPath();
        logger.info("Loading secure connect bundle from: " + bundlePath);
        logger.info("Cassandra username: " + username);

        return CqlSession.builder()
                .withCloudSecureConnectBundle(bundlePath)
                .withAuthCredentials(username, password)
                .withKeyspace(keySpace)
                .build();
    }

    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession session) {
        return new CassandraTemplate(session);
    }


}

