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
 * Supported validations:
 * - Birthday-based: "Nth dose on or after Yth birthday"
 *   Examples: "1st dose on or after 1st birthday", "4th dose on or after 4th birthday"
 *
 * - Month-based: "Nth dose on or after Yth month"
 *   Examples: "1st dose on or after 15th month", "2nd dose on or after 6th month"
 *
 * @author David Saaka
 * @version 2.1 - Added month-based condition support
 */
@Slf4j
@Component
public class DateConditionEvaluator {

    // Pattern to match: "Nth dose on or after Yth birthday"
    // Examples: "4th dose on or after 4th birthday", "1st dose on or after 1st birthday"
    private static final Pattern BIRTHDAY_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)\\s+dose\\s+on or after\\s+(\\d+)(?:st|nd|rd|th)\\s+birthday",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to match: "Nth dose on or after Yth month"
    // Examples: "1st dose on or after 15th month", "2nd dose on or after 6th month"
    private static final Pattern MONTH_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)\\s+dose\\s+on or after\\s+(\\d+)(?:st|nd|rd|th)\\s+month",
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
     * Supports two types of conditions:
     * 1. Birthday-based: "4th dose on or after 4th birthday"
     * 2. Month-based: "1st dose on or after 15th month"
     *
     * @param condition Date condition to evaluate
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

        // Try birthday pattern first
        Matcher birthdayMatcher = BIRTHDAY_PATTERN.matcher(condition);
        if (birthdayMatcher.find()) {
            return evaluateBirthdayCondition(birthdayMatcher, condition, immunizations, birthDate);
        }

        // Try month pattern
        Matcher monthMatcher = MONTH_PATTERN.matcher(condition);
        if (monthMatcher.find()) {
            return evaluateMonthCondition(monthMatcher, condition, immunizations, birthDate);
        }

        // If neither pattern matches, log warning and default to true
        log.warn("Could not parse date condition: '{}'. Defaulting to true to avoid false negatives",
                condition);
        return true; // Don't fail validation on unparseable conditions
    }

    /**
     * Evaluates a birthday-based condition.
     *
     * @param matcher Regex matcher containing dose number and birthday year
     * @param condition Original condition string (for logging)
     * @param immunizations List of immunizations
     * @param birthDate Patient's birth date
     * @return true if condition is met, false otherwise
     */
    private boolean evaluateBirthdayCondition(Matcher matcher,
                                              String condition,
                                              List<Immunization> immunizations,
                                              LocalDate birthDate) {
        try {
            int doseNumber = Integer.parseInt(matcher.group(1));
            int birthdayYear = Integer.parseInt(matcher.group(2));

            log.debug("Parsed BIRTHDAY condition - Dose: {}, Birthday year: {}", doseNumber, birthdayYear);

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
            LocalDate targetDate = birthDate.plusYears(birthdayYear);

            log.debug("Checking if dose date {} is on or after {}th birthday {}",
                    doseDate, birthdayYear, targetDate);

            // Check if dose date is on or after the target birthday
            // CRITICAL: Use !isBefore to include the birthday itself (on or after)
            boolean conditionMet = !doseDate.isBefore(targetDate);

            logConditionResult(conditionMet, doseNumber, doseDate, birthdayYear, targetDate, "birthday");

            return conditionMet;

        } catch (Exception e) {
            log.error("Error evaluating birthday condition '{}': {}", condition, e.getMessage(), e);
            return true; // Don't fail validation on unexpected errors
        }
    }

    /**
     * Evaluates a month-based condition.
     *
     * @param matcher Regex matcher containing dose number and month number
     * @param condition Original condition string (for logging)
     * @param immunizations List of immunizations
     * @param birthDate Patient's birth date
     * @return true if condition is met, false otherwise
     */
    private boolean evaluateMonthCondition(Matcher matcher,
                                           String condition,
                                           List<Immunization> immunizations,
                                           LocalDate birthDate) {
        try {
            int doseNumber = Integer.parseInt(matcher.group(1));
            int monthsAfterBirth = Integer.parseInt(matcher.group(2));

            log.debug("Parsed MONTH condition - Dose: {}, Months after birth: {}",
                    doseNumber, monthsAfterBirth);

            // Check if we have enough doses
            if (immunizations == null || immunizations.size() < doseNumber) {
                log.debug("Not enough doses to check: have {}, need dose #{}",
                        immunizations == null ? 0 : immunizations.size(), doseNumber);
                return false;
            }

            // Get the specified dose (1-indexed)
            Immunization dose = immunizations.get(doseNumber - 1);
            LocalDate doseDate = LocalDate.parse(dose.getOccurrenceDateTime());

            // Calculate the target date (birth date + N months)
            LocalDate targetDate = birthDate.plusMonths(monthsAfterBirth);

            log.debug("Checking if dose date {} is on or after {} months from birth ({})",
                    doseDate, monthsAfterBirth, targetDate);

            // Check if dose date is on or after the target date
            // CRITICAL: Use !isBefore to include the target date itself (on or after)
            boolean conditionMet = !doseDate.isBefore(targetDate);

            logConditionResult(conditionMet, doseNumber, doseDate, monthsAfterBirth, targetDate, "month");

            return conditionMet;

        } catch (Exception e) {
            log.error("Error evaluating month condition '{}': {}", condition, e.getMessage(), e);
            return true; // Don't fail validation on unexpected errors
        }
    }

    /**
     * Logs the result of a condition evaluation.
     *
     * @param conditionMet Whether the condition was met
     * @param doseNumber Dose number being checked
     * @param doseDate Date the dose was given
     * @param timeValue Birthday year or month number
     * @param targetDate Calculated target date
     * @param timeUnit "birthday" or "month"
     */
    private void logConditionResult(boolean conditionMet,
                                    int doseNumber,
                                    LocalDate doseDate,
                                    int timeValue,
                                    LocalDate targetDate,
                                    String timeUnit) {
        if (conditionMet) {
            long daysAfter = ChronoUnit.DAYS.between(targetDate, doseDate);
            log.info("✓ Date condition MET: {}th dose ({}) is on or after {}th {} ({}). " +
                            "Dose given {} days after target.",
                    doseNumber, doseDate, timeValue, timeUnit, targetDate, daysAfter);
        } else {
            long daysBefore = ChronoUnit.DAYS.between(doseDate, targetDate);
            log.info("✗ Date condition NOT MET: {}th dose ({}) is before {}th {} ({}). " +
                            "Dose was given {} days too early.",
                    doseNumber, doseDate, timeValue, timeUnit, targetDate, daysBefore);
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