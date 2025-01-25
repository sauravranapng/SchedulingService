package com.saurav.schedulingService.entity;

import com.saurav.schedulingService.util.TaskSchedulePrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table("task_schedule")
public class TaskSchedule {
    // Getters and Setters
    @PrimaryKey
    private TaskSchedulePrimaryKey key;

}