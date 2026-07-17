package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidWinnerResponseDTO {

    private Long id;
    private String bidWinnerName;
    private String contactNumber;
    private String emailAddress;

    private String licenseNumber;

    private String companyRegistrationNumber;
    private String companyType;
    private String cidNumber;
    private String bidAmount;

    private String dzongkhagId;
    private String gewogId;
    private String villageId;

    private String regionId;
}