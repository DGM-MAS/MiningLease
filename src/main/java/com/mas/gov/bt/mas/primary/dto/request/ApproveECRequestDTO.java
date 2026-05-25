package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveECRequestDTO {

    @NotNull
    private Long renewalId;

    @NotNull
    private Long ecCertificateFileId;

    private Boolean forwardToDECC;

}