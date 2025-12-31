package com.immunization.validator;

import com.immunization.validator.model.*;
import com.immunization.validator.service.RequirementsService;
import com.immunization.validator.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;

/**
 * Unit tests for ValidationService.
 */
class ValidationServiceTest {
    
    private RequirementsService requirementsService;
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        requirementsService = mock(RequirementsService.class);
        validationService = new ValidationService(requirementsService);
    }
    
    @Test
    void testValidPatient() {
        // Arrange
        List<ValidationRequirement> requirements = Arrays.asList(
                ValidationRequirement.builder()
                        .vaccineCode("DTaP")
                        .minDoses(2)
                        .description("DTaP vaccine required")
                        .build()
        );
        
        // Mock to return requirements for any age >= 5 (since we're testing nearest lower age logic)
        when(requirementsService.getRequirements(eq("CA"), intThat(age -> age >= 5))).thenReturn(requirements);
        
        Patient patient = Patient.builder()
                .id("test-patient-1")
                .birthDate("2018-01-01")
                .immunizations(Arrays.asList(
                        Immunization.builder()
                                .vaccineCode("DTaP")
                                .occurrenceDateTime("2018-06-01")
                                .doseNumber(1)
                                .build(),
                        Immunization.builder()
                                .vaccineCode("DTaP")
                                .occurrenceDateTime("2019-06-01")
                                .doseNumber(2)
                                .build()
                ))
                .build();
        
        // Act
        ValidationResponse response = validationService.validate(
                patient, "CA", null, null, false);
        
        // Assert
        assertTrue(response.getValid());
        assertNull(response.getUnmetRequirements());
    }
    
    @Test
    void testInvalidPatient_MissingDoses() {
        // Arrange
        List<ValidationRequirement> requirements = Arrays.asList(
                ValidationRequirement.builder()
                        .vaccineCode("DTaP")
                        .minDoses(5)
                        .description("DTaP - 5 doses required")
                        .build()
        );
        
        // Mock to return requirements for any age >= 5
        when(requirementsService.getRequirements(eq("CA"), intThat(age -> age >= 5))).thenReturn(requirements);
        
        Patient patient = Patient.builder()
                .id("test-patient-2")
                .birthDate("2018-01-01")
                .immunizations(Arrays.asList(
                        Immunization.builder()
                                .vaccineCode("DTaP")
                                .occurrenceDateTime("2018-06-01")
                                .doseNumber(1)
                                .build()
                ))
                .build();
        
        // Act
        ValidationResponse response = validationService.validate(
                patient, "CA", null, null, true);
        
        // Assert
        assertFalse(response.getValid());
        assertNotNull(response.getUnmetRequirements());
        assertEquals(1, response.getUnmetRequirements().size());
        assertEquals("DTaP", response.getUnmetRequirements().get(0).getVaccineCode());
        assertEquals(5, response.getUnmetRequirements().get(0).getRequiredDoses());
        assertEquals(1, response.getUnmetRequirements().get(0).getFoundDoses());
    }
    
    @Test
    void testPatientWithNoImmunizations() {
        // Arrange
        List<ValidationRequirement> requirements = Arrays.asList(
                ValidationRequirement.builder()
                        .vaccineCode("DTaP")
                        .minDoses(2)
                        .description("DTaP vaccine required")
                        .build()
        );
        
        // Mock to return requirements for any age >= 5
        when(requirementsService.getRequirements(eq("CA"), intThat(age -> age >= 5))).thenReturn(requirements);
        
        Patient patient = Patient.builder()
                .id("test-patient-3")
                .birthDate("2018-01-01")
                .immunizations(Collections.emptyList())
                .build();
        
        // Act
        ValidationResponse response = validationService.validate(
                patient, "CA", null, null, true);
        
        // Assert
        assertFalse(response.getValid());
        assertNotNull(response.getUnmetRequirements());
        assertEquals(1, response.getUnmetRequirements().size());
        assertEquals(0, response.getUnmetRequirements().get(0).getFoundDoses());
    }
    
    @Test
    void testSchoolYearBasedValidation() {
        // Arrange
        List<ValidationRequirement> requirements = Arrays.asList(
                ValidationRequirement.builder()
                        .vaccineCode("Tdap")
                        .minDoses(1)
                        .description("Tdap booster required for 7th grade")
                        .build()
        );
        
        when(requirementsService.getRequirements("TX", "7th Grade")).thenReturn(requirements);
        
        Patient patient = Patient.builder()
                .id("test-patient-4")
                .immunizations(Arrays.asList(
                        Immunization.builder()
                                .vaccineCode("Tdap")
                                .occurrenceDateTime("2020-09-01")
                                .doseNumber(1)
                                .build()
                ))
                .build();
        
        // Act
        ValidationResponse response = validationService.validate(
                patient, "TX", null, "7th Grade", false);
        
        // Assert
        assertTrue(response.getValid());
    }
}

