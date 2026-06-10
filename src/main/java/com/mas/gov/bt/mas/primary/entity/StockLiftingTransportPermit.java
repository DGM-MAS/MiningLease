package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_stock_lifting_transport_permit")
@Data
public class StockLiftingTransportPermit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draft_number", unique = true)
    private String draftNumber;

    @Column(name = "tp_number", unique = true)
    private String tpNumber;

    /** FK to stock_lifting_application.id — the approved SL permit this TP is raised against */
    @Column(name = "source_id")
    private Long sourceId;

    private String source;

    @Column(name = "material_id")
    private Long materialId;

    /** The stock lifting permit number (e.g. SLP-20260324-000033) this TP is raised against */
    @Column(name = "stock_lifting_permit_no")
    private String stockLiftingPermitNo;

    @Column(name = "site_id")
    private Long siteId;

    @Column(name = "site_name")
    private String siteName;

    private String destinationType;

    private String domesticDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_lc_invoice_id")
    private ApprovedLCInvoice approvedLCInvoice;

    private String transportDetails;

    private String vehicleNumber;

    private String vehicleType;

    private String destination;

    @Column(name = "weight_slip_path")
    private String weightSlipPath;

    @Column(name = "invoice_file_path")
    private String invoiceFilePath;

    private String grade;

    private String category;

    private String country;

    private String exportingCountry;

    @Column(name = "exporter_name")
    private String exporterName;

    @Column(name = "gross_weight", precision = 14, scale = 3)
    private BigDecimal grossWeight;

    @Column(name = "tare_weight", precision = 14, scale = 3)
    private BigDecimal tareWeight;

    private BigDecimal quantity;

    private BigDecimal calculatedRoyalty;

    @Column(name = "applicable_royalty", precision = 19, scale = 2)
    private BigDecimal applicableRoyalty;

    @Column(name = "applicable_mineral_rent", precision = 19, scale = 2)
    private BigDecimal applicableMineralRent;

    private String status;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdOn;

    private Long reviewedBy;

    private LocalDateTime reviewedOn;

    @Column(length = 500)
    private String remarks;

    @Column(name = "adjustment_reason", length = 1000)
    private String adjustmentReason;

    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    /** RC who currently owns this TP for review (set on first action; updated on reassign) */
    @Column(name = "assigned_rc")
    private Long assignedRc;

    /** Shared identifier for all TPs submitted in the same batch. Null for single submissions. */
    @Column(name = "batch_no")
    private String batchNo;

    /** Shared identifier for all permits saved as draft in the same batch. */
    @Column(name = "batch_draft_no")
    private String batchDraftNo;

    // APPLICANT DETAILS

    @Column(name = "applicant_type", length = 50)
    private String applicantType;

    @Column(name = "applicant_cid", length = 11)
    private String applicantCid;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    @Column(name = "applicant_contact", length = 20)
    private String applicantContact;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "postal_address", length = 500)
    private String postalAddress;

    @Column(name = "telephone_no", length = 20)
    private String telephoneNo;

    @Column(name = "license_no", length = 50)
    private String licenseNo;

    @Column(name = "business_license_no", length = 50)
    private String businessLicenseNo;

    @Column(name = "company_registration_no", length = 255)
    private String companyRegistrationNo;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "company_type", length = 255)
    private String companyType;
}
