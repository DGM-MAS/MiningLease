package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Setter
@Getter
public class MiningLeaseNoteSheetRequest {
    private String applicationNo;
    private String noteSheetDocId;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
}
