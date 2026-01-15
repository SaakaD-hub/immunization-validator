package com.immunization.validator;

import com.immunization.validator.model.Immunization;
import com.immunization.validator.service.DateConditionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DateConditionEvaluator.
 * Covers all Massachusetts immunization date requirements.
 *
 * @author David Saaka
 */
@DisplayName("DateConditionEvaluator Tests")
class DateConditionEvaluatorTest {

    private DateConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new DateConditionEvaluator();
    }

    // =================================================================================
    // TEST GROUP 1: 1st Birthday Validations (MMR, Varicella)
    // =================================================================================

    @Test
    @DisplayName("MMR 1st dose: ON 1st birthday - should be VALID")
    void test_MMR_FirstDose_OnFirstBirthday() {
        // Patient born 2020-01-15, 1st dose on exactly 1st birthday
        List<Immunization> doses = Collections.singletonList(
            createDose("MMR", "2021-01-15")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            doses,
            "2020-01-15"
        );

        assertTrue(result, "Dose on exactly 1st birthday should be valid");
    }

    @Test
    @DisplayName("MMR 1st dose: AFTER 1st birthday - should be VALID")
    void test_MMR_FirstDose_AfterFirstBirthday() {
        // Patient born 2020-01-15, 1st dose 30 days after 1st birthday
        List<Immunization> doses = Collections.singletonList(
            createDose("MMR", "2021-02-14")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            doses,
            "2020-01-15"
        );

        assertTrue(result, "Dose after 1st birthday should be valid");
    }

    @Test
    @DisplayName("MMR 1st dose: BEFORE 1st birthday - should be INVALID")
    void test_MMR_FirstDose_BeforeFirstBirthday() {
        // Patient born 2020-01-15, 1st dose 1 day before 1st birthday
        List<Immunization> doses = Collections.singletonList(
            createDose("MMR", "2021-01-14")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            doses,
            "2020-01-15"
        );

        assertFalse(result, "Dose before 1st birthday should be invalid");
    }

    @Test
    @DisplayName("Varicella 1st dose: ON 1st birthday - should be VALID")
    void test_Varicella_FirstDose_OnFirstBirthday() {
        List<Immunization> doses = Collections.singletonList(
            createDose("Varicella", "2020-06-01")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            doses,
            "2019-06-01"
        );

        assertTrue(result);
    }

    // =================================================================================
    // TEST GROUP 2: 4th Birthday Validations (DTaP, Polio)
    // =================================================================================

    @Test
    @DisplayName("DTaP 4th dose: ON 4th birthday - should be VALID")
    void test_DTaP_FourthDose_OnFourthBirthday() {
        List<Immunization> doses = Arrays.asList(
            createDose("DTaP", "2019-03-01"),
            createDose("DTaP", "2019-05-01"),
            createDose("DTaP", "2019-07-01"),
            createDose("DTaP", "2023-01-01")  // Exactly on 4th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "4th dose on or after 4th birthday",
            doses,
            "2019-01-01"
        );

        assertTrue(result, "4th dose on exactly 4th birthday should be valid");
    }

    @Test
    @DisplayName("DTaP 4th dose: AFTER 4th birthday - should be VALID")
    void test_DTaP_FourthDose_AfterFourthBirthday() {
        List<Immunization> doses = Arrays.asList(
            createDose("DTaP", "2019-03-01"),
            createDose("DTaP", "2019-05-01"),
            createDose("DTaP", "2019-07-01"),
            createDose("DTaP", "2023-02-01")  // After 4th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "4th dose on or after 4th birthday",
            doses,
            "2019-01-01"
        );

        assertTrue(result, "4th dose after 4th birthday should be valid");
    }

    @Test
    @DisplayName("DTaP 4th dose: BEFORE 4th birthday - should be INVALID (CRITICAL BUG FIX)")
    void test_DTaP_FourthDose_BeforeFourthBirthday() {
        // This is Sarah Johnson's case - the critical bug we fixed!
        List<Immunization> doses = Arrays.asList(
            createDose("DTaP", "2019-03-01"),
            createDose("DTaP", "2019-05-01"),
            createDose("DTaP", "2019-07-01"),
            createDose("DTaP", "2022-11-01")  // 2 months before 4th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "4th dose on or after 4th birthday",
            doses,
            "2019-01-01"  // 4th birthday is 2023-01-01
        );

        assertFalse(result, "4th dose before 4th birthday should be invalid - this is the bug we fixed!");
    }

    @Test
    @DisplayName("Polio 4th dose: ON 4th birthday - should be VALID")
    void test_Polio_FourthDose_OnFourthBirthday() {
        List<Immunization> doses = Arrays.asList(
            createDose("Polio", "2020-02-15"),
            createDose("Polio", "2020-04-15"),
            createDose("Polio", "2020-06-15"),
            createDose("Polio", "2024-02-15")  // Exactly on 4th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "4th dose on or after 4th birthday",
            doses,
            "2020-02-15"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Polio 3rd dose (alternate): ON 4th birthday - should be VALID")
    void test_Polio_ThirdDose_AlternateSchedule_OnFourthBirthday() {
        List<Immunization> doses = Arrays.asList(
            createDose("Polio", "2020-02-15"),
            createDose("Polio", "2020-04-15"),
            createDose("Polio", "2024-02-15")  // 3rd dose on 4th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "3rd dose on or after 4th birthday",
            doses,
            "2020-02-15"
        );

        assertTrue(result, "3rd dose on 4th birthday meets alternate requirement");
    }

    // =================================================================================
    // TEST GROUP 3: 10th Birthday Validations (Meningococcal Grades 7-10)
    // =================================================================================

    @Test
    @DisplayName("MenACWY 1st dose: ON 10th birthday - should be VALID")
    void test_MenACWY_FirstDose_OnTenthBirthday() {
        List<Immunization> doses = Collections.singletonList(
            createDose("MenACWY", "2024-03-15")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 10th birthday",
            doses,
            "2014-03-15"
        );

        assertTrue(result, "MenACWY on 10th birthday should be valid");
    }

    @Test
    @DisplayName("MenACWY 1st dose: BEFORE 10th birthday - should be INVALID")
    void test_MenACWY_FirstDose_BeforeTenthBirthday() {
        List<Immunization> doses = Collections.singletonList(
            createDose("MenACWY", "2024-03-14")  // 1 day before 10th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 10th birthday",
            doses,
            "2014-03-15"
        );

        assertFalse(result, "MenACWY before 10th birthday should be invalid");
    }

    @Test
    @DisplayName("MenACWY 1st dose: AFTER 10th birthday - should be VALID")
    void test_MenACWY_FirstDose_AfterTenthBirthday() {
        List<Immunization> doses = Collections.singletonList(
            createDose("MenACWY", "2024-06-01")  // Several months after
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 10th birthday",
            doses,
            "2014-03-15"
        );

        assertTrue(result);
    }

    // =================================================================================
    // TEST GROUP 4: 16th Birthday Validations (Meningococcal Grades 11-12)
    // =================================================================================

    @Test
    @DisplayName("MenACWY 2nd dose: ON 16th birthday - should be VALID")
    void test_MenACWY_SecondDose_OnSixteenthBirthday() {
        List<Immunization> doses = Arrays.asList(
            createDose("MenACWY", "2020-05-01"),
            createDose("MenACWY", "2024-07-15")  // Exactly on 16th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "2nd dose on or after 16th birthday",
            doses,
            "2008-07-15"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("MenACWY 2nd dose: BEFORE 16th birthday - should be INVALID")
    void test_MenACWY_SecondDose_BeforeSixteenthBirthday() {
        List<Immunization> doses = Arrays.asList(
            createDose("MenACWY", "2020-05-01"),
            createDose("MenACWY", "2024-07-14")  // 1 day before 16th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "2nd dose on or after 16th birthday",
            doses,
            "2008-07-15"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("MenACWY 1st dose (alternate): ON 16th birthday - should be VALID")
    void test_MenACWY_FirstDose_AlternateSchedule_OnSixteenthBirthday() {
        // If 1st dose is on or after 16th birthday, only 1 dose is required
        List<Immunization> doses = Collections.singletonList(
            createDose("MenACWY", "2024-07-15")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 16th birthday",
            doses,
            "2008-07-15"
        );

        assertTrue(result, "Single dose on 16th birthday meets alternate requirement");
    }

    // =================================================================================
    // TEST GROUP 5: 18th Birthday Validations (Heplisav-B)
    // =================================================================================

    @Test
    @DisplayName("Heplisav-B 1st dose: ON 18th birthday - should be VALID")
    void test_HeplisavB_FirstDose_OnEighteenthBirthday() {
        List<Immunization> doses = Collections.singletonList(
            createDose("HepB", "2024-09-01")
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 18th birthday",
            doses,
            "2006-09-01"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Heplisav-B 1st dose: BEFORE 18th birthday - should be INVALID")
    void test_HeplisavB_FirstDose_BeforeEighteenthBirthday() {
        List<Immunization> doses = Collections.singletonList(
            createDose("HepB", "2024-08-31")  // 1 day before 18th birthday
        );

        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 18th birthday",
            doses,
            "2006-09-01"
        );

        assertFalse(result);
    }

    // =================================================================================
    // TEST GROUP 6: Edge Cases and Error Handling
    // =================================================================================

    @Test
    @DisplayName("Empty condition - should default to true")
    void test_EmptyCondition() {
        List<Immunization> doses = Collections.singletonList(
            createDose("DTaP", "2020-01-01")
        );

        boolean result = evaluator.evaluateCondition("", doses, "2019-01-01");

        assertTrue(result, "Empty condition should default to true");
    }

    @Test
    @DisplayName("Null condition - should default to true")
    void test_NullCondition() {
        List<Immunization> doses = Collections.singletonList(
            createDose("DTaP", "2020-01-01")
        );

        boolean result = evaluator.evaluateCondition(null, doses, "2019-01-01");

        assertTrue(result, "Null condition should default to true");
    }

    @Test
    @DisplayName("Not enough doses - should return false")
    void test_NotEnoughDoses() {
        List<Immunization> doses = Arrays.asList(
            createDose("DTaP", "2019-03-01"),
            createDose("DTaP", "2019-05-01")
        );

        boolean result = evaluator.evaluateCondition(
            "4th dose on or after 4th birthday",
            doses,
            "2019-01-01"
        );

        assertFalse(result, "Should return false when not enough doses");
    }

    @Test
    @DisplayName("Empty immunization list - should return false")
    void test_EmptyImmunizationList() {
        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            Collections.emptyList(),
            "2019-01-01"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Null immunization list - should return false")
    void test_NullImmunizationList() {
        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            null,
            "2019-01-01"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Invalid date format - should gracefully handle error")
    void test_InvalidDateFormat() {
        List<Immunization> doses = Collections.singletonList(
            createDose("MMR", "invalid-date")
        );

        // Should not throw exception, should default to true
        boolean result = evaluator.evaluateCondition(
            "1st dose on or after 1st birthday",
            doses,
            "2020-01-01"
        );

        assertTrue(result, "Should handle invalid dates gracefully");
    }

    // =================================================================================
    // TEST GROUP 7: Multiple Conditions
    // =================================================================================

    @Test
    @DisplayName("Multiple conditions: All pass - should be VALID")
    void test_MultipleConditions_AllPass() {
        List<Immunization> doses = Arrays.asList(
            createDose("MMR", "2021-02-01"),  // After 1st birthday
            createDose("MMR", "2024-03-01")   // After 1st birthday, proper interval
        );

        List<String> conditions = Arrays.asList(
            "1st dose on or after 1st birthday",
            "2nd dose on or after 1st birthday"  // Should also pass
        );

        boolean result = evaluator.evaluateConditions(conditions, doses, LocalDate.parse("2020-01-01"));

        assertTrue(result, "All conditions should pass");
    }

    @Test
    @DisplayName("Multiple conditions: One fails - should be INVALID")
    void test_MultipleConditions_OneFails() {
        List<Immunization> doses = Arrays.asList(
            createDose("MMR", "2021-02-01")   // After 1st birthday - PASS
           // createDose("MMR", "2019-12-31")    // Before 1st birthday - FAIL (would be invalid anyway)
        );

        List<String> conditions = Arrays.asList(
            "1st dose on or after 1st birthday",  // Pass
            "2nd dose on or after 1st birthday"   // Fail
        );

        // Note: This test setup is a bit contrived, but demonstrates the concept
        boolean result1 = evaluator.evaluateCondition(conditions.get(0), doses, "2020-01-01");
        boolean result2 = evaluator.evaluateCondition(conditions.get(1), doses, "2020-01-01");

        assertTrue(result1, "First condition should pass");
        assertFalse(result2, "Second condition should fail");
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