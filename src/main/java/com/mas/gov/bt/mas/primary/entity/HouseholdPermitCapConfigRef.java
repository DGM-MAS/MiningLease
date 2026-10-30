package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Read-only view of shared mas_db.household_permit_threshold_config
 * (owned/edited by mas-backend-masters' admin "Manage Application Caps" page).
 */
@Entity
@Data
@Table(name = "household_permit_threshold_config", schema = "mas_db")
public class HouseholdPermitCapConfigRef {

    @Id
    private Long id;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "registration_type")
    private String registrationType;

    @Column(name = "max_allowed")
    private Integer maxAllowed;
}
