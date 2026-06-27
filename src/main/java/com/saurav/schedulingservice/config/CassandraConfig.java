package com.saurav.schedulingservice.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class CassandraConfig {

    private static final Logger logger = LoggerFactory.getLogger(CassandraConfig.class);

    @Value("${spring.cassandra.keyspace-name}")
    private String keySpace;

    @Value("${datastax.astra.secure-connect-bundle}")
    private Resource cloudSecureBundle;

    @Value("${spring.cassandra.username}")
    private String username;

    @Value("${spring.cassandra.password}")
    private String password;

    @Bean
    public CqlSession cqlSession() throws IOException {
        if (cloudSecureBundle == null || !cloudSecureBundle.exists()) {
            throw new IllegalStateException("Secure connect bundle not found");
        }

        Path tempFile = Files.createTempFile("secure-connect-", ".zip");
        Files.copy(cloudSecureBundle.getInputStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        logger.info("Loading secure connect bundle from classpath resource");

        return CqlSession.builder()
                .withCloudSecureConnectBundle(tempFile)
                .withAuthCredentials(username, password)
                .withKeyspace(keySpace)
                .build();
    }

    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession session) {
        return new CassandraTemplate(session);
    }
}
