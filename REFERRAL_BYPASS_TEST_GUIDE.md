# Referral Bypass Test Guide

## 🚀 Application Status
✅ **Running at**: http://localhost:8083  
✅ **Clean Build**: Completed with updated BPMN  
✅ **Clear Labels**: Flow labels updated for better visibility  

## 🎯 Updated Flow Labels (Now Visible in Diagram)

From HR Decision Gateway, you should now see these **4 clear paths**:

1. **"Normal: HR Accept → TL/PM"** - For non-referral applications
2. **"REFERRAL: HR Accept → DIRECT ONBOARDING"** - **This is your yellow line!**
3. **"HR Reject"** - Rejection path
4. **"HR Onboarding"** - Direct onboarding path

## 📋 Step-by-Step Test Instructions

### Test 1: Referral Bypass (Yellow Line Path)

1. **Submit Referral Application**:
   - Go to: http://localhost:8083/
   - Fill out the form completely
   - **Important**: Enter referral ID: `REF12345`
   - Submit application

2. **Company Manager Approval**:
   - Go to: http://localhost:8083/companymanager-dashboard.html
   - Find your application in the list
   - Click "Approve" button

3. **HR Approval (Critical Step)**:
   - Go to: http://localhost:8083/hr-dashboard.html
   - Find your application
   - **Click "Accept" button** (NOT "Onboarding" button)
   - This should trigger the referral bypass

4. **Verify Direct Path**:
   - Go to: http://localhost:8083/camunda
   - Login: admin/admin
   - Look at the process instance
   - **Should be at "Candidate Onboarding Form" task**
   - **Should NOT have gone through Team Lead, Project Manager, or Head HR**

### Test 2: Normal Flow (Verification)

1. **Submit Normal Application**:
   - Go to: http://localhost:8083/
   - Fill out form **without referral ID** (leave empty)
   - Submit application

2. **HR Approval**:
   - Go to: http://localhost:8083/hr-dashboard.html
   - Click "Accept" for the normal application

3. **Verify Normal Path**:
   - Should go to Team Lead and Project Manager tasks
   - Should follow the complete approval chain

## 🔍 Debugging Tips

### If Referral Bypass Doesn't Work:

1. **Check Process Variables**:
   - In Camunda Cockpit, look at process variables
   - Should see: `bypassedApprovals = true`
   - Should see: `hasValidReferral = true`

2. **Check Application Logs**:
   - Look for: "Application X has valid referral ID: REF12345 - will bypass normal approval process"

3. **Verify Referral ID**:
   - Valid IDs: REF12345, REF67890, COMP2024, HIRE123, FAST001
   - Must be exact match (case-insensitive)

### If Diagram Still Shows Wrong Path:

1. **Clear Browser Cache**: Ctrl+F5 on Camunda Cockpit
2. **Check Process Definition**: Ensure latest version is deployed
3. **Restart Application**: If needed for cache refresh

## 🎯 Expected Results

### Referral Application (Yellow Line):
```
Submit → Company Manager → HR "Accept" → DIRECT TO ONBOARDING
```

### Normal Application:
```
Submit → HR "Accept" → Team Lead + Project Manager → Head HR → Company Manager → HR → Onboarding
```

## 📊 Visual Verification

In the Camunda Cockpit diagram, you should now see:
- **Clear flow labels** on each path
- **"REFERRAL: HR Accept → DIRECT ONBOARDING"** path going directly from HR Decision Gateway to Candidate Onboarding
- **Yellow highlighting** when a referral process follows this path

## 🆘 Troubleshooting

If the referral bypass still doesn't work:

1. **Check HR Dashboard**: Ensure you're clicking "Accept" not "Onboarding"
2. **Verify Referral ID**: Must be one of the valid IDs listed above
3. **Check Logs**: Look for referral validation messages
4. **Process Variables**: Verify `bypassedApprovals = true` in Camunda Cockpit

The referral bypass should now work correctly with clear visual indicators! 🎉