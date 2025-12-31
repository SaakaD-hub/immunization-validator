package com.immunization.validator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single immunization requirement that must be met.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequirement {
    
    /**
     * Vaccine code required (e.g., "DTaP", "MMR")
     */
    private String vaccineCode;
    
    /**
     * Minimum number of doses required
     */
    private Integer minDoses;
    
    /**
     * Maximum age for which this requirement applies (in years)
     * Null means no maximum age limit
     */
    private Integer maxAge;
    
    /**
     * Minimum age for which this requirement applies (in years)
     * Null means no minimum age requirement
     */
    private Integer minAge;
    
    /**
     * Description of the requirement for reporting purposes
     */
    private String description;
    
    /**
     * Alternate requirements that can satisfy this requirement.
     * For example, 4 doses of DTaP if 4th dose is given on or after 4th birthday.
     */
    private List<AlternateRequirement> alternateRequirements;
    
    /**
     * List of exception types that can satisfy this requirement.
     * For example, MEDICAL_CONTRAINDICATION, LABORATORY_EVIDENCE, RELIABLE_HISTORY
     */
    private List<String> acceptedExceptions;
}

