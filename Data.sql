-- Massachusetts Immunization Validator Database Schema
-- PostgreSQL 15+
-- Author: David Saaka
-- Date: January 12, 2026

-- Drop tables if they exist (for clean reinstall)
DROP TABLE IF EXISTS validation_history CASCADE;
DROP TABLE IF EXISTS exemptions CASCADE;
DROP TABLE IF EXISTS immunizations CASCADE;
DROP TABLE IF EXISTS patients CASCADE;

-- =============================================================================
-- PATIENTS TABLE
-- =============================================================================
CREATE TABLE patients (
                          id VARCHAR(50) PRIMARY KEY,
                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL,
                          birth_date DATE NOT NULL,
                          state VARCHAR(2) NOT NULL,
                          age INTEGER,
                          school_year VARCHAR(50),
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT chk_state_code CHECK (LENGTH(state) = 2),
                          CONSTRAINT chk_age_range CHECK (age IS NULL OR (age >= 0 AND age <= 120))
);

CREATE INDEX idx_patients_state ON patients(state);
CREATE INDEX idx_patients_birth_date ON patients(birth_date);
CREATE INDEX idx_patients_created_at ON patients(created_at);

COMMENT ON TABLE patients IS 'Stores patient demographic information';
COMMENT ON COLUMN patients.id IS 'Patient identifier (e.g., PAT-001)';
COMMENT ON COLUMN patients.birth_date IS 'Patient date of birth in ISO format';
COMMENT ON COLUMN patients.state IS 'Two-letter state code (e.g., MA, CA)';
COMMENT ON COLUMN patients.age IS 'Current age in years (can be null)';
COMMENT ON COLUMN patients.school_year IS 'School year/grade (e.g., Kindergarten, 7th Grade)';

-- =============================================================================
-- IMMUNIZATIONS TABLE
-- =============================================================================
CREATE TABLE immunizations (
                               id BIGSERIAL PRIMARY KEY,
                               patient_id VARCHAR(50) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                               vaccine_code VARCHAR(50) NOT NULL,
                               occurrence_date_time DATE NOT NULL,
                               dose_number INTEGER,
                               vaccine_product VARCHAR(100),
                               lot_number VARCHAR(50),
                               site VARCHAR(50),
                               route VARCHAR(50),
                               notes TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT chk_dose_number CHECK (dose_number IS NULL OR dose_number > 0),
                               CONSTRAINT chk_occurrence_date CHECK (occurrence_date_time <= CURRENT_DATE)
);

CREATE INDEX idx_immunizations_patient ON immunizations(patient_id);
CREATE INDEX idx_immunizations_vaccine ON immunizations(vaccine_code);
CREATE INDEX idx_immunizations_date ON immunizations(occurrence_date_time);

COMMENT ON TABLE immunizations IS 'Stores immunization/vaccination records for patients';
COMMENT ON COLUMN immunizations.vaccine_code IS 'Vaccine type (e.g., DTaP, MMR, MenACWY)';
COMMENT ON COLUMN immunizations.occurrence_date_time IS 'Date vaccine was administered';
COMMENT ON COLUMN immunizations.dose_number IS 'Dose sequence number (1, 2, 3, etc.)';
COMMENT ON COLUMN immunizations.vaccine_product IS 'Specific vaccine product name';
COMMENT ON COLUMN immunizations.lot_number IS 'Vaccine lot number';
COMMENT ON COLUMN immunizations.site IS 'Injection site (e.g., left deltoid)';
COMMENT ON COLUMN immunizations.route IS 'Route of administration (e.g., intramuscular)';

-- =============================================================================
-- EXEMPTIONS TABLE
-- =============================================================================
CREATE TABLE exemptions (
                            id BIGSERIAL PRIMARY KEY,
                            patient_id VARCHAR(50) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                            vaccine_code VARCHAR(50) NOT NULL,
                            exception_type VARCHAR(50) NOT NULL,
                            description TEXT,
                            effective_date DATE,
                            expiration_date DATE,
                            physician_name VARCHAR(200),
                            physician_signature VARCHAR(200),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT chk_exemption_type CHECK (exception_type IN (
                                                                                    'MEDICAL_CONTRAINDICATION',
                                                                                    'RELIGIOUS_BELIEF',
                                                                                    'PHILOSOPHICAL_BELIEF',
                                                                                    'LABORATORY_EVIDENCE',
                                                                                    'RELIABLE_HISTORY',
                                                                                    'BIRTH_BEFORE_1957',
                                                                                    'BIRTH_BEFORE_1980',
                                                                                    'SIGNED_WAIVER'
                                )),
                            CONSTRAINT chk_exemption_dates CHECK (
                                expiration_date IS NULL OR
                                effective_date IS NULL OR
                                expiration_date >= effective_date
                                )
);

