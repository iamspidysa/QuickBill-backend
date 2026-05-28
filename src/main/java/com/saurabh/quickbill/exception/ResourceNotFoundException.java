package com.saurabh.quickbill.exception;

//Used whenever something is looked up by ID and doesn't exist — orders, items, categories, users.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message){
        super(message);
    }

}
