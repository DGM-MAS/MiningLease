package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_dzongkhag_lookup", schema = "mas_db")
@Data
@Getter
@Setter
public class DzongkhagLookup {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "dzongkhag_name", nullable = false, length = 100)
    private String dzongkhagName;

    @Column(name = "dzongkhag_code", nullable = false, length = 10)
    private String dzongkhagCode;
}
