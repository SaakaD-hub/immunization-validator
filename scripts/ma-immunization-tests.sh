# Massachusetts Immunization Validator - HTTPie Test Commands
# HTTPie is a user-friendly HTTP client (https://httpie.io/)
# Install: pip install httpie (or brew install httpie on Mac)

# ============================================================================
# BASIC SYNTAX
# ============================================================================
# HTTPie syntax: http METHOD URL key=value
# Query params: http GET url param==value
# JSON body: http POST url key=value
# ============================================================================

BASE_URL="http://localhost:8080/api/v1/validate"

# ============================================================================
# HEALTH CHECK
# ============================================================================

echo "=== Health Check ==="
http GET $BASE_URL/health

# ============================================================================
# PRESCHOOL REQUIREMENTS (≥2 years)
# ============================================================================

echo ""
echo "=== PRESCHOOL TESTS ==="
echo ""

# Test 1: Preschool Hib - 1 dose (minimum requirement)
echo "Test 1: Preschool Hib - 1 dose"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="preschool-hib-001" \
  birthDate="2022-01-01" \
  immunization:='[
    {"vaccineCode": "Hib", "occurrenceDateTime": "2022-03-01"}
  ]'

# Test 2: Preschool DTaP - 4 doses
echo ""
echo "Test 2: Preschool DTaP - 4 doses"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="preschool-dtap-001" \
  birthDate="2021-06-15" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2021-08-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2021-10-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2021-12-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2022-06-15"}
  ]'

# Test 3: Preschool MMR - Dose EXACTLY ON 1st birthday (Edge case - should PASS)
echo ""
echo "Test 3: Preschool MMR - ON 1st birthday (Edge Case)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="preschool-mmr-edge" \
  birthDate="2020-01-15" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2021-01-15"}
  ]'

# Test 4: Preschool MMR - Dose BEFORE 1st birthday (should FAIL)
echo ""
echo "Test 4: Preschool MMR - BEFORE 1st birthday (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  responseMode==detailed \
  id="preschool-mmr-early" \
  birthDate="2020-01-15" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2021-01-14"}
  ]'

# Test 5: Preschool MMR - Dose AFTER 1st birthday (should PASS)
echo ""
echo "Test 5: Preschool MMR - AFTER 1st birthday"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="preschool-mmr-after" \
  birthDate="2020-01-15" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2021-02-20"}
  ]'

# Test 6: Preschool - Religious exemption (0 doses but should PASS)
echo ""
echo "Test 6: Preschool - Religious Exemption (0 doses)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="preschool-exemption" \
  birthDate="2021-01-01" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "DTaP", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "MMR", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "Varicella", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "HepB", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "Polio", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "Hib", "exceptionType": "RELIGIOUS_EXEMPTION"}
  ]'

# Test 7: Preschool - Fully compliant student
echo ""
echo "Test 7: Preschool - Fully Compliant Student"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="preschool-compliant" \
  birthDate="2021-06-15" \
  immunization:='[
    {"vaccineCode": "Hib", "occurrenceDateTime": "2021-08-15"},
    {"vaccineCode": "Hib", "occurrenceDateTime": "2021-10-15"},
    {"vaccineCode": "Hib", "occurrenceDateTime": "2021-12-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2021-08-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2021-10-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2021-12-15"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2022-06-15"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2021-08-15"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2021-10-15"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2021-12-15"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2021-06-16"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2021-07-16"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2021-12-16"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2022-07-01"},
    {"vaccineCode": "Varicella", "occurrenceDateTime": "2022-07-01"}
  ]'

# ============================================================================
# KINDERGARTEN - GRADE 6 REQUIREMENTS (≥5 years)
# ============================================================================

echo ""
echo "=== K-6 TESTS ==="
echo ""

# Test 8: 🔴 CRITICAL - Sarah Johnson Bug (4th dose BEFORE 4th birthday)
echo "Test 8: 🔴 CRITICAL - Sarah Johnson Bug (4th dose BEFORE 4th birthday)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  responseMode==detailed \
  id="SARAH-JOHNSON" \
  birthDate="2019-01-01" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-07-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2022-11-01"}
  ]'

