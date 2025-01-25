package com.saurav.schedulingService.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@PrimaryKeyClass
public class TaskSchedulePrimaryKey {

    @PrimaryKeyColumn(name = "next_execution_time", type = PrimaryKeyType.PARTITIONED,ordinal = 0)
    private long nextExecutionTime;

    @PrimaryKeyColumn(name = "segment", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private int segment;

    @PrimaryKeyColumn(name = "job_id", type = PrimaryKeyType.CLUSTERED,ordinal = 0)
    private UUID jobId;

}