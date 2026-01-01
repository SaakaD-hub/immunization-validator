package com.immunization.validator.service;

import com.immunization.validator.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * Service for validating patient immunization records against state requirements.
 *
 * This version includes date-based condition validation for alternate requirements.
 *
 * @author Saakad
 * @since 2026-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RequirementsService requirementsService;
    private final DateConditionEvaluator dateConditionEvaluator;

    /**
     * Validate a patient's immunization record against state requirements.
     *
     * @param patient Patient with immunization records
     * @param stateCode State code (e.g., "CA")
     * @param age Patient's age in years (optional if birthDate provided)
     * @param schoolYear School year (e.g., "K", "1st") - optional, alternative to age
     * @param includeDetails Whether to include unmet requirements in response
     * @return ValidationResponse with validation result
     */
    public ValidationResponse validate(Patient patient, String stateCode, Integer age,
                                       String schoolYear, boolean includeDetails) {

        String maskedId = maskPatientId(patient.getId());
        log.info("Validating patient {} against {} requirements", maskedId, stateCode);

        // Calculate age from birthDate if not provided
        Integer effectiveAge = age;
        if (effectiveAge == null && patient.getBirthDate() != null) {
            effectiveAge = calculateAge(patient.getBirthDate());
        }

        // Get requirements for the state and age/school year
        List<ValidationRequirement> requirements;
        if (schoolYear != null) {
            requirements = requirementsService.getRequirements(stateCode, schoolYear);
        } else {
            requirements = requirementsService.getRequirements(stateCode, effectiveAge);
        }

        if (requirements == null || requirements.isEmpty()) {
            log.warn("No requirements found for state: {}, age: {}, schoolYear: {}",
                    stateCode, effectiveAge, schoolYear);
            return ValidationResponse.builder()
                    .patientId(patient.getId())
                    .valid(false)
                    .unmetRequirements(includeDetails ? List.of() : null)
                    .build();
        }

        // Validate requirements
        List<UnmetRequirement> unmetRequirements = validateRequirements(patient, requirements);

        boolean isValid = unmetRequirements.isEmpty();

        log.info("Validation complete for patient {}: {}", maskedId, isValid ? "VALID" : "INVALID");

        return ValidationResponse.builder()
                .patientId(patient.getId())
                .valid(isValid)
                .unmetRequirements(includeDetails ? unmetRequirements : null)
                .build();
    }

    /**
     * Validate patient's immunizations against a list of requirements.
     *
     * This method now properly evaluates date-based conditions for alternate requirements.
     *
     * @param patient Patient with immunization records
     * @param requirements List of validation requirements
     * @return List of unmet requirements (empty if all satisfied)
     */

    private List<UnmetRequirement> validateRequirements(Patient patient,
                                                        List<ValidationRequirement> requirements) {
        List<UnmetRequirement> unmetRequirements = new ArrayList<>();

        // ✅ FIX: Initialize empty list instead of early return
        // This ensures exception checking still happens even with no immunizations
        List<Immunization> patientImmunizations = patient.getImmunizations();
        if (patientImmunizations == null) {
            patientImmunizations = List.of();
        }

        // Group immunizations by vaccine code and count doses
        Map<String, Long> vaccineCounts = patientImmunizations.stream()
                .collect(Collectors.groupingBy(
                        Immunization::getVaccineCode,
                        Collectors.counting()
                ));

        // Also group full immunization objects for date-based condition checking
        Map<String, List<Immunization>> immunizationsByVaccine = patientImmunizations.stream()
                .collect(Collectors.groupingBy(Immunization::getVaccineCode));

        // Build map of exceptions by vaccine code
        Map<String, String> exceptionsByVaccine = new HashMap<>();
        if (patient.getExceptions() != null) {
            for (VaccineException exception : patient.getExceptions()) {
                exceptionsByVaccine.put(exception.getVaccineCode(), exception.getExceptionType());
            }
        }

        // Parse patient birth date once for all condition evaluations
        LocalDate birthDate = null;
        if (patient.getBirthDate() != null) {
            try {
                birthDate = LocalDate.parse(patient.getBirthDate(), DATE_FORMATTER);
                log.debug("Parsed birth date: {}", birthDate);
            } catch (DateTimeParseException e) {
                log.warn("Invalid birth date format for patient {}: {}",
                        patient.getId(), patient.getBirthDate());
            }
        }

        // Check each requirement
        for (ValidationRequirement requirement : requirements) {
            String vaccineCode = requirement.getVaccineCode();

            // ✅ FIX: Check exceptions FIRST, before checking doses
            // This allows exemptions to work even for patients with no immunizations
            if (requirement.getAcceptedExceptions() != null && !requirement.getAcceptedExceptions().isEmpty()) {
                String exceptionType = exceptionsByVaccine.get(vaccineCode);
                if (exceptionType != null && requirement.getAcceptedExceptions().contains(exceptionType)) {
                    log.debug("Requirement satisfied by exception {} for vaccine {}",
                            exceptionType, vaccineCode);
                    continue; // Requirement satisfied by exception
                }
            }

            Long foundDoses = vaccineCounts.getOrDefault(vaccineCode, 0L);
            Integer requiredDoses = requirement.getMinDoses() != null ? requirement.getMinDoses() : 1;

            // Check alternate requirements WITH date condition validation
            boolean alternateRequirementMet = false;
            if (requirement.getAlternateRequirements() != null) {
                for (AlternateRequirement altReq : requirement.getAlternateRequirements()) {
                    String altVaccineCode = altReq.getAlternateVaccineCode() != null ?
                            altReq.getAlternateVaccineCode() : vaccineCode;
                    Integer altRequiredDoses = altReq.getMinDoses() != null ? altReq.getMinDoses() : 1;
                    Long altFoundDoses = vaccineCounts.getOrDefault(altVaccineCode, 0L);

                    // Check dose count first
                    if (altFoundDoses >= altRequiredDoses) {

                        // Evaluate date-based condition if present
                        if (altReq.getCondition() != null && !altReq.getCondition().isEmpty()) {

                            // Get immunizations for this vaccine, sorted chronologically
                            List<Immunization> vaccineImmunizations = immunizationsByVaccine
                                    .getOrDefault(altVaccineCode, new ArrayList<>())
                                    .stream()
                                    .sorted(Comparator.comparing(Immunization::getOccurrenceDateTime))
                                    .collect(Collectors.toList());

                            // Only evaluate condition if we have a valid birth date
                            if (birthDate != null) {
                                boolean conditionMet = dateConditionEvaluator.evaluateCondition(
                                        altReq.getCondition(),
                                        vaccineImmunizations,
                                        birthDate
                                );

                                if (conditionMet) {
                                    log.debug("Alternate requirement satisfied: {} doses of {} with condition '{}'",
                                            altFoundDoses, altVaccineCode, altReq.getCondition());
                                    alternateRequirementMet = true;
                                    break;
                                } else {
                                    log.debug("Alternate requirement NOT satisfied: {} doses found but condition '{}' not met",
                                            altFoundDoses, altReq.getCondition());
                                }
                            } else {
                                log.warn("Cannot evaluate condition '{}' - missing or invalid birth date",
                                        altReq.getCondition());
                            }
                        } else {
                            // No condition specified - dose count alone is sufficient
                            log.debug("Alternate requirement satisfied: {} doses of {} (no date condition required)",
                                    altFoundDoses, altVaccineCode);
                            alternateRequirementMet = true;
                            break;
                        }
                    }
                }
            }

            if (alternateRequirementMet) {
                continue; // Requirement satisfied by alternate
            }

            // Check primary requirement
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
     * Calculate age in years from birth date.
     *
     * @param birthDateStr Birth date in ISO format (YYYY-MM-DD)
     * @return Age in years, or null if unable to calculate
     */
    private Integer calculateAge(String birthDateStr) {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr, DATE_FORMATTER);
            LocalDate now = LocalDate.now();
            return Period.between(birthDate, now).getYears();
        } catch (DateTimeParseException e) {
            log.error("Invalid birth date format: {}", birthDateStr);
            return null;
        }
    }

    /**
     * Mask patient ID for privacy in logs.
     * Shows first 4 and last 4 characters, masks the middle.
     *
     * @param patientId Original patient ID
     * @return Masked patient ID (e.g., "pati****t-123")
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