package com.immunization.validator.controller;

import com.immunization.validator.model.*;
import com.immunization.validator.service.ValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for immunization validation endpoints.
 * Provides single patient and batch validation capabilities.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validate")
@RequiredArgsConstructor
@Validated
public class ValidationController {
    
    private final ValidationService validationService;
    
    /**
     * Validate a single patient's immunization record.
     * 
     * @param patient Patient to validate
     * @param state State code to validate against
     * @param age Age in years (optional if patient has birthDate)
     * @param schoolYear School year identifier (alternative to age)
     * @param responseMode Response mode: "simple" or "detailed"
     * @param request HTTP request for logging
     * @return Validation response
     */
    @PostMapping("/single")
    public ResponseEntity<ValidationResponse> validateSingle(
            @Valid @RequestBody Patient patient,
            @RequestParam("state") String state,
            @RequestParam(value = "age", required = false) Integer age,
            @RequestParam(value = "schoolYear", required = false) String schoolYear,
            @RequestParam(value = "responseMode", defaultValue = "simple") String responseMode,
            HttpServletRequest request) {
        
        logRequest(request, patient.getId(), responseMode);
        
        boolean detailedResponse = "detailed".equalsIgnoreCase(responseMode);
        ValidationResponse response = validationService.validate(
                patient, state, age, schoolYear, detailedResponse);
        
        logResponse(patient.getId(), response.getValid(), responseMode, null);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate multiple patients' immunization records in a batch.
     * 
     * @param batchRequest Batch validation request containing patients and parameters
     * @param request HTTP request for logging
     * @return Batch validation response
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchValidationResponse> validateBatch(
            @Valid @RequestBody BatchValidationRequest batchRequest,
            HttpServletRequest request) {
        
        String firstPatientId = batchRequest.getPatients() != null && 
                               !batchRequest.getPatients().isEmpty() ?
                               batchRequest.getPatients().get(0).getId() : "unknown";
        
        logRequest(request, firstPatientId, batchRequest.getResponseMode());
        log.info("Batch validation: {} patients, state: {}, age: {}, schoolYear: {}, mode: {}",
                batchRequest.getPatients() != null ? batchRequest.getPatients().size() : 0,
                batchRequest.getState(),
                batchRequest.getAge(),
                batchRequest.getSchoolYear(),
                batchRequest.getResponseMode());
        
        boolean detailedResponse = "detailed".equalsIgnoreCase(batchRequest.getResponseMode());
        
        List<ValidationResponse> results = batchRequest.getPatients().stream()
                .map(patient -> validationService.validate(
                        patient,
                        batchRequest.getState(),
                        batchRequest.getAge(),
                        batchRequest.getSchoolYear(),
                        detailedResponse))
                .collect(Collectors.toList());
        
        BatchValidationResponse response = BatchValidationResponse.builder()
                .results(results)
                .build();
        
        long validCount = results.stream().mapToLong(r -> Boolean.TRUE.equals(r.getValid()) ? 1 : 0).sum();
        logResponse(firstPatientId, validCount == results.size(), batchRequest.getResponseMode(), 
                   String.format("%d/%d valid", validCount, results.size()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
    
    /**
     * Log incoming request with privacy protection.
     * 
     * @param request HTTP request
     * @param patientId Patient identifier (will be masked)
     * @param responseMode Response mode used
     */
    private void logRequest(HttpServletRequest request, String patientId, String responseMode) {
        String maskedId = maskPatientId(patientId);
        String clientIp = getClientIp(request);
        log.info("Request received - Source: {}, Patient ID: {}, Response Mode: {}, Timestamp: {}",
                clientIp, maskedId, responseMode, System.currentTimeMillis());
    }
    
    /**
     * Log response with privacy protection.
     * 
     * @param patientId Patient identifier (will be masked)
     * @param valid Whether validation was successful
     * @param responseMode Response mode used
     * @param additionalInfo Additional information (e.g., for batch requests)
     */
    private void logResponse(String patientId, Boolean valid, String responseMode, String additionalInfo) {
        String maskedId = maskPatientId(patientId);
        String logMessage = String.format("Response sent - Patient ID: %s, Valid: %s, Response Mode: %s, Timestamp: %d",
                maskedId, valid, responseMode, System.currentTimeMillis());
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            logMessage += ", " + additionalInfo;
        }
        log.info(logMessage);
    }
    
    /**
     * Mask patient identifier for logging.
     * Shows first 4 and last 4 characters, masks the middle.
     * 
     * @param patientId Original patient ID
     * @return Masked patient ID
     */
    private String maskPatientId(String patientId) {
        if (patientId == null || patientId.length() <= 8) {
            return "****";
        }
        int length = patientId.length();
        String start = patientId.substring(0, Math.min(4, length));
        String end = length > 4 ? patientId.substring(length - 4) : "";
        return start + "****" + end;
    }
    
    /**
     * Extract client IP address from request, handling proxies.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Exception handler for validation errors.
     * 
     * @param ex Exception that occurred
     * @param request HTTP request
     * @return Error response
     */
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ValidationResponse> handleException(Exception ex, HttpServletRequest request) {
//        String clientIp = getClientIp(request);
//        log.error("Error processing request - Source: {}, Error: {}, Timestamp: {}",
//                clientIp, ex.getMessage(), System.currentTimeMillis(), ex);
//
//        // Return error response without patient information
//        ValidationResponse errorResponse = ValidationResponse.builder()
//                .valid(false)
//                .build();
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.warn("Missing required parameter - Source: {}, Parameter: {}, Timestamp: {}",
                clientIp, ex.getParameterName(), System.currentTimeMillis());

        ValidationResponse errorResponse = ValidationResponse.builder()
                .valid(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ValidationResponse> handleBadRequest(
            Exception ex, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.warn("Invalid request - Source: {}, Error: {}, Timestamp: {}",
                clientIp, ex.getMessage(), System.currentTimeMillis());

        ValidationResponse errorResponse = ValidationResponse.builder()
                .valid(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ValidationResponse> handleException(
            Exception ex, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.error("Error processing request - Source: {}, Error: {}, Timestamp: {}",
                clientIp, ex.getMessage(), System.currentTimeMillis(), ex);

        ValidationResponse errorResponse = ValidationResponse.builder()
                .valid(false)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

