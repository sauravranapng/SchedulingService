package com.saurav.schedulingService.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
/*
    it is a custom exception class to handle the exception
    when a valid user is not linked with a valid product in the table
 */
@Getter
public class JobServiceApiException extends RuntimeException {
    private HttpStatus httpStatus;
    private String message;
    public JobServiceApiException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
