package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAttachmentResponseDTO {

    private Long id;

    private String attachmentType;

    private String uploadFileId;
}