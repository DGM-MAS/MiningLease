package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
public class TerminationApplicationRequest {

    private Long promoterUserId;
    private Long fileId;
    private boolean permanentTermination;
    private LocalDate terminationEndDate;
    private String remarksChief;
    private List<String> applicationNumber;

}
