package com.mas.gov.bt.mas.primary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Result of a pre-flight application-cap check, surfaced before the user opens the form. */
@Getter
@Setter
@AllArgsConstructor
public class CapCheckResponse {
    private boolean allowed;
    private int currentCount;
    private int maxAllowed;
    private String message;
}
