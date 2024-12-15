package com.saurav.schedulingService.dto;


import lombok.Getter;

import java.util.Date;

@Getter
public class ErrorResponse {
    private Date timestamp;
    private String message;
    private String details;

    public ErrorResponse(Date timestamp, String message, String details) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
    }
}