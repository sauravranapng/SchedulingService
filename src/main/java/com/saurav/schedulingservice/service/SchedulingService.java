package com.saurav.schedulingservice.service;
import com.saurav.schedulingservice.entity.TaskSchedule;
import com.saurav.schedulingservice.event.JobExecutionEvent;
import com.saurav.schedulingservice.leader.LeaderElectionService;
import com.saurav.schedulingservice.mapper.TaskScheduleMapper;
import com.saurav.schedulingservice.repository.TaskScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SchedulingService {
    private final TaskScheduleMapper taskScheduleMapper;
    private final LeaderElectionService leaderElectionService;
    private final TaskScheduleRepository taskScheduleRepository;
    private final KafkaTemplate<String, JobExecutionEvent> kafkaTemplate;
    private List<Integer> assignedSegments;
    @Value("${app.kafka.topic}")
    private String kafkaTopic;
    private static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);

    @Autowired
    public SchedulingService(TaskScheduleMapper taskScheduleMapper, LeaderElectionService leaderElectionService,
                             TaskScheduleRepository taskScheduleRepository, KafkaTemplate<String, JobExecutionEvent> kafkaTemplate) {
        this.taskScheduleMapper = taskScheduleMapper;
        this.leaderElectionService = leaderElectionService;
        this.taskScheduleRepository = taskScheduleRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.assignedSegments = leaderElectionService.getAssignedSegmentsForCurrentInstance();
    }

    /**
     * Fetches the assigned segment for the current instance from ZooKeeper.
     * Fetch jobs to execute for the assigned segment in the current minute.
     *
     * @return List of job IDs
     */
    private List<TaskSchedule> getJobsForExecution() {
        if (assignedSegments == null) {
            logger.info("Assigned segment is not set. Ensure ZooKeeper is configured correctly.");
        }
        long currentMinute = Instant.now().getEpochSecond() / 60;
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

            jobsToExecute.forEach(this::processTask);

        } catch (Exception e) {
            logger.error("Error fetching or publishing jobs: {}", e.getMessage(), e);
        }
    }

    private void processTask(TaskSchedule taskSchedule) {

        JobExecutionEvent event = new JobExecutionEvent(
                taskSchedule.getUserId(),
                taskSchedule.getKey().getJobId()
        );

        kafkaTemplate.send(kafkaTopic,  event.getJobId().toString(),event)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        logger.error("Failed to publish JobExecutionEvent: {}", event, ex);
                        return;
                    }

                    logger.info("Published JobExecutionEvent: {}", event);

                    rescheduleTask(taskSchedule);

                });
    }

    private void rescheduleTask(TaskSchedule taskSchedule) {

        if (!taskSchedule.isRecurring()) {
            taskScheduleRepository.delete(taskSchedule);

            logger.info("Removed one-time job {}",
                    taskSchedule.getKey().getJobId());
            return;
        }

        long nextExecutionTime = taskSchedule.getKey().getNextExecutionTime()
                + Duration.parse(taskSchedule.getInterval()).toMinutes();

        taskScheduleRepository.delete(taskSchedule);

        TaskSchedule nextSchedule =
                taskScheduleMapper.copyWithNextExecutionTime(
                        taskSchedule,
                        nextExecutionTime
                );

        taskScheduleRepository.save(nextSchedule);

        logger.info("Rescheduled job {} for {}",
                nextSchedule.getKey().getJobId(),
                nextExecutionTime);
    }

}

