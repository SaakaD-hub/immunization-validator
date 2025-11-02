package com.immunization.validator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Patient model based on FHIR standard.
 * Contains only the information necessary for immunization validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    
    /**
     * Patient identifier (non-PII identifier used for tracking)
     */
    @NotBlank(message = "Patient identifier is required")
    @JsonProperty("id")
    private String id;
    
    /**
     * Patient's date of birth (used to determine age for requirement validation)
     * Format: YYYY-MM-DD
     */
    @JsonProperty("birthDate")
    private String birthDate;
    
    /**
     * List of immunizations received by the patient
     */
    @JsonProperty("immunization")
    private List<Immunization> immunizations;
}

