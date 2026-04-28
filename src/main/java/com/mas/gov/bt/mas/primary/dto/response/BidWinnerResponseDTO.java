package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidWinnerResponseDTO {

    private Long id;
    private String bidWinnerName;
    private String contactNumber;
    private String emailAddress;
    private String otherDetails;
}