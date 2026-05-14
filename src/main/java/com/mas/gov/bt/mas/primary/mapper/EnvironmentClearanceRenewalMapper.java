package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.EnvironmentClearanceRenewalRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.entity.EnvironmentClearanceRenewal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnvironmentClearanceRenewalMapper {

    EnvironmentClearanceRenewal toEntity(
            EnvironmentClearanceRenewalRequestDTO dto
    );

    EnvironmentClearanceRenewalResponseDTO toResponseDTO(
            EnvironmentClearanceRenewal entity
    );
}