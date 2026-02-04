package com.dynamicworkflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SecureOnboardingController {
    
    /**
     * Serve the secure onboarding form page
     */
    @GetMapping("/secure-onboarding")
    public String secureOnboardingForm(@RequestParam String session) {
        // Redirect to the static HTML file with the session parameter
        return "redirect:/secure-onboarding.html?session=" + session;
    }
}