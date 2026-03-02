package com.mas.gov.bt.mas.primary.dto.request;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class RenewalMiningLeaseRequest {

    private String applicationNumber;
    private String applicantName;

    // Applicant Details
    private String applicantContact;
    private String applicantEmail;
    private String applicantCid;
    private String applicantType;
    private String postalAddress;
    private String telephoneNo;

    // Mine Lease Description
    private Integer leasePeriodYears;
    private LocalDate leaseEndDate;
    private Integer proposedLeaseRenewalPeriod;
    private String typeOfMineralsProducts;

    // Location of the Mine
    private String placeOfMiningActivity;
    private String dungkhag;
    private String dzongkhag;
    private String gewog;
    private String nearestVillage;

    private String DepositAssessmentReportId;
    private boolean declarationStatus;

}
