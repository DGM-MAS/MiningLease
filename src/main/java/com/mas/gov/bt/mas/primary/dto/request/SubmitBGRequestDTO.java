package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitBGRequestDTO {
    private String bgFileId;
    private String promoterRemarks;
    private Long auctionId;
}