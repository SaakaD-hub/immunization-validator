package com.immunization.validator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an alternate way to satisfy a vaccination requirement.
 * For example, 4 doses of DTaP if the 4th dose is given on or after the 4th birthday.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlternateRequirement {
    
    /**
     * Alternative vaccine code (e.g., "DT" instead of "DTaP")
     * If null, uses the same vaccine code as the main requirement
     */
    private String alternateVaccineCode;
    
    /**
     * Minimum number of doses required for this alternate requirement
     */
    private Integer minDoses;
    
    /**
     * Description of the alternate requirement
     */
    private String description;
    
    /**
     * Conditions that must be met for this alternate requirement to apply.
     * Examples: "4th dose on or after 4th birthday", "3rd dose on or after 4th birthday"
     */
    private String condition;
}

