package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per (serviceCode, feeType) — a service can carry an arbitrary number of
 * independently priced/enabled fees, not a fixed application/registration/renewal 3.
 * amount is nullable by design for fees whose amount is case-specific (set by an
 * officer/citizen at charge time) — those rows only control whether the fee is
 * collected at all.
 */
@Entity
@Table(name = "payment_master", schema = "mas_db")
@Getter
@Setter
public class PaymentMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceCode;
    private String serviceName;
    private String feeType;
    private String feeLabel;
    private BigDecimal amount;
    private Boolean isEnabled;

    /** Nullable: null = applies at any application status. */
    private String triggerStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String createdBy;
    private LocalDateTime createdOn;

    private String updatedBy;
    private LocalDateTime updatedOn;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
