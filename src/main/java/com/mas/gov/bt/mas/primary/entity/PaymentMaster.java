package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_master", schema = "mas_db")
@Getter
@Setter
public class PaymentMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal applicationFee;
    private Boolean isApplicationFeeEnabled;

    private BigDecimal registrationFee;
    private Boolean isRegistrationFeeEnabled;

    private String serviceCode;
    private String serviceName;

    private String createdBy;
    private LocalDateTime createdOn;

    private String updatedBy;
    private LocalDateTime updatedOn;

    private BigDecimal renewalFee;
    private Boolean isRenewalFeeEnabled;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
