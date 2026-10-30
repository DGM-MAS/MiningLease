package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Table(name = "immediate_suspension_reason_master", schema = "mas_db")
public class ImmediateSuspensionReasonMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reason_name", nullable = false)
    private String reasonName;

    /* ===== Audit ===== */
    @Column(updatable = false)
    private Long createdBy;

    @Column(updatable = false)
    private LocalDateTime createdOn;

    private Long updatedBy;
    private LocalDateTime updatedOn;

    /* ===== Soft Delete ===== */
    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
        this.isActive = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
