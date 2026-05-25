package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResubmitBGRequestDTO {
    private Long auctionId;
    private String bgFileId;
    private String remarks;
}