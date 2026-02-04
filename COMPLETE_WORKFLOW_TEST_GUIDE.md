# Complete Workflow Test Guide

## 🎯 **Complete Job Recruitment Workflow with Professional Onboarding**

**Application Status**: ✅ Running at http://localhost:8083

## 📋 **Full End-to-End Testing**

### **Test 1: Referral Application (Yellow Line Path)**

#### **Step 1: Submit Referral Application**
1. Go to: http://localhost:8083/
2. Fill out complete application form
3. **Important**: Enter referral ID: `REF12345`
4. Submit application
5. **Expected**: Application submitted successfully

#### **Step 2: Company Manager Approval**
1. Go to: http://localhost:8083/companymanager-dashboard.html
2. Find your referral application
3. Click "Approve" button
4. **Expected**: Application moves to HR

#### **Step 3: HR Approval (Referral Bypass)**
1. Go to: http://localhost:8083/hr-dashboard.html
2. Find your application
3. **Click "Accept" button** (NOT "Onboarding")
4. **Expected**: Application goes DIRECTLY to onboarding (bypasses TL/PM/Head HR)

#### **Step 4: Professional Onboarding Form**
1. **Automatic**: Candidate receives onboarding task
2. Access: http://localhost:8083/candidate-onboarding.html?applicationId=YOUR_APP_ID
3. **Fill comprehensive form**:

**Personal Information:**
- Full Name: John Doe
- Date of Birth: 1990-01-01
- Gender: Male
- Marital Status: Single
- Nationality: Indian

**Contact Information:**
- Personal Email: john.doe@email.com
- Mobile Number: 9876543210

**Identity Documents:**
- Aadhar Number: 123456789012
- PAN Number: ABCDE1234F

**Address Details:**
- Current Address: 123 Main Street, Mumbai
- City: Mumbai
- State: Maharashtra
- Pincode: 400001
- Country: India

**Banking Details:**
- Bank Name: State Bank of India
- Branch: Main Branch
- Account Number: 1234567890123456
- Confirm Account: 1234567890123456
- IFSC Code: SBIN0001234
- Account Type: Savings

**Emergency Contact:**
- Name: Jane Doe
- Relationship: Mother
- Contact: 9876543211

**Professional Information:**
- Expected Joining Date: (Select future date)

**Declaration:**
- ✅ Check both declaration boxes

4. Click "Complete Onboarding"
5. **Expected**: Success message and process completion

### **Test 2: Normal Application (Complete Flow)**

#### **Step 1: Submit Normal Application**
1. Go to: http://localhost:8083/
2. Fill application form **WITHOUT referral ID**
3. Submit application

#### **Step 2: HR Initial Review**
1. Go to: http://localhost:8083/hr-dashboard.html
2. Click "Accept" for normal application
3. **Expected**: Goes to Team Lead + Project Manager

#### **Step 3: Team Lead & Project Manager Review**
1. **Team Lead**: http://localhost:8083/teamlead-dashboard.html
   - Find application and click "Accept"
2. **Project Manager**: http://localhost:8083/projectmanager-dashboard.html
   - Find application and click "Accept"
3. **Expected**: Both approvals move to Head HR

#### **Step 4: Head HR Review**
1. Go to: http://localhost:8083/headhr-dashboard.html
2. Find application and click "Accept"
3. **Expected**: Moves to Company Manager

#### **Step 5: Company Manager Final Review**
1. Go to: http://localhost:8083/companymanager-dashboard.html
2. Find application and click "Approve"
3. **Expected**: Goes back to HR for final confirmation

#### **Step 6: HR Final Confirmation**
1. Go to: http://localhost:8083/hr-dashboard.html
2. Find the application (now from Company Manager)
3. Click "Accept" button
4. **Expected**: Goes to onboarding

#### **Step 7: Professional Onboarding**
- Same as Test 1, Step 4 above
- Fill comprehensive onboarding form
- **Expected**: Process completes successfully

## 🔍 **Verification Points**

### **In Camunda Cockpit** (http://localhost:8083/camunda - admin/admin)

#### **Referral Path Verification:**
```
Start → Personal Info → Job Preferences → Experience → 
Collect Data → Referral Check → Company Manager → 
HR Decision → DIRECT TO ONBOARDING → End
```

#### **Normal Path Verification:**
```
Start → Personal Info → Job Preferences → Experience → 
Collect Data → Referral Check → HR Review → 
TL/PM Review → Head HR → Company Manager → 
HR Final → ONBOARDING → End
```

### **Process Variables to Check:**
- `hasValidReferral`: true/false
- `bypassedApprovals`: true/false
- `hrDecision`: "accept"
- `onboardingCompleted`: true
- `applicationStatus`: "ONBOARDING_COMPLETED"

## 🎯 **Expected Results**

### **Referral Application:**
- ⚡ **Fast Track**: Bypasses TL/PM/Head HR
- 🎯 **Direct Path**: Company Manager → HR → Onboarding
- ✅ **Process End**: Completes after onboarding form

### **Normal Application:**
- 📋 **Complete Review**: Goes through all approval stages
- 🔄 **Full Process**: HR → TL/PM → Head HR → Company Manager → HR → Onboarding
- ✅ **Process End**: Completes after onboarding form

### **Professional Onboarding:**
- 📝 **Comprehensive Form**: 9 sections with complete professional details
- ✅ **Validation**: Real-time validation with clear error messages
- 🎉 **Success Screen**: Professional completion message
- 🔚 **Process End**: Workflow completes automatically

## 📱 **Dashboard Access Points**

| Role | Dashboard URL | Purpose |
|------|---------------|---------|
| **Applicant** | http://localhost:8083/ | Submit applications |
| **HR** | http://localhost:8083/hr-dashboard.html | Initial & final review |
| **Team Lead** | http://localhost:8083/teamlead-dashboard.html | Technical review |
| **Project Manager** | http://localhost:8083/projectmanager-dashboard.html | Project fit review |
| **Head HR** | http://localhost:8083/headhr-dashboard.html | Senior HR review |
| **Company Manager** | http://localhost:8083/companymanager-dashboard.html | Final approval |
| **Candidate** | http://localhost:8083/candidate-onboarding.html?applicationId=XXX | Onboarding form |

## 🔧 **Troubleshooting**

### **If Referral Bypass Doesn't Work:**
1. Check referral ID is exactly: `REF12345`
2. Verify in Camunda Cockpit: `bypassedApprovals = true`
3. Ensure HR clicks "Accept" not "Onboarding"

### **If Onboarding Form Doesn't Load:**
1. Check application ID in URL
2. Verify application reached onboarding stage in Camunda
3. Check browser console for errors

### **If Process Doesn't End:**
1. Verify all required onboarding fields are filled
2. Check both declaration checkboxes are selected
3. Ensure account numbers match exactly

## 🎉 **Success Indicators**

### **Process Completion:**
- ✅ Success message: "Professional onboarding completed successfully!"
- ✅ Welcome screen with company branding
- ✅ Process status: "ONBOARDING_COMPLETED"
- ✅ Camunda process shows as completed
- ✅ No active tasks remaining

### **Data Storage:**
- ✅ All onboarding data stored in application record
- ✅ Process variables updated with completion status
- ✅ Audit trail with timestamps

**The complete workflow now provides a professional, end-to-end recruitment and onboarding experience!** 🚀