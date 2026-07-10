package com.mas.gov.bt.mas.primary.utility;

import com.mas.gov.bt.mas.primary.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public class LookupHelper {

    public <T, ID> T fetchLookup(
            ID id,
            JpaRepository<T, ID> repository,
            String lookupName) {

        if (id == null) {
            throw new BusinessException(
                    ErrorCodes.DATA_INTEGRITY_VIOLATION,
                    lookupName + " id cannot be null."
            );
        }

        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.DATA_INTEGRITY_VIOLATION,
                        lookupName + " not found for id: " + id
                ));
    }

    public <T> T fetchLookup(
            String id,
            JpaRepository<T, Integer> repository,
            String lookupName) {

        if (id == null || id.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.DATA_INTEGRITY_VIOLATION,
                    lookupName + " id cannot be null."
            );
        }

        Integer lookupId;

        try {
            lookupId = Integer.valueOf(id);
        } catch (NumberFormatException ex) {
            throw new BusinessException(
                    ErrorCodes.DATA_INTEGRITY_VIOLATION,
                    "Invalid " + lookupName + " id: " + id
            );
        }

        return fetchLookup(lookupId, repository, lookupName);
    }
}