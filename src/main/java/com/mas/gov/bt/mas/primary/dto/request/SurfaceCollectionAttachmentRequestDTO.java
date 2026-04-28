package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurfaceCollectionAttachmentRequestDTO {

    private String fileName;
    private String filePath;
    private String attachmentType;
}