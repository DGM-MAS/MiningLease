package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Shared mas_db.household_permit_threshold table (owned by mas-royalty-service).
 * Mapped here (write-capable) only to record an ACTIVE entry when a mining
 * lease reaches final approval — mirrors HouseholdPermitThresholdEntity in
 * mas-royalty-service.
 */
@Entity
@Data
@Table(name = "household_permit_threshold", schema = "mas_db")
public class HouseholdPermitThresholdEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_cid", nullable = false)
    private String applicantCid;

    /** MINING_LEASE for this service. */
    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "application_no")
    private String applicationNo;

    @Column(name = "permit_no")
    private String permitNo;

    /** ACTIVE — permit issued and operational. */
    @Column(nullable = false)
    private String status;

    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @PrePersist
    void prePersist() {
        createdOn = LocalDateTime.now();
        updatedOn = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
