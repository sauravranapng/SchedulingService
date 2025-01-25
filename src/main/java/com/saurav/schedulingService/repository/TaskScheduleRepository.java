package com.saurav.schedulingService.repository;

import com.saurav.schedulingService.entity.TaskSchedule;
import com.saurav.schedulingService.util.TaskSchedulePrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface TaskScheduleRepository extends CassandraRepository<TaskSchedule, TaskSchedulePrimaryKey> {

    @Query("SELECT * FROM task_schedule WHERE next_execution_time = :currentMinute AND segment IN :assignedSegment")
    List<TaskSchedule> findJobsForCurrentMinute(@Param("currentMinute") long currentMinute,
                                                @Param("assignedSegment") List<Integer> assignedSegment);
}

