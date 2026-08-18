package com.mas.gov.bt.mas.primary.dto.response;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleTransportClearanceResponseDTO {

    private Long id;
    private String siteName;

    private String siteApplicationNo;

    private String applicationNo;
    private String applicantName;
    private String contactNo;
    private String emailAddress;
    private String applicantScope;

    private List<SampleMineralResponseDTO> minerals;

    private String shippingPurpose;
    private String shippingMode;
    private String destination;
    private String destinationCountry;
    private String samplePhotoFileId;
    private String othersFileId;

    private String fileIdGSDFocal;

    private Long createdBy;

    private LocalDateTime createdOn;

    private Long updatedBy;

    private LocalDateTime updatedOn;

    private String status;

    private String dzongkhagName;
    private String gewogName;
    private String villageName;
    private String regionName;

    private String sampleTransportClearanceCertificateFileId;

    private String assignedGSDChiefRemarks;
    private String assignedGSDFocalRemarks;
}