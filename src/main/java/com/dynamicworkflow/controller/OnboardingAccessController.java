package com.dynamicworkflow.controller;

import com.dynamicworkflow.service.JobApplicationService;
import com.dynamicworkflow.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/onboarding-access")
public class OnboardingAccessController {
    
    private static final Logger logger = LoggerFactory.getLogger(OnboardingAccessController.class);
    
    private final JobApplicationService jobApplicationService;
    private final EmailService emailService;
    
    public OnboardingAccessController(JobApplicationService jobApplicationService, EmailService emailService) {
        this.jobApplicationService = jobApplicationService;
        this.emailService = emailService;
    }
    
    /**
     * Handle onboarding access via email token
     */
    @GetMapping
    public String handleOnboardingAccess(@RequestParam String token, Model model) {
        try {
            // Validate token using EmailService
            String applicationId = emailService.getApplicationIdFromToken(token);
            String email = emailService.getEmailFromToken(token);
            
            if (applicationId == null || email == null) {
                logger.warn("Invalid onboarding token accessed: {}", token);
                return "redirect:/onboarding-error.html?error=Invalid or expired onboarding link";
            }
            
            // Get application data
            Map<String, Object> applicationData = jobApplicationService.getApplicationById(applicationId);
            if (applicationData == null) {
                return "redirect:/onboarding-error.html?error=Application not found";
            }
            
            // Verify email matches
            String applicationEmail = (String) applicationData.get("email");
            if (!email.equals(applicationEmail)) {
                logger.warn("Email mismatch for onboarding token: {} vs {}", email, applicationEmail);
                return "redirect:/onboarding-error.html?error=Access denied";
            }
            
            // Check if application is in correct state for onboarding
            String status = (String) applicationData.get("applicationStatus");
            logger.info("Onboarding access attempt for application {} with status: {}", applicationId, status);
            
            if (!"HIRED".equals(status) && 
                !"PENDING_ONBOARDING".equals(status) && 
                !"ACCEPTED".equals(status) &&
                !"ONBOARDING_INITIATED".equals(status) &&
                !"PENDING_HR_HIRING".equals(status)) {
                logger.warn("Application {} not ready for onboarding. Current status: {}", applicationId, status);
                return "redirect:/onboarding-error.html?error=Application is not ready for onboarding. Current status: " + status;
            }
            
            // Redirect to onboarding form with secure session
            return "redirect:/secure-onboarding?session=" + token;
            
        } catch (Exception e) {
            logger.error("Error handling onboarding access", e);
            return "redirect:/onboarding-error.html?error=An error occurred while processing your request";
        }
    }
}