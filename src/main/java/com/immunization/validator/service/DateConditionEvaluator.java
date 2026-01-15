package com.immunization.validator.service;

import com.immunization.validator.model.Immunization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DateConditionEvaluator for Massachusetts immunization requirements.
 * Validates that vaccine doses meet date-based conditions (e.g., "on or after 4th birthday").
 *
 * Supported birthday validations:
 * - 1st birthday (MMR, Varicella)
 * - 4th birthday (DTaP, Polio)
 * - 10th birthday (MenACWY for grades 7-10)
 * - 16th birthday (MenACWY for grades 11-12)
 * - 18th birthday (Heplisav-B alternate)
 *
 * @author David Saaka
 * @version 2.0
 */
@Slf4j
@Component
public class DateConditionEvaluator {

    // Pattern to match: "Nth dose on or after Yth birthday"
    // Examples: "4th dose on or after 4th birthday", "1st dose on or after 1st birthday"
    private static final Pattern DATE_CONDITION_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)\\s+dose\\s+on or after\\s+(\\d+)(?:st|nd|rd|th)\\s+birthday",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Evaluates multiple date conditions (all must be met).
     *
     * This method is called when a requirement has multiple date conditions.
     * All conditions must be satisfied for the requirement to be met.
     *
     * @param conditions List of date conditions to evaluate
     * @param immunizations List of immunizations for the vaccine, sorted by date
     * @param birthDate Patient's birth date
     * @return true if ALL conditions are met, false otherwise
     */
    public boolean evaluateConditions(List<String> conditions,
                                      List<Immunization> immunizations,
                                      LocalDate birthDate) {
        if (conditions == null || conditions.isEmpty()) {
            log.debug("No date conditions to evaluate");
            return true;
        }

        log.debug("Evaluating {} date conditions", conditions.size());

        for (String condition : conditions) {
            boolean conditionMet = evaluateCondition(condition, immunizations, birthDate);
            if (!conditionMet) {
                log.info("Date condition failed: '{}'", condition);
                return false;
            }
        }

        log.debug("All {} date conditions met", conditions.size());
        return true;
    }

    /**
     * Evaluates a single date condition.
     *
     * Parses conditions like "4th dose on or after 4th birthday" and validates
     * that the specified dose was given on or after the required birthday.
     *
     * @param condition Date condition to evaluate (e.g., "4th dose on or after 4th birthday")
     * @param immunizations List of immunizations for the vaccine, sorted by date
     * @param birthDate Patient's birth date
     * @return true if condition is met, false otherwise
     */
    public boolean evaluateCondition(String condition,
                                     List<Immunization> immunizations,
                                     LocalDate birthDate) {
        if (condition == null || condition.trim().isEmpty()) {
            log.warn("Empty date condition provided, defaulting to true");
            return true;
        }

        log.debug("Evaluating date condition: '{}'", condition);

        Matcher matcher = DATE_CONDITION_PATTERN.matcher(condition);

        if (!matcher.find()) {
            log.warn("Could not parse date condition: '{}'. Defaulting to true to avoid false negatives",
                    condition);
            return true; // Don't fail validation on unparseable conditions
        }

        try {
            int doseNumber = Integer.parseInt(matcher.group(1));
            int birthdayYear = Integer.parseInt(matcher.group(2));

            log.debug("Parsed condition - Dose number: {}, Birthday year: {}", doseNumber, birthdayYear);

            // Check if we have enough doses
            if (immunizations == null || immunizations.size() < doseNumber) {
                log.debug("Not enough doses to check: have {}, need dose #{}",
                        immunizations == null ? 0 : immunizations.size(), doseNumber);
                return false;
            }

            // Get the specified dose (1-indexed)
            Immunization dose = immunizations.get(doseNumber - 1);
            LocalDate doseDate = LocalDate.parse(dose.getOccurrenceDateTime());

            // Calculate the target birthday
            LocalDate targetBirthday = birthDate.plusYears(birthdayYear);

            log.debug("Checking if dose date {} is on or after target birthday {}",
                    doseDate, targetBirthday);

            // Check if dose date is on or after the target birthday
            // CRITICAL: Use !isBefore to include the birthday itself (on or after)
            boolean conditionMet = !doseDate.isBefore(targetBirthday);

            if (conditionMet) {
                long daysAfter = ChronoUnit.DAYS.between(targetBirthday, doseDate);
                log.info("✓ Date condition MET: {}th dose ({}) is on or after {}th birthday ({}). " +
                                "Dose given {} days after birthday.",
                        doseNumber, doseDate, birthdayYear, targetBirthday, daysAfter);
            } else {
                long daysBefore = ChronoUnit.DAYS.between(doseDate, targetBirthday);
                log.info("✗ Date condition NOT MET: {}th dose ({}) is before {}th birthday ({}). " +
                                "Dose was given {} days too early.",
                        doseNumber, doseDate, birthdayYear, targetBirthday, daysBefore);
            }

            return conditionMet;

        } catch (Exception e) {
            log.error("Error evaluating date condition '{}': {}", condition, e.getMessage(), e);
            return true; // Don't fail validation on unexpected errors
        }
    }

    /**
     * Convenience method for evaluating a single condition with string birthDate.
     *
     * @param condition Date condition to evaluate
     * @param immunizations List of immunizations
     * @param birthDateStr Birth date as string (ISO format)
     * @return true if condition is met, false otherwise
     */
    public boolean evaluateCondition(String condition,
                                     List<Immunization> immunizations,
                                     String birthDateStr) {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            return evaluateCondition(condition, immunizations, birthDate);
        } catch (Exception e) {
            log.error("Invalid birth date format: {}", birthDateStr);
            return true; // Don't fail validation on invalid birth date
        }
    }
}