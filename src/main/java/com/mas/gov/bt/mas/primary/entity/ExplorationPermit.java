package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

// Created ONLY AFTER approval
@Entity
@Table(name = "exploration_permit")
@Data
public class ExplorationPermit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;

    private String permitNumber;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    private String permitIssuedTo;

    private Boolean active;
}
