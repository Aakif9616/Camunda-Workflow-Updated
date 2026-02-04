package com.dynamicworkflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delegate for HR Onboarding Process
 * Handles the HR onboarding stage after company manager approval
 */
@Component
public class HROnboardingDelegate implements JavaDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(HROnboardingDelegate.class);
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing HR Onboarding Review Process for application: {}", 
                   execution.getVariable("applicationId"));
        
        // Get HR onboarding decision variables
        String hrOnboardingDecision = (String) execution.getVariable("hrOnboardingDecision");
        String hrOnboardingComments = (String) execution.getVariable("hrOnboardingComments");
        String joiningDate = (String) execution.getVariable("joiningDate");
        String reportingManager = (String) execution.getVariable("reportingManager");
        String department = (String) execution.getVariable("department");
        
        logger.info("HR Onboarding Decision: {}", hrOnboardingDecision);
        logger.info("HR Onboarding Comments: {}", hrOnboardingComments);
        logger.info("Expected Joining Date: {}", joiningDate);
        logger.info("Reporting Manager: {}", reportingManager);
        logger.info("Department: {}", department);
        
        // Set process variables for next stages
        execution.setVariable("hrOnboardingCompleted", true);
        execution.setVariable("onboardingStage", "HR_ONBOARDING_COMPLETE");
        
        if ("proceed".equals(hrOnboardingDecision)) {
            execution.setVariable("onboardingApproved", true);
            logger.info("HR approved onboarding process - proceeding to candidate onboarding form");
        } else {
            execution.setVariable("onboardingApproved", false);
            execution.setVariable("rejectionReason", "Rejected during HR onboarding review process");
            logger.info("HR rejected onboarding process - application will be terminated");
        }
        
        // Store onboarding data for future reference
        execution.setVariable("hrOnboardingData", String.format(
            "Decision: %s, Comments: %s, Joining Date: %s, Manager: %s, Department: %s",
            hrOnboardingDecision, hrOnboardingComments, joiningDate, reportingManager, department
        ));
    }
}