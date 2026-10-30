package com.mas.gov.bt.mas.primary.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for mas-backend-masters' /api/workflow-assignment/resolve.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveAssigneeApiRequest {
    private String serviceCode;
    private String triggerStatus;
    private Long regionId;
    /** Required so masters can write the escalation task row when no candidate is found. */
    private String applicationNumber;
}
