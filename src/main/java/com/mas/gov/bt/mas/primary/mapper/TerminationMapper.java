package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.response.TemporaryClosureNotificationResponse;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.entity.TemporaryClosureEntity;
import com.mas.gov.bt.mas.primary.entity.TerminationApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for termination entities and DTOs.
 */

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TerminationMapper {

    TerminationApplicationResponse toResponse(TerminationApplicationEntity terminationApplicationEntity);

    TerminationApplicationResponse toListResponse(TerminationApplicationEntity terminationApplicationEntity);

}
