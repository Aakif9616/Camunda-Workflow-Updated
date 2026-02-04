# Updated Job Recruitment Workflow - Back to HR Flow

**Updated**: February 3, 2026  
**Change**: After Company Manager approval, process now goes back to HR for onboarding review

---

## 🔄 **Updated Workflow Flow**

### **Complete Process Flow**

```
📝 Application Submitted (3-Step Form)
        ↓
🔍 Data Collection & Validation
        ↓
👔 HR Review (Gate 1) - Initial Screening
        ↓
   [HR Decision?]
    ↙         ↘
❌ Reject    ✅ Accept
  ↓            ↓
 🔚 End    🔀 Parallel Gateway
           ↙       ↘
    🎯 TL Review  📊 PM Review
           ↘       ↙
        🔀 Parallel Join
              ↓
      [Both Approved?]
       ↙           ↘
    ❌ No         ✅ Yes
      ↓             ↓
     🔚 End    👑 Head HR Review (Gate 3)
                    ↓
            [Head HR Decision?]
             ↙            ↘
          ❌ Reject    ✅ Accept
            ↓              ↓
           🔚 End    🏢 Company Manager Review (Gate 4)
                           ↓
                   [Company Manager Decision?]
                    ↙                    ↘
                 ❌ Reject            ✅ Accept
                   ↓                     ↓
                  🔚 End          🔄 **BACK TO HR**
                                        ↓
                              👔 HR Onboarding Review
                                        ↓
                              [HR Onboarding Decision?]
                               ↙                    ↘
                            ❌ Reject            ✅ Accept
                              ↓                     ↓
                             🔚 End          📋 Candidate Onboarding Form
                                                   ↓
                                           👔 HR Final Confirmation
                                                   ↓
                                           [HR Final Decision?]
                                            ↙              ↘
                                         ❌ Reject      ✅ Accept
                                           ↓                ↓
                                          🔚 End      💾 Store & Success
                                                           ↓
                                                      🎉 HIRED!
```

---

## 🆕 **Key Changes Made**

### **1. Modified BPMN Workflow**
- **Old Flow**: Company Manager → HR Onboarding Task → Candidate Form → HR Final
- **New Flow**: Company Manager → **Back to HR** → Candidate Form → HR Final

### **2. Updated Task Names**
- **Old**: `HROnboardingTask` (separate task)
- **New**: `HROnboardingReviewTask` (back to HR)

### **3. Updated Variable Names**
- **Old**: `onboardingDecision`
- **New**: `hrOnboardingDecision`

### **4. Updated Dashboard**
- **Title**: "HR Onboarding Review Dashboard"
- **Description**: "Review and approve onboarding process after company manager approval (Back to HR)"
- **Status Names**: Updated to reflect "HR Onboarding Review" process

---

## 📋 **Detailed Stage Breakdown**

### **Stage 1-4: Standard Approval Process**
1. **Application Submission** → 3-step form
2. **HR Review** → Initial screening
3. **Parallel Review** → Team Lead & Project Manager
4. **Head HR Review** → Final hiring decision
5. **Company Manager Review** → Executive approval

### **Stage 5: Back to HR (NEW FLOW)**
6. **HR Onboarding Review** → HR reviews onboarding details
   - **Task**: `HROnboardingReviewTask`
   - **Assignee**: HR team (`hr,managers`)
   - **Decision**: `hrOnboardingDecision` (proceed/reject)
   - **Fields**: Joining date, reporting manager, department, comments

### **Stage 6-7: Onboarding Process**
7. **Candidate Onboarding Form** → Personal & banking details
8. **HR Final Confirmation** → Employee ID assignment & final approval

---

## 🎯 **Workflow Validation**

### **✅ Approval Requirements**
**For FINAL ACCEPTANCE** (All must approve):
- ✅ HR (Initial screening)
- ✅ Team Lead (Technical assessment)
- ✅ Project Manager (Project fit)
- ✅ Head HR (Final hiring decision)
- ✅ Company Manager (Executive approval)
- ✅ **HR Onboarding Review** (Back to HR - NEW)
- ✅ HR Final Confirmation (After candidate form)

