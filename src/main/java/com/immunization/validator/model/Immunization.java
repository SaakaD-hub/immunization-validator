package com.immunization.validator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immunization model based on FHIR standard.
 * Represents a single immunization record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Immunization {
    
    /**
     * Vaccine code (e.g., "DTaP", "MMR", "HepB")
     * Uses standard vaccine codes where applicable
     */
    @NotBlank(message = "Vaccine code is required")
    @JsonProperty("vaccineCode")
    private String vaccineCode;
    
    /**
     * Date when the immunization was administered
     * Format: YYYY-MM-DD
     */
    @NotBlank(message = "Administration date is required")
    @JsonProperty("occurrenceDateTime")
    private String occurrenceDateTime;
    
    /**
     * Number of doses (for vaccines requiring multiple doses)
     * Optional, defaults to 1 if not specified
     */
    @JsonProperty("doseNumber")
    private Integer doseNumber;
}

