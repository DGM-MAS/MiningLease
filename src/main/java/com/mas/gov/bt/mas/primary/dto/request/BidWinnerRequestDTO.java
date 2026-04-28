package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidWinnerRequestDTO {

    private String bidWinnerName;
    private String contactNumber;
    private String emailAddress;
    private String otherDetails;
}