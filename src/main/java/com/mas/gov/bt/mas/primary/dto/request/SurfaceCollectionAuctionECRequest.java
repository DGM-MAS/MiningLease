package com.mas.gov.bt.mas.primary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurfaceCollectionAuctionECRequest {

    private String ecFileId;

    private String ecNumber;

    private LocalDate ecValidUpto;

    private String remarks;
}
