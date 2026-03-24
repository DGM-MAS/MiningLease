package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.response.TemporaryClosureNotificationResponse;
import com.mas.gov.bt.mas.primary.entity.TemporaryClosureEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TemporaryClosureMapper {

    @Mapping(target = "currentStatusDisplayName", expression = "java(getStatusDisplayName(temporaryClosureEntity.getCurrentStatus()))")
    TemporaryClosureNotificationResponse toResponse(TemporaryClosureEntity  temporaryClosureEntity);

    // ========== Helper Methods ========== //
    @Named("statusDisplayName")
    default String getStatusDisplayName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "DRAFT" -> "Draft";
            case "SUBMITTED" -> "Submitted";
            case "MPCD_REVIEW" -> "MPCD Review";
            case "GEOLOGIST_REVIEW" -> "Geologist Review";
            case "ME_REVIEW" -> "Mining Engineer Review";
            case "CHIEF_REVIEW" -> "Mining Chief Review";
            case "DIRECTOR_REVIEW" -> "Director Review";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "ADDITIONAL DATA NEEDED" -> "Additional Data Needed";
            case "PAYMENT PENDING" -> "Payment Pending";
            default -> status.replace("_", " ");
        };
    }

    @Mapping(target = "currentStatusDisplayName", expression = "java(getStatusDisplayName(temporaryClosureEntity.getCurrentStatus()))")
    TemporaryClosureNotificationResponse toListResponse(TemporaryClosureEntity temporaryClosureEntity);
}
