package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermitResponseDTO {

    private Long permitId;
    private String permitNo;
    private String permitStatus;
    private LocalDateTime issuedOn;
}