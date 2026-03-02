package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReassignTaskRequest {

    @NotNull(message = "New assignee user ID is required")
    private Long newAssigneeUserId;

    @Size(max = 2000, message = "Remarks must not exceed 2000 characters")
    private String remarks;

    private String applicationNumber;
}
