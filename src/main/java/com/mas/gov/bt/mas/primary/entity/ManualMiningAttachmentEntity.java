package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_manual_mining_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMiningAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "manual_entry_id", nullable = false)
    private Long manualMiningEntryId;
}