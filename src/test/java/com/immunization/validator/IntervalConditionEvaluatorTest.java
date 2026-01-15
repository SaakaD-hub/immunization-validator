package com.immunization.validator;

import com.immunization.validator.model.Immunization;
import com.immunization.validator.service.IntervalConditionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for IntervalConditionEvaluator.
 * Tests interval requirements between vaccine doses per Massachusetts DPH guidelines.
 *
 * Key Interval Requirements:
 * - MMR: 28 days minimum between doses
 * - Varicella: 28 days minimum (or 3 months for ages 13+)
 * - Polio: 6 months between last two doses
 * - MenACWY: 8 weeks minimum between doses
 * - HepB: Various intervals
 *
 * @author David Saaka
 */
@DisplayName("IntervalConditionEvaluator Tests")
class IntervalConditionEvaluatorTest {

    private IntervalConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new IntervalConditionEvaluator();
    }

    // =================================================================================
    // TEST GROUP 1: MMR Interval Validations (28 days minimum)
    // =================================================================================

    @Test
    @DisplayName("MMR: Two doses with exactly 28 days apart - should be VALID")
    void testMMR_TwoDoses_ExactlyTwentyEightDays() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "2020-02-01"),
                createDose("MMR", "2020-02-29")  // 28 days (leap year)
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertTrue(result, "28 days should be valid");
    }

    @Test
    @DisplayName("MMR: Two doses with 27 days apart - should be INVALID")
    void testMMR_TwoDoses_InvalidInterval() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "2020-02-01"),
                createDose("MMR", "2020-02-28")  // Only 27 days
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertFalse(result, "27 days should be invalid");
    }

    @Test
    @DisplayName("MMR: Two doses with more than 28 days - should be VALID")
    void testMMR_TwoDoses_MoreThanTwentyEightDays() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "2020-01-01"),
                createDose("MMR", "2020-03-01")  // 60 days
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertTrue(result, "60 days should be valid");
    }

    // =================================================================================
    // TEST GROUP 2: Varicella Interval Validations (28 days or 3 months)
    // =================================================================================

    @Test
    @DisplayName("Varicella: Two doses with exactly 28 days - should be VALID")
    void testVaricella_TwoDoses_TwentyEightDays() {
        List<Immunization> doses = Arrays.asList(
                createDose("Varicella", "2020-03-01"),
                createDose("Varicella", "2020-03-29")  // 28 days
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Varicella: Two doses with 3 months (90 days) - should be VALID")
    void testVaricella_TwoDoses_ThreeMonths() {
        List<Immunization> doses = Arrays.asList(
                createDose("Varicella", "2020-01-01"),
                createDose("Varicella", "2020-04-01")  // 91 days ~ 3 months
        );

        boolean result = evaluator.evaluateCondition(
                "at least 3 months between doses",
                doses
        );

        assertTrue(result);
    }

    // =================================================================================
    // TEST GROUP 3: Polio Interval Validations (6 months between last two)
    // =================================================================================

    @Test
    @DisplayName("Polio: 4 doses with exactly 6 months between last two - should be VALID")
    void testPolio_FourDoses_SixMonthsBetweenLastTwo() {
        List<Immunization> doses = Arrays.asList(
                createDose("Polio", "2020-02-01"),
                createDose("Polio", "2020-04-01"),
                createDose("Polio", "2020-06-01"),
                createDose("Polio", "2020-12-01")  // 6 months (183 days) after 3rd dose
        );

        boolean result = evaluator.evaluateCondition(
                "at least 6 months between last two doses",
                doses
        );

        assertTrue(result, "6 months (183 days) should be valid");
    }

    @Test
    @DisplayName("Polio: 4 doses with less than 6 months between last two - should be INVALID")
    void testPolio_FourDoses_InvalidInterval() {
        List<Immunization> doses = Arrays.asList(
                createDose("Polio", "2020-02-01"),
                createDose("Polio", "2020-04-01"),
                createDose("Polio", "2020-06-01"),
                createDose("Polio", "2020-11-01")  // Only 5 months after 3rd dose
        );

        boolean result = evaluator.evaluateCondition(
                "at least 6 months between last two doses",
                doses
        );

        assertFalse(result, "5 months should be invalid");
    }

    // =================================================================================
    // TEST GROUP 4: MenACWY Interval Validations (8 weeks minimum)
    // =================================================================================

    @Test
    @DisplayName("MenACWY: Two doses with exactly 8 weeks (56 days) - should be VALID")
    void testMenACWY_TwoDoses_ExactlyEightWeeks() {
        List<Immunization> doses = Arrays.asList(
                createDose("MenACWY", "2020-03-01"),
                createDose("MenACWY", "2020-04-26")  // 56 days = 8 weeks
        );

        boolean result = evaluator.evaluateCondition(
                "at least 8 weeks between doses",
                doses
        );

        assertTrue(result, "8 weeks (56 days) should be valid");
    }

    @Test
    @DisplayName("MenACWY: Two doses with 7 weeks - should be INVALID")
    void testMenACWY_TwoDoses_SevenWeeks() {
        List<Immunization> doses = Arrays.asList(
                createDose("MenACWY", "2020-03-01"),
                createDose("MenACWY", "2020-04-19")  // 49 days = 7 weeks
        );

        boolean result = evaluator.evaluateCondition(
                "at least 8 weeks between doses",
                doses
        );

        assertFalse(result, "7 weeks should be invalid");
    }

    // =================================================================================
    // TEST GROUP 5: HepB Interval Validations (4 weeks minimum)
    // =================================================================================

    @Test
    @DisplayName("HepB: 4 weeks between doses - should be VALID")
    void testHepB_FourWeeksBetweenDoses() {
        List<Immunization> doses = Arrays.asList(
                createDose("HepB", "2020-01-01"),
                createDose("HepB", "2020-01-29")  // 28 days = 4 weeks
        );

        boolean result = evaluator.evaluateCondition(
                "at least 4 weeks between doses",
                doses
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("HepB: Three doses with valid intervals - should be VALID")
    void testHepB_ThreeDoses_ValidIntervals() {
        List<Immunization> doses = Arrays.asList(
                createDose("HepB", "2020-01-01"),
                createDose("HepB", "2020-02-01"),  // 31 days from 1st
                createDose("HepB", "2020-04-01")   // 60 days from 2nd
        );

        boolean result = evaluator.evaluateCondition(
                "at least 4 weeks between doses",
                doses
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("HepB: Three doses with 8+ weeks between consecutive doses - should be VALID")
    void testHepB_LongInterval_ConsecutiveDoses() {
        List<Immunization> doses = Arrays.asList(
                createDose("HepB", "2020-01-01"),
                createDose("HepB", "2020-03-01"),  // 60 days = 8.5 weeks from 1st
                createDose("HepB", "2020-05-15")   // 75 days = 10.7 weeks from 2nd
        );

        boolean result = evaluator.evaluateCondition(
                "at least 8 weeks between doses",
                doses
        );

        assertTrue(result);
    }

    // =================================================================================
    // TEST GROUP 6: DTaP Interval Validations (4 weeks minimum)
    // =================================================================================

    @Test
    @DisplayName("DTaP: 4 weeks between consecutive doses - should be VALID")
    void testDTaP_FourWeeksBetweenDoses() {
        List<Immunization> doses = Arrays.asList(
                createDose("DTaP", "2020-01-01"),
                createDose("DTaP", "2020-01-29")  // 28 days = 4 weeks
        );

        boolean result = evaluator.evaluateCondition(
                "at least 4 weeks between doses",
                doses
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("DTaP: 6 months between last two doses - should be VALID")
    void testDTaP_SixMonthsBetweenLastTwo() {
        List<Immunization> doses = Arrays.asList(
                createDose("DTaP", "2020-01-01"),
                createDose("DTaP", "2020-03-01"),
                createDose("DTaP", "2020-05-01"),
                createDose("DTaP", "2020-07-01"),
                createDose("DTaP", "2021-01-01")  // 6 months after 4th
        );

        boolean result = evaluator.evaluateCondition(
                "at least 6 months between last two doses",
                doses
        );

        assertTrue(result);
    }

    // =================================================================================
    // TEST GROUP 7: Edge Cases and Error Handling
    // =================================================================================

    @Test
    @DisplayName("Empty immunization list - should default to true (no interval to check)")
    void testEmptyImmunizationList() {
        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                Collections.emptyList()
        );

        assertTrue(result, "Empty list should default to true (lenient evaluation)");
    }

    @Test
    @DisplayName("Null immunization list - should default to true (no interval to check)")
    void testNullImmunizationList() {
        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                null
        );

        assertTrue(result, "Null list should default to true (lenient evaluation)");
    }

    @Test
    @DisplayName("Single dose - should default to true (no interval to check)")
    void testSingleDose() {
        List<Immunization> doses = Collections.singletonList(
                createDose("MMR", "2020-01-01")
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertTrue(result, "Single dose should default to true (no second dose to compare)");
    }

    @Test
    @DisplayName("Empty condition string - should default to true")
    void testEmptyCondition() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "2020-01-01"),
                createDose("MMR", "2020-02-01")
        );

        boolean result = evaluator.evaluateCondition("", doses);

        assertTrue(result, "Empty condition should default to true");
    }

    @Test
    @DisplayName("Null condition string - should default to true")
    void testNullCondition() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "2020-01-01"),
                createDose("MMR", "2020-02-01")
        );

        boolean result = evaluator.evaluateCondition(null, doses);

        assertTrue(result, "Null condition should default to true");
    }

    @Test
    @DisplayName("Invalid date format - should handle gracefully")
    void testInvalidDateFormat() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "invalid-date"),
                createDose("MMR", "2020-02-01")
        );

        // Should not throw exception
        assertDoesNotThrow(() -> {
            evaluator.evaluateCondition(
                    "at least 28 days between doses",
                    doses
            );
        }, "Should handle invalid dates gracefully");
    }

    @Test
    @DisplayName("Same day doses - should return false (0 days apart)")
    void testSameDayDoses() {
        String sameDay = "2020-01-01";
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", sameDay),
                createDose("MMR", sameDay)
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertFalse(result, "Same day doses should be invalid (0 days < 28 days)");
    }

    @Test
    @DisplayName("Three doses - validates all consecutive pairs")
    void testThreeDoses_ValidatesAllPairs() {
        List<Immunization> doses = Arrays.asList(
                createDose("MMR", "2020-01-01"),
                createDose("MMR", "2020-02-01"),  // 31 days from 1st - VALID
                createDose("MMR", "2020-02-15")   // 14 days from 2nd - INVALID
        );

        boolean result = evaluator.evaluateCondition(
                "at least 28 days between doses",
                doses
        );

        assertFalse(result, "Should fail if any consecutive pair violates interval");
    }

    // =================================================================================
    // TEST GROUP 8: Special Cases for "Last Two Doses" Requirements
    // =================================================================================

    @Test
    @DisplayName("Last two doses requirement - ignores earlier intervals")
    void testLastTwoDoses_IgnoresEarlierIntervals() {
        List<Immunization> doses = Arrays.asList(
                createDose("Polio", "2020-01-01"),
                createDose("Polio", "2020-01-15"),  // Only 14 days - but not checked
                createDose("Polio", "2020-02-01"),  // Only 17 days - but not checked
                createDose("Polio", "2020-08-01")   // 6 months from 3rd - THIS is checked
        );

        boolean result = evaluator.evaluateCondition(
                "at least 6 months between last two doses",
                doses
        );

        assertTrue(result, "Should only check interval between last two doses");
    }

    @Test
    @DisplayName("Last two doses requirement - with only 2 doses total")
    void testLastTwoDoses_OnlyTwoDoses() {
        List<Immunization> doses = Arrays.asList(
                createDose("Polio", "2020-01-01"),
                createDose("Polio", "2020-07-01")  // 6 months later
        );

        boolean result = evaluator.evaluateCondition(
                "at least 6 months between last two doses",
                doses
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Four weeks between doses - valid example")
    void testFourWeeks_ValidExample() {
        List<Immunization> doses = Arrays.asList(
                createDose("DTaP", "2020-01-01"),
                createDose("DTaP", "2020-02-01")  // 31 days = 4+ weeks
        );

        boolean result = evaluator.evaluateCondition(
                "at least 4 weeks between doses",
                doses
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Four weeks between doses - invalid example")
    void testFourWeeks_InvalidExample() {
        List<Immunization> doses = Arrays.asList(
                createDose("DTaP", "2020-01-01"),
                createDose("DTaP", "2020-01-22")  // 21 days = 3 weeks (< 4 weeks)
        );

        boolean result = evaluator.evaluateCondition(
                "at least 4 weeks between doses",
                doses
        );

        assertFalse(result);
    }

    // =================================================================================
    // HELPER METHODS
    // =================================================================================

    private Immunization createDose(String vaccineCode, String date) {
        Immunization imm = new Immunization();
        imm.setVaccineCode(vaccineCode);
        imm.setOccurrenceDateTime(date);
        return imm;
    }
}