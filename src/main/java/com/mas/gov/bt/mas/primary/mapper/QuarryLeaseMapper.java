package com.mas.gov.bt.mas.primary.mapper;

import com.mas.gov.bt.mas.primary.dto.QuarryLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.QuarryLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.entity.QuarryLeaseApplication;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Quarry Lease entities and DTOs.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface QuarryLeaseMapper {

    @Mapping(target = "dzongkhag", ignore = true)
    @Mapping(target = "gewog", ignore = true)
    @Mapping(target = "nearestVillage", ignore = true)
    QuarryLeaseApplication toEntity(QuarryLeaseApplicationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "dzongkhag", ignore = true)
    @Mapping(target = "gewog", ignore = true)
    @Mapping(target = "nearestVillage", ignore = true)
    void updateEntityFromRequest(
            QuarryLeaseApplicationRequest request,
            @MappingTarget QuarryLeaseApplication entity);

    // ========== Application Mappings ==========

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "applicationNumber", ignore = true)
//    @Mapping(target = "applicationMaster", ignore = true)
//    @Mapping(target = "applicantUserId", ignore = true)
//    @Mapping(target = "currentStatus", ignore = true)
//    @Mapping(target = "submittedAt", ignore = true)
//    @Mapping(target = "approvedAt", ignore = true)
//    @Mapping(target = "rejectedAt", ignore = true)
//    @Mapping(target = "rejectionReason", ignore = true)
//    @Mapping(target = "leaseStartDate", ignore = true)
//    @Mapping(target = "leaseEndDate", ignore = true)
//    @Mapping(target = "leasePeriodYears", ignore = true)
//    @Mapping(target = "isActive", ignore = true)
//    @Mapping(target = "createdBy", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedBy", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
//    @Mapping(target = "remarksMPCD", ignore = true)
//    @Mapping(target = "mpcdFileUploadIdPA", ignore = true)
//    @Mapping(target = "remarksGeologist", ignore = true)
//    @Mapping(target = "geologicalReportStatus", ignore = true)
//    @Mapping(target = "remarksME", ignore = true)
//    @Mapping(target = "fmfsStatus", ignore = true)
//    @Mapping(target = "approvedArea", ignore = true)
//    @Mapping(target = "approvedErb", ignore = true)
//    @Mapping(target = "approvedLeasePeriod", ignore = true)
//    @Mapping(target = "approvedMineral", ignore = true)
//    @Mapping(target = "llcDocId", ignore = true)
//    @Mapping(target = "notesheetDocId", ignore = true)
//    @Mapping(target = "mlaDocId", ignore = true)
//    @Mapping(target = "mlaStatus", ignore = true)
//    @Mapping(target = "mlaSignedAt", ignore = true)
//    @Mapping(target = "remarksChief", ignore = true)
//    @Mapping(target = "remarksDirector", ignore = true)
//    @Mapping(target = "mlaSignedDocId", ignore = true)
//    @Mapping(target = "mpcdReviewedAt", ignore = true)
//    @Mapping(target = "geologistReviewedAt", ignore = true)
//    @Mapping(target = "meReviewedAt", ignore = true)
//    @Mapping(target = "chiefReviewedAt", ignore = true)
//    @Mapping(target = "directorReviewedAt", ignore = true)
//    QuarryLeaseApplication toEntity(QuarryLeaseApplicationRequest request);

    @Mapping(target = "currentStatusDisplayName", expression = "java(getStatusDisplayName(entity.getCurrentStatus()))")
    @Mapping(target = "dzongkhag", source = "dzongkhag.dzongkhagName")
    @Mapping(target = "dzongkhagId", source = "dzongkhag.id")
    @Mapping(target = "gewog", source = "gewog.gewogName")
    @Mapping(target = "gewogId", source = "gewog.gewogSerialNo")
    @Mapping(target = "nearestVillage", source = "nearestVillage.villageName")
    @Mapping(target = "nearestVillageId", source = "nearestVillage.villageSerialNo")
    QuarryLeaseResponse toResponse(QuarryLeaseApplication entity);

    List<QuarryLeaseResponse> toResponseList(List<QuarryLeaseApplication> entities);

    @Mapping(target = "currentStatusDisplayName", expression = "java(getStatusDisplayName(entity.getCurrentStatus()))")
    @Mapping(target = "gewog", ignore = true)
    @Mapping(target = "nearestVillage", ignore = true)
    ApplicationListResponse toListResponse(QuarryLeaseApplication entity);

    List<ApplicationListResponse> toListResponseList(List<QuarryLeaseApplication> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationNumber", ignore = true)
    @Mapping(target = "applicationMaster", ignore = true)
    @Mapping(target = "applicantUserId", ignore = true)
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "leaseStartDate", ignore = true)
    @Mapping(target = "leaseEndDate", ignore = true)
    @Mapping(target = "leasePeriodYears", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "remarksMPCD", ignore = true)
    @Mapping(target = "mpcdFileUploadIdPA", ignore = true)
    @Mapping(target = "remarksGeologist", ignore = true)
    @Mapping(target = "geologicalReportStatus", ignore = true)
    @Mapping(target = "remarksME", ignore = true)
    @Mapping(target = "fmfsStatus", ignore = true)
    @Mapping(target = "approvedArea", ignore = true)
    @Mapping(target = "approvedErb", ignore = true)
    @Mapping(target = "approvedLeasePeriod", ignore = true)
    @Mapping(target = "approvedMineral", ignore = true)
    @Mapping(target = "llcDocId", ignore = true)
    @Mapping(target = "notesheetDocId", ignore = true)
    @Mapping(target = "mlaDocId", ignore = true)
    @Mapping(target = "mlaStatus", ignore = true)
    @Mapping(target = "mlaSignedAt", ignore = true)
    @Mapping(target = "remarksChief", ignore = true)
    @Mapping(target = "remarksDirector", ignore = true)
    @Mapping(target = "mlaSignedDocId", ignore = true)
    @Mapping(target = "mpcdReviewedAt", ignore = true)
    @Mapping(target = "geologistReviewedAt", ignore = true)
    @Mapping(target = "meReviewedAt", ignore = true)
    @Mapping(target = "chiefReviewedAt", ignore = true)
    @Mapping(target = "directorReviewedAt", ignore = true)
    @Mapping(target = "dzongkhag", ignore = true)
    @Mapping(target = "gewog", ignore = true)
    @Mapping(target = "nearestVillage", ignore = true)
    void updateEntity(QuarryLeaseApplicationRequest request, @MappingTarget QuarryLeaseApplication entity);

    // ========== Helper Methods ==========

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
}
