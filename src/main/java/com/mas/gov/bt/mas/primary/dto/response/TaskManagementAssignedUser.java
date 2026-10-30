package com.mas.gov.bt.mas.primary.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskManagementAssignedUser {
    private String applicationNumber;
    private Long assignedToUserId;
    private String assignedToRole;

}
