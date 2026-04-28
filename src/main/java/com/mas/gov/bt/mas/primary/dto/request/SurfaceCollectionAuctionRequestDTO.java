package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurfaceCollectionAuctionRequestDTO {

    private String location;
    private BigDecimal area;
    private String material;

    private List<SurfaceCollectionAttachmentRequestDTO> attachments;
}