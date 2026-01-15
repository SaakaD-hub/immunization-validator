package com.immunization.validator.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationExplanation {
    private String vaccineCode;           // e.g., "DTaP"
    private String status;                // COMPLIANT / NON_COMPLIANT / EXEMPT
    private String regulation;            // "105 CMR 220.000"
    private String rule;                  // e.g. "4th dose on or after 4th birthday"
    private String expected;              // human readable
    private String actual;                // human readable
    private String reason;                // why failed/passed
    private List<String> evidence;        // facts used to decide
}
