package com.immunization.validator.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents a validation requirement for a specific vaccine.
 *
 * A requirement specifies:
 * - Which vaccine is required (vaccineCode)
 * - Minimum number of doses needed (minDoses)
 * - Date-based conditions (e.g., "on or after 4th birthday")
 * - Interval-based conditions (e.g., "at least 28 days between doses")
 * - Acceptable exemptions (e.g., medical contraindication)
 * - Alternate ways to meet the requirement
 *
 * @author Saakad
 * @since 2026-01-12
 * @version 2.0 - Added intervalConditions support
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequirement {

    /**
     * Vaccine code (e.g., "DTaP", "MMR", "Polio", "MenACWY")
     */
    private String vaccineCode;

    /**
     * Minimum number of doses required for this vaccine.
     * Example: 5 for DTaP, 2 for MMR
     */
    private Integer minDoses;

    /**
     * Human-readable description of the requirement.
     * Example: "DTaP - 5 doses required for kindergarten entry"
     */
    private String description;

    /**
     * List of acceptable exception types that satisfy this requirement.
     * Examples: "MEDICAL_CONTRAINDICATION", "LABORATORY_EVIDENCE", "RELIABLE_HISTORY"
     */
    private List<String> acceptedExceptions;

    /**
     * List of date-based conditions that must be met.
     * Format: "Nth dose on or after Yth birthday"
     *
     * Examples:
     * - "1st dose on or after 1st birthday"
     * - "4th dose on or after 4th birthday"
     * - "2nd dose on or after 16th birthday"
     *
     * All date conditions must be satisfied for the requirement to be met.
     */
    private List<String> dateConditions;

    /**
     * List of interval-based conditions that must be met.
     * Format: "at least N days/weeks/months/years between [last two] doses"
     *
     * Examples:
     * - "at least 28 days between doses"
     * - "at least 6 months between last two doses"
     * - "at least 8 weeks between doses"
     *
     * All interval conditions must be satisfied for the requirement to be met.
     */
    private List<String> intervalConditions;

    /**
     * List of alternate ways to satisfy this requirement.
     *
     * Example: DTaP requires 5 doses, but 4 doses are acceptable if the 4th dose
     * is given on or after the 4th birthday.
     *
     * If any alternate requirement is met, the main requirement is considered satisfied.
     */
    private List<AlternateRequirement> alternateRequirements;

    /**
     * Additional notes about this requirement.
     * Example: "DT is only acceptable with letter stating medical contraindication to DTaP"
     */
    private String notes;
}