package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import org.mapstruct.*;

/**
 * MapStruct mapper for Quarry Lease entities and DTOs.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MiningLeaseMapper {


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "dzongkhag", ignore = true)
    @Mapping(target = "gewog", ignore = true)
    @Mapping(target = "nearestVillage", ignore = true)
    void updateEntityFromRequest(
            MiningLeaseApplicationRequest request,
            @MappingTarget MiningLeaseApplication entity);

    @Mapping(target = "currentStatusDisplayName", expression = "java(getStatusDisplayName(application.getCurrentStatus()))")
    @Mapping(target = "dzongkhag", source = "dzongkhag.dzongkhagName")
    @Mapping(target = "dzongkhagId", source = "dzongkhag.id")
    @Mapping(target = "gewog", source = "gewog.gewogName")
    @Mapping(target = "gewogId", source = "gewog.gewogId")
    @Mapping(target = "nearestVillage", source = "nearestVillage.villageName")
    @Mapping(target = "nearestVillageId", source = "nearestVillage.villageSerialNo")
    MiningLeaseResponse toResponse(MiningLeaseApplication application);

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

    @Mapping(target = "currentStatusDisplayName", expression = "java(getStatusDisplayName(miningLeaseApplication.getCurrentStatus()))")
    @Mapping(target = "gewog", ignore = true)
    @Mapping(target = "nearestVillage", ignore = true)
    ApplicationListResponse toListResponse(MiningLeaseApplication miningLeaseApplication);
}
