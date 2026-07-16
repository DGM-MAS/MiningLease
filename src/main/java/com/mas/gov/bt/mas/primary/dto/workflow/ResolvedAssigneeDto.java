package com.mas.gov.bt.mas.primary.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The user resolved by mas-backend-masters' /api/workflow-assignment/resolve
 * for a given service + application status, per the admin-configured
 * t_workflow_step rules. Same field shape as the old local
 * UserWorkloadProjection so call sites barely changed when migrated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedAssigneeDto {
    private Long userId;
    private String username;
    private String email;
    private Long workload;
    /** True when no eligible candidate was found — masters already recorded this in the escalation queue. userId is null in that case. */
    private boolean escalated;
}
