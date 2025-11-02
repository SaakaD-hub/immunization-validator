package com.immunization.validator.service;

import com.immunization.validator.model.StateRequirements;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Service for accessing immunization requirements by state.
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class RequirementsService {
    
    private final Map<String, StateRequirements> requirementsByState;
    
    /**
     * Get requirements for a specific state and age.
     * 
     * @param stateCode State code (e.g., "CA", "NY")
     * @param age Age in years
     * @return List of validation requirements for the specified state and age
     */
    public List<com.immunization.validator.model.ValidationRequirement> getRequirements(String stateCode, Integer age) {
        StateRequirements stateRequirements = requirementsByState.get(stateCode);
        if (stateRequirements == null) {
            log.warn("No requirements found for state: {}", stateCode);
            return List.of();
        }
        
        Map<Integer, List<com.immunization.validator.model.ValidationRequirement>> requirementsByAge = 
                stateRequirements.getRequirementsByAge();
        
        if (requirementsByAge == null || !requirementsByAge.containsKey(age)) {
            log.warn("No requirements found for state {} and age {}", stateCode, age);
            return List.of();
        }
        
        return requirementsByAge.get(age);
    }
    
    /**
     * Get requirements for a specific state and school year.
     * 
     * @param stateCode State code (e.g., "CA", "NY")
     * @param schoolYear School year identifier
     * @return List of validation requirements for the specified state and school year
     */
    public List<com.immunization.validator.model.ValidationRequirement> getRequirements(String stateCode, String schoolYear) {
        StateRequirements stateRequirements = requirementsByState.get(stateCode);
        if (stateRequirements == null) {
            log.warn("No requirements found for state: {}", stateCode);
            return List.of();
        }
        
        Map<String, List<com.immunization.validator.model.ValidationRequirement>> requirementsBySchoolYear = 
                stateRequirements.getRequirementsBySchoolYear();
        
        if (requirementsBySchoolYear == null || !requirementsBySchoolYear.containsKey(schoolYear)) {
            log.warn("No requirements found for state {} and school year {}", stateCode, schoolYear);
            return List.of();
        }
        
        return requirementsBySchoolYear.get(schoolYear);
    }
    
    /**
     * Check if requirements exist for a given state.
     * 
     * @param stateCode State code to check
     * @return true if requirements exist, false otherwise
     */
    public boolean hasRequirements(String stateCode) {
        return requirementsByState.containsKey(stateCode);
    }
}

