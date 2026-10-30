package com.mas.gov.bt.mas.primary.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes audit trail entries into the shared mas_db.audit_logs table.
 * Actor identity is read from the X-User-Id / X-Username headers the gateway's
 * JwtAuthenticationFilter injects on every proxied request, so callers deep in the
 * service layer (e.g. an AOP aspect wrapping repository saves) don't need an actor
 * parameter threaded through every method signature.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String actionType, String description, String resourceType, Long resourceId) {
        logAction(actionType, description, resourceType, resourceId, null, null, true, null);
    }

    public void logAction(String actionType, String description, String resourceType, Long resourceId,
                           String oldValue, String newValue) {
        logAction(actionType, description, resourceType, resourceId, oldValue, newValue, true, null);
    }

    public void logFailure(String actionType, String description, String resourceType, Long resourceId,
                            String errorMessage) {
        logAction(actionType, description, resourceType, resourceId, null, null, false, errorMessage);
    }

    // Runs in its own transaction so a rollback of the calling business transaction
    // doesn't erase the audit trail, and so the entry is visible even if the caller's
    // transaction is still open when this fires from inside an AOP aspect.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String actionType, String description, String resourceType, Long resourceId,
                           String oldValue, String newValue, boolean success, String errorMessage) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(currentUserId());
            entry.setUsername(currentUsername());
            entry.setActionType(actionType);
            entry.setActionDescription(description);
            entry.setResourceType(resourceType);
            entry.setResourceId(resourceId);
            entry.setOldValue(oldValue);
            entry.setNewValue(newValue);
            entry.setSuccess(success);
            entry.setErrorMessage(errorMessage);

            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setIpAddress(getClientIpAddress(request));
                entry.setUserAgent(request.getHeader("User-Agent"));
                entry.setRequestUri(request.getRequestURI());
                entry.setRequestMethod(request.getMethod());
            }

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log for action: {} - {} (error: {})", actionType, description, e.getMessage(), e);
        }
    }

    private Long currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String header = request.getHeader("X-User-Id");
        Long userId;
        try {
            userId = header != null ? Long.parseLong(header) : null;
        } catch (NumberFormatException e) {
            return null;
        }
        // A still-valid JWT can outlive the user it was issued to (deletion, cleanup, etc.);
        // audit_logs.user_id has a FK into users, so an insert for a stale id would otherwise
        // fail and roll back the caller's transaction too. Drop the id rather than the entry.
        if (userId != null && !auditLogRepository.userExists(userId)) {
            log.warn("Audit actor user_id={} no longer exists in users table; logging entry with null user_id", userId);
            return null;
        }
        return userId;
    }

    private String currentUsername() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getHeader("X-Username") : null;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
