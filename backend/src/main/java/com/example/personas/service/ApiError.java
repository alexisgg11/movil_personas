package com.example.personas.service;
public class ApiError extends RuntimeException {
    private final int status;
    public ApiError(int status, String mensaje) { super(mensaje); this.status = status; }
    public int getStatus() { return status; }
}
