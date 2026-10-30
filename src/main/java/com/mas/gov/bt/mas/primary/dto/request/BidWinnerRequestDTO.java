package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidWinnerRequestDTO {

    private String siteName;

    private String bidWinnerName;

    private String contactNumber;
    private String emailAddress;

    private String cidNumber;
    private String houseHoldNumber;

    private String licenseNumber;

    private String companyRegistrationNumber;
    private String companyType;

    private Long promoterId;
    private String bidAmount;
    private String bankGuaranteeAmount;
    private String upfrontAmount;

    private String dzongkhagId;
    private String gewogId;
    private String villageId;

    private String additionalFileId;

    // Changed after final UTA where bid winner and instruction will be updated together.
    private String bgInstruction;

}