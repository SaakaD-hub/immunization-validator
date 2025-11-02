package com.immunization.validator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a requirement that was not met during validation.
 * Used in detailed response mode.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnmetRequirement {
    
    /**
     * Description of the unmet requirement
     */
    private String description;
    
    /**
     * Vaccine code that was required but not met
     */
    private String vaccineCode;
    
    /**
     * Number of doses required
     */
    private Integer requiredDoses;
    
    /**
     * Number of doses found in patient record
     */
    private Integer foundDoses;
}

