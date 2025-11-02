package com.immunization.validator.service;

import com.immunization.validator.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for validating patient immunization records against state requirements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private final RequirementsService requirementsService;
    
    /**
     * Validate a single patient's immunization record.
     * 
     * @param patient Patient to validate
     * @param stateCode State code to validate against
     * @param age Age in years (optional if patient has birthDate)
     * @param schoolYear School year identifier (alternative to age)
     * @param detailedResponse Whether to include detailed unmet requirements
     * @return Validation response
     */
    public ValidationResponse validate(Patient patient, String stateCode, Integer age, 
                                       String schoolYear, boolean detailedResponse) {
        
        // Determine patient age if not provided
        Integer patientAge = age;
        if (patientAge == null && patient.getBirthDate() != null) {
            patientAge = calculateAge(patient.getBirthDate());
        }
        
        // Get requirements based on age or school year
        List<ValidationRequirement> requirements;
        if (schoolYear != null && !schoolYear.isEmpty()) {
            requirements = requirementsService.getRequirements(stateCode, schoolYear);
        } else if (patientAge != null) {
            requirements = requirementsService.getRequirements(stateCode, patientAge);
        } else {
            log.error("Cannot determine requirements: no age or school year provided for patient {}", 
                     maskPatientId(patient.getId()));
            return ValidationResponse.builder()
                    .patientId(patient.getId())
                    .valid(false)
                    .unmetRequirements(detailedResponse ? 
                        List.of(UnmetRequirement.builder()
                            .description("Unable to determine validation requirements: age or school year required")
                            .build()) : null)
                    .build();
        }
        
        if (requirements.isEmpty()) {
            log.warn("No requirements found for state {} with age {} or school year {}", 
                    stateCode, patientAge, schoolYear);
            return ValidationResponse.builder()
                    .patientId(patient.getId())
                    .valid(false)
                    .unmetRequirements(detailedResponse ? 
                        List.of(UnmetRequirement.builder()
                            .description("No validation requirements found for specified state and age/school year")
                            .build()) : null)
                    .build();
        }
        
        // Validate against requirements
        List<UnmetRequirement> unmetRequirements = validateRequirements(patient, requirements);
        boolean isValid = unmetRequirements.isEmpty();
        
        return ValidationResponse.builder()
                .patientId(patient.getId())
                .valid(isValid)
                .unmetRequirements(detailedResponse && !isValid ? unmetRequirements : null)
                .build();
    }
    
    /**
     * Validate patient's immunizations against a list of requirements.
     * 
     * @param patient Patient to validate
     * @param requirements List of requirements to check
     * @return List of unmet requirements
     */
    private List<UnmetRequirement> validateRequirements(Patient patient, 
                                                        List<ValidationRequirement> requirements) {
        List<UnmetRequirement> unmetRequirements = new ArrayList<>();
        
        if (patient.getImmunizations() == null || patient.getImmunizations().isEmpty()) {
            // If patient has no immunizations, all requirements are unmet
            for (ValidationRequirement requirement : requirements) {
                unmetRequirements.add(UnmetRequirement.builder()
                        .description(requirement.getDescription() != null ? 
                                   requirement.getDescription() : 
                                   "Missing required vaccine: " + requirement.getVaccineCode())
                        .vaccineCode(requirement.getVaccineCode())
                        .requiredDoses(requirement.getMinDoses())
                        .foundDoses(0)
                        .build());
            }
            return unmetRequirements;
        }
        
        // Group immunizations by vaccine code and count doses
        Map<String, Long> vaccineCounts = patient.getImmunizations().stream()
                .collect(Collectors.groupingBy(
                    Immunization::getVaccineCode,
                    Collectors.counting()
                ));
        
        // Check each requirement
        for (ValidationRequirement requirement : requirements) {
            String vaccineCode = requirement.getVaccineCode();
            Integer requiredDoses = requirement.getMinDoses() != null ? 
                                  requirement.getMinDoses() : 1;
            
            Long foundDoses = vaccineCounts.getOrDefault(vaccineCode, 0L);
            
            if (foundDoses < requiredDoses) {
                unmetRequirements.add(UnmetRequirement.builder()
                        .description(requirement.getDescription() != null ? 
                                   requirement.getDescription() : 
                                   String.format("Insufficient doses of %s: required %d, found %d", 
                                               vaccineCode, requiredDoses, foundDoses))
                        .vaccineCode(vaccineCode)
                        .requiredDoses(requiredDoses)
                        .foundDoses(foundDoses.intValue())
                        .build());
            }
        }
        
        return unmetRequirements;
    }
    
    /**
     * Calculate patient age from birth date.
     * 
     * @param birthDate Birth date in YYYY-MM-DD format
     * @return Age in years, or null if date cannot be parsed
     */
    private Integer calculateAge(String birthDate) {
        try {
            LocalDate birth = LocalDate.parse(birthDate, DATE_FORMATTER);
            LocalDate now = LocalDate.now();
            return Period.between(birth, now).getYears();
        } catch (DateTimeParseException e) {
            log.error("Invalid birth date format: {}", birthDate);
            return null;
        }
    }
    
    /**
     * Mask patient identifier for logging purposes.
     * Shows first 4 and last 4 characters, masks the middle.
     * 
     * @param patientId Original patient ID
     * @return Masked patient ID
     */
    private String maskPatientId(String patientId) {
        if (patientId == null || patientId.length() <= 8) {
            return "****";
        }
        int length = patientId.length();
        String start = patientId.substring(0, Math.min(4, length));
        String end = length > 4 ? patientId.substring(length - 4) : "";
        return start + "****" + end;
    }
}

