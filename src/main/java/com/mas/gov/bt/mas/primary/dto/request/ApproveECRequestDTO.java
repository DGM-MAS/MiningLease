package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

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

    // Optional — the DECC-issued EC reference, only known once DECC has actually
    // issued/renewed the clearance (not always available at the moment MD approves,
    // e.g. when forwarding a lease renewal to DECC for later processing).
    private String ecNumber;

    private String ecFileId;

    private LocalDate ecExpiryDate;

}