package com.saurav.schedulingService.service;
import com.saurav.schedulingService.entity.TaskSchedule;
import com.saurav.schedulingService.leader.LeaderElectionService;
import com.saurav.schedulingService.repository.TaskScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {
    private final LeaderElectionService leaderElectionService;
    private final TaskScheduleRepository taskScheduleRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private List<Integer> assignedSegments;
    @Value("${app.kafka.topic}")
    private String kafkaTopic;
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    @Autowired
    public JobService(LeaderElectionService leaderElectionService,
                      TaskScheduleRepository taskScheduleRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.leaderElectionService = leaderElectionService;
        this.taskScheduleRepository = taskScheduleRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.assignedSegments = leaderElectionService.getAssignedSegmentsForCurrentInstance();
    }
    /**
     * Fetches the assigned segment for the current instance from ZooKeeper.
     */

    /**
     * Fetch jobs to execute for the assigned segment in the current minute.
     *
     * @return List of job IDs
     */
    private List<TaskSchedule> getJobsForExecution() {
        if (assignedSegments == null) {
            System.out.println("Assigned segment is not set. Ensure ZooKeeper is configured correctly.");
        }
        long currentMinuteInSeconds = Instant.now()  // Get current UTC time
                .atZone(ZoneOffset.UTC)  // Ensure the time zone is UTC
                .withSecond(0)  // Round to the start of the minute
                .withNano(0)  // Remove the nanoseconds part
                .toEpochSecond();
         long currentMinute = currentMinuteInSeconds / 60;
         return taskScheduleRepository.findJobsForCurrentMinute(currentMinute, assignedSegments);
    }
    /**
     * Scheduled task to fetch jobs every minute and publish them to Kafka.
     */
    @Scheduled(cron = "0 * * * * *") // Runs at the start of every minute
    public void fetchAndPublishJobs() {
        try {
            logger.info("Scheduled job triggered at: {}", Instant.now());

            assignedSegments = leaderElectionService.getAssignedSegmentsForCurrentInstance();
            if (assignedSegments == null || assignedSegments.isEmpty()) {
                logger.warn("No assigned segments. Skipping job execution.");
                return;
            }

            List<TaskSchedule> jobsToExecute = getJobsForExecution();
            logger.info("Jobs fetched for execution: {}", jobsToExecute.size());

            if (jobsToExecute.isEmpty()) {
                logger.info("No jobs to execute for the current minute.");
                return;
            }

            jobsToExecute.parallelStream().forEach(job -> {
                UUID jobId = job.getKey().getJobId();
                kafkaTemplate.send(kafkaTopic, jobId.toString());
            });

        } catch (Exception e) {
            logger.error("Error fetching or publishing jobs: {}", e.getMessage(), e);
        }
    }

}

