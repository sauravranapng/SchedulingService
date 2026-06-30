package com.saurav.schedulingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceMetadata {

    private String instanceId;

    private String host;

    private int port;

    private Instant startedAt;

}