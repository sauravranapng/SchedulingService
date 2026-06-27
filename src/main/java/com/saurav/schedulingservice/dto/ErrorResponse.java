package com.saurav.schedulingservice.dto;


import java.time.Instant;

public record ErrorResponse(Instant timestamp, String message, String details) {
}