package com.saurav.schedulingService.leader;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LeaderElectionService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionService.class);

    private String instanceId;

    @Value("${zookeeper.connect-string}")
    private String connectString;

    @Value("${zookeeper.assignment-path}")
    private String assignmentPath;

    @Value("${zookeeper.instances-path}")
    private String instancesPath;

    private CuratorFramework client;
    private LeaderLatch leaderLatch;

    private Map<String, List<Integer>> segmentAssignments = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() {
        try {
            // Initialize Curator client
            client = CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(1000, 3));
            client.start();

            if (client.getState() != CuratorFrameworkState.STARTED) {
                throw new IllegalStateException("CuratorFramework failed to start.");
            }
            registerInstance();
            // Initialize leader latch
            leaderLatch = new LeaderLatch(client, "/scheduling-service/leader");
            leaderLatch.start();
            logger.info("Leader election service started. Instance ID: {}", leaderLatch.getId());
            // Watch for updates
            watchInstances();
            watchSegmentAssignments();
            try {
                leaderLatch.await(); // This will block until the instance becomes the leader
                logger.info("Leader election service finished. Instance ID: {}", leaderLatch.getId());
                // This will now be printed once the instance becomes the leader
            } catch (InterruptedException e) {
                logger.error("Leader election interrupted: {}", e.getMessage());
            }
            if (isLeader()) {
                assignSegmentsToAllInstances();
                logger.info("It is leader ");
            }

        } catch (Exception e) {
            logger.error("Error starting leader election service: {}", e.getMessage());
            stop(); // Clean up resources
        }
    }

    public boolean isLeader() {
        return leaderLatch.hasLeadership();
    }

    private void assignSegmentsToAllInstances() {
        try {
            List<String> activeInstances = client.getChildren().forPath(instancesPath);

            if (activeInstances.isEmpty()) {
                logger.warn("No active instances found for segment assignment.");
                return;
            }

            int totalSegments = 10; // Number of segments (you can configure this dynamically)
            int segmentsPerInstance = totalSegments / activeInstances.size();
            int remainder = totalSegments % activeInstances.size();

            // Distribute segments evenly among instances
            int currentSegment = 1;
            for (String instance : activeInstances) {
                List<Integer> assignedSegments = new ArrayList<>();

                for (int i = 0; i < segmentsPerInstance; i++) {
                    assignedSegments.add(currentSegment++);
                }

                if (remainder > 0) {
                    assignedSegments.add(currentSegment++);
                    remainder--;
                }

                segmentAssignments.put(instance, assignedSegments);
                logger.info("Assigned segments {} to instance {}", assignedSegments, instance);
            }

            // Save segment assignments in Zookeeper
            updateSegmentAssignmentsInZookeeper();
        } catch (Exception e) {
            logger.error("Error assigning segments: {}", e.getMessage());
        }
    }

    private void watchInstances() {
        try {
            PathChildrenCache cache = new PathChildrenCache(client, instancesPath, true);
            cache.getListenable().addListener((client, event) -> {
                switch (event.getType()) {
                    case CHILD_ADDED:
                    case CHILD_REMOVED:
                        if (isLeader()) {
                            assignSegmentsToAllInstances();
                        }
                        break;
                    default:
                        break;
                }
            });
            cache.start();
        } catch (Exception e) {
            logger.error("Error watching instances: {}", e.getMessage());
        }
    }

    private void updateSegmentAssignmentsInZookeeper() {
        try {
            //String path = "/scheduling-service/segments/assignments";
            // Serialize the segmentAssignments map to byte array
            byte[] data = new ObjectMapper().writeValueAsBytes(segmentAssignments);
            // Log the serialized data to verify what is being sent to Zookeeper
            logger.info("Serialized segment assignments: {}", new String(data, StandardCharsets.UTF_8));

            // Check if the path exists
            if (client.checkExists().forPath(assignmentPath) == null) {
                // Path does not exist, create it as a persistent node
                logger.info("Path does not exist. Creating path at: {}", assignmentPath);

                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(assignmentPath, data);
            } else {
                // Path exists, update the data
                logger.info("Path exists. Updating data at: {}", assignmentPath);

                client.setData().forPath(assignmentPath, data);
            }
        } catch (Exception e) {
            // Log the full exception to capture detailed stack trace and message
            logger.error("Error updating segment assignments in Zookeeper at path {}: {}", "/scheduling-service/segments/assignments", e.getMessage(), e);
            // Optionally, you can add retry logic here in case of transient failures
        }
    }

    private void watchSegmentAssignments() {
        try {
            PathChildrenCache cache = new PathChildrenCache(client, assignmentPath, true);
            cache.getListenable().addListener((client, event) -> {
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED ||
                        event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                    updateLocalSegmentCache();
                }
            });

            cache.start();
        } catch (Exception e) {
            logger.error("Error watching segment assignments: {}", e.getMessage());
        }
    }

    private void updateLocalSegmentCache() {
        try {

            if (client.checkExists().forPath(assignmentPath) == null) {
                logger.warn("No segment assignments found in Zookeeper.");
                segmentAssignments.clear();
                return;
            }

            byte[] data = client.getData().forPath(assignmentPath);
            Map<String, List<Integer>> updatedAssignments = new ObjectMapper().readValue(data, Map.class);
            segmentAssignments.clear();
            segmentAssignments.putAll(updatedAssignments);

            logger.info("Updated local segment cache with data: {}", updatedAssignments);
        } catch (Exception e) {
            logger.error("Error updating local segment cache: {}", e.getMessage());
        }
    }
    private void registerInstance() {
        try {
            // Generate a unique ID for this instance
                instanceId = UUID.randomUUID().toString(); // Or you could use a more meaningful ID
            String instancePath = instancesPath + "/" + instanceId;

            // Register as an ephemeral node
            byte[] data = instanceId.getBytes(); // This could be more complex data if needed
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(instancePath, data);

            logger.info("Registered instance with ID {} at path {}", instanceId, instancePath);
        } catch (Exception e) {
            logger.error("Failed to register instance: {}", e.getMessage());
        }
    }
    public List<Integer> getAssignedSegmentsForCurrentInstance() {
        return segmentAssignments.getOrDefault(instanceId, Collections.emptyList());
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
        } catch (Exception e) {
            logger.error("Error stopping leader election service: {}", e.getMessage());
        }
    }
}
