package com.mas.gov.bt.mas.primary.utility;

import java.util.HashMap;
import java.util.Map;

public class ErrorCodes {
    // Authentication and Authorization Errors
    public static final String INVALID_CREDENTIALS = "401";
    public static final String ACCESS_DENIED = "403";
    public static final String UNAUTHORIZED_TOKEN = "401 Unauthorized HTTP";
    public static final String TOKEN_INVALID = "401 AUTH004";
    public static final String TOKEN_EXPIRED = "401 AUTH005";
    public static final String ACCOUNT_LOCKED = "401 AUTH006";
    public static final String ACCOUNT_DISABLED = "401 AUTH007";

    // Validation Errors
    public static final String MISSING_REQUIRED_FIELD = "400 VAL001";
    public static final String INVALID_INPUT_DATA = "400 VAL002";
    public static final String DATA_TYPE_MISMATCH = "400 VAL003";
    public static final String MISSING_REQUIRED_PARAMETER = "400 VAL004";
    public static final String INVALID_DATA_FORMAT = "DATA001";
    public static final String VALUE_OUT_OF_RANGE = "DATA002";
    public static final String DATA_LENGTH_EXCEEDED = "DATA003";
    public static final String UNSUPPORTED_DATA_TYPE = "DATA004";

    // Database Errors
    public static final String DATABASE_CONNECTION_FAILED = "DB001";
    public static final String RECORD_NOT_FOUND = "DB002";
    public static final String DUPLICATE_ENTRY = "DB003";
    public static final String DATA_INTEGRITY_VIOLATION = "DB004";
    public static final String QUERY_EXECUTION_FAILED = "DB005";
    public static final String TRANSACTION_FAILED = "DB006";

    // API Errors
    public static final String BAD_REQUEST = "API001";
    public static final String UNAUTHORIZED_REQUEST = "API002";
    public static final String FORBIDDEN_REQUEST = "API003";
    public static final String RESOURCE_NOT_FOUND = "API004";
    public static final String METHOD_NOT_ALLOWED = "API005";
    public static final String UNSUPPORTED_MEDIA_TYPE = "API006";
    public static final String RATE_LIMIT_EXCEEDED = "API007";

    // Server Errors
    public static final String INTERNAL_SERVER_ERROR = "PROC001";
    public static final String SERVICE_UNAVAILABLE = "PROC002";
    public static final String TIMEOUT = "PROC003";
    public static final String EXTERNAL_SERVICE_ERROR = "PROC004";
    public static final String GATEWAY_TIMEOUT = "PROC005";
    public static final String BAD_GATEWAY = "PROC006";

    // File and I/O Errors
    public static final String FILE_NOT_FOUND = "FILE001";
    public static final String FILE_READ_ERROR = "FILE002";
    public static final String FILE_WRITE_ERROR = "FILE003";
    public static final String FILE_DELETE_ERROR = "FILE004";
    public static final String FILE_SIZE_EXCEEDED = "FILE005";
    public static final String INVALID_FILE_FORMAT = "FILE006";

    // Network Errors
    public static final String NETWORK_ERROR = "NET001";
    public static final String CONNECTION_TIMEOUT = "NET002";
    public static final String SSL_HANDSHAKE_FAILED = "NET003";
    public static final String DNS_RESOLUTION_FAILED = "NET004";

    // Security Errors
    public static final String SECURITY_VIOLATION = "SEC001";
    public static final String CSRF_TOKEN_MISSING = "SEC002";
    public static final String CSRF_TOKEN_INVALID = "SEC003";
    public static final String INVALID_SIGNATURE = "SEC004";
    public static final String ENCRYPTION_ERROR = "SEC005";
    public static final String DECRYPTION_ERROR = "SEC006";

    // Configuration Errors
    public static final String MISSING_CONFIGURATION = "CONF001";
    public static final String INVALID_CONFIGURATION = "CONF002";
    public static final String CONFIGURATION_LOAD_FAILED = "CONF003";

    // Business Logic Errors
    public static final String INVALID_OPERATION = "BIZ001";
    public static final String INVALID_STATE = "BIZ002";
    public static final String INVALID_TRANSITION = "BIZ003";
    public static final String BUSINESS_RULE_VIOLATION = "BIZ004";

    // External Service Errors
    public static final String EXTERNAL_API_ERROR = "EXT001";
    public static final String EXTERNAL_SERVICE_TIMEOUT = "EXT002";
    public static final String EXTERNAL_SERVICE_UNAVAILABLE = "EXT003";

