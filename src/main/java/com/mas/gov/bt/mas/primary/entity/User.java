package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity as per Section 34
 * Represents internal users (Government and System Staff)
 * User categories and account statuses are now fully dynamic (String-based)
 */
@Entity
@Table(name = "users", schema = "mas_db")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"roles", "directPermissions", "deniedPermissions"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "contact", length = 20)
    private String contact;

    @Column(name = "cid", unique = true, length = 20)
    private String cid;

    @Column(name = "designation", length = 150)
    private String designation;

    @Column(name = "agency", length = 150)
    private String agency;

    @Column(name = "division", length = 150)
    private String division;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id")
    private RegionMaster region;

    @Column(name = "license_no", length = 50)
    private String licenseNo;

    @Column(name = "account_status", nullable = false, length = 50)
    private String accountStatus = "PENDING";

    @Column(name = "profile_photo_id")
    private Long profilePhotoId;

    @Column(name = "e_signature_id")
    private Long eSignatureId;

    // Login tracking
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "last_login_device_fingerprint", length = 500)
    private String lastLoginDeviceFingerprint;

    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_failed_login")
    private LocalDateTime lastFailedLogin;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    // Password management
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Column(name = "force_password_change")
    private Boolean forcePasswordChange = false;

    // Many-to-Many relationship with Role
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            schema = "mas_db",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Direct per-user permissions (supplements role-based permissions)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_direct_permissions",
            schema = "mas_db",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> directPermissions = new HashSet<>();

    // Per-user denied permissions (revokes a permission the user's role(s) would otherwise grant)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_denied_permissions",
            schema = "mas_db",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> deniedPermissions = new HashSet<>();

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivated_by")
    private Long deactivatedBy;

    @Column(name = "deactivation_reason", length = 500)
    private String deactivationReason;

    // Helper methods
    public boolean isActive() {
        return "ACTIVE".equals(this.accountStatus);
    }

    public boolean isLocked() {
        return "LOCKED".equals(this.accountStatus) ||
               (this.accountLockedUntil != null && this.accountLockedUntil.isAfter(LocalDateTime.now()));
    }

    public boolean isAccountNonExpired() {
        return !"DEACTIVATED".equals(this.accountStatus);
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public void incrementLoginCount() {
        this.loginCount++;
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.lastFailedLogin = null;
        this.accountLockedUntil = null;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLogin = LocalDateTime.now();

        int attempts = this.failedLoginAttempts;
        if (attempts == 3) {
            this.accountLockedUntil = LocalDateTime.now().plusSeconds(30);
        } else if (attempts == 4) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(2);
        } else if (attempts == 5) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(5);
        } else if (attempts == 6) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(15);
        } else if (attempts == 7) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
        } else if (attempts == 8) {
            this.accountLockedUntil = LocalDateTime.now().plusHours(1);
        } else if (attempts == 9) {
            this.accountLockedUntil = LocalDateTime.now().plusHours(4);
        } else if (attempts >= 10) {
            this.accountStatus = "LOCKED";
            this.accountLockedUntil = null;
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLogin = null;
    }

    public void unlockAccount() {
        this.accountStatus = "ACTIVE";
        this.accountLockedUntil = null;
        this.failedLoginAttempts = 0;
    }
}