# Test 9: K-6 DTaP - 4th dose EXACTLY ON 4th birthday (Edge case - should PASS)
echo ""
echo "Test 9: K-6 DTaP - 4th dose ON 4th birthday (Edge Case)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-dtap-edge" \
  birthDate="2019-01-01" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-07-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2023-01-01"}
  ]'

# Test 10: K-6 DTaP - 4th dose AFTER 4th birthday (should PASS)
echo ""
echo "Test 10: K-6 DTaP - 4th dose AFTER 4th birthday"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-dtap-after" \
  birthDate="2019-01-01" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-07-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2023-06-15"}
  ]'

# Test 11: K-6 DTaP - 5 doses (Primary requirement - always valid)
echo ""
echo "Test 11: K-6 DTaP - 5 doses (Primary Requirement)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-dtap-5doses" \
  birthDate="2019-01-01" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-07-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2020-04-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2023-06-01"}
  ]'

# Test 12: K-6 MMR - EXACTLY 28 days between doses (Edge case - should PASS)
echo ""
echo "Test 12: K-6 MMR - Exactly 28 days interval (Edge Case)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-mmr-edge" \
  birthDate="2018-01-15" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2019-02-01"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2019-03-01"}
  ]'

# Test 13: K-6 MMR - Only 14 days between doses (should FAIL)
echo ""
echo "Test 13: K-6 MMR - Only 14 days interval (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  responseMode==detailed \
  id="k6-mmr-short" \
  birthDate="2018-01-15" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2019-02-01"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2019-02-15"}
  ]'

# Test 14: K-6 MMR - More than 28 days (should PASS)
echo ""
echo "Test 14: K-6 MMR - 60 days interval"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-mmr-long" \
  birthDate="2018-01-15" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2019-02-01"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2019-04-01"}
  ]'

# Test 15: K-6 Varicella - Exactly 28 days (should PASS)
echo ""
echo "Test 15: K-6 Varicella - Exactly 28 days interval"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-varicella-edge" \
  birthDate="2018-01-15" \
  immunization:='[
    {"vaccineCode": "Varicella", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "Varicella", "occurrenceDateTime": "2019-03-29"}
  ]'

# Test 16: K-6 Polio - 4 doses with BOTH date AND interval conditions (Complex - should PASS)
echo ""
echo "Test 16: K-6 Polio - 4 doses (COMPLEX: date + interval)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-polio-complex" \
  birthDate="2018-03-01" \
  immunization:='[
    {"vaccineCode": "Polio", "occurrenceDateTime": "2018-05-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2018-07-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2019-01-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2022-08-15"}
  ]'

# Test 17: K-6 Polio - 3 doses (Alternate requirement - should PASS)
echo ""
echo "Test 17: K-6 Polio - 3 doses (Alternate)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="k6-polio-alt" \
  birthDate="2018-03-01" \
  immunization:='[
    {"vaccineCode": "Polio", "occurrenceDateTime": "2018-05-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2018-07-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2022-08-15"}
  ]'

# Test 18: K-6 Polio - 4 doses but 4th BEFORE 4th birthday (should FAIL)
echo ""
echo "Test 18: K-6 Polio - 4th dose BEFORE 4th birthday (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  responseMode==detailed \
  id="k6-polio-fail" \
  birthDate="2018-03-01" \
  immunization:='[
    {"vaccineCode": "Polio", "occurrenceDateTime": "2018-05-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2018-07-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2019-01-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2022-01-01"}
  ]'

# Test 19: K-6 - Fully compliant student
echo ""
echo "Test 19: K-6 - Fully Compliant Student (Michael Chen)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="MICHAEL-CHEN" \
  birthDate="2019-01-01" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-07-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2020-04-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2023-02-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2019-07-01"},
    {"vaccineCode": "Polio", "occurrenceDateTime": "2023-02-01"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2019-01-15"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2019-02-15"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2019-07-15"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2020-02-01"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2020-03-15"},
    {"vaccineCode": "Varicella", "occurrenceDateTime": "2020-02-01"},
    {"vaccineCode": "Varicella", "occurrenceDateTime": "2020-03-15"}
  ]'

# ============================================================================
# GRADES 7-10 REQUIREMENTS (≥12 years)
# ============================================================================

echo ""
echo "=== GRADES 7-10 TESTS ==="
echo ""

