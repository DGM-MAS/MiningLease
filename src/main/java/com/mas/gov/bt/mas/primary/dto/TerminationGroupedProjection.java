package com.mas.gov.bt.mas.primary.dto;

import java.time.LocalDateTime;

public interface TerminationGroupedProjection {

    String getTerminationId();

    String[] getApplicationNumbers(); // PostgreSQL ARRAY_AGG

    String getCurrentStatus();

    LocalDateTime getCreatedAt();

    String getApplicantName();     // ✅ NEW

    Long getPromoterUserId();
}