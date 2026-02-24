package com.mas.gov.bt.mas.primary.exception;

import com.mas.gov.bt.mas.primary.utility.ErrorCodes;

public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode) {
        super(ErrorCodes.getErrorDescription(errorCode));
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
