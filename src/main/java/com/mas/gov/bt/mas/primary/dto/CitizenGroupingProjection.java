package com.mas.gov.bt.mas.primary.dto;

/** Fields of t_citizens needed to resolve the application-cap grouping key. */
public interface CitizenGroupingProjection {
    String getRegistrationType();
    String getHouseholdNumber();
    String getLicenseNo();
    String getCompanyRegistrationNumber();
    String getCid();
}
