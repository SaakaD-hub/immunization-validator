package com.immunization.validator.config;

import com.immunization.validator.model.StateRequirements;
import com.immunization.validator.model.ValidationRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class for loading immunization requirements by state.
 * Requirements are loaded from application properties or external configuration files.
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "immunization.requirements")
public class RequirementsConfiguration {
    
    /**
     * Map of state codes to their requirements configuration
     */
    private Map<String, Map<String, List<RequirementConfig>>> states = new HashMap<>();
    
    /**
     * Creates a service bean for accessing requirements.
     * This maps the configuration structure to our StateRequirements model.
     */
    @Bean
    public RequirementsService requirementsService() {
        Map<String, StateRequirements> requirementsMap = new HashMap<>();
        
        for (Map.Entry<String, Map<String, List<RequirementConfig>>> stateEntry : states.entrySet()) {
            String stateCode = stateEntry.getKey();
            Map<String, List<RequirementConfig>> stateConfig = stateEntry.getValue();
            
            StateRequirements stateRequirements = StateRequirements.builder()
                    .stateCode(stateCode)
                    .requirementsByAge(new HashMap<>())
                    .requirementsBySchoolYear(new HashMap<>())
                    .build();
            
            // Process age-based requirements
            if (stateConfig.containsKey("age")) {
                Map<Integer, List<ValidationRequirement>> requirementsByAge = new HashMap<>();
                for (RequirementConfig config : stateConfig.get("age")) {
                    int age = config.getAge();
                    requirementsByAge.putIfAbsent(age, new ArrayList<>());
                    requirementsByAge.get(age).add(mapToValidationRequirement(config));
                }
                stateRequirements.setRequirementsByAge(requirementsByAge);
            }
            
            // Process school year-based requirements
            if (stateConfig.containsKey("schoolYear")) {
                Map<String, List<ValidationRequirement>> requirementsBySchoolYear = new HashMap<>();
                for (RequirementConfig config : stateConfig.get("schoolYear")) {
                    String schoolYear = config.getSchoolYear();
                    requirementsBySchoolYear.putIfAbsent(schoolYear, new ArrayList<>());
                    requirementsBySchoolYear.get(schoolYear).add(mapToValidationRequirement(config));
                }
                stateRequirements.setRequirementsBySchoolYear(requirementsBySchoolYear);
            }
            
            requirementsMap.put(stateCode, stateRequirements);
        }
        
        log.info("Loaded immunization requirements for {} states", requirementsMap.size());
        return new RequirementsService(requirementsMap);
    }
    
    private ValidationRequirement mapToValidationRequirement(RequirementConfig config) {
        return ValidationRequirement.builder()
                .vaccineCode(config.getVaccineCode())
                .minDoses(config.getMinDoses())
                .minAge(config.getMinAge())
                .maxAge(config.getMaxAge())
                .description(config.getDescription())
                .build();
    }
    
    public Map<String, Map<String, List<RequirementConfig>>> getStates() {
        return states;
    }
    
    public void setStates(Map<String, Map<String, List<RequirementConfig>>> states) {
        this.states = states;
    }
    
    /**
     * Internal configuration class for loading from properties
     */
    public static class RequirementConfig {
        private String vaccineCode;
        private Integer minDoses;
        private Integer minAge;
        private Integer maxAge;
        private String description;
        private Integer age;
        private String schoolYear;
        
        // Getters and setters
        public String getVaccineCode() { return vaccineCode; }
        public void setVaccineCode(String vaccineCode) { this.vaccineCode = vaccineCode; }
        
        public Integer getMinDoses() { return minDoses; }
        public void setMinDoses(Integer minDoses) { this.minDoses = minDoses; }
        
        public Integer getMinAge() { return minAge; }
        public void setMinAge(Integer minAge) { this.minAge = minAge; }
        
        public Integer getMaxAge() { return maxAge; }
        public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public String getSchoolYear() { return schoolYear; }
        public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }
    }
}

