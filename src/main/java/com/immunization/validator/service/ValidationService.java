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
 * This version includes:
 * - Date-based condition validation (1st, 4th, 10th, 16th, 18th birthdays)
 * - Interval-based condition validation (28 days, 6 months, 8 weeks)
 * - Alternate requirements support
 * - Medical exemptions support
 *
 * @author Saakad
 * @since 2026-01-12
 * @version 2.0 - Added interval condition validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RequirementsService requirementsService;
    private final DateConditionEvaluator dateConditionEvaluator;
    private final IntervalConditionEvaluator intervalConditionEvaluator;  // ⬅️ NEW

    /**
     * Validate a patient's immunization record against state requirements.
     *
     * @param patient Patient with immunization records
     * @param stateCode State code (e.g., "CA", "MA")
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
     * This method evaluates:
     * 1. Dose counts (minimum required doses)
     * 2. Date-based conditions (doses on or after specific birthdays)
     * 3. Interval-based conditions (minimum time between doses)
     * 4. Alternate requirements (with their own date/interval conditions)
     * 5. Medical exemptions
     *
     * @param patient Patient with immunization records
     * @param requirements List of validation requirements
     * @return List of unmet requirements (empty if all satisfied)
     */
    private List<UnmetRequirement> validateRequirements(Patient patient,
                                                        List<ValidationRequirement> requirements) {
        List<UnmetRequirement> unmetRequirements = new ArrayList<>();

        // Initialize empty list instead of early return
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

        // Group full immunization objects for date-based and interval condition checking
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

            // Check exceptions FIRST, before checking doses
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

            // Check alternate requirements WITH date and interval condition validation
            boolean alternateRequirementMet = false;
            if (requirement.getAlternateRequirements() != null) {
                for (AlternateRequirement altReq : requirement.getAlternateRequirements()) {
                    String altVaccineCode = altReq.getAlternateVaccineCode() != null ?
                            altReq.getAlternateVaccineCode() : vaccineCode;
                    Integer altRequiredDoses = altReq.getMinDoses() != null ? altReq.getMinDoses() : 1;
                    Long altFoundDoses = vaccineCounts.getOrDefault(altVaccineCode, 0L);

                    // Check dose count first
                    if (altFoundDoses >= altRequiredDoses) {

                        // Get immunizations for this vaccine, sorted chronologically
                        List<Immunization> vaccineImmunizations = immunizationsByVaccine
                                .getOrDefault(altVaccineCode, new ArrayList<>())
                                .stream()
                                .sorted(Comparator.comparing(Immunization::getOccurrenceDateTime))
                                .collect(Collectors.toList());

                        // Evaluate date-based condition if present
                        boolean dateConditionMet = true;
                        if (altReq.getCondition() != null && !altReq.getCondition().isEmpty()) {
                            if (birthDate != null) {
                                dateConditionMet = dateConditionEvaluator.evaluateCondition(
                                        altReq.getCondition(),
                                        vaccineImmunizations,
                                        birthDate
                                );
                                if (!dateConditionMet) {
                                    log.debug("Alternate requirement date condition NOT satisfied: '{}'",
                                            altReq.getCondition());
                                }
                            } else {
                                log.warn("Cannot evaluate date condition '{}' - missing or invalid birth date",
                                        altReq.getCondition());
                                dateConditionMet = false;
                            }
                        }

                        // ⬇️ NEW: Evaluate interval-based conditions if present
                        boolean intervalConditionMet = true;
                        if (altReq.getIntervalConditions() != null && !altReq.getIntervalConditions().isEmpty()) {
                            intervalConditionMet = intervalConditionEvaluator.evaluateConditions(
                                    altReq.getIntervalConditions(),
                                    vaccineImmunizations
                            );
                            if (!intervalConditionMet) {
                                log.debug("Alternate requirement interval condition NOT satisfied");
                            }
                        }

                        // Both date and interval conditions must be met
                        if (dateConditionMet && intervalConditionMet) {
                            log.debug("Alternate requirement satisfied: {} doses of {} with all conditions met",
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

            // ⬇️ NEW: Check primary requirement with date and interval conditions
            // Get immunizations for this vaccine, sorted chronologically
            List<Immunization> vaccineImmunizations = immunizationsByVaccine
                    .getOrDefault(vaccineCode, new ArrayList<>())
                    .stream()
                    .sorted(Comparator.comparing(Immunization::getOccurrenceDateTime))
                    .collect(Collectors.toList());

            // Check dose count
            if (foundDoses >= requiredDoses) {
                boolean requirementMet = true;
                String failureReason = null;

                // Evaluate date conditions if present
                if (requirement.getDateConditions() != null && !requirement.getDateConditions().isEmpty()) {
                    if (birthDate != null) {
                        boolean dateConditionsMet = dateConditionEvaluator.evaluateConditions(
                                requirement.getDateConditions(),
                                vaccineImmunizations,
                                birthDate
                        );
                        if (!dateConditionsMet) {
                            requirementMet = false;
                            failureReason = "Date condition not met";
                            log.debug("Date conditions not met for vaccine {}", vaccineCode);
                        }
                    } else {
                        requirementMet = false;
                        failureReason = "Cannot evaluate date conditions - missing birth date";
                        log.warn("Cannot evaluate date conditions for {} - missing or invalid birth date",
                                vaccineCode);
                    }
                }

                // ⬇️ NEW: Evaluate interval conditions if present
                if (requirementMet && requirement.getIntervalConditions() != null &&
                        !requirement.getIntervalConditions().isEmpty()) {
                    boolean intervalConditionsMet = intervalConditionEvaluator.evaluateConditions(
                            requirement.getIntervalConditions(),
                            vaccineImmunizations
                    );
                    if (!intervalConditionsMet) {
                        requirementMet = false;
                        failureReason = "Interval condition not met";
                        log.debug("Interval conditions not met for vaccine {}", vaccineCode);
                    }
                }

                // If requirement is met with all conditions, continue to next requirement
                if (requirementMet) {
                    log.debug("Requirement fully satisfied for vaccine {}: {} doses with all conditions met",
                            vaccineCode, foundDoses);
                    continue;
                }

                // If we have enough doses but conditions not met, add specific unmet requirement
                if (failureReason != null) {
                    unmetRequirements.add(UnmetRequirement.builder()
                            .description(String.format("%s: %d doses found, but %s. %s",
                                    vaccineCode, foundDoses.intValue(), failureReason,
                                    requirement.getDescription() != null ? requirement.getDescription() : ""))
                            .vaccineCode(vaccineCode)
                            .requiredDoses(requiredDoses)
                            .foundDoses(foundDoses.intValue())
                            .build());
                    continue;
                }
            }

            // Not enough doses
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