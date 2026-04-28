package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.SampleTransportClearanceDTO;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SampleTransportClearanceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SampleTransportClearanceMapper {

    SampleTransportClearanceEntity toEntity(
            SampleTransportClearanceDTO dto);

    SampleTransportClearanceResponseDTO toResponseDTO(
            SampleTransportClearanceEntity entity);

    SampleTransportClearanceResponseDTO toListResponse(SampleTransportClearanceEntity sampleTransportClearanceEntity);
}