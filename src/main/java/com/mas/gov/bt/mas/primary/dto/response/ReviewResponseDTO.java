package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {

    private Long reviewId;
    private Long bgId;
    private Long assignedMeId;
    private String reviewStatus;
    private String remarks;
}