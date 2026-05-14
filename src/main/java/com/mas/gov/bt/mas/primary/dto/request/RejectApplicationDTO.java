package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RejectApplicationDTO {

    @NotNull
    private Long renewalId;

    @NotBlank
    private String rejectionRemarks;
}