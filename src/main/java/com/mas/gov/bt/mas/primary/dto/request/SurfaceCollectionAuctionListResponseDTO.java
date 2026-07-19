package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAuctionListResponseDTO {

    private Long id;

    private String siteName;

    private String applicationNo;

    // Location details
    private String dzongkhagName;
    private String gewogName;
    private String villageName;
    private String regionName;

    private String location;

    private BigDecimal area;

    private String material;

    private String ecFileId;

    private String ecNumber;

    private LocalDate ecValidUpto;

    private String ecStatus;

    private String fcStatus;

    private String auctionStatus;

    private Boolean bgRequested;

    private String bgInstruction;

    private Boolean permitGenerated;

    private LocalDateTime createdOn;

    private Long assignedMdUserId;

    private String assignedMdUserName;

    private String issuePermitFileId;
}