### **❌ Rejection Points**
**Any of these can reject**:
- ❌ HR rejects → Immediate rejection
- ❌ Team Lead OR Project Manager rejects → Rejection
- ❌ Head HR rejects → Final rejection
- ❌ Company Manager rejects → Executive rejection
- ❌ **HR Onboarding Review rejects** → Onboarding rejection (NEW)
- ❌ HR Final Confirmation rejects → Document rejection

---

## 🔧 **Technical Implementation**

### **BPMN Changes**
```xml
<!-- Company Manager Decision Gateway -->
<bpmn:sequenceFlow id="Flow_BackToHR" name="Company Manager Accept - Back to HR" 
                   sourceRef="CompanyManagerDecisionGateway" 
                   targetRef="HROnboardingReviewTask">
  <bpmn:conditionExpression>${companyManagerDecision == 'accept'}</bpmn:conditionExpression>
</bpmn:sequenceFlow>

<!-- HR Onboarding Review Task -->
<bpmn:userTask id="HROnboardingReviewTask" name="HR Onboarding Review" 
               camunda:candidateGroups="hr,managers">
  <bpmn:extensionElements>
    <camunda:formData>
      <camunda:formField id="hrOnboardingDecision" type="enum">
        <camunda:value id="proceed" name="Proceed with Onboarding" />
        <camunda:value id="reject" name="Reject - Do not proceed" />
      </camunda:formField>
      <!-- Additional fields for joining date, manager, department, comments -->
    </camunda:formData>
  </bpmn:extensionElements>
</bpmn:userTask>
```

### **Delegate Updates**
- **HROnboardingDelegate.java**: Updated to use `hrOnboardingDecision` variable
- **Dashboard**: Updated to show "HR Onboarding Review" process
- **Status Names**: Updated to reflect new flow

---

## 🌐 **Dashboard Access**

### **HR Onboarding Review Dashboard**
- **URL**: `http://localhost:8083/hr-onboarding-dashboard.html`
- **Purpose**: HR reviews applications after Company Manager approval
- **Features**: 
  - View company manager approved applications
  - Make onboarding decisions
  - Set joining date, reporting manager, department
  - Add onboarding comments

### **Camunda Integration**
- **Tasklist**: `http://localhost:8083/camunda/app/tasklist/default/`
- **Cockpit**: `http://localhost:8083/camunda/app/cockpit/default/`
- **Login**: admin/admin

---

## 🧪 **Testing the Updated Flow**

### **Complete Test Scenario**
1. **Submit Application** → `http://localhost:8083/`
2. **HR Review** → `http://localhost:8083/hr-dashboard.html` (Accept)
3. **Team Lead Review** → `http://localhost:8083/teamlead-dashboard.html` (Accept)
4. **Project Manager Review** → `http://localhost:8083/projectmanager-dashboard.html` (Accept)
5. **Head HR Review** → `http://localhost:8083/headhr-dashboard.html` (Accept)
6. **Company Manager Review** → `http://localhost:8083/companymanager-dashboard.html` (Accept)
7. **🆕 HR Onboarding Review** → `http://localhost:8083/hr-onboarding-dashboard.html` (Back to HR)
8. **Candidate Onboarding** → Camunda Tasklist (Personal/banking details)
9. **HR Final Confirmation** → Camunda Tasklist (Employee ID assignment)
10. **🎉 Process Complete** → Candidate hired!

---

## ✅ **Validation Complete**

### **✅ Changes Implemented**
- ✅ BPMN workflow updated
- ✅ Task names changed
- ✅ Variable names updated
- ✅ Dashboard modified
- ✅ Delegate updated
- ✅ Server restarted and tested

### **✅ Flow Confirmed**
- ✅ Company Manager → Back to HR ✓
- ✅ HR Onboarding Review → Candidate Form ✓
- ✅ Candidate Form → HR Final Confirmation ✓
- ✅ All rejection paths working ✓

**The workflow now correctly flows back to HR after Company Manager approval as requested!** 🎉

---

**Updated Flow Status**: ✅ **IMPLEMENTED & TESTED**  
**Server Status**: 🟢 **RUNNING ON PORT 8083**  
**Ready for Testing**: ✅ **YES**