package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_sample_transport_clearance_mineral")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleTransportClearanceMineralEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sample_transport_clearance_id",
            nullable = false
    )
    private SampleTransportClearanceEntity sampleTransportClearance;

    @Column(name = "rock_mineral_name")
    private String rockMineralName;

    @Column(name = "rock_mineral_name_specify")
    private String rockMineralNameSpecify;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "sample_form")
    private String sampleForm;

    @Column(name = "sample_form_specify")
    private String sampleFormSpecify;

    @Column(name = "total_weight")
    private Double totalWeight;

    @Column(name = "weight_unit")
    private String weightUnit;
}