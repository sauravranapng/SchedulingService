package com.saurav.schedulingService.leader;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LeaderElectionService {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionService.class);

    @Value("${zookeeper.connect-string}")
    private String connectString;

    @Value("${zookeeper.leader-path}")
    private String leaderPath;

    private CuratorFramework client;
    private LeaderLatch leaderLatch;

    @PostConstruct
    public void start() {
        try {
            client = CuratorFrameworkFactory.newClient(connectString,
                    new ExponentialBackoffRetry(1000, 3));
            client.start();

            leaderLatch = new LeaderLatch(client, leaderPath);
            leaderLatch.start();

            logger.info("Leader election service started. Instance ID: {}", leaderLatch.getId());
        } catch (Exception e) {
            logger.error("Error starting leader election service: {}", e.getMessage());
        }
    }

    public boolean isLeader() {
        return leaderLatch.hasLeadership();
    }

    @PreDestroy
    public void stop() {
        try {
            if (leaderLatch != null) {
                leaderLatch.close();
            }
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            logger.error("Error stopping leader election service: {}", e.getMessage());
        }
    }
}