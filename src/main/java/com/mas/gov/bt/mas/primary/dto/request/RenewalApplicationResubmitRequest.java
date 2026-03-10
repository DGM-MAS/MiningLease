package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class RenewalApplicationResubmitRequest {

    private String applicationNumber;
    private String applicantContact;
    private String applicantEmail;
    private String applicantCid;
    private String applicantType;
    private String postalAddress;
    private String telephoneNo;

    private Integer leasePeriodYears;
    private LocalDate leaseEndDate;
    private Integer proposedLeaseRenewalPeriod;

    private String placeOfMiningActivity;
    private String dungkhag;
    private String dzongkhag;
    private String gewog;
    private String nearestVillage;

    private String depositAssessmentReportId;
    private boolean declarationStatus;
}
