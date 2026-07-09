package com.mas.gov.bt.mas.primary.dto.response;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAuctionResponseDTO {

    private Long id;

    private String siteName;
    private String applicationNo;
    private String location;
    private BigDecimal area;
    private String material;

    private String ecStatus;
    private String fcStatus;
    private String auctionStatus;

    private Boolean submittedForEc;
    private Boolean submittedForFc;
    private Boolean bgRequested;
    private String bgInstruction;
    private Boolean permitGenerated;

    private LocalDateTime createdOn;

    private List<SurfaceCollectionAttachmentResponseDTO> attachments;
    private BidWinnerResponseDTO bidWinner;
}