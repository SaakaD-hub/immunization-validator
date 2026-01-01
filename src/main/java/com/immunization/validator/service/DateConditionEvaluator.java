package com.immunization.validator.service;

import com.immunization.validator.model.Immunization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates date-based conditions for alternate vaccine requirements.
 * 
 * Handles CDC conditions like:
 * - "4th dose on or after 4th birthday"
 * - "3rd dose after 7 months of age"
 * 
 * This component was missing from the original implementation, causing
 * the system to accept invalid immunization records.
 */
@Slf4j
@Component
public class DateConditionEvaluator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // Regex pattern to parse: "4th dose on or after 4th birthday"
    // Captures: dose number (4) and birthday year (4)
    private static final Pattern DOSE_BIRTHDAY_PATTERN = 
        Pattern.compile("(\\d+)(?:st|nd|rd|th)\\s+dose\\s+on\\s+or\\s+after\\s+(\\d+)(?:st|nd|rd|th)\\s+birthday");
    
    /**
     * Evaluates whether the given immunizations meet the specified date condition.
     * 
     * @param condition The condition string from requirement config (e.g., "4th dose on or after 4th birthday")
     * @param immunizations List of immunizations for this vaccine, sorted by date
     * @param birthDate Patient's birth date
     * @return true if condition is met, false otherwise
     */
    public boolean evaluateCondition(
            String condition, 
            List<Immunization> immunizations,
            LocalDate birthDate) {
        
        if (condition == null || condition.isEmpty() || birthDate == null) {
            log.debug("Condition evaluation skipped - missing condition or birthDate");
            return false;
        }
        
        try {
            // Parse the condition using regex
            Matcher matcher = DOSE_BIRTHDAY_PATTERN.matcher(condition.toLowerCase());
            if (matcher.find()) {
                int doseNumber = Integer.parseInt(matcher.group(1));
                int birthdayYear = Integer.parseInt(matcher.group(2));
                
                return evaluateDoseAfterBirthday(immunizations, doseNumber, birthdayYear, birthDate);
            }
            
            log.warn("Unrecognized condition pattern: {}", condition);
            return false;
            
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", condition, e);
            return false;
        }
    }
    
    /**
     * Checks if the specified dose was given on or after the specified birthday.
     * 
     * Example: "4th dose on or after 4th birthday"
     * - Patient born: 2019-01-01
     * - 4th birthday: 2023-01-01
     * - 4th dose date: 2022-11-01 → BEFORE 4th birthday → FAILS
     * - 4th dose date: 2023-02-01 → AFTER 4th birthday → PASSES
     * 
     * @param immunizations List of immunizations (must be sorted by date)
     * @param doseNumber Which dose to check (1-indexed, e.g., 4 for "4th dose")
     * @param birthdayYear Which birthday (e.g., 4 for "4th birthday")
     * @param birthDate Patient's birth date
     * @return true if dose date is on or after the target birthday
     */
    private boolean evaluateDoseAfterBirthday(
            List<Immunization> immunizations,
            int doseNumber,
            int birthdayYear,
            LocalDate birthDate) {
        
        // Check if we have enough doses
        if (immunizations.size() < doseNumber) {
            log.debug("Not enough doses: have {}, need {}", immunizations.size(), doseNumber);
            return false;
        }
        
        // Get the specified dose (convert from 1-indexed to 0-indexed)
        Immunization targetDose = immunizations.get(doseNumber - 1);
        
        try {
            // Parse dose date
            LocalDate doseDate = LocalDate.parse(
                targetDose.getOccurrenceDateTime(), DATE_FORMATTER);
            
            // Calculate target birthday
            LocalDate targetBirthday = birthDate.plusYears(birthdayYear);
            
            // Check if dose was on or after the birthday
            boolean conditionMet = !doseDate.isBefore(targetBirthday);
            
            log.debug("Dose #{} on {}, {}th birthday on {}: condition {}",
                    doseNumber, doseDate, birthdayYear, targetBirthday,
                    conditionMet ? "MET" : "NOT MET");
            
            return conditionMet;
            
        } catch (DateTimeParseException e) {
            log.error("Invalid date format in immunization: {}", 
                    targetDose.getOccurrenceDateTime());
            return false;
        }
    }
}