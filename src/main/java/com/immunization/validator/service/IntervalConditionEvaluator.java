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
 * Validates minimum intervals between vaccine doses.
 * Supports: "at least 28 days between doses", "at least 6 months between last two doses", etc.
 */
@Slf4j
@Component
public class IntervalConditionEvaluator {
    
    private static final Pattern INTERVAL_PATTERN = Pattern.compile(
        "at least\\s+(\\d+)\\s+(days?|weeks?|months?|years?)\\s+between\\s+(last two\\s+)?doses",
        Pattern.CASE_INSENSITIVE
    );
    
    public boolean evaluateCondition(String condition, List<Immunization> immunizations) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }
        
        Matcher matcher = INTERVAL_PATTERN.matcher(condition);
        if (!matcher.find()) {
            log.warn("Could not parse interval condition: '{}'", condition);
            return true;
        }
        
        try {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            boolean lastTwoOnly = matcher.group(3) != null;
            
            if (immunizations == null || immunizations.size() < 2) {
                return true;
            }
            
            long requiredDays = convertToDays(amount, unit);
            
            if (lastTwoOnly) {
                return checkLastTwoDoses(immunizations, requiredDays);
            } else {
                return checkAllConsecutivePairs(immunizations, requiredDays);
            }
        } catch (Exception e) {
            log.error("Error evaluating interval condition: {}", e.getMessage());
            return true;
        }
    }
    
    public boolean evaluateConditions(List<String> conditions, List<Immunization> immunizations) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (String condition : conditions) {
            if (!evaluateCondition(condition, immunizations)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkLastTwoDoses(List<Immunization> immunizations, long requiredDays) {
        int size = immunizations.size();
        Immunization secondToLast = immunizations.get(size - 2);
        Immunization last = immunizations.get(size - 1);
        
        LocalDate date1 = LocalDate.parse(secondToLast.getOccurrenceDateTime());
        LocalDate date2 = LocalDate.parse(last.getOccurrenceDateTime());
        
        long actualDays = ChronoUnit.DAYS.between(date1, date2);
        boolean met = actualDays >= requiredDays;
        
        log.info("Interval check (last two): {} days (required: {}) - {}", 
                actualDays, requiredDays, met ? "MET" : "NOT MET");
        
        return met;
    }
    
    private boolean checkAllConsecutivePairs(List<Immunization> immunizations, long requiredDays) {
        for (int i = 0; i < immunizations.size() - 1; i++) {
            Immunization dose1 = immunizations.get(i);
            Immunization dose2 = immunizations.get(i + 1);
            
            LocalDate date1 = LocalDate.parse(dose1.getOccurrenceDateTime());
            LocalDate date2 = LocalDate.parse(dose2.getOccurrenceDateTime());
            
            long actualDays = ChronoUnit.DAYS.between(date1, date2);
            
            if (actualDays < requiredDays) {
                log.info("Interval NOT MET between doses {} and {}: {} days (required: {})",
                        i + 1, i + 2, actualDays, requiredDays);
                return false;
            }
        }
        return true;
    }
    
    private long convertToDays(int amount, String unit) {
        switch (unit) {
            case "day":
            case "days":
                return amount;
            case "week":
            case "weeks":
                return amount * 7L;
            case "month":
            case "months":
                return amount * 30L;
            case "year":
            case "years":
                return amount * 365L;
            default:
                return amount;
        }
    }
}