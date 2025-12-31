package com.careercompass.careercompass.exception;

/**
 * Custom exception for external service failures (Groq, Pinecone, Gemini)
 */
public class ExternalServiceException extends RuntimeException {
    private final String serviceName;

    public ExternalServiceException(String serviceName, String message) {
        super(String.format("%s service error: %s", serviceName, message));
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(String.format("%s service error: %s", serviceName, message), cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
