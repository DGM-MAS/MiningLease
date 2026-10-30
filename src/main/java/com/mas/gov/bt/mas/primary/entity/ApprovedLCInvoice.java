package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "approved_lc_invoice", schema = "mas_db")
@Data
public class ApprovedLCInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicationNumber;
    private String referenceNumber;
    private String documentCode;

    private String destination;
    private BigDecimal quantity;
    private BigDecimal ratePerMt;
    private BigDecimal saleValue;
    private LocalDate validityDate;

    @Column(name = "assigned_rc")
    private Long approvedBy;

    @Column(name = "reviewed_on")
    private LocalDateTime approvedOn;

    private BigDecimal utilizedQuantity;
}
