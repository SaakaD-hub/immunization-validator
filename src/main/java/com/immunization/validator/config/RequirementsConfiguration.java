package com.immunization.validator.config;

import com.immunization.validator.model.AlternateRequirement;
import com.immunization.validator.model.StateRequirements;
import com.immunization.validator.model.ValidationRequirement;
import com.immunization.validator.service.RequirementsService;
import lombok.Data;
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
@Data
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
        List<AlternateRequirement> alternateRequirements = null;
        if (config.getAlternateRequirements() != null && !config.getAlternateRequirements().isEmpty()) {
            alternateRequirements = config.getAlternateRequirements().stream()
                    .map(alt -> AlternateRequirement.builder()
                            .alternateVaccineCode(alt.getAlternateVaccineCode())
                            .minDoses(alt.getMinDoses())
                            .description(alt.getDescription())
                            .condition(alt.getCondition())
                            .dateConditions(alt.getDateConditions())         // ✅ FIXED: Added
                            .intervalConditions(alt.getIntervalConditions()) // ✅ FIXED: Added
                            .notes(alt.getNotes())                           // ✅ FIXED: Added
                            .build())
                    .collect(Collectors.toList());
        }

        return ValidationRequirement.builder()
                .vaccineCode(config.getVaccineCode())
                .minDoses(config.getMinDoses())
                // ❌ REMOVED: .minAge(config.getMinAge())      - Field doesn't exist
                // ❌ REMOVED: .maxAge(config.getMaxAge())      - Field doesn't exist
                .description(config.getDescription())
                .dateConditions(config.getDateConditions())           // ✅ FIXED: Added
                .intervalConditions(config.getIntervalConditions())   // ✅ FIXED: Added
                .alternateRequirements(alternateRequirements)
                .acceptedExceptions(config.getAcceptedExceptions())
                .notes(config.getNotes())                             // ✅ FIXED: Added
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
        // ❌ REMOVED: private Integer minAge;       - Not used in ValidationRequirement
        // ❌ REMOVED: private Integer maxAge;       - Not used in ValidationRequirement
        private String description;
        private Integer age;
        private String schoolYear;
        private List<String> dateConditions;           // ✅ FIXED: Added
        private List<String> intervalConditions;       // ✅ FIXED: Added
        private List<AlternateRequirementConfig> alternateRequirements;
        private List<String> acceptedExceptions;
        private String notes;                          // ✅ FIXED: Added

        // Getters and setters
        public String getVaccineCode() { return vaccineCode; }
        public void setVaccineCode(String vaccineCode) { this.vaccineCode = vaccineCode; }

        public Integer getMinDoses() { return minDoses; }
        public void setMinDoses(Integer minDoses) { this.minDoses = minDoses; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getSchoolYear() { return schoolYear; }
        public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }

        public List<String> getDateConditions() { return dateConditions; }
        public void setDateConditions(List<String> dateConditions) {
            this.dateConditions = dateConditions;
        }

        public List<String> getIntervalConditions() { return intervalConditions; }
        public void setIntervalConditions(List<String> intervalConditions) {
            this.intervalConditions = intervalConditions;
        }

        public List<AlternateRequirementConfig> getAlternateRequirements() {
            return alternateRequirements;
        }
        public void setAlternateRequirements(List<AlternateRequirementConfig> alternateRequirements) {
            this.alternateRequirements = alternateRequirements;
        }

        public List<String> getAcceptedExceptions() { return acceptedExceptions; }
        public void setAcceptedExceptions(List<String> acceptedExceptions) {
            this.acceptedExceptions = acceptedExceptions;
        }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    /**
     * Internal configuration class for alternate requirements
     */
    public static class AlternateRequirementConfig {
        private String alternateVaccineCode;
        private Integer minDoses;
        private String description;
        private String condition;
        private List<String> dateConditions;           // ✅ FIXED: Added
        private List<String> intervalConditions;       // ✅ FIXED: Added
        private String notes;                          // ✅ FIXED: Added

        public String getAlternateVaccineCode() { return alternateVaccineCode; }
        public void setAlternateVaccineCode(String alternateVaccineCode) {
            this.alternateVaccineCode = alternateVaccineCode;
        }

        public Integer getMinDoses() { return minDoses; }
        public void setMinDoses(Integer minDoses) { this.minDoses = minDoses; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public List<String> getDateConditions() { return dateConditions; }
        public void setDateConditions(List<String> dateConditions) {
            this.dateConditions = dateConditions;
        }

        public List<String> getIntervalConditions() { return intervalConditions; }
        public void setIntervalConditions(List<String> intervalConditions) {
            this.intervalConditions = intervalConditions;
        }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}