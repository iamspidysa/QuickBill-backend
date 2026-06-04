package com.saurabh.quickbill.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when an authenticated user tries to act on a resource they don't own.
// Produces HTTP 403 Forbidden — not 401 (unauthenticated) and not 404.
// Returning 404 instead of 403 is a common "security through obscurity" trick,
// but 403 is the semantically correct status and clearer for API consumers.
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException{
    public AccessDeniedException(String message) {
        super(message);
    }
}
