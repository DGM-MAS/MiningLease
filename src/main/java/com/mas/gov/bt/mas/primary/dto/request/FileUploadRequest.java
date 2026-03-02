package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileUploadRequest {
    private String applicationNumber;
    private String fileType;
    private String fileId;
}
