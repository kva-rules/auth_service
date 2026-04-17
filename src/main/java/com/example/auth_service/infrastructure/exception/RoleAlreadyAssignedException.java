package com.example.auth_service.infrastructure.exception;

public class RoleAlreadyAssignedException extends RuntimeException {
    
    public RoleAlreadyAssignedException(String message) {
        super(message);
    }
    
    public RoleAlreadyAssignedException(String message, Throwable cause) {
        super(message, cause);
    }
}
