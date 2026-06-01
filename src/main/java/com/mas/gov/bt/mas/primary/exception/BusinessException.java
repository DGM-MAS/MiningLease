package com.mas.gov.bt.mas.primary.exception;

import com.mas.gov.bt.mas.primary.utility.ErrorCodes;

public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String details;

    public BusinessException(String errorCode) {
        this(errorCode, null, null);
    }

    public BusinessException(String errorCode, String details) {
        this(errorCode, details, null);
    }

    public BusinessException(String errorCode, String details, Throwable cause) {
        super(details != null ? details : getMessageFromCode(errorCode), cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    private static String getMessageFromCode(String errorCode) {
        // Import ErrorCodes utility to get descriptive message
        return ErrorCodes.getErrorDescription(errorCode);
    }
}
