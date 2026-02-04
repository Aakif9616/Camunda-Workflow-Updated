package com.dynamicworkflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delegate for HR Final Confirmation Process
 * Handles the final HR confirmation after candidate onboarding form submission
 */
@Component
public class HRFinalConfirmationDelegate implements JavaDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(HRFinalConfirmationDelegate.class);
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing HR Final Confirmation Process for application: {}", 
                   execution.getVariable("applicationId"));
        
        // Get HR final confirmation variables
        String finalConfirmation = (String) execution.getVariable("finalConfirmation");
        String hrFinalComments = (String) execution.getVariable("hrFinalComments");
        String employeeId = (String) execution.getVariable("employeeId");
        String workLocation = (String) execution.getVariable("workLocation");
        
        logger.info("HR Final Confirmation Decision: {}", finalConfirmation);
        logger.info("HR Final Comments: {}", hrFinalComments);
        logger.info("Employee ID: {}", employeeId);
        logger.info("Work Location: {}", workLocation);
        
        // Set process variables based on final decision
        execution.setVariable("hrFinalConfirmationCompleted", true);
        execution.setVariable("onboardingStage", "HR_FINAL_CONFIRMATION_COMPLETE");
        
        if ("confirm".equals(finalConfirmation)) {
            execution.setVariable("finalOnboardingApproved", true);
            execution.setVariable("applicationStatus", "HIRED");
            execution.setVariable("hiringComplete", true);
            
            // Generate final hiring data
            String hiringData = String.format(
                "Employee ID: %s, Work Location: %s, Final Comments: %s, Confirmation Date: %s",
                employeeId, workLocation, hrFinalComments, java.time.LocalDateTime.now()
            );
            execution.setVariable("finalHiringData", hiringData);
            
            logger.info("HR confirmed final onboarding - candidate is now hired!");
            logger.info("Employee ID assigned: {}", employeeId);
            logger.info("Work Location: {}", workLocation);
            
        } else {
            execution.setVariable("finalOnboardingApproved", false);
            execution.setVariable("applicationStatus", "REJECTED_FINAL_ONBOARDING");
            execution.setVariable("rejectionReason", "Rejected during final HR confirmation: " + hrFinalComments);
            
            logger.info("HR rejected final onboarding - application terminated");
            logger.info("Rejection reason: {}", hrFinalComments);
        }
        
        // Store complete onboarding history
        String onboardingHistory = String.format(
            "HR Onboarding: %s | Candidate Form: %s | HR Final: %s (%s)",
            execution.getVariable("hrOnboardingData"),
            execution.getVariable("candidateOnboardingData"),
            finalConfirmation,
            hrFinalComments
        );
        execution.setVariable("completeOnboardingHistory", onboardingHistory);
        
        // Set final process completion timestamp
        execution.setVariable("processCompletedAt", java.time.LocalDateTime.now().toString());
    }
}