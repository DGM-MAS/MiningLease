package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ManualEntryValidator {

    public void validate(ManualMiningEntryRequestDTO request) {
        // all fields are optional for manual entry across all activity types
    }
}
