package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MiningLeaseGRRequest {
    private String applicantType;

    private String expPermitNo;

    private String applicationType;

    private String applicantCid;

    @Size(max = 255, message = "Applicant name must not exceed 255 characters")
    private String applicantName;


    @Pattern(regexp = "^[0-9]{8}$", message = "Contact must be exactly 8 digits")
    private String applicantContact;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String applicantEmail;

    private String licenseNo;

    private String companyName;

    private Long gRDocId;

    private Long KmzDocId;
}
