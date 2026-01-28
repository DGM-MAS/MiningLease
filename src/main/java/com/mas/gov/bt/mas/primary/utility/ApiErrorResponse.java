package com.mas.gov.bt.mas.primary.utility;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ApiErrorResponse {

    private String errorCode;
    private String message;
    private String details;
    private LocalDateTime timestamp;

    public ApiErrorResponse(String errorCode, String message, String details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }
}
