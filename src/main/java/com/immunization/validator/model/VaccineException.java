package com.immunization.validator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an exception/exemption for a specific vaccine requirement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineException {
    
    /**
     * Vaccine code for which this exception applies (e.g., "DTaP", "MMR", "Varicella")
     */
    @JsonProperty("vaccineCode")
    private String vaccineCode;
    
    /**
     * Type of exception/exemption.
     * Valid values: MEDICAL_CONTRAINDICATION, LABORATORY_EVIDENCE, RELIABLE_HISTORY, RELIGIOUS_EXEMPTION
     */
    @JsonProperty("exceptionType")
    private String exceptionType;
    
    /**
     * Optional description or documentation reference for the exception
     */
    @JsonProperty("description")
    private String description;
}

