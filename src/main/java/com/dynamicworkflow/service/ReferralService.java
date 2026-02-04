package com.dynamicworkflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ReferralService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReferralService.class);
    
    // In-memory storage for valid referral IDs
    private final Set<String> validReferralIds = new HashSet<>();
    
    public ReferralService() {
        initializeValidReferralIds();
    }
    
    private void initializeValidReferralIds() {
        // Add some sample valid referral IDs
        validReferralIds.add("REF12345");
        validReferralIds.add("REF67890");
        validReferralIds.add("COMP2024");
        validReferralIds.add("HIRE123");
        validReferralIds.add("FAST001");
        
        logger.info("Initialized {} valid referral IDs", validReferralIds.size());
    }
    
    public boolean isValidReferralId(String referralId) {
        if (referralId == null || referralId.trim().isEmpty()) {
            return false;
        }
        
        boolean isValid = validReferralIds.contains(referralId.trim().toUpperCase());
        logger.info("Referral ID validation: {} -> {}", referralId, isValid);
        return isValid;
    }
    
    public void addReferralId(String referralId) {
        if (referralId != null && !referralId.trim().isEmpty()) {
            validReferralIds.add(referralId.trim().toUpperCase());
            logger.info("Added new referral ID: {}", referralId);
        }
    }
    
    public void removeReferralId(String referralId) {
        if (referralId != null) {
            boolean removed = validReferralIds.remove(referralId.trim().toUpperCase());
            logger.info("Removed referral ID: {} -> {}", referralId, removed);
        }
    }
    
    public Set<String> getAllValidReferralIds() {
        return new HashSet<>(validReferralIds);
    }
}