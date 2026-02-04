# Complete Email-Based Onboarding Testing Guide

## Prerequisites

### 1. Gmail App Password Setup
1. Go to your Google Account: https://myaccount.google.com/
2. Navigate to Security → 2-Step Verification (enable if not already)
3. Go to Security → App passwords
4. Select "Mail" and generate a 16-character app password
5. Copy this password and update `application.yml`:

```yaml
app:
  email:
    username: mohamedaakif10616@gmail.com
    password: YOUR_16_CHARACTER_APP_PASSWORD_HERE
```

### 2. Restart Application
After updating the email password, restart the application:
```bash
mvn spring-boot:run
```

## Complete Testing Flow

### Step 1: Submit a New Application
1. Go to: `http://localhost:8083/application-form.html`
2. Fill out the application form with:
   - **Email**: Use your email `mohamedaakif10616@gmail.com` (so you receive the congratulations email)
   - **First Name**: Mohamed
   - **Last Name**: Aakif
   - **Position**: Software Engineer
   - **Referral ID**: REF12345 (for referral bypass testing)
   - Fill other required fields
3. Submit the application
4. Note the Application ID (e.g., APP-1234567890-ABCD1234)

### Step 2: Check Application Status
1. Go to: `http://localhost:8083/application-status.html`
2. Enter your Application ID
3. You should see the referral bypass flow (HR → Company Manager directly)

### Step 3: Company Manager Approval (This triggers the email)
1. Go to: `http://localhost:8083/companymanager-dashboard.html`
2. Find your application in the list
3. Click "Approve" and add comments like "Welcome to the team!"
4. **This step will trigger the congratulations email to be sent**

### Step 4: Check Email and Application Status
1. **Check your Gmail inbox** for the congratulations email
2. Go back to: `http://localhost:8083/application-status.html`
3. Enter your Application ID again
4. You should now see:
   - Status: "Application Accepted"
   - Congratulations message with celebration animation
   - Email notification status (sent/pending)
   - Next steps for onboarding

### Step 5: Complete Onboarding via Email
1. **Click the onboarding link in your email**
2. You'll be redirected to the secure onboarding form
3. Fill out all 9 sections of the comprehensive onboarding form:
   - Personal Information
   - Contact Information  
   - Identity Documents (Aadhar, PAN)
   - Address Details
   - Banking Details for Salary
   - Emergency Contact
   - Medical Information
   - Professional Information
   - Declaration
4. Submit the form
5. **The Camunda process will end after successful submission**

## Alternative Testing (If Email Doesn't Work Initially)

### Manual Email Trigger API
If the automatic email doesn't work, you can manually trigger it:

```bash
# First, get your application ID from the status page
# Then call this API to manually send the email:
curl -X POST "http://localhost:8083/api/job-applications/YOUR_APPLICATION_ID/mark-accepted"
```

### Direct Onboarding Access
If you need to access the onboarding form directly:
1. Complete steps 1-3 above
2. Go to: `http://localhost:8083/candidate-onboarding.html`
3. Fill out the comprehensive onboarding form

## Expected Email Content

You should receive an email with:
- **Subject**: "🎉 Congratulations! You're Hired - Complete Your Onboarding"
- **From**: HR Team <mohamedaakif10616@gmail.com>
- **Content**: Professional HTML email with:
  - Congratulations message
  - Position details
  - Secure onboarding form link
  - Instructions and security notice

## Troubleshooting

### Email Not Received
1. **Check Gmail App Password**: Ensure you used the correct 16-character app password
2. **Check Spam Folder**: Gmail might filter the email
3. **Check Application Logs**: Look for email sending errors in the console
4. **Verify Email Configuration**: Ensure `application.yml` has correct settings

### Application Status Not Updating
1. **Refresh the status page**: The status updates in real-time
2. **Check Company Manager approval**: Ensure the approval was successful
3. **Check application logs**: Look for status update errors

### Onboarding Form Issues
1. **Use the email link**: Always access via the secure email link
2. **Check token validity**: Tokens expire after use
3. **Fill all required fields**: The form has comprehensive validation

## Success Indicators

✅ **Email Sent Successfully**: Check application logs for "Hire notification email sent successfully"
✅ **Status Updated**: Application status shows "ACCEPTED" with congratulations
✅ **Email Received**: Professional congratulations email in your inbox
✅ **Secure Access**: Onboarding form accessible via email link
✅ **Process Complete**: Camunda workflow ends after onboarding submission

## API Endpoints for Testing

- **Application Status**: `GET /api/job-applications/{applicationId}/status`
- **Manual Email Trigger**: `POST /api/job-applications/{applicationId}/mark-accepted`
- **All Applications**: `GET /api/job-applications/all`
- **Health Check**: `GET /api/job-applications/health`

## File Locations

- **Application Form**: `http://localhost:8083/application-form.html`
- **Status Check**: `http://localhost:8083/application-status.html`
- **Company Manager Dashboard**: `http://localhost:8083/companymanager-dashboard.html`
- **Secure Onboarding**: Accessed via email link only

This complete flow demonstrates the professional email-based onboarding system working end-to-end!