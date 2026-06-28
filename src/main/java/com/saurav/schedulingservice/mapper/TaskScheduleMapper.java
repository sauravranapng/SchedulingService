package com.saurav.schedulingservice.mapper;

import com.saurav.schedulingservice.entity.TaskSchedule;
import com.saurav.schedulingservice.util.TaskSchedulePrimaryKey;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduleMapper {

    public TaskSchedule copyWithNextExecutionTime(TaskSchedule taskSchedule,
                                                  long nextExecutionTime) {

        TaskSchedulePrimaryKey primaryKey = new TaskSchedulePrimaryKey(
                nextExecutionTime,
                taskSchedule.getKey().getSegment(),
                taskSchedule.getKey().getJobId()
        );

        TaskSchedule nextTask = new TaskSchedule();
        nextTask.setKey(primaryKey);
        nextTask.setUserId(taskSchedule.getUserId());
        nextTask.setRecurring(taskSchedule.isRecurring());
        nextTask.setInterval(taskSchedule.getInterval());

        return nextTask;
    }
}
