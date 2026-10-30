package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "t_household_permit_threshold",
    schema = "mas_db",
    indexes = {
        @Index(name = "idx_hpt_cid_service_status", columnList = "applicant_cid, service_type, status")
    }
)
public class HouseholdPermitThresholdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_cid", nullable = false)
    private String applicantCid;

    /**
     * Identifies the permit type: SURFACE_COLLECTION_PERMIT | MINING_LEASE | QUARRYING
     * Use the constants on HouseholdPermitThresholdService.
     */
    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "application_no")
    private String applicationNo;

    @Column(name = "permit_no")
    private String permitNo;

    /**
     * PENDING  — application submitted, permit not yet issued
     * ACTIVE   — permit issued and operational
     * INACTIVE — application rejected or permit revoked
     */
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
    }

    @PreUpdate
    void preUpdate() {
        updatedOn = LocalDateTime.now();
    }
}
