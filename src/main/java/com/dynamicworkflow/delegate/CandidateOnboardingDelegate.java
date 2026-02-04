package com.dynamicworkflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delegate for Candidate Onboarding Process
 * Handles the candidate onboarding form submission with personal and banking details
 */
@Component
public class CandidateOnboardingDelegate implements JavaDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(CandidateOnboardingDelegate.class);
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing Candidate Onboarding Process for application: {}", 
                   execution.getVariable("applicationId"));
        
        // Get candidate onboarding form variables
        String aadharNumber = (String) execution.getVariable("aadharNumber");
        String panNumber = (String) execution.getVariable("panNumber");
        String bankAccountNumber = (String) execution.getVariable("bankAccountNumber");
        String bankIFSC = (String) execution.getVariable("bankIFSC");
        String bankName = (String) execution.getVariable("bankName");
        String emergencyContact = (String) execution.getVariable("emergencyContact");
        String emergencyContactName = (String) execution.getVariable("emergencyContactName");
        String currentAddress = (String) execution.getVariable("currentAddress");
        String permanentAddress = (String) execution.getVariable("permanentAddress");
        String bloodGroup = (String) execution.getVariable("bloodGroup");
        
        logger.info("Candidate Onboarding Details:");
        logger.info("Aadhar Number: {}", maskSensitiveData(aadharNumber));
        logger.info("PAN Number: {}", maskSensitiveData(panNumber));
        logger.info("Bank Account: {}", maskSensitiveData(bankAccountNumber));
        logger.info("Bank IFSC: {}", bankIFSC);
        logger.info("Bank Name: {}", bankName);
        logger.info("Emergency Contact: {}", emergencyContact);
        logger.info("Emergency Contact Name: {}", emergencyContactName);
        logger.info("Blood Group: {}", bloodGroup);
        
        // Validate required fields
        boolean isValid = validateOnboardingData(aadharNumber, panNumber, bankAccountNumber, 
                                               bankIFSC, emergencyContact, currentAddress);
        
        if (isValid) {
            // Set process variables for next stage
            execution.setVariable("candidateOnboardingCompleted", true);
            execution.setVariable("onboardingStage", "CANDIDATE_ONBOARDING_COMPLETE");
            execution.setVariable("onboardingDataValid", true);
            
            // Store onboarding data for HR review
            execution.setVariable("candidateOnboardingData", String.format(
                "Aadhar: %s, PAN: %s, Bank: %s-%s, Emergency: %s (%s), Blood Group: %s",
                maskSensitiveData(aadharNumber), maskSensitiveData(panNumber), 
                bankName, bankIFSC, emergencyContactName, emergencyContact, bloodGroup
            ));
            
            logger.info("Candidate onboarding data validated successfully - proceeding to HR final confirmation");
        } else {
            execution.setVariable("onboardingDataValid", false);
            execution.setVariable("validationError", "Invalid or missing required onboarding information");
            logger.error("Candidate onboarding data validation failed");
        }
    }
    
    /**
     * Validates the candidate onboarding data
     */
    private boolean validateOnboardingData(String aadhar, String pan, String bankAccount, 
                                         String ifsc, String emergencyContact, String address) {
        // Basic validation - in production, you'd have more sophisticated validation
        if (aadhar == null || aadhar.trim().length() != 12) {
            logger.error("Invalid Aadhar number");
            return false;
        }
        
        if (pan == null || pan.trim().length() != 10) {
            logger.error("Invalid PAN number");
            return false;
        }
        
        if (bankAccount == null || bankAccount.trim().length() < 9) {
            logger.error("Invalid bank account number");
            return false;
        }
        
        if (ifsc == null || ifsc.trim().length() != 11) {
            logger.error("Invalid IFSC code");
            return false;
        }
        
        if (emergencyContact == null || emergencyContact.trim().length() != 10) {
            logger.error("Invalid emergency contact number");
            return false;
        }
        
        if (address == null || address.trim().length() < 10) {
            logger.error("Invalid address");
            return false;
        }
        
        return true;
    }
    
    /**
     * Masks sensitive data for logging
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() < 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}