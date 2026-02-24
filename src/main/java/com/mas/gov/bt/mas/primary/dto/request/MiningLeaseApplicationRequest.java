package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating a Quarry Lease Application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiningLeaseApplicationRequest {

    private String applicationNo;

    private String applicantType;

    private String applicationType;

    private String applicantCid;

    @Size(max = 255, message = "Applicant name must not exceed 255 characters")
    private String applicantName;


    @Pattern(regexp = "^[0-9]{8}$", message = "Contact must be exactly 8 digits")
    private String applicantContact;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String applicantEmail;

    private String postalAddress;

    private String telephoneNo;

    private String licenseNo;

    private String businessLicenseNo;

    private String companyName;

    private String typeOfMines;

    private String typeOfMineralsProducts;

    private String requiredInvestment;

    private String sourceOfFinance;

    private String technicalCompetenceExperience;

    private String workforceRequirementRecruitment;

    private String proposedLeasePeriod;

    private String srf;

    private String landPrivate;

    private String totalLand;

    private String placeOfMiningActivity;

    private String dungkhag;

    @Size(max = 100, message = "Dzongkhag must not exceed 100 characters")
    private String dzongkhag;

    @Size(max = 100, message = "Gewog must not exceed 100 characters")
    private String gewog;

    @Size(max = 100, message = "Village must not exceed 100 characters")
    private String nearestVillage;

    private Boolean applicationFeesRequired;

    private String pfsDocId;
    private String locationMapDocId;
    private String financialCapabilityDocId;
    private String explorationReportDocId;
    private String consentLetterDocId;
    private String geologicalReportDocId;
    private String fmfsDocId;
}
