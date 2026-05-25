package com.mas.gov.bt.mas.primary.dto.request;

import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionBankGuarantee;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAuctionListResponseDTO {

    private Long id;

    private String applicationNo;

    private String location;

    private BigDecimal area;

    private String material;

    private String ecStatus;

    private String fcStatus;

    private String auctionStatus;

    private Boolean bgRequested;

    private String bgInstruction;

    private Boolean permitGenerated;

    private LocalDateTime createdOn;
}