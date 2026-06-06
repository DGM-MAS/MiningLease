package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ManualEntryValidator {

    public void validate(ManualMiningEntryRequestDTO request) {
        List<String> missing = new ArrayList<>();

        String type = request.getActivityType().toUpperCase();

        switch (type) {
            case "MINING_LEASE"       -> validateMiningOrQuarry(request, missing);
            case "QUARRY_LEASE"       -> validateMiningOrQuarry(request, missing);
            case "SURFACE_COLLECTION" -> validateSurfaceCollectionOrStockLifting(request, missing);
            case "STOCK_LIFTING"      -> validateSurfaceCollectionOrStockLifting(request, missing);
            default -> missing.add("activityType: must be MINING_LEASE, QUARRY_LEASE, SURFACE_COLLECTION or STOCK_LIFTING");
        }

        if (!missing.isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "Missing required fields: " + String.join(", ", missing)
            );
        }
    }

    // -------------------------------------------------------
    // Mining Lease & Quarry Lease
    // -------------------------------------------------------
    private void validateMiningOrQuarry(ManualMiningEntryRequestDTO r, List<String> missing) {
        requireText(r.getApplicantType(),     "applicantType",     missing);
        requireText(r.getApplicantName(),     "applicantName",     missing);
        requireText(r.getApplicantCid(),      "applicantCid",      missing);
        requireText(r.getApplicantContact(),  "applicantContact",  missing);
        requireText(r.getApplicantEmail(),    "applicantEmail",    missing);
        requireText(r.getDzongkhag(),         "dzongkhag",         missing);
        requireText(r.getGewog(),             "gewog",             missing);
        requireText(r.getPlaceOfActivity(),   "placeOfActivity",   missing);
        requireText(r.getTypeOfMines(),       "typeOfMines",       missing);
        requireText(r.getTypeOfMinerals(),    "typeOfMinerals",    missing);
        requireText(r.getProposedLeasePeriod(),"proposedLeasePeriod", missing);
        requireText(r.getApprovedArea(),      "approvedArea",      missing);
        requireText(r.getApprovedMineral(),   "approvedMineral",   missing);
        requireText(r.getApprovedLeasePeriod(),"approvedLeasePeriod", missing);

        if (r.getLeaseStartDate() == null) missing.add("leaseStartDate");
        if (r.getLeaseEndDate()   == null) missing.add("leaseEndDate");
    }

    // -------------------------------------------------------
    // Surface Collection & Stock Lifting
    // -------------------------------------------------------
    private void validateSurfaceCollectionOrStockLifting(ManualMiningEntryRequestDTO r, List<String> missing) {
        requireText(r.getApplicantName(),             "applicantName",             missing);
        requireText(r.getApplicantCid(),              "applicantCid",              missing);
        requireText(r.getApplicantContact(),          "applicantContact",          missing);
        requireText(r.getDzongkhag(),                 "dzongkhag",                 missing);
        requireText(r.getGewog(),                     "gewog",                     missing);
        requireText(r.getNearestVillage(),            "nearestVillage",            missing);
        requireText(r.getTypeOfActivity(),            "typeOfActivity",            missing);
        requireText(r.getTypeOfMaterials(),           "typeOfMaterials",           missing);
        requireText(r.getCollectionSite(),            "collectionSite",            missing);
        requireText(r.getPermitNo(),                  "permitNo",                  missing);

        boolean hasArea = (r.getProposedAreaSrf() != null && r.getProposedAreaSrf() > 0)
                || (r.getProposedAreaStateLand() != null && r.getProposedAreaStateLand() > 0)
                || (r.getProposedAreaPrivate() != null && r.getProposedAreaPrivate() > 0)
                || (r.getProposedAreaRow() != null && r.getProposedAreaRow() > 0);

        if (!hasArea) {
            missing.add("proposedArea: at least one of proposedAreaSrf, proposedAreaStateLand, proposedAreaPrivate, proposedAreaRow is required");
        }
    }

    private void requireText(String value, String fieldName, List<String> missing) {
        if (value == null || value.isBlank()) {
            missing.add(fieldName);
        }
    }
}
