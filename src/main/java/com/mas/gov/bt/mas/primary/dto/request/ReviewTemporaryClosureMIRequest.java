package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReviewTemporaryClosureMIRequest {

    private Long id;
    private String status;
    private String remarks;
    private Long fileId;
}
