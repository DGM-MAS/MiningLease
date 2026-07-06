package com.mas.gov.bt.mas.primary.audit;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Maps to the same mas_db.audit_logs table owned by mas-backend-masters
 * (see AuditLogController there for the read API). Written directly here
 * since all MAS services share one Postgres schema — no cross-service HTTP call.
 * userId is a plain column (not a JPA relation) since this service has no User entity.
 */
@Entity
@Table(name = "audit_logs", schema = "mas_db")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "action_description", length = 1000)
    private String actionDescription;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
