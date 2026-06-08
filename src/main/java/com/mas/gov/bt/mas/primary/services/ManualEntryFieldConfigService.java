package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.response.FieldMetadata;
import com.mas.gov.bt.mas.primary.dto.response.ManualEntryFieldConfigResponse;
import com.mas.gov.bt.mas.primary.dto.response.SectionConfig;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManualEntryFieldConfigService {

    public ManualEntryFieldConfigResponse getConfig(String activityType) {
        return switch (activityType.toUpperCase()) {
            case "MINING_LEASE"       -> miningLeaseConfig();
            case "QUARRY_LEASE"       -> quarryLeaseConfig();
            case "SURFACE_COLLECTION" -> surfaceCollectionConfig();
            case "STOCK_LIFTING"      -> stockLiftingConfig();
            default -> throw new BusinessException(
                    ErrorCodes.INVALID_INPUT_DATA,
                    "Unknown activityType. Must be MINING_LEASE, QUARRY_LEASE, SURFACE_COLLECTION or STOCK_LIFTING"
            );
        };
    }

    // -------------------------------------------------------
    // Mining Lease
    // -------------------------------------------------------
    private ManualEntryFieldConfigResponse miningLeaseConfig() {
        return ManualEntryFieldConfigResponse.builder()
                .activityType("MINING_LEASE")
                .label("Mining Lease Manual Entry")
                .sections(List.of(
                        applicantSection(true),
                        locationSection(true),
                        mineDetailsSection("Mining Lease"),
                        approvedDetailsSection(),
                        miningLeaseDocumentsSection()
                ))
                .build();
    }

    // -------------------------------------------------------
    // Quarry Lease
    // -------------------------------------------------------
    private ManualEntryFieldConfigResponse quarryLeaseConfig() {
        return ManualEntryFieldConfigResponse.builder()
                .activityType("QUARRY_LEASE")
                .label("Quarry Lease Manual Entry")
                .sections(List.of(
                        applicantSection(true),
                        locationSection(true),
                        mineDetailsSection("Quarry Lease"),
                        approvedDetailsSection(),
                        quarryLeaseDocumentsSection()
                ))
                .build();
    }

    // -------------------------------------------------------
    // Surface Collection
    // -------------------------------------------------------
    private ManualEntryFieldConfigResponse surfaceCollectionConfig() {
        return ManualEntryFieldConfigResponse.builder()
                .activityType("SURFACE_COLLECTION")
                .label("Surface Collection Manual Entry")
                .sections(List.of(
                        applicantSection(false),
                        locationSection(false),
                        scActivitySection("SURFACE_COLLECTION"),
                        scDocumentsSection()
                ))
                .build();
    }

    // -------------------------------------------------------
    // Stock Lifting
    // -------------------------------------------------------
    private ManualEntryFieldConfigResponse stockLiftingConfig() {
        return ManualEntryFieldConfigResponse.builder()
                .activityType("STOCK_LIFTING")
                .label("Stock Lifting Manual Entry")
                .sections(List.of(
                        applicantSection(false),
                        locationSection(false),
                        scActivitySection("STOCK_LIFTING"),
                        scDocumentsSection()
                ))
                .build();
    }

    // -------------------------------------------------------
    // Shared sections
    // -------------------------------------------------------

    private SectionConfig applicantSection(boolean includeLicenseFields) {
        List<FieldMetadata> fields = new java.util.ArrayList<>(List.of(
                field("applicantType",    "Applicant Type",   "TEXT",  true,  "Individual or Company"),
                field("applicantCid",     "CID Number",       "TEXT",  true,  "11-digit CID"),
                field("applicantName",    "Full Name",        "TEXT",  true,  null),
                field("applicantContact", "Mobile Number",    "PHONE", true,  "8-digit mobile number"),
                field("applicantEmail",   "Email Address",    "EMAIL", true,  null),
                field("postalAddress",    "Postal Address",   "TEXT",  false, null),
                field("telephoneNo",      "Telephone Number", "PHONE", false, null)
        ));

        if (includeLicenseFields) {
            fields.addAll(List.of(
                    field("licenseNo",            "License Number",            "TEXT", false, null),
                    field("businessLicenseNo",    "Business License Number",   "TEXT", false, null),
                    field("companyRegistrationNo","Company Registration No.",  "TEXT", false, null),
                    field("companyName",          "Company Name",              "TEXT", false, null),
                    field("companyType",          "Company Type",              "TEXT", false, null)
            ));
        }

        return SectionConfig.builder()
                .sectionKey("APPLICANT_DETAILS")
                .label("Applicant / Company Details")
                .fields(fields)
                .build();
    }

    private SectionConfig locationSection(boolean includeMiningFields) {
        List<FieldMetadata> fields = new java.util.ArrayList<>(List.of(
                field("dzongkhag",      "Dzongkhag",     "TEXT", true,  null),
                field("gewog",          "Gewog",         "TEXT", true,  null),
                field("nearestVillage", "Nearest Village","TEXT",!includeMiningFields, null),
                field("placeOfActivity","Place of Activity","TEXT",includeMiningFields, null),
                field("dungkhag",       "Dungkhag",      "TEXT", false, null)
        ));

        return SectionConfig.builder()
                .sectionKey("LOCATION")
                .label("Location Details")
                .fields(fields)
                .build();
    }

    private SectionConfig mineDetailsSection(String leaseType) {
        return SectionConfig.builder()
                .sectionKey("MINE_DETAILS")
                .label(leaseType + " Details")
                .fields(List.of(
                        field("typeOfMines",                     "Type of Mines",                   "TEXT",     true,  null),
                        field("typeOfMinerals",                  "Type of Minerals / Products",     "TEXT",     true,  null),
                        field("requiredInvestment",              "Required Investment",             "TEXT",     false, null),
                        field("sourceOfFinance",                 "Source of Finance",               "TEXT",     false, null),
                        field("technicalCompetenceExperience",   "Technical Competence",            "TEXTAREA", false, null),
                        field("workforceRequirementRecruitment", "Workforce Requirement",           "TEXTAREA", false, null),
                        field("proposedLeasePeriod",             "Proposed Lease Period",           "TEXT",     true,  "e.g. 10 years"),
                        field("srf",                             "SRF Area",                        "TEXT",     false, null),
                        field("landPrivate",                     "Private Land Area",               "TEXT",     false, null),
                        field("totalLand",                       "Total Land Area",                 "TEXT",     false, null)
                ))
                .build();
    }

    private SectionConfig approvedDetailsSection() {
        return SectionConfig.builder()
                .sectionKey("APPROVED_DETAILS")
                .label("Approved / Final Details")
                .fields(List.of(
                        field("approvedArea",        "Approved Area",           "TEXT",    true,  null),
                        field("approvedErb",         "Approved ERB",            "TEXT",    false, null),
                        field("approvedMineral",     "Approved Mineral",        "TEXT",    true,  null),
                        field("approvedLeasePeriod", "Approved Lease Period",   "TEXT",    true,  null),
                        field("leaseStartDate",      "Lease Start Date",        "DATE",    true,  null),
                        field("leaseEndDate",        "Lease End Date",          "DATE",    true,  null),
                        field("leasePeriodYears",    "Lease Period (Years)",    "NUMBER",  false, null),
                        field("upfrontPaymentAmount","Upfront Payment Amount",  "DECIMAL", false, null),
                        field("fmfsStatus",          "FMFS Status",             "TEXT",    false, null),
                        field("fmfsId",              "FMFS ID",                 "TEXT",    false, null),
                        field("ecStatus",            "EC Status",               "TEXT",    false, null),
                        field("ecExpiryDate",        "EC Expiry Date",          "DATE",    false, null),
                        field("mlaStatus",           "MLA Status",              "TEXT",    false, null),
                        field("geologicalReportStatus","Geological Report Status","TEXT",  false, null)
                ))
                .build();
    }

    private SectionConfig miningLeaseDocumentsSection() {
        return SectionConfig.builder()
                .sectionKey("DOCUMENTS")
                .label("Documents")
                .fields(List.of(
                        field("pfsDocId",                  "PFS Document",               "FILE_ID", false, null),
                        field("locationMapDocId",          "Location Map",               "FILE_ID", false, null),
                        field("financialCapabilityDocId",  "Financial Capability Doc",   "FILE_ID", false, null),
                        field("explorationReportDocId",    "Exploration Report",         "FILE_ID", false, null),
                        field("consentLetterDocId",        "Consent Letter",             "FILE_ID", false, null),
                        field("geologicalReportDocId",     "Geological Report",          "FILE_ID", false, null),
                        field("fmfsDocId",                 "FMFS Document",              "FILE_ID", false, null),
                        field("llcDocId",                  "LLC Document",               "FILE_ID", false, null),
                        field("notesheetDocId",            "Notesheet",                  "FILE_ID", false, null),
                        field("mlaDocId",                  "MLA Document",               "FILE_ID", false, null),
                        field("fileUploadIdGr",            "GR File",                    "FILE_ID", false, null),
                        field("fileUploadIdKmz",           "KMZ File",                   "FILE_ID", false, null),
                        field("fileUploadIdPA",            "PA File",                    "FILE_ID", false, null),
                        field("fileUploadIdFC",            "FC File",                    "FILE_ID", false, null),
                        field("fileUploadIdPublicClearance","Public Clearance File",     "FILE_ID", false, null),
                        field("mpcdFileUploadIdPA",        "MPCD PA File",               "FILE_ID", false, null),
                        field("mpcdFileUploadIdMa",        "MPCD Ma File",               "FILE_ID", false, null),
                        field("signedPFSId",               "Signed PFS",                 "FILE_ID", false, null),
                        field("bankGuarantorDocId",        "Bank Guarantor Document",    "FILE_ID", false, null),
                        field("workOrderDocId",            "Work Order Document",        "FILE_ID", false, null),
                        field("fileIds",                   "Additional Attachments",     "FILE_ID", false, "Multiple files allowed")
                ))
                .build();
    }

    private SectionConfig quarryLeaseDocumentsSection() {
        return SectionConfig.builder()
                .sectionKey("DOCUMENTS")
                .label("Documents")
                .fields(List.of(
                        field("pfsDocId",                  "PFS Document",               "FILE_ID", false, null),
                        field("locationMapDocId",          "Location Map",               "FILE_ID", false, null),
                        field("financialCapabilityDocId",  "Financial Capability Doc",   "FILE_ID", false, null),
                        field("explorationReportDocId",    "Exploration Report",         "FILE_ID", false, null),
                        field("consentLetterDocId",        "Consent Letter",             "FILE_ID", false, null),
                        field("geologicalReportDocId",     "Geological Report",          "FILE_ID", false, null),
                        field("fmfsDocId",                 "FMFS Document",              "FILE_ID", false, null),
                        field("llcDocId",                  "LLC Document",               "FILE_ID", false, null),
                        field("notesheetDocId",            "Notesheet",                  "FILE_ID", false, null),
                        field("mlaDocId",                  "MLA Document",               "FILE_ID", false, null),
                        field("fileUploadIdPA",            "PA File",                    "FILE_ID", false, null),
                        field("fileUploadIdFC",            "FC File",                    "FILE_ID", false, null),
                        field("fileUploadIdPublicClearance","Public Clearance File",     "FILE_ID", false, null),
                        field("bankGuarantorDocId",        "Bank Guarantor Document",    "FILE_ID", false, null),
                        field("workOrderDocId",            "Work Order Document",        "FILE_ID", false, null),
                        field("fileIds",                   "Additional Attachments",     "FILE_ID", false, "Multiple files allowed")
                ))
                .build();
    }

    private SectionConfig scActivitySection(String activityType) {
        return SectionConfig.builder()
                .sectionKey("ACTIVITY_DETAILS")
                .label("Activity Details")
                .fields(List.of(
                        field("typeOfActivity",           "Type of Activity",            "MULTI_SELECT", true,  "Surface collection, Dredging, Stock lifting, Migration works"),
                        field("typeOfMaterials",          "Type of Materials",           "MULTI_SELECT", true,  "Stones, Sand, Boulder, Minerals"),
                        field("collectionSite",           "Collection Site",             "MULTI_SELECT", true,  "Land surface, Riverbeds/Riverbanks, Road Right of Way, Land Developments, Others"),
                        field("proposedAreaSrf",          "Proposed Area — SRF (acres)", "DECIMAL",      false, null),
                        field("proposedAreaStateLand",    "Proposed Area — State Land",  "DECIMAL",      false, null),
                        field("proposedAreaPrivate",      "Proposed Area — Private",     "DECIMAL",      false, null),
                        field("proposedAreaRow",          "Proposed Area — ROW",         "DECIMAL",      false, null),
                        field("permitNo",                 "Permit Number",               "TEXT",         false,  null),
                        field("ecNo",                     "EC Number",                   "TEXT",         false, null),
                        field("securityClearanceValidity","Security Clearance Validity", "TEXT",         false, null),
                        field("taxClearanceValidity",     "Tax Clearance Validity",      "TEXT",         false, null),
                        field("isStateOwned",             "State Owned Entity",          "BOOLEAN",      false, null),
                        field("eligibleForExport",        "Eligible for Export",         "TEXT",         false, "Yes or No"),
                        field("isRpBased",                "RP Based",                    "BOOLEAN",      false, "true = RP-based (export eligible); false = Direct Allocation (domestic)")
                ))
                .build();
    }

    private SectionConfig scDocumentsSection() {
        return SectionConfig.builder()
                .sectionKey("DOCUMENTS")
                .label("Documents")
                .fields(List.of(
                        field("attachmentMapFileId",         "Attachment Map",            "FILE_ID", false, null),
                        field("recommendationLetterFileId",  "Recommendation Letter",     "FILE_ID", false, null),
                        field("scConsentLetterFileId",       "Consent Letter",            "FILE_ID", false, null),
                        field("fcFileId",                    "FC Document",               "FILE_ID", false, null),
                        field("ieeFileId",                   "IEE Document",              "FILE_ID", false, null),
                        field("empFileId",                   "EMP Document",              "FILE_ID", false, null),
                        field("admApprovalFileId",           "ADM Approval",              "FILE_ID", false, null),
                        field("undertakingFileId",           "Undertaking Document",      "FILE_ID", false, null),
                        field("bgFileId",                    "Bank Guarantee",            "FILE_ID", false, null),
                        field("mpcdReportFileId",            "MPCD Report",               "FILE_ID", false, null),
                        field("iomFileId",                   "IOM Document",              "FILE_ID", false, null),
                        field("rcReportFileId",              "RC Report",                 "FILE_ID", false, null),
                        field("miReportFileId",              "MI Report",                 "FILE_ID", false, null),
                        field("meReportFileId",              "ME Report",                 "FILE_ID", false, null),
                        field("scEcFileId",                  "EC Document",               "FILE_ID", false, null),
                        field("fileIds",                     "Additional Attachments",    "FILE_ID", false, "Multiple files allowed")
                ))
                .build();
    }

    private FieldMetadata field(String name, String label, String type, boolean required, String hint) {
        return FieldMetadata.builder()
                .name(name)
                .label(label)
                .type(type)
                .required(required)
                .hint(hint)
                .build();
    }
}
