package com.immunization.validator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for batch validation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchValidationResponse {
    
    /**
     * List of validation responses for each patient
     */
    @JsonProperty("results")
    private List<ValidationResponse> results;
}

