package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.entity.ManualMiningEntryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ManualMiningEntryMapper {

    // DTO → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationNo", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ManualMiningEntryEntity toEntity(ManualMiningEntryRequestDTO dto);

    // Entity → Response DTO
    ManualMiningEntryResponseDTO toResponse(ManualMiningEntryEntity entity);
}