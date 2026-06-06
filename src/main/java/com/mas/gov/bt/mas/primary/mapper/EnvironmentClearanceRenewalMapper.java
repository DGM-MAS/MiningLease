package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.EnvironmentClearanceRenewalRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.entity.EnvironmentClearanceRenewal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnvironmentClearanceRenewalMapper {

    @Mapping(target = "id", ignore = true)
    EnvironmentClearanceRenewal toEntity(
            EnvironmentClearanceRenewalRequestDTO dto
    );

    EnvironmentClearanceRenewalResponseDTO toResponseDTO(
            EnvironmentClearanceRenewal entity
    );
}