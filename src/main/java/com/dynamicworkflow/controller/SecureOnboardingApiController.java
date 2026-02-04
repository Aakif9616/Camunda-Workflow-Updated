package com.dynamicworkflow.controller;

import com.dynamicworkflow.service.JobApplicationService;
import com.dynamicworkflow.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/secure-onboarding")
public class SecureOnboardingApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureOnboardingApiController.class);
    
    private final JobApplicationService jobApplicationService;
    private final EmailService emailService;
    
    public SecureOnboardingApiController(JobApplicationService jobApplicationService, EmailService emailService) {
        this.jobApplicationService = jobApplicationService;
        this.emailService = emailService;
    }
    
    /**
     * API endpoint to get application info using secure session token
     */
    @GetMapping("/application-info")
    public ResponseEntity<Map<String, Object>> getSecureApplicationInfo(@RequestParam String session) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Validate session token using EmailService
            String applicationId = emailService.getApplicationIdFromToken(session);
            String email = emailService.getEmailFromToken(session);
            
            if (applicationId == null || email == null) {
                response.put("success", false);
                response.put("error", "Invalid or expired session");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get application data
            Map<String, Object> applicationData = jobApplicationService.getApplicationById(applicationId);
            if (applicationData == null) {
                response.put("success", false);
                response.put("error", "Application not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify email matches
            String applicationEmail = (String) applicationData.get("email");
            if (!email.equals(applicationEmail)) {
                response.put("success", false);
                response.put("error", "Access denied");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("data", applicationData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting secure application info", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to load application information");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API endpoint to complete onboarding using secure session token
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> completeSecureOnboarding(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            String session = (String) request.get("session");
            @SuppressWarnings("unchecked")
            Map<String, Object> onboardingData = (Map<String, Object>) request.get("onboardingData");
            
            if (session == null || onboardingData == null) {
                response.put("success", false);
                response.put("error", "Invalid request data");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate session token using EmailService
            String applicationId = emailService.getApplicationIdFromToken(session);
            String email = emailService.getEmailFromToken(session);
            
            if (applicationId == null || email == null) {
                response.put("success", false);
                response.put("error", "Invalid or expired session");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Complete onboarding through JobApplicationService
            Map<String, Object> result = jobApplicationService.completeOnboarding(applicationId, onboardingData);
            
            // Remove token mapping after successful completion
            emailService.removeTokenMapping(session);
            
            response.put("success", true);
            response.put("message", "Onboarding completed successfully!");
            response.put("data", result);
            
            logger.info("Secure onboarding completed for application: {}", applicationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error completing secure onboarding", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to complete onboarding: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}