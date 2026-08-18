package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "site_name")
    private String siteName;

    @Column(name = "site_application_no")
    private String siteApplicationNo;

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

    // In-country / Ex-country
    @Column(name = "applicant_scope", length = 50)
    private String applicantScope;

    // =========================================================
    // MINERALS
    // =========================================================

    @OneToMany(
            mappedBy = "sampleTransportClearance",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<SampleTransportClearanceMineralEntity> minerals =
            new ArrayList<>();

    // Name of the rock/Mineral
//    @Column(name = "rock_mineral_name", nullable = false, length = 255)
//    private String rockMineralName;
//
//    // Specify (only if rock/mineral name = Other)
//    @Column(name = "rock_mineral_name_specify", length = 255)
//    private String rockMineralNameSpecify;
//
//    // Number of Samples
//    @Column(name = "sample_count", nullable = false)
//    private Integer sampleCount;
//
//    // Form of Sample (Solid/Powder/Other)
//    @Column(name = "sample_form", nullable = false, length = 50)
//    private String sampleForm;
//
//    // Specify (only if sample form = Other)
//    @Column(name = "sample_form_specify", length = 255)
//    private String sampleFormSpecify;
//
//    // Total Weight
//    @Column(name = "total_weight", nullable = false)
//    private Double totalWeight;
//
//    // Unit (KG/G)
//    @Column(name = "weight_unit", nullable = false, length = 20)
//    private String weightUnit;

    // Purpose of shipping
    @Column(name = "shipping_purpose", length = 500)
    private String shippingPurpose;

    // Mode of shipping (Air/Road/Rail)
    @Column(name = "shipping_mode", nullable = false, length = 50)
    private String shippingMode;

    // Destination
    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    // Destination country (Ex-country applications only)
    @Column(name = "destination_country", length = 100)
    private String destinationCountry;

    // Sample photo (mandatory)
    @Column(name = "sample_photo_file_id", length = 100)
    private String samplePhotoFileId;

    // Others attachment (optional)
    @Column(name = "others_file_id", length = 100)
    private String othersFileId;


    // Assigned Focal and remarks
    @Column(name = "assgined_gsd_chief_id")
    private Long assignedGSDChiefId;

    @Column(name = "assigned_gsd_chief_remarks", columnDefinition = "TEXT")
    private String assignedGSDChiefRemarks;

    @Column(name = "assigned_gsd_focal_id")
    private Long assignedGSDFocalId;

    @Column(name = "assigned_gsd_focal_remarks", columnDefinition = "TEXT")
    private String assignedGSDFocalRemarks;

    @Column(name = "file_id_gsd_focal")
    private String fileIdGSDFocal;


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

    @Column(name = "region_id")
    private Long regionId;

    // Dzongkhag, gewog and village details have been saved
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dzongkhag_id")
    private DzongkhagLookup dzongkhagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gewog_id")
    private GewogLookup gewogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private VillageLookup villageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_master")
    private RegionMaster regionMaster;

    @Column(name = "sample_transport_clearance_certificate_file_id")
    private String sampleTransportClearanceCertificateFileId;

    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
