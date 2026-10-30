package com.mas.gov.bt.mas.primary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenRegisterRequest {
    private String registrationType; // "INDIVIDUAL" | "BUSINESS_LICENSE" | "REGISTERED_COMPANY"
    private String email;
    private String phoneNumber;
    private String cid;
    private String name;
    private String mobileNumber;
    private String householdNumber;
    private String licenseNumber;
    private String businessName;
    private String businessOwner;
    private String companyRegistrationNumber;
    private String companyName;
    private String companyType;
}