CREATE INDEX idx_exemptions_patient ON exemptions(patient_id);
CREATE INDEX idx_exemptions_vaccine ON exemptions(vaccine_code);
CREATE INDEX idx_exemptions_type ON exemptions(exception_type);

COMMENT ON TABLE exemptions IS 'Stores medical, religious, and other exemptions for vaccines';
COMMENT ON COLUMN exemptions.exception_type IS 'Type of exemption (e.g., MEDICAL_CONTRAINDICATION)';
COMMENT ON COLUMN exemptions.effective_date IS 'Date exemption becomes effective';
COMMENT ON COLUMN exemptions.expiration_date IS 'Date exemption expires (null if permanent)';
COMMENT ON COLUMN exemptions.physician_name IS 'Name of physician granting medical exemption';

-- =============================================================================
-- VALIDATION_HISTORY TABLE
-- =============================================================================
CREATE TABLE validation_history (
                                    id BIGSERIAL PRIMARY KEY,
                                    patient_id VARCHAR(50) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                                    validation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    state VARCHAR(2) NOT NULL,
                                    age INTEGER,
                                    school_year VARCHAR(50),
                                    grade_level VARCHAR(50),
                                    response_mode VARCHAR(20) NOT NULL,
                                    valid BOOLEAN NOT NULL,
                                    unmet_requirements JSONB,
                                    immunization_count INTEGER NOT NULL DEFAULT 0,
                                    exemption_count INTEGER NOT NULL DEFAULT 0,
                                    validation_duration_ms INTEGER,

                                    CONSTRAINT chk_response_mode CHECK (response_mode IN ('simple', 'detailed'))
);

CREATE INDEX idx_validation_history_patient ON validation_history(patient_id);
CREATE INDEX idx_validation_history_date ON validation_history(validation_date);
CREATE INDEX idx_validation_history_state ON validation_history(state);
CREATE INDEX idx_validation_history_valid ON validation_history(valid);
CREATE INDEX idx_validation_history_unmet ON validation_history USING GIN (unmet_requirements);

COMMENT ON TABLE validation_history IS 'Audit trail of all validation requests';
COMMENT ON COLUMN validation_history.valid IS 'Whether patient met all requirements';
COMMENT ON COLUMN validation_history.unmet_requirements IS 'JSON array of unmet requirements';
COMMENT ON COLUMN validation_history.validation_duration_ms IS 'Time taken to validate (milliseconds)';

-- =============================================================================
-- VIEWS
-- =============================================================================

-- View: Patient Summary with Counts
CREATE OR REPLACE VIEW patient_summary AS
SELECT
    p.id,
    p.first_name,
    p.last_name,
    p.birth_date,
    p.state,
    p.age,
    p.school_year,
    COUNT(DISTINCT i.id) AS immunization_count,
    COUNT(DISTINCT e.id) AS exemption_count,
    MAX(vh.validation_date) AS last_validation_date,
    MAX(vh.valid) AS last_validation_result
FROM patients p
         LEFT JOIN immunizations i ON p.id = i.patient_id
         LEFT JOIN exemptions e ON p.id = e.patient_id
         LEFT JOIN validation_history vh ON p.id = vh.patient_id
GROUP BY p.id, p.first_name, p.last_name, p.birth_date, p.state, p.age, p.school_year;

COMMENT ON VIEW patient_summary IS 'Summary view showing patient counts and last validation';

-- View: Validation Statistics by State
CREATE OR REPLACE VIEW validation_stats_by_state AS
SELECT
    state,
    COUNT(*) AS total_validations,
    SUM(CASE WHEN valid THEN 1 ELSE 0 END) AS valid_count,
    SUM(CASE WHEN NOT valid THEN 1 ELSE 0 END) AS invalid_count,
    ROUND(100.0 * SUM(CASE WHEN valid THEN 1 ELSE 0 END) / COUNT(*), 2) AS compliance_rate,
    AVG(validation_duration_ms) AS avg_duration_ms
FROM validation_history
GROUP BY state;

COMMENT ON VIEW validation_stats_by_state IS 'Validation statistics aggregated by state';

-- =============================================================================
-- FUNCTIONS
-- =============================================================================

-- Function: Update patient updated_at timestamp
CREATE OR REPLACE FUNCTION update_patient_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Auto-update patient timestamp
CREATE TRIGGER trigger_update_patient_timestamp
    BEFORE UPDATE ON patients
    FOR EACH ROW
    EXECUTE FUNCTION update_patient_timestamp();

-- Function: Calculate patient age from birth date
CREATE OR REPLACE FUNCTION calculate_age(birth_date DATE)
RETURNS INTEGER AS $$
BEGIN
RETURN EXTRACT(YEAR FROM AGE(CURRENT_DATE, birth_date));
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_age IS 'Calculates age in years from birth date';

