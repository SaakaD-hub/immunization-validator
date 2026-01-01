package com.immunization.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immunization.validator.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Health check endpoint - should return OK")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/validate/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
    
    @Test
    @DisplayName("Single validation - valid patient - returns 200")
    void testSingleValidation_ValidPatient_Returns200() throws Exception {
        Patient patient = Patient.builder()
                .id("test-patient-001")
                .birthDate("2019-01-01")
                .immunizations(List.of(
                    Immunization.builder()
                        .vaccineCode("DTaP")
                        .occurrenceDateTime("2019-03-01")
                        .build()
                ))
                .build();
        
        mockMvc.perform(post("/api/v1/validate/single")
                .param("state", "CA")
                .param("age", "5")
                .param("responseMode", "simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-patient-001"))
                .andExpect(jsonPath("$.valid").exists());
    }
    
    @Test
    @DisplayName("Single validation - detailed mode - returns unmet requirements")
    void testSingleValidation_DetailedMode_ReturnsUnmetRequirements() throws Exception {
        Patient patient = Patient.builder()
                .id("test-patient-002")
                .birthDate("2019-01-01")
                .immunizations(List.of())
                .build();
        
        mockMvc.perform(post("/api/v1/validate/single")
                .param("state", "CA")
                .param("age", "5")
                .param("responseMode", "detailed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-patient-002"))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.unmetRequirements").isArray());
    }
    
    @Test
    @DisplayName("Batch validation - multiple patients - returns all results")
    void testBatchValidation_MultiplePatients_ReturnsAllResults() throws Exception {
        BatchValidationRequest request = BatchValidationRequest.builder()
                .state("CA")
                .age(5)
                .responseMode("simple")
                .patients(List.of(
                    Patient.builder()
                        .id("batch-patient-001")
                        .birthDate("2019-01-01")
                        .immunizations(List.of())
                        .build(),
                    Patient.builder()
                        .id("batch-patient-002")
                        .birthDate("2019-01-01")
                        .immunizations(List.of())
                        .build()
                ))
                .build();
        
        mockMvc.perform(post("/api/v1/validate/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2));
    }
}