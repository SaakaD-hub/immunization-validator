package com.immunization.validator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Represents an alternate requirement for vaccine compliance.
 *
 * Alternate requirements allow flexibility in meeting immunization standards.
 * For example, DTaP may require 5 doses, but 4 doses are acceptable if the
 * 4th dose is given on or after the child's 4th birthday.
 *
 * @author Saakad
 * @since 2026-01-12
 * @version 2.0 - Added intervalConditions support
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternateRequirement {

    /**
     * Minimum number of doses required for this alternate requirement.
     * Example: 4 (for DTaP alternate)
     */
    private Integer minDoses;

    /**
     * Alternative vaccine code if different from the primary requirement.
     * Example: "DT" as an alternate to "DTaP" with medical contraindication
     */
    private String alternateVaccineCode;

    /**
     * Text description of the condition for this alternate requirement.
     * This is a human-readable explanation.
     * Example: "4 doses acceptable if 4th dose given on or after 4th birthday"
     */
    private String condition;

    /**
     * Description of the alternate requirement for display purposes.
     * Example: "4 doses of DTaP if 4th dose given on or after 4th birthday"
     */
    private String description;

    /**
     * List of date-based conditions that must be met for this alternate requirement.
     * Each condition is evaluated by DateConditionEvaluator.
     *
     * Format: "Nth dose on or after Yth birthday"
     * Examples:
     * - "4th dose on or after 4th birthday"
     * - "3rd dose on or after 4th birthday"
     * - "1st dose on or after 16th birthday"
     *
     * All conditions must be met for the alternate requirement to be satisfied.
     */
    private List<String> dateConditions;

    /**
     * List of interval-based conditions that must be met for this alternate requirement.
     * Each condition is evaluated by IntervalConditionEvaluator.
     *
     * Format: "at least N days/weeks/months/years between [last two] doses"
     * Examples:
     * - "at least 28 days between doses"
     * - "at least 6 months between last two doses"
     * - "at least 8 weeks between doses"
     *
     * All conditions must be met for the alternate requirement to be satisfied.
     */
    private List<String> intervalConditions;

    /**
     * Additional notes about this alternate requirement.
     * Example: "Requires physician letter documenting DTaP contraindication"
     */
    private String notes;
}