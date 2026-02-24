package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_gewog_lookup", schema = "mas_db")
@Data
@Getter
@Setter
public class GewogLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gewog_serial_no")
    private Integer gewogSerialNo;

    @Column(name = "gewog_id")
    private String gewogId;

    @Column(name = "gewog_name")
    private String gewogName;

    @Column(name = "dzongkhag_serial_no")
    private Integer dzongkhagSerialNo;

    @Column(name = "gewog_name_bh")
    private String gewogNameBh;
}
