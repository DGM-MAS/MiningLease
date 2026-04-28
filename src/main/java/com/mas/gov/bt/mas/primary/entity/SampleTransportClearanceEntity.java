package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_sample_transport_clearance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleTransportClearanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_no", unique = true, nullable = false, length = 100)
    private String applicationNo;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    // Name of Individual/agency/organization/corporation/company
    @Column(name = "applicant_name", nullable = false, length = 255)
    private String applicantName;

    // Contact no
    @Column(name = "contact_no", nullable = false)
    private String contactNo;

    // Email Address
    @Column(name = "email_address", length = 255)
    private String emailAddress;

    // Name of the rock/Mineral
    @Column(name = "rock_mineral_name", nullable = false, length = 255)
    private String rockMineralName;

    // Number of Samples
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    // Form of Sample (Solid/Powder/Other)
    @Column(name = "sample_form", nullable = false, length = 50)
    private String sampleForm;

    // Specify (only if sample form = Other)
    @Column(name = "sample_form_specify", length = 255)
    private String sampleFormSpecify;

    // Total Weight
    @Column(name = "total_weight", nullable = false)
    private Double totalWeight;

    // Unit (KG/G)
    @Column(name = "weight_unit", nullable = false, length = 20)
    private String weightUnit;

    // Purpose of shipping
    @Column(name = "shipping_purpose", length = 500)
    private String shippingPurpose;

    // Mode of shipping (Air/Road/Rail)
    @Column(name = "shipping_mode", nullable = false, length = 50)
    private String shippingMode;

    // Destination
    @Column(name = "destination", nullable = false, length = 255)
    private String destination;


    // Assigned Focal and remarks
    @Column(name = "assgined_gsd_chief_id")
    private Long assignedGSDChiefId;

    @Column(name = "assigned_gsd_chief_remarks", columnDefinition = "TEXT")
    private String assignedGSDChiefRemarks;

    @Column(name = "assigned_gsd_focal_id")
    private Long assignedGSDFocalId;

    @Column(name = "assigned_gsd_focal_remarks")
    private String assignedGSDFocalRemarks;

    /* ================= AUDIT COLUMNS ================= */

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "status")
    private String status;

    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
