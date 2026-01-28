package com.mas.gov.bt.mas.primary.utility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private boolean success = false;
    private String message;
    private String error;
    private int statusCode;
    private LocalDateTime timestamp;
    private String path;
    private List<String> details;

    public ErrorResponse(String failed, String recordNotFound, String errorDescription) {
    }
}
