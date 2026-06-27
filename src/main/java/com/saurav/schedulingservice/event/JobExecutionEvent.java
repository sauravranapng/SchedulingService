package com.saurav.schedulingservice.event;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEvent {

    private UUID userId;
    private UUID jobId;
}
