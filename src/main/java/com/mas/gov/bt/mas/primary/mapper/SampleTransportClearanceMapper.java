package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.SampleTransportClearanceDTO;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SampleTransportClearanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SampleTransportClearanceMapper {

    SampleTransportClearanceEntity toEntity(
            SampleTransportClearanceDTO dto);

    @Mapping(target = "dzongkhagName",
            source = "dzongkhagId.dzongkhagName")

    @Mapping(target = "gewogName",
            source = "gewogId.gewogName")

    @Mapping(target = "villageName",
            source = "villageId.villageName")

    @Mapping(target = "regionName",
            source = "regionMaster.regionName")

    SampleTransportClearanceResponseDTO toResponseDTO(
            SampleTransportClearanceEntity entity);

    SampleTransportClearanceResponseDTO toListResponse(SampleTransportClearanceEntity sampleTransportClearanceEntity);
}