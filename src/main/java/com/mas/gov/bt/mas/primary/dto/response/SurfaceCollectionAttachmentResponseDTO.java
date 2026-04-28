package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurfaceCollectionAttachmentResponseDTO {

    private Long id;
    private String fileName;
    private String filePath;
    private String attachmentType;
}