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
     * Returns requirements for the nearest age requirement that is less than or equal to the patient's age.
     * For example, if patient is 6, returns age 5 requirements. If patient is 9, returns age 7 requirements.
     * 
     * @param stateCode State code (e.g., "CA", "NY")
     * @param age Age in years
     * @return List of validation requirements for the specified state and nearest lower age
     */
    public List<com.immunization.validator.model.ValidationRequirement> getRequirements(String stateCode, Integer age) {
        StateRequirements stateRequirements = requirementsByState.get(stateCode);
        if (stateRequirements == null) {
            log.warn("No requirements found for state: {}", stateCode);
            return List.of();
        }
        
        Map<Integer, List<com.immunization.validator.model.ValidationRequirement>> requirementsByAge = 
                stateRequirements.getRequirementsByAge();
        
        if (requirementsByAge == null || requirementsByAge.isEmpty()) {
            log.warn("No age-based requirements found for state: {}", stateCode);
            return List.of();
        }
        
        // Find the nearest age requirement that is <= the patient's age
        Integer nearestAge = requirementsByAge.keySet().stream()
                .filter(requiredAge -> requiredAge <= age)
                .max(Integer::compareTo)
                .orElse(null);
        
        if (nearestAge == null) {
            log.warn("No requirements found for state {} and age {} (no age requirements <= patient age)", 
                    stateCode, age);
            return List.of();
        }
        
        log.debug("Using age {} requirements for patient age {}", nearestAge, age);
        return requirementsByAge.get(nearestAge);
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
