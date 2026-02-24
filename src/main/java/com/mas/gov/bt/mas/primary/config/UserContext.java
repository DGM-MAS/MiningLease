package com.mas.gov.bt.mas.primary.config;

import com.mas.gov.bt.mas.primary.exception.UnauthorizedOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility component to extract user information from request headers
 * set by the master service after JWT validation.
 *
 * Expected headers:
 *   X-User-Id      — numeric user ID
 *   X-User-Name    — username / subject
 *   X-User-Email   — email address
 *   X-User-Type    — user type (e.g. "Agency")
 */
@Component
public class UserContext {

    public Long getCurrentUserId() {
        String value = getHeader("X-User-Id");
        if (value == null) {
            throw new UnauthorizedOperationException("X-User-Id header missing");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new UnauthorizedOperationException("Invalid X-User-Id header value");
        }
    }

    public String getCurrentUsername() {
        String value = getHeader("X-User-Name");
        if (value == null) {
            throw new UnauthorizedOperationException("X-User-Name header missing");
        }
        return value;
    }

    public String getCurrentUserEmail() {
        return getHeader("X-User-Email");
    }

    public String getCurrentUserType() {
        return getHeader("X-User-Type");
    }

    public boolean isAgencyUser() {
        return "Agency".equalsIgnoreCase(getCurrentUserType());
    }

    private String getHeader(String name) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new UnauthorizedOperationException("No request context available");
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(name);
    }
}
