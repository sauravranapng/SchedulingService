package com.saurav.schedulingService.exception;

import lombok.Getter;
/*
    it is a custom exception class to handle the exception
    when a resource is not found in the table
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private String resourceName;
    private String firstFieldName;
    private String secondFieldName;
    private Object firstFieldValue;
    private Object secondFieldValue;
    public ResourceNotFoundException(String resourceName, String firstFieldName,String secondFieldName, Object firstFieldValue , Object secondFieldValue) {
        super(String.format("%s not found with %s : '%s' and %s :'%s'", resourceName,
                firstFieldName ,  firstFieldValue,secondFieldName , secondFieldValue));
        this.resourceName = resourceName;
        this.firstFieldName = firstFieldName;
        this.secondFieldName = secondFieldName;
        this.firstFieldValue = firstFieldValue;
        this.secondFieldValue = secondFieldValue;
    }
}