# Test 20: Grades 7-10 Tdap - 1 dose
echo "Test 20: Grades 7-10 Tdap - 1 dose"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==7-10 \
  id="g7-10-tdap" \
  birthDate="2012-01-15" \
  immunization:='[
    {"vaccineCode": "Tdap", "occurrenceDateTime": "2022-06-01"}
  ]'

# Test 21: Grades 7-10 MenACWY - EXACTLY ON 10th birthday (Edge case - should PASS)
echo ""
echo "Test 21: Grades 7-10 MenACWY - ON 10th birthday (Edge Case)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==7-10 \
  id="g7-10-menacwy-edge" \
  birthDate="2012-05-15" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2022-05-15"}
  ]'

# Test 22: Grades 7-10 MenACWY - BEFORE 10th birthday (should FAIL)
echo ""
echo "Test 22: Grades 7-10 MenACWY - BEFORE 10th birthday (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==7-10 \
  responseMode==detailed \
  id="g7-10-menacwy-early" \
  birthDate="2012-05-15" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2022-05-14"}
  ]'

# Test 23: Grades 7-10 MenACWY - AFTER 10th birthday (should PASS)
echo ""
echo "Test 23: Grades 7-10 MenACWY - AFTER 10th birthday"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==7-10 \
  id="g7-10-menacwy-after" \
  birthDate="2012-05-15" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2022-09-01"}
  ]'

# Test 24: Grades 7-10 Heplisav-B - 2 doses after 18th birthday (should PASS)
echo ""
echo "Test 24: Grades 7-10 Heplisav-B - 2 doses after 18th birthday"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==7-10 \
  id="g7-10-heplisav" \
  birthDate="2006-09-01" \
  immunization:='[
    {"vaccineCode": "Heplisav-B", "occurrenceDateTime": "2024-09-15"},
    {"vaccineCode": "Heplisav-B", "occurrenceDateTime": "2024-10-15"}
  ]'

# Test 25: Grades 7-10 Heplisav-B - 2 doses but 1st BEFORE 18th birthday (should FAIL)
echo ""
echo "Test 25: Grades 7-10 Heplisav-B - 1st dose BEFORE 18th birthday (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==7-10 \
  responseMode==detailed \
  id="g7-10-heplisav-early" \
  birthDate="2006-09-01" \
  immunization:='[
    {"vaccineCode": "Heplisav-B", "occurrenceDateTime": "2024-08-15"},
    {"vaccineCode": "Heplisav-B", "occurrenceDateTime": "2024-10-15"}
  ]'

# ============================================================================
# GRADES 11-12 REQUIREMENTS
# ============================================================================

echo ""
echo "=== GRADES 11-12 TESTS ==="
echo ""

# Test 26: Grades 11-12 MenACWY - 1 dose after 16th birthday (Alternate - should PASS)
echo "Test 26: Grades 11-12 MenACWY - 1 dose after 16th (Alternate)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==11-12 \
  id="g11-12-menacwy-alt" \
  birthDate="2008-05-10" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2024-09-15"}
  ]'

# Test 27: Grades 11-12 MenACWY - 1 dose BEFORE 16th birthday (should FAIL)
echo ""
echo "Test 27: Grades 11-12 MenACWY - 1 dose BEFORE 16th (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==11-12 \
  responseMode==detailed \
  id="g11-12-menacwy-early" \
  birthDate="2008-05-10" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2024-05-01"}
  ]'

# Test 28: Grades 11-12 MenACWY - 2 doses with proper timing (Primary - should PASS)
echo ""
echo "Test 28: Grades 11-12 MenACWY - 2 doses (Primary)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==11-12 \
  id="g11-12-menacwy-primary" \
  birthDate="2008-03-15" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2020-05-01"},
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2024-08-01"}
  ]'

# Test 29: Grades 11-12 MenACWY - 2 doses, interval too short (should FAIL)
echo ""
echo "Test 29: Grades 11-12 MenACWY - Interval too short (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==11-12 \
  responseMode==detailed \
  id="g11-12-menacwy-fail" \
  birthDate="2008-03-15" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2024-06-01"},
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2024-07-15"}
  ]'

