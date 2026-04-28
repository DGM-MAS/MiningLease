package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BGResponseDTO {

    private Long id;
    private Long auctionId;
    private String bgFileId;
    private String bgInstruction;
    private String status;
    private LocalDateTime submittedOn;
    private LocalDateTime resubmittedOn;
}