    // Miscellaneous Errors
    public static final String UNKNOWN_ERROR = "MISC001";
    public static final String UNSUPPORTED_OPERATION = "MISC002";
    public static final String INVALID_RESPONSE = "MISC003";
    public static final String INVALID_REQUEST = "MISC004";

    private static final Map<String, String> errorDescriptions = new HashMap<>();

    static {
        // Authentication and Authorization Errors
        errorDescriptions.put(INVALID_CREDENTIALS,
                "Invalid credentials. The provided username or password is incorrect.");
        errorDescriptions.put(ACCESS_DENIED,
                "Access denied. The user does not have the required permissions to access this resource.");
        errorDescriptions.put(UNAUTHORIZED_TOKEN, "Token expired. The authentication token has expired.");
        errorDescriptions.put(TOKEN_INVALID, "Token invalid. The authentication token is invalid.");
        errorDescriptions.put(TOKEN_EXPIRED, "Token expired. The authentication token has expired.");
        errorDescriptions.put(ACCOUNT_LOCKED,
                "Account locked. The account has been locked due to multiple failed login attempts.");
        errorDescriptions.put(ACCOUNT_DISABLED, "Account disabled. The account has been disabled by an administrator.");

        // Validation Errors
        errorDescriptions.put(MISSING_REQUIRED_FIELD,
                "Missing required field. A required field is missing in the request.");
        errorDescriptions.put(INVALID_INPUT_DATA, "Invalid input data. The input data format is incorrect.");
        errorDescriptions.put(DATA_TYPE_MISMATCH,
                "Data type mismatch. The data type provided does not match the expected type.");
        errorDescriptions.put(MISSING_REQUIRED_PARAMETER,
                "Missing required parameter. A required parameter is missing in the request.");
        errorDescriptions.put(INVALID_DATA_FORMAT,
                "Invalid data format. The data provided does not conform to the expected format.");
        errorDescriptions.put(VALUE_OUT_OF_RANGE,
                "Value out of range. The value provided is outside the allowed range.");
        errorDescriptions.put(DATA_LENGTH_EXCEEDED,
                "Data length exceeded. The length of the data provided exceeds the maximum allowed length.");
        errorDescriptions.put(UNSUPPORTED_DATA_TYPE, "Unsupported data type. The data type provided is not supported.");

        // Database Errors
        errorDescriptions.put(DATABASE_CONNECTION_FAILED,
                "Database connection failed. Unable to connect to the database.");
        errorDescriptions.put(RECORD_NOT_FOUND,
                "Record not found. The requested record does not exist in the database.");
        errorDescriptions.put(DUPLICATE_ENTRY, "Duplicate entry. A record with the same unique key already exists.");
        errorDescriptions.put(DATA_INTEGRITY_VIOLATION,
                "Data integrity violation. The operation violates data integrity constraints.");
        errorDescriptions.put(QUERY_EXECUTION_FAILED,
                "Query execution failed. The database query could not be executed.");
        errorDescriptions.put(TRANSACTION_FAILED,
                "Transaction failed. The database transaction could not be completed.");

        // API Errors
        errorDescriptions.put(BAD_REQUEST, "Bad request. The request cannot be processed due to malformed syntax.");
        errorDescriptions.put(UNAUTHORIZED_REQUEST, "Unauthorized. The request requires user authentication.");
        errorDescriptions.put(FORBIDDEN_REQUEST,
                "Forbidden. The server understands the request but refuses to authorize it.");
        errorDescriptions.put(RESOURCE_NOT_FOUND, "Not found. The requested resource could not be found.");
        errorDescriptions.put(METHOD_NOT_ALLOWED,
                "Method not allowed. The HTTP method used is not supported by this resource.");
        errorDescriptions.put(UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type. The request entity has a media type which the server or resource does not support.");
        errorDescriptions.put(RATE_LIMIT_EXCEEDED, "Rate limit exceeded. The request rate limit has been exceeded.");

        // Server Errors
        errorDescriptions.put(INTERNAL_SERVER_ERROR,
                "Internal server error. An unexpected error occurred while processing the request.");
        errorDescriptions.put(SERVICE_UNAVAILABLE,
                "Service unavailable. The service is temporarily unavailable, please try again later.");
        errorDescriptions.put(TIMEOUT, "Timeout. The request timed out.");
        errorDescriptions.put(EXTERNAL_SERVICE_ERROR,
                "External service error. An error occurred while communicating with an external service.");
        errorDescriptions.put(GATEWAY_TIMEOUT,
                "Gateway timeout. The server, while acting as a gateway or proxy, did not receive a timely response from the upstream server.");
        errorDescriptions.put(BAD_GATEWAY,
                "Bad gateway. The server, while acting as a gateway or proxy, received an invalid response from the upstream server.");

        // File and I/O Errors
        errorDescriptions.put(FILE_NOT_FOUND, "File not found. The requested file could not be found.");
        errorDescriptions.put(FILE_READ_ERROR, "File read error. An error occurred while reading the file.");
        errorDescriptions.put(FILE_WRITE_ERROR, "File write error. An error occurred while writing to the file.");
        errorDescriptions.put(FILE_DELETE_ERROR, "File delete error. An error occurred while deleting the file.");
        errorDescriptions.put(FILE_SIZE_EXCEEDED,
                "File size exceeded. The file size exceeds the maximum allowed limit.");
        errorDescriptions.put(INVALID_FILE_FORMAT, "Invalid file format. The file format is not supported.");

        // Network Errors
        errorDescriptions.put(NETWORK_ERROR, "Network error. A network-related error occurred.");
        errorDescriptions.put(CONNECTION_TIMEOUT, "Connection timeout. The connection timed out.");
        errorDescriptions.put(SSL_HANDSHAKE_FAILED, "SSL handshake failed. The SSL handshake could not be completed.");
        errorDescriptions.put(DNS_RESOLUTION_FAILED, "DNS resolution failed. The domain name could not be resolved.");

        // Security Errors
        errorDescriptions.put(SECURITY_VIOLATION, "Security violation. A security violation has been detected.");
        errorDescriptions.put(CSRF_TOKEN_MISSING, "CSRF token missing. The CSRF token is missing from the request.");
        errorDescriptions.put(CSRF_TOKEN_INVALID, "CSRF token invalid. The CSRF token is invalid or expired.");
        errorDescriptions.put(INVALID_SIGNATURE, "Invalid signature. The signature provided is invalid.");
        errorDescriptions.put(ENCRYPTION_ERROR, "Encryption error. An error occurred during encryption.");
        errorDescriptions.put(DECRYPTION_ERROR, "Decryption error. An error occurred during decryption.");

        // Configuration Errors
        errorDescriptions.put(MISSING_CONFIGURATION, "Missing configuration. A required configuration is missing.");
        errorDescriptions.put(INVALID_CONFIGURATION, "Invalid configuration. The configuration provided is invalid.");
        errorDescriptions.put(CONFIGURATION_LOAD_FAILED,
                "Configuration load failed. The configuration could not be loaded.");

        // Business Logic Errors
        errorDescriptions.put(INVALID_OPERATION,
                "Invalid operation. The operation is not allowed in the current context.");
        errorDescriptions.put(INVALID_STATE,
                "Invalid state. The system is in an invalid state to perform the operation.");
        errorDescriptions.put(INVALID_TRANSITION, "Invalid transition. The requested state transition is not allowed.");
        errorDescriptions.put(BUSINESS_RULE_VIOLATION,
                "Business rule violation. The operation violates a business rule.");

        // External Service Errors
        errorDescriptions.put(EXTERNAL_API_ERROR,
                "External API error. An error occurred while calling an external API.");
        errorDescriptions.put(EXTERNAL_SERVICE_TIMEOUT,
                "External service timeout. The external service did not respond in time.");
        errorDescriptions.put(EXTERNAL_SERVICE_UNAVAILABLE,
                "External service unavailable. The external service is temporarily unavailable.");

        // Miscellaneous Errors
        errorDescriptions.put(UNKNOWN_ERROR, "Unknown error. An unexpected error occurred.");
        errorDescriptions.put(UNSUPPORTED_OPERATION, "Unsupported operation. The operation is not supported.");
        errorDescriptions.put(INVALID_RESPONSE, "Invalid response. The response received is invalid or malformed.");
        errorDescriptions.put(INVALID_REQUEST, "Invalid request. The request is invalid or malformed.");
    }

    public static String getErrorDescription(String errorCode) {
        return errorDescriptions.getOrDefault(errorCode, "Unknown error.");
    }
}