# Test 30: Grades 11-12 MenACWY - EXACTLY 8 weeks (56 days) interval (Edge case - should PASS)
echo ""
echo "Test 30: Grades 11-12 MenACWY - Exactly 8 weeks interval (Edge Case)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==11-12 \
  id="g11-12-menacwy-exact" \
  birthDate="2008-03-15" \
  immunization:='[
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2020-05-01"},
    {"vaccineCode": "MenACWY", "occurrenceDateTime": "2024-06-26"}
  ]'

# ============================================================================
# COLLEGE REQUIREMENTS
# ============================================================================

echo ""
echo "=== COLLEGE TESTS ==="
echo ""

# Test 31: College MMR - Birth before 1957 exemption (should PASS)
echo "Test 31: College MMR - Birth before 1957 exemption"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==college \
  id="college-birth-1956" \
  birthDate="1956-06-15" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "MMR", "exceptionType": "BIRTH_BEFORE_1957"}
  ]'

# Test 32: College Varicella - Birth before 1980 exemption (should PASS)
echo ""
echo "Test 32: College Varicella - Birth before 1980 exemption"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==college \
  id="college-birth-1979" \
  birthDate="1979-03-20" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "Varicella", "exceptionType": "BIRTH_BEFORE_1980"}
  ]'

# Test 33: College MenACWY - Signed waiver (should PASS)
echo ""
echo "Test 33: College MenACWY - Signed waiver"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==college \
  id="college-waiver" \
  birthDate="2005-01-01" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "MenACWY", "exceptionType": "SIGNED_WAIVER", "description": "Declined after reading MDPH waiver"}
  ]'

# ============================================================================
# EDGE CASES & ERROR HANDLING
# ============================================================================

echo ""
echo "=== EDGE CASES & ERROR HANDLING ==="
echo ""

# Test 34: Medical exemption - 0 doses but valid exemption
echo "Test 34: Medical exemption (0 doses)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="medical-exemption" \
  birthDate="2019-01-01" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "DTaP", "exceptionType": "MEDICAL_CONTRAINDICATION", "description": "Severe allergic reaction to pertussis"}
  ]'

# Test 35: Laboratory evidence exemption
echo ""
echo "Test 35: Laboratory evidence exemption"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="lab-evidence" \
  birthDate="2019-01-01" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "HepB", "exceptionType": "LABORATORY_EVIDENCE", "description": "Titer shows immunity"}
  ]'

# Test 36: Reliable history of chickenpox
echo ""
echo "Test 36: Reliable history of chickenpox"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  id="chickenpox-history" \
  birthDate="2019-01-01" \
  immunization:='[]' \
  exceptions:='[
    {"vaccineCode": "Varicella", "exceptionType": "RELIABLE_HISTORY_CHICKENPOX", "description": "Physician-verified chickenpox disease"}
  ]'

# Test 37: Combination - Some doses + some exemptions
echo ""
echo "Test 37: Combination - Doses + Exemptions"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==preschool \
  id="combination" \
  birthDate="2021-01-01" \
  immunization:='[
    {"vaccineCode": "MMR", "occurrenceDateTime": "2022-02-01"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2021-02-01"},
    {"vaccineCode": "HepB", "occurrenceDateTime": "2021-03-01"}
  ]' \
  exceptions:='[
    {"vaccineCode": "DTaP", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "Varicella", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "Polio", "exceptionType": "RELIGIOUS_EXEMPTION"},
    {"vaccineCode": "Hib", "exceptionType": "RELIGIOUS_EXEMPTION"}
  ]'

# Test 38: Multiple vaccine types - partial compliance
echo ""
echo "Test 38: Multiple vaccines - Partial compliance (Should FAIL)"
http POST $BASE_URL/single \
  state==MA \
  schoolYear==K-6 \
  responseMode==detailed \
  id="partial-compliance" \
  birthDate="2019-01-01" \
  immunization:='[
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-03-01"},
    {"vaccineCode": "DTaP", "occurrenceDateTime": "2019-05-01"},
    {"vaccineCode": "MMR", "occurrenceDateTime": "2020-01-15"}
  ]'

echo ""
echo "=== ALL TESTS COMPLETE ==="
echo ""
echo "Total Tests: 38"
echo "Coverage:"
echo "  - Preschool: 7 tests"
echo "  - K-6: 12 tests"
echo "  - Grades 7-10: 6 tests"
echo "  - Grades 11-12: 5 tests"
echo "  - College: 3 tests"
echo "  - Edge Cases: 5 tests"
echo ""