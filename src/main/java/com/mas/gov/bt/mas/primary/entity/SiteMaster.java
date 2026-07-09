package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Shared mas_db.site_master table (owned by mas-backend-masters).
 * Mapped here only to auto-create a site when a mining lease is approved.
 */
@Entity
@Data
@Table(name = "site_master", schema = "mas_db")
public class SiteMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name")
    private String siteName;

    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    // MINING_LEASE | QUARRY_LEASE
    @Column(name = "lease_type", length = 20)
    private String leaseType;

    @Column(name = "lease_application_id")
    private Long leaseApplicationId;

    @Column(name = "lease_application_number", length = 30)
    private String leaseApplicationNumber;

    @Column(name = "dzongkhag_id")
    private String dzongkhagId;

    @Column(name = "gewog_name_id")
    private String gewogNameId;

    @Column(name = "dungkhag_name")
    private String dungkhagName;

    @Column(name = "nearest_village_id")
    private String nearestVillageId;

    private String place;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
        this.isActive = true;
    }
}
