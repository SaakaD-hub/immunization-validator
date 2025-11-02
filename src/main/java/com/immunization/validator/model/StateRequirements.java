package com.immunization.validator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration for immunization requirements by state and age/school year.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateRequirements {
    
    /**
     * State code (e.g., "CA", "NY", "TX")
     */
    private String stateCode;
    
    /**
     * Map of age groups (in years) to their required immunizations
     * Key: age in years, Value: list of requirements for that age
     */
    private Map<Integer, List<ValidationRequirement>> requirementsByAge;
    
    /**
     * Alternative: Map of school year/grades to their required immunizations
     * Key: school year/grade identifier, Value: list of requirements
     */
    private Map<String, List<ValidationRequirement>> requirementsBySchoolYear;
}

