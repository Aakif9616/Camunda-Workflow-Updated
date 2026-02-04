# Referral Onboarding Bypass Implementation - CORRECTED

## Requirement Implemented ✅

**Updated the BPMN workflow so that referral applications approved by HR go directly to onboarding, bypassing Team Lead, Project Manager, and Head HR approval steps.**

## New Workflow Flow

### For Referral Applications:
1. **Application with Referral ID** → Company Manager Review
2. **Company Manager Approves** → HR Review  
3. **HR Approves** → **DIRECTLY TO ONBOARDING** ⚡ (NEW BYPASS - Yellow Line)

### For Normal Applications:
1. **Application** → HR Review
2. **HR Approves** → Team Lead + Project Manager Review
3. **Both TL/PM Approve** → Head HR Review
4. **Head HR Approves** → Company Manager Review
5. **Company Manager Approves** → HR Review
6. **HR Approves** → Onboarding

## BPMN Changes Made

### 1. Modified HR Decision Gateway

**Added new outgoing flow from HR Decision Gateway directly to Onboarding for referral applications.**

**New Conditions**:
- **Normal HR Accept**: `${hrDecision == 'accept' && bypassedApprovals != true}` → Continue to TL/PM Review
- **Referral HR Accept**: `${hrDecision == 'accept' && bypassedApprovals == true}` → **Direct to Onboarding**
- **HR Reject**: `${hrDecision == 'reject'}` → Rejection
- **HR Onboarding**: `${hrDecision == 'onboarding'}` → Direct to Onboarding

### 2. Direct Flow Implementation

```xml
<!-- Direct referral bypass flow -->
<bpmn:sequenceFlow id="Flow_ReferralToOnboarding" name="Referral - Direct to Onboarding" 
    sourceRef="HRDecisionGateway" targetRef="CandidateOnboardingTask">
  <bpmn:conditionExpression>${hrDecision == 'accept' && bypassedApprovals == true}</bpmn:conditionExpression>
</bpmn:sequenceFlow>

<!-- Normal flow for non-referral applications -->
<bpmn:sequenceFlow id="Flow_7" name="HR Accept - Normal Flow" 
    sourceRef="HRDecisionGateway" targetRef="ParallelReviewGateway">
  <bpmn:conditionExpression>${hrDecision == 'accept' && bypassedApprovals != true}</bpmn:conditionExpression>
</bpmn:sequenceFlow>
```

### 3. Visual Flow - Matches Your Yellow Line Diagram

The BPMN now correctly shows:
- **Yellow Line**: HR Decision Gateway → Direct to Candidate Onboarding (for referral applications)
- **Normal Path**: HR Decision Gateway → Team Lead/Project Manager → Head HR → Company Manager → HR → Onboarding

## Process Variable Used

**`bypassedApprovals`**: Set to `true` in `CollectApplicantDataDelegate` when a valid referral ID is detected.

This variable is already implemented and working correctly from the previous referral bypass fix.

## Visual Flow Diagram - CORRECTED

```
Referral Application Flow (Yellow Line):
┌─────────────┐    ┌──────────────────┐    ┌─────────────┐    ┌─────────────┐
│   Submit    │───▶│  Company Manager │───▶│ HR Review   │───▶│ Onboarding  │
│ Application │    │     Review       │    │             │    │             │
└─────────────┘    └──────────────────┘    └─────────────┘    └─────────────┘
                                                   │
                                                   ▼ (Normal Apps)
                                          ┌─────────────────────┐
                                          │ TL/PM → Head HR →   │
                                          │ Company Manager →   │
                                          │ HR → Onboarding    │
                                          └─────────────────────┘
```

## Testing Steps

### Test Referral Bypass to Onboarding:

1. **Submit Application with Referral**:
   - Go to http://localhost:8083/
   - Fill application form with referral ID: `REF12345`
   - Submit application

2. **Company Manager Approval**:
   - Go to http://localhost:8083/companymanager-dashboard.html
   - Approve the referral application

3. **HR Approval**:
   - Go to http://localhost:8083/hr-dashboard.html
   - **Use "Accept" button** (not "Onboarding" button)

4. **Verify Direct Onboarding**:
   - Check Camunda Cockpit: http://localhost:8083/camunda
   - Process should be at "Candidate Onboarding Form" task
   - Should NOT go through Team Lead, Project Manager, or Head HR

### Test Normal Flow (No Change):

1. **Submit Application without Referral**:
   - Submit application with empty or invalid referral ID

2. **HR Approval**:
   - HR approves → Should go to Team Lead + Project Manager

3. **Continue Normal Flow**:
   - TL/PM → Head HR → Company Manager → HR → Onboarding

## Files Modified

- `src/main/resources/processes/job-recruitment-workflow.bpmn` - Updated BPMN workflow with direct flow from HR to Onboarding for referral applications

## Valid Referral IDs (Unchanged)

- REF12345
- REF67890  
- COMP2024
- HIRE123
- FAST001

## Benefits

✅ **Faster Onboarding**: Referral candidates get onboarded immediately after HR approval  
✅ **Maintains Quality**: Company Manager still reviews referral applications first  
✅ **Preserves Normal Flow**: Non-referral applications follow complete approval chain  
✅ **Clear Visual Separation**: Yellow line shows direct path as requested  
✅ **No Intermediate Gateways**: Clean direct flow from HR to Onboarding

The referral onboarding bypass now correctly matches your yellow line diagram! 🚀