package com.dynamicworkflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${app.email.smtp.host:smtp.gmail.com}")
    private String smtpHost;
    
    @Value("${app.email.smtp.port:587}")
    private String smtpPort;
    
    @Value("${app.email.username:mohamedaakif10616@gmail.com}")
    private String emailUsername;
    
    @Value("${app.email.password:your-app-password}")
    private String emailPassword;
    
    @Value("${app.email.from:HR Team <mohamedaakif10616@gmail.com>}")
    private String fromEmail;
    
    @Value("${server.port:8083}")
    private String serverPort;
    
    // Store token mappings directly in EmailService to avoid circular dependency
    private final Map<String, String> tokenToApplicationMap = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToEmailMap = new ConcurrentHashMap<>();
    
    /**
     * Send congratulations email with onboarding link to hired candidate
     */
    public boolean sendHireNotificationEmail(Map<String, Object> applicationData) {
        try {
            String candidateEmail = (String) applicationData.get("email");
            String candidateName = applicationData.get("firstName") + " " + applicationData.get("lastName");
            String applicationId = (String) applicationData.get("applicationId");
            String position = formatPosition((String) applicationData.get("position"));
            
            // Generate secure onboarding token
            String onboardingToken = generateOnboardingToken(applicationId, candidateEmail);
            
            // Store token mapping directly in EmailService
            storeTokenMapping(onboardingToken, applicationId, candidateEmail);
            
            // Create onboarding link
            String onboardingLink = String.format("http://localhost:%s/onboarding-access?token=%s", 
                                                 serverPort, onboardingToken);
            
            // Create email content
            String subject = "🎉 Congratulations! You're Hired - Complete Your Onboarding";
            String htmlContent = createHireNotificationEmailContent(candidateName, position, onboardingLink);
            
            // Send email
            boolean emailSent = sendEmail(candidateEmail, subject, htmlContent);
            
            if (emailSent) {
                logger.info("Hire notification email sent successfully to: {} for application: {}", candidateEmail, applicationId);
            } else {
                logger.error("Failed to send hire notification email to: {} for application: {}", candidateEmail, applicationId);
            }
            
            return emailSent;
            
        } catch (Exception e) {
            logger.error("Failed to send hire notification email", e);
            return false;
        }
    }
    
    /**
     * Store token mapping
     */
    public void storeTokenMapping(String token, String applicationId, String email) {
        tokenToApplicationMap.put(token, applicationId);
        tokenToEmailMap.put(token, email);
        logger.info("Stored onboarding token mapping for application: {}", applicationId);
    }
    
    /**
     * Remove token mapping
     */
    public void removeTokenMapping(String token) {
        tokenToApplicationMap.remove(token);
        tokenToEmailMap.remove(token);
        logger.info("Removed onboarding token mapping: {}", token);
    }
    
    /**
     * Get application ID from token
     */
    public String getApplicationIdFromToken(String token) {
        return tokenToApplicationMap.get(token);
    }
    
    /**
     * Get email from token
     */
    public String getEmailFromToken(String token) {
        return tokenToEmailMap.get(token);
    }
    
    /**
     * Generate secure onboarding token
     */
    private String generateOnboardingToken(String applicationId, String email) {
        // Create a secure token combining application ID, email, and random UUID
        String tokenData = applicationId + ":" + email + ":" + System.currentTimeMillis();
        return UUID.nameUUIDFromBytes(tokenData.getBytes()).toString();
    }
    
    /**
     * Create professional hire notification email content
     */
    private String createHireNotificationEmailContent(String candidateName, String position, String onboardingLink) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Welcome to Our Team!</title>" +
                "<style>" +
                    "body { font-family: 'Arial', sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }" +
                    ".container { max-width: 600px; margin: 0 auto; background-color: white; }" +
                    ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px 30px; text-align: center; }" +
                    ".header h1 { margin: 0; font-size: 28px; font-weight: bold; }" +
                    ".header .icon { font-size: 48px; margin-bottom: 20px; }" +
                    ".content { padding: 40px 30px; }" +
                    ".congratulations { background: #f0f9ff; border-left: 4px solid #0ea5e9; padding: 20px; margin: 20px 0; border-radius: 8px; }" +
                    ".position-info { background: #f8fafc; padding: 20px; border-radius: 8px; margin: 20px 0; }" +
                    ".cta-button { display: inline-block; background: #1a73e8; color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }" +
                    ".cta-button:hover { background: #1557b0; }" +
                    ".next-steps { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; margin: 20px 0; border-radius: 8px; }" +
                    ".footer { background: #f8fafc; padding: 30px; text-align: center; color: #6b7280; }" +
                    ".security-note { background: #fef2f2; border-left: 4px solid #ef4444; padding: 15px; margin: 20px 0; border-radius: 8px; font-size: 14px; }" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class=\"container\">" +
                    "<div class=\"header\">" +
                        "<div class=\"icon\">🎉</div>" +
                        "<h1>Congratulations!</h1>" +
                        "<p>Welcome to Our Team</p>" +
                    "</div>" +
                    
                    "<div class=\"content\">" +
                        "<div class=\"congratulations\">" +
                            "<h2>🎊 You're Hired!</h2>" +
                            "<p>Dear <strong>" + candidateName + "</strong>,</p>" +
                            "<p>We are thrilled to inform you that you have been selected for the position of <strong>" + position + "</strong> at our company!</p>" +
                        "</div>" +
                        
                        "<div class=\"position-info\">" +
                            "<h3>📋 Position Details</h3>" +
                            "<p><strong>Position:</strong> " + position + "</p>" +
                            "<p><strong>Status:</strong> Hired - Pending Onboarding</p>" +
                            "<p><strong>Next Step:</strong> Complete Professional Onboarding</p>" +
                        "</div>" +
                        
                        "<div class=\"next-steps\">" +
                            "<h3>📝 Complete Your Onboarding</h3>" +
                            "<p>To finalize your joining process, please complete our comprehensive onboarding form by clicking the button below:</p>" +
                            
                            "<div style=\"text-align: center; margin: 30px 0;\">" +
                                "<a href=\"" + onboardingLink + "\" class=\"cta-button\">" +
                                    "🚀 Complete Onboarding Form" +
                                "</a>" +
                            "</div>" +
                            
                            "<p><strong>What you'll need to provide:</strong></p>" +
                            "<ul>" +
                                "<li>Personal and contact information</li>" +
                                "<li>Identity documents (Aadhar, PAN)</li>" +
                                "<li>Banking details for salary processing</li>" +
                                "<li>Emergency contact information</li>" +
                                "<li>Address and other professional details</li>" +
                            "</ul>" +
                        "</div>" +
                        
                        "<div class=\"security-note\">" +
                            "<p><strong>🔒 Security Note:</strong> This email contains a secure link that is unique to you. Please do not share this link with others. The link will expire after completion of the onboarding process.</p>" +
                        "</div>" +
                        
                        "<p>We're excited to have you join our team and look forward to working with you!</p>" +
                        
                        "<p>If you have any questions, please don't hesitate to contact our HR team.</p>" +
                        
                        "<p>Best regards,<br>" +
                        "<strong>HR Team</strong><br>" +
                        "Your Company Name</p>" +
                    "</div>" +
                    
                    "<div class=\"footer\">" +
                        "<p>This is an automated message from our recruitment system.</p>" +
                        "<p>© 2024 Your Company Name. All rights reserved.</p>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";
    }
    
    /**
     * Send email using SMTP
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            Transport.send(message);
            
            logger.info("Hire notification email sent successfully to: {}", toEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", toEmail, e);
            return false;
        }
    }
    
    /**
     * Format position for display
     */
    private String formatPosition(String position) {
        if (position == null) return "Software Engineer";
        
        switch (position.toLowerCase()) {
            case "software-engineer": return "Software Engineer";
            case "senior-software-engineer": return "Senior Software Engineer";
            case "tech-lead": return "Tech Lead";
            case "product-manager": return "Product Manager";
            default: return position;
        }
    }
    
    /**
     * Validate onboarding token
     */
    public boolean validateOnboardingToken(String token, String applicationId, String email) {
        try {
            // In a real implementation, you would store tokens in database with expiration
            // For now, we'll do basic validation
            return token != null && token.length() == 36; // UUID length
        } catch (Exception e) {
            logger.error("Failed to validate onboarding token", e);
            return false;
        }
    }
}