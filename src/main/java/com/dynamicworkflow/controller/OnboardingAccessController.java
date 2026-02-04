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
                model.addAttribute("error", "Invalid or expired onboarding link");
                return "onboarding-error";
            }
            
            // Get application data
            Map<String, Object> applicationData = jobApplicationService.getApplicationById(applicationId);
            if (applicationData == null) {
                model.addAttribute("error", "Application not found");
                return "onboarding-error";
            }
            
            // Verify email matches
            String applicationEmail = (String) applicationData.get("email");
            if (!email.equals(applicationEmail)) {
                logger.warn("Email mismatch for onboarding token: {} vs {}", email, applicationEmail);
                model.addAttribute("error", "Access denied");
                return "onboarding-error";
            }
            
            // Check if application is in correct state for onboarding
            String status = (String) applicationData.get("applicationStatus");
            if (!"HIRED".equals(status) && !"PENDING_ONBOARDING".equals(status) && !"ACCEPTED".equals(status)) {
                model.addAttribute("error", "Application is not ready for onboarding");
                return "onboarding-error";
            }
            
            // Redirect to onboarding form with secure session
            return "redirect:/secure-onboarding?session=" + token;
            
        } catch (Exception e) {
            logger.error("Error handling onboarding access", e);
            model.addAttribute("error", "An error occurred while processing your request");
            return "onboarding-error";
        }
    }
}