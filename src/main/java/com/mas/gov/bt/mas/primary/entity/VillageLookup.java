package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_village_lookup", schema = "mas_db")
@Data
public class VillageLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "village_serial_no")
    private Integer villageSerialNo;

    @Column(name = "village_id", unique = true)
    private String villageId;

    @Column(name = "village_name")
    private String villageName;

    @Column(name = "village_name_bh")
    private String villageNameBh;

    @Column(name = "gewog_serial_no")
    private Integer gewogSerialNo;
}
