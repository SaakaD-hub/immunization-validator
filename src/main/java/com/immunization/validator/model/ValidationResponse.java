package com.immunization.validator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for validation results.
 * Based on FHIR standard for resource responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResponse {
    
    /**
     * Patient identifier
     */
    @JsonProperty("id")
    private String patientId;
    
    /**
     * Whether the patient's immunization record meets all requirements
     */
    @JsonProperty("valid")
    private Boolean valid;
    
    /**
     * List of unmet requirements (only included in detailed mode)
     */
    @JsonProperty("unmetRequirements")
    private List<UnmetRequirement> unmetRequirements;
}

