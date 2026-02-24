package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for task assignment and tracking.
 */
@Entity
@Table(name = "t_task_management", schema = "mas_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, length = 30)
    private String applicationNumber;

    @Column(name = "service_code", nullable = false, length = 30)
    private String serviceCode;

    // Assignment
    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "assigned_to_role", length = 50)
    private String assignedToRole;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // Status
    @Column(name = "task_status", length = 50)
    private String taskStatus;

    // Action taken
    @Column(name = "action_taken", length = 30)
    private String actionTaken;

    @Column(name = "action_remarks", columnDefinition = "TEXT")
    private String actionRemarks;

    @Column(name = "reassignment_count")
    private Integer reassignmentCount = 0;

    @Column(name = "deadline_date")
    private LocalDateTime deadlineDate;

    // Audit fields
    @Column(name = "created_by")
    private Long createdBy;
}