-- =============================================================================
-- SEED DATA FOR TESTING
-- =============================================================================

-- Test Patient 1: Sarah Johnson (Under-Vaccinated)
INSERT INTO patients (id, first_name, last_name, birth_date, state, age, school_year)
VALUES ('PAT-001', 'Sarah', 'Johnson', '2019-01-01', 'MA', 5, 'Kindergarten');

INSERT INTO immunizations (patient_id, vaccine_code, occurrence_date_time, dose_number)
VALUES
    ('PAT-001', 'DTaP', '2019-03-01', 1),
    ('PAT-001', 'DTaP', '2019-05-01', 2),
    ('PAT-001', 'DTaP', '2019-07-01', 3),
    ('PAT-001', 'DTaP', '2022-11-01', 4);  -- Before 4th birthday!

-- Test Patient 2: Michael Chen (Fully Vaccinated)
INSERT INTO patients (id, first_name, last_name, birth_date, state, age, school_year)
VALUES ('PAT-002', 'Michael', 'Chen', '2019-01-01', 'MA', 5, 'Kindergarten');

INSERT INTO immunizations (patient_id, vaccine_code, occurrence_date_time, dose_number)
VALUES
    ('PAT-002', 'DTaP', '2019-03-01', 1),
    ('PAT-002', 'DTaP', '2019-05-01', 2),
    ('PAT-002', 'DTaP', '2019-07-01', 3),
    ('PAT-002', 'DTaP', '2020-01-01', 4),
    ('PAT-002', 'DTaP', '2023-02-01', 5),
    ('PAT-002', 'Polio', '2019-03-01', 1),
    ('PAT-002', 'Polio', '2019-05-01', 2),
    ('PAT-002', 'Polio', '2019-07-01', 3),
    ('PAT-002', 'Polio', '2023-02-01', 4),
    ('PAT-002', 'MMR', '2020-02-01', 1),
    ('PAT-002', 'MMR', '2020-03-01', 2),
    ('PAT-002', 'Varicella', '2020-02-01', 1),
    ('PAT-002', 'Varicella', '2020-03-01', 2),
    ('PAT-002', 'HepB', '2019-01-15', 1),
    ('PAT-002', 'HepB', '2019-03-15', 2),
    ('PAT-002', 'HepB', '2019-09-15', 3);

-- Test Patient 3: Emma Rodriguez (Medical Exemptions)
INSERT INTO patients (id, first_name, last_name, birth_date, state, age, school_year)
VALUES ('PAT-003', 'Emma', 'Rodriguez', '2019-06-15', 'MA', 5, 'Kindergarten');

INSERT INTO exemptions (patient_id, vaccine_code, exception_type, description, physician_name)
VALUES
    ('PAT-003', 'DTaP', 'MEDICAL_CONTRAINDICATION', 'Severe allergic reaction to previous dose', 'Dr. Smith'),
    ('PAT-003', 'Polio', 'MEDICAL_CONTRAINDICATION', 'Immunocompromised patient', 'Dr. Smith'),
    ('PAT-003', 'MMR', 'MEDICAL_CONTRAINDICATION', 'Severe egg allergy', 'Dr. Smith'),
    ('PAT-003', 'HepB', 'MEDICAL_CONTRAINDICATION', 'Severe allergic reaction to vaccine component', 'Dr. Smith');

-- Test Patient 4: Olivia Martinez (Dose on Birthday)
INSERT INTO patients (id, first_name, last_name, birth_date, state, age, school_year)
VALUES ('PAT-004', 'Olivia', 'Martinez', '2019-01-01', 'MA', 5, 'Kindergarten');

INSERT INTO immunizations (patient_id, vaccine_code, occurrence_date_time, dose_number)
VALUES
    ('PAT-004', 'DTaP', '2019-03-01', 1),
    ('PAT-004', 'DTaP', '2019-05-01', 2),
    ('PAT-004', 'DTaP', '2019-07-01', 3),
    ('PAT-004', 'DTaP', '2023-01-01', 4);  -- Exactly on 4th birthday

-- =============================================================================
-- GRANTS (Adjust as needed for your application user)
-- =============================================================================

-- Grant permissions to application user (replace 'app_user' with your actual username)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;
-- GRANT SELECT ON ALL VIEWS IN SCHEMA public TO app_user;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================

-- Count records
SELECT 'patients' AS table_name, COUNT(*) AS count FROM patients
UNION ALL
SELECT 'immunizations', COUNT(*) FROM immunizations
UNION ALL
SELECT 'exemptions', COUNT(*) FROM exemptions
UNION ALL
SELECT 'validation_history', COUNT(*) FROM validation_history;

-- Patient summary
SELECT * FROM patient_summary ORDER BY id;

-- Display schema info
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
ORDER BY table_name, ordinal_position;

COMMIT;