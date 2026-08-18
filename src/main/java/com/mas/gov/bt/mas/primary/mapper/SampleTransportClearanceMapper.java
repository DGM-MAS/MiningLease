package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.SampleMineralDTO;
import com.mas.gov.bt.mas.primary.dto.request.SampleTransportClearanceDTO;
import com.mas.gov.bt.mas.primary.dto.response.SampleMineralResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SampleTransportClearanceEntity;
import com.mas.gov.bt.mas.primary.entity.SampleTransportClearanceMineralEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SampleTransportClearanceMapper {

    // =========================================================
    // REQUEST -> ENTITY
    // =========================================================
    SampleTransportClearanceEntity toEntity(
            SampleTransportClearanceDTO dto);

    SampleTransportClearanceMineralEntity toMineralEntity(
            SampleMineralDTO dto);

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    SampleMineralResponseDTO toMineralResponse(
            SampleTransportClearanceMineralEntity entity);

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

    @Mapping(target = "dzongkhagName",
            source = "dzongkhagId.dzongkhagName")

    @Mapping(target = "gewogName",
            source = "gewogId.gewogName")

    @Mapping(target = "villageName",
            source = "villageId.villageName")

    @Mapping(target = "regionName",
            source = "regionMaster.regionName")
    SampleTransportClearanceResponseDTO toListResponse(SampleTransportClearanceEntity sampleTransportClearanceEntity);
}