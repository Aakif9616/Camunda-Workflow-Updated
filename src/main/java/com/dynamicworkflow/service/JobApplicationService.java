package com.dynamicworkflow.service;

import com.dynamicworkflow.dto.ApplicationResponse;
import com.dynamicworkflow.model.WorkflowStep;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobApplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationService.class);
    
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ValidationService validationService;
    private final ReferralService referralService;
    private final EmailService emailService;
    
    // In-memory storage for application data
    private final Map<String, Map<String, Object>> applicationDataStore = new ConcurrentHashMap<>();
    private final Map<String, String> applicationStatusStore = new ConcurrentHashMap<>();
    
    public JobApplicationService(ProcessEngine processEngine, 
                               WorkflowDefinitionService workflowDefinitionService,
                               ValidationService validationService,
                               ReferralService referralService,
                               EmailService emailService) {
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
        this.historyService = processEngine.getHistoryService();
        this.workflowDefinitionService = workflowDefinitionService;
        this.validationService = validationService;
        this.referralService = referralService;
        this.emailService = emailService;
    }
    
    public ApplicationResponse startApplication() {
        try {
            // Generate unique application ID
            String applicationId = generateApplicationId();
            
            // Get first step
            Optional<WorkflowStep> firstStep = workflowDefinitionService.getStepByOrder(1);
            if (!firstStep.isPresent()) {
                throw new RuntimeException("No first step found in workflow definition");
            }
            
            // Initialize application data storage
            Map<String, Object> applicationData = new HashMap<>();
            applicationData.put("applicationId", applicationId);
            applicationData.put("applicationStatus", "STARTED");
            applicationData.put("submissionTimestamp", LocalDateTime.now().toString());
            applicationData.put("currentStep", firstStep.get().getStepId());
            applicationData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            
            // Store in memory
            applicationDataStore.put(applicationId, applicationData);
            applicationStatusStore.put(applicationId, "STARTED");
            
            // Start BPMN Process Instance
            try {
                Map<String, Object> processVariables = new HashMap<>();
                processVariables.put("applicationId", applicationId);
                processVariables.put("applicationStatus", "STARTED");
                processVariables.put("validationResult", true); // Default to true for demo
                
                ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "job-recruitment-workflow-india", 
                    applicationId, // Use applicationId as business key
                    processVariables
                );
                
                logger.info("Started BPMN process instance: {} for application: {}", 
                           processInstance.getId(), applicationId);
                
                // Store process instance ID
                applicationData.put("processInstanceId", processInstance.getId());
                applicationDataStore.put(applicationId, applicationData);
                
            } catch (Exception e) {
                logger.warn("Failed to start BPMN process for application {}: {}", applicationId, e.getMessage());
                // Continue without BPMN process - the application will still work
            }
            
            ApplicationResponse response = new ApplicationResponse();
            response.setApplicationId(applicationId);
            response.setProcessInstanceId("process-" + System.currentTimeMillis());
            response.setCurrentStep(firstStep.get().getStepId());
            response.setStatus("STARTED");
            response.setMessage("Application started successfully");
            response.setTimestamp(LocalDateTime.now());
            
            logger.info("Started new application: {}", applicationId);
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Failed to start application: " + e.getMessage());
        }
    }
    
    public ApplicationResponse submitStep(String applicationId, Map<String, Object> stepData) {
        try {
            String currentStepId = (String) stepData.get("currentStep");
            logger.info("Submitting step {} for application {}", currentStepId, applicationId);
            logger.info("Step data received: {}", stepData);
            
            // Validate step data
            Optional<WorkflowStep> currentStep = workflowDefinitionService.getStepById(currentStepId);
            if (!currentStep.isPresent()) {
                throw new RuntimeException("Invalid step ID: " + currentStepId);
            }

            // Perform validation
            validationService.validateStepData(currentStep.get(), stepData);
            
            // Get existing application data
            Map<String, Object> applicationData = applicationDataStore.getOrDefault(applicationId, new HashMap<>());
            logger.info("Existing application data: {}", applicationData);
            
            // Merge step data into application data (excluding control fields)
            stepData.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("currentStep"))
                .forEach(entry -> applicationData.put(entry.getKey(), entry.getValue()));
            
            // Update metadata
            applicationData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            applicationData.put("lastCompletedStep", currentStepId);
            
            // Determine next step and check for referral bypass
            String nextStepId = null;
            String status = "IN_PROGRESS";
            
            if (workflowDefinitionService.isLastStep(currentStepId)) {
                // Check if referral ID is provided and valid
                String referralId = (String) stepData.get("referralId");
                boolean hasValidReferral = referralId != null && !referralId.trim().isEmpty() && 
                                         referralService.isValidReferralId(referralId);
                
                if (hasValidReferral) {
                    status = "PENDING_COMPANY_MANAGER_REVIEW";
                    applicationData.put("referralId", referralId.trim().toUpperCase());
                    applicationData.put("hasValidReferral", true);
                    applicationData.put("bypassedApprovals", true);
                    logger.info("Application {} has valid referral ID: {} - bypassing normal approval process", 
                               applicationId, referralId);
                } else {
                    status = "PENDING_HR_REVIEW";
                    applicationData.put("hasValidReferral", false);
                    applicationData.put("bypassedApprovals", false);
                    if (referralId != null && !referralId.trim().isEmpty()) {
                        applicationData.put("referralId", referralId.trim().toUpperCase());
                        applicationData.put("invalidReferralId", true);
                        logger.warn("Application {} has invalid referral ID: {}", applicationId, referralId);
                    }
                }
                
                nextStepId = null;
                applicationData.put("submissionTimestamp", LocalDateTime.now().toString());
                applicationData.put("applicationStatus", status);
                applicationStatusStore.put(applicationId, status);
                logger.info("Application {} submitted for review with status: {}", applicationId, status);
            } else {
                Optional<WorkflowStep> nextStep = workflowDefinitionService.getNextStep(currentStepId);
                if (nextStep.isPresent()) {
                    nextStepId = nextStep.get().getStepId();
                }
                applicationData.put("currentStep", nextStepId);
                applicationStatusStore.put(applicationId, "IN_PROGRESS");
                logger.info("Application {} moving to next step: {}", applicationId, nextStepId);
            }
            
            // Store updated data
            applicationDataStore.put(applicationId, applicationData);
            logger.info("Updated application data: {}", applicationData);
            
            // Handle BPMN workflow
            if (workflowDefinitionService.isLastStep(currentStepId)) {
                String referralId = (String) stepData.get("referralId");
                boolean hasValidReferral = referralId != null && !referralId.trim().isEmpty() && 
                                         referralService.isValidReferralId(referralId);
                
                if (hasValidReferral) {
                    // For referral applications, let BPMN workflow run with referral bypass
                    logger.info("Referral application {} - running BPMN workflow with referral bypass", applicationId);
                    updateBPMNProcess(applicationId, applicationData, stepData, currentStepId);
                } else {
                    // Normal BPMN workflow for non-referral applications
                    updateBPMNProcess(applicationId, applicationData, stepData, currentStepId);
                }
            } else {
                // Normal BPMN workflow for non-final steps
                updateBPMNProcess(applicationId, applicationData, stepData, currentStepId);
            }
            
            ApplicationResponse response = new ApplicationResponse();
            response.setApplicationId(applicationId);
            response.setCurrentStep(nextStepId);
            response.setStatus(status);
            response.setMessage("Step submitted successfully");
            response.setTimestamp(LocalDateTime.now());
            
            // Add step data to response for confirmation
            Map<String, Object> responseData = new HashMap<>(stepData);
            responseData.put("nextStep", nextStepId);
            response.setData(responseData);
            
            logger.info("Step submitted successfully for application {}: {} -> {}", applicationId, currentStepId, nextStepId);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to submit step for application: {}", applicationId, e);
            throw new RuntimeException("Failed to submit step: " + e.getMessage());
        }
    }
    
    private void updateBPMNProcess(String applicationId, Map<String, Object> applicationData, 
                                  Map<String, Object> stepData, String currentStepId) {
        try {
            String processInstanceId = (String) applicationData.get("processInstanceId");
            if (processInstanceId != null) {
                // Find and complete current user task
                Task currentTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .singleResult();
                
                if (currentTask != null) {
                    // Set task variables and complete
                    Map<String, Object> taskVariables = new HashMap<>(stepData);
                    taskVariables.put("stepCompleted", currentStepId);
                    taskVariables.put("validationResult", true);
                    
                    // Add referral information to process variables
                    if (applicationData.containsKey("hasValidReferral")) {
                        Boolean hasValidReferral = (Boolean) applicationData.get("hasValidReferral");
                        taskVariables.put("hasValidReferral", hasValidReferral != null ? hasValidReferral : false);
                        
                        Boolean bypassedApprovals = (Boolean) applicationData.get("bypassedApprovals");
                        taskVariables.put("bypassedApprovals", bypassedApprovals != null ? bypassedApprovals : false);
                        
                        if (applicationData.containsKey("referralId")) {
                            taskVariables.put("referralId", applicationData.get("referralId"));
                        }
                    } else {
                        // Set default values for non-referral applications
                        taskVariables.put("hasValidReferral", false);
                        taskVariables.put("bypassedApprovals", false);
                    }
                    
                    taskService.complete(currentTask.getId(), taskVariables);
                    logger.info("Completed BPMN task: {} for application: {}", currentTask.getId(), applicationId);
                    
                } else {
                    logger.warn("No active BPMN task found for process instance: {}", processInstanceId);
                }
            } else {
                logger.warn("No process instance ID found for application: {}", applicationId);
            }
        } catch (Exception e) {
            logger.warn("Failed to update BPMN process for application {}: {}", applicationId, e.getMessage());
            // Continue without BPMN update - the application will still work
        }
    }
    
    public ApplicationResponse getApplication(String applicationId) {
        try {
            // Get stored application data
            Map<String, Object> applicationData = applicationDataStore.get(applicationId);
            
            if (applicationData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            String status = applicationStatusStore.getOrDefault(applicationId, "UNKNOWN");
            
            ApplicationResponse response = new ApplicationResponse();
            response.setApplicationId(applicationId);
            response.setStatus(status);
            response.setCurrentStep((String) applicationData.get("currentStep"));
            response.setMessage("Application retrieved successfully");
            response.setTimestamp(LocalDateTime.now());
            
            // Return the actual stored data
            response.setData(applicationData);
            
            logger.info("Retrieved application {}: status={}, dataSize={}", 
                       applicationId, status, applicationData.size());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve application: {}", applicationId, e);
            throw new RuntimeException("Failed to retrieve application: " + e.getMessage());
        }
    }
    
    // Add method to get all applications for debugging
    public Map<String, Object> getAllApplications() {
        // Sync with Camunda process instances to get latest status
        syncApplicationStatusWithCamunda();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalApplications", applicationDataStore.size());
        result.put("applications", applicationDataStore);
        result.put("statuses", applicationStatusStore);
        return result;
    }
    
    // Method to sync application status with Camunda process instances
    private void syncApplicationStatusWithCamunda() {
        try {
            // Get all process instances for our workflow (both active and ended)
            List<ProcessInstance> activeProcesses = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("job-recruitment-workflow-india")
                .active()
                .list();
            
            // Sync active processes
            for (ProcessInstance processInstance : activeProcesses) {
                String applicationId = processInstance.getBusinessKey();
                if (applicationId != null && applicationDataStore.containsKey(applicationId)) {
                    syncProcessVariables(processInstance.getId(), applicationId, false);
                }
            }
            
            // Also check for ended processes using HistoryService
            List<HistoricProcessInstance> endedProcesses = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("job-recruitment-workflow-india")
                .finished()
                .list();
            
            for (HistoricProcessInstance processInstance : endedProcesses) {
                String applicationId = processInstance.getBusinessKey();
                if (applicationId != null && applicationDataStore.containsKey(applicationId)) {
                    syncProcessVariables(processInstance.getId(), applicationId, true);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to sync application status with Camunda: {}", e.getMessage());
        }
    }
    
    private void syncProcessVariables(String processInstanceId, String applicationId, boolean isEnded) {
        try {
            Map<String, Object> processVariables;
            
            if (isEnded) {
                processVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                        v -> v.getName(),
                        v -> v.getValue()
                    ));
            } else {
                processVariables = runtimeService.getVariables(processInstanceId);
            }
            
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            
            // Sync all decisions
            syncDecisionData(appData, processVariables, "hr");
            syncDecisionData(appData, processVariables, "tl");
            syncDecisionData(appData, processVariables, "pm");
            syncDecisionData(appData, processVariables, "headHR");
            syncDecisionData(appData, processVariables, "companyManager");
            
            if (isEnded) {
                // Determine final status for ended processes
                String finalStatus = determineFinalStatus(processVariables);
                appData.put("applicationStatus", finalStatus);
                applicationStatusStore.put(applicationId, finalStatus);
                logger.info("Updated completed application {} to final status: {}", applicationId, finalStatus);
            } else {
                // Update status based on current task for active processes
                updateStatusFromCurrentTask(processInstanceId, applicationId, appData);
            }
            
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            
        } catch (Exception e) {
            logger.warn("Failed to sync process variables for application {}: {}", applicationId, e.getMessage());
        }
    }
    
    private void syncDecisionData(Map<String, Object> appData, Map<String, Object> processVariables, String role) {
        String decisionKey = role + "Decision";
        String commentsKey = role + "Comments";
        
        if (processVariables.containsKey(decisionKey)) {
            appData.put(decisionKey, processVariables.get(decisionKey));
            appData.put(commentsKey, processVariables.get(commentsKey));
            
            // Handle special fields
            if ("hr".equals(role) && processVariables.containsKey("interviewRequired")) {
                Object interviewRequired = processVariables.get("interviewRequired");
                appData.put("interviewRequired", interviewRequired != null ? interviewRequired : false);
            } else if ("headHR".equals(role) && processVariables.containsKey("offerCTC")) {
                appData.put("offerCTC", processVariables.get("offerCTC"));
            } else if ("companyManager".equals(role) && processVariables.containsKey("finalOfferCTC")) {
                appData.put("finalOfferCTC", processVariables.get("finalOfferCTC"));
            }
            
            logger.debug("Synced {} decision for application", role);
        }
    }
    
    private String determineFinalStatus(Map<String, Object> processVariables) {
        if (processVariables.containsKey("companyManagerDecision")) {
            String companyManagerDecision = (String) processVariables.get("companyManagerDecision");
            return "accept".equals(companyManagerDecision) ? "ACCEPTED" : "REJECTED_BY_COMPANY_MANAGER";
        } else if (processVariables.containsKey("headHRDecision")) {
            String headHRDecision = (String) processVariables.get("headHRDecision");
            return "accept".equals(headHRDecision) ? "PENDING_COMPANY_MANAGER_REVIEW" : "REJECTED_BY_HEAD_HR";
        } else if (processVariables.containsKey("tlDecision") || processVariables.containsKey("pmDecision")) {
            String tlDecision = (String) processVariables.get("tlDecision");
            String pmDecision = (String) processVariables.get("pmDecision");
            if ("reject".equals(tlDecision) || "reject".equals(pmDecision)) {
                return "REJECTED_BY_TL_PM";
            }
        } else if (processVariables.containsKey("hrDecision")) {
            String hrDecision = (String) processVariables.get("hrDecision");
            if ("reject".equals(hrDecision)) {
                return "REJECTED_BY_HR";
            }
        }
        return "COMPLETED";
    }
    
    private void updateStatusFromCurrentTask(String processInstanceId, String applicationId, Map<String, Object> appData) {
        try {
            Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();
            
            if (currentTask != null) {
                String taskName = currentTask.getName();
                String newStatus = determineStatusFromTaskName(taskName);
                
                appData.put("applicationStatus", newStatus);
                applicationStatusStore.put(applicationId, newStatus);
                logger.debug("Updated application {} status to: {} (task: {})", applicationId, newStatus, taskName);
            }
        } catch (Exception e) {
            logger.warn("Failed to update status from current task for application {}: {}", applicationId, e.getMessage());
        }
    }
    
    private String determineStatusFromTaskName(String taskName) {
        if (taskName.contains("HR Application Review")) {
            return "PENDING_HR_REVIEW";
        } else if (taskName.contains("Team Lead Review")) {
            return "PENDING_TL_REVIEW";
        } else if (taskName.contains("Project Manager Review")) {
            return "PENDING_PM_REVIEW";
        } else if (taskName.contains("Head HR Final Review")) {
            return "PENDING_HEAD_HR_REVIEW";
        } else if (taskName.contains("Company Manager Final Review")) {
            return "PENDING_COMPANY_MANAGER_REVIEW";
        }
        return "IN_PROGRESS";
    }
    
    // Method to manually update application status (can be called by Camunda delegates)
    public void updateApplicationStatus(String applicationId, String status, Map<String, Object> additionalData) {
        try {
            if (applicationDataStore.containsKey(applicationId)) {
                applicationStatusStore.put(applicationId, status);
                Map<String, Object> appData = applicationDataStore.get(applicationId);
                appData.put("applicationStatus", status);
                appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
                
                // Add any additional data
                if (additionalData != null) {
                    appData.putAll(additionalData);
                }
                
                logger.info("Manually updated application {} status to: {}", applicationId, status);
            }
        } catch (Exception e) {
            logger.error("Failed to update application status for {}: {}", applicationId, e.getMessage());
        }
    }
    
    /**
     * Approve an application by a specific role
     */
    public Map<String, Object> approveApplication(String applicationId, String role, String comments, String offerCTC) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get application data
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            // Find the Camunda task for this application and role (optional for Company Manager)
            String processInstanceId = (String) appData.get("processInstanceId");
            Task activeTask = null;
            
            if (processInstanceId != null) {
                activeTask = findTaskForRole(processInstanceId, role);
            }
            
            // For Company Manager, allow approval even without Camunda task (referral applications)
            if (activeTask == null && !"companymanager".equals(role.toLowerCase())) {
                if (processInstanceId == null) {
                    throw new RuntimeException("No process instance found for application: " + applicationId);
                } else {
                    throw new RuntimeException("No active task found for role: " + role + " in application: " + applicationId);
                }
            }
            
            // Prepare task variables for approval
            Map<String, Object> taskVariables = new HashMap<>();
            
            switch (role.toLowerCase()) {
                case "hr":
                    taskVariables.put("hrDecision", "accept");
                    taskVariables.put("hrComments", comments != null ? comments : "");
                    taskVariables.put("interviewRequired", true); // Default for approved applications
                    appData.put("hrDecision", "accept");
                    appData.put("hrComments", comments);
                    appData.put("applicationStatus", "HR_APPROVED");
                    applicationStatusStore.put(applicationId, "HR_APPROVED");
                    break;
                    
                case "teamlead":
                case "tl":
                    taskVariables.put("tlDecision", "accept");
                    taskVariables.put("tlComments", comments != null ? comments : "");
                    appData.put("tlDecision", "accept");
                    appData.put("tlComments", comments);
                    // Status will be updated based on PM decision
                    break;
                    
                case "projectmanager":
                case "pm":
                    taskVariables.put("pmDecision", "accept");
                    taskVariables.put("pmComments", comments != null ? comments : "");
                    appData.put("pmDecision", "accept");
                    appData.put("pmComments", comments);
                    // Status will be updated based on TL decision
                    break;
                    
                case "headhr":
                    taskVariables.put("headHRDecision", "accept");
                    taskVariables.put("headHRComments", comments != null ? comments : "");
                    if (offerCTC != null && !offerCTC.trim().isEmpty()) {
                        taskVariables.put("offerCTC", offerCTC);
                        appData.put("offerCTC", offerCTC);
                    }
                    appData.put("headHRDecision", "accept");
                    appData.put("headHRComments", comments);
                    
                    // Normal flow - always goes to Company Manager after Head HR
                    appData.put("applicationStatus", "PENDING_COMPANY_MANAGER_REVIEW");
                    applicationStatusStore.put(applicationId, "PENDING_COMPANY_MANAGER_REVIEW");
                    break;
                    
                case "companymanager":
                    taskVariables.put("companyManagerDecision", "accept");
                    taskVariables.put("companyManagerComments", comments != null ? comments : "");
                    if (offerCTC != null && !offerCTC.trim().isEmpty()) {
                        taskVariables.put("finalOfferCTC", offerCTC);
                        appData.put("finalOfferCTC", offerCTC);
                    }
                    appData.put("companyManagerDecision", "accept");
                    appData.put("companyManagerComments", comments);
                    appData.put("applicationStatus", "PENDING_HR_HIRING");
                    applicationStatusStore.put(applicationId, "PENDING_HR_HIRING");
                    break;
                    
                default:
                    throw new RuntimeException("Invalid role: " + role);
            }
            
            // Complete the Camunda task (optional for Company Manager with referral applications)
            if (activeTask != null) {
                taskService.complete(activeTask.getId(), taskVariables);
            } else if ("companymanager".equals(role.toLowerCase())) {
                // For Company Manager, allow approval without Camunda task (referral applications)
                logger.info("Company Manager approval for application {} without Camunda task (likely referral application)", applicationId);
            } else {
                throw new RuntimeException("No active task found for role: " + role + " in application: " + applicationId);
            }
            
            // Update application data
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            applicationDataStore.put(applicationId, appData);
            
            // Check if both TL and PM have approved (for parallel gateway)
            if (("teamlead".equals(role.toLowerCase()) || "tl".equals(role.toLowerCase()) || 
                 "projectmanager".equals(role.toLowerCase()) || "pm".equals(role.toLowerCase()))) {
                
                String tlDecision = (String) appData.get("tlDecision");
                String pmDecision = (String) appData.get("pmDecision");
                
                if ("accept".equals(tlDecision) && "accept".equals(pmDecision)) {
                    appData.put("applicationStatus", "PENDING_HEAD_HR_REVIEW");
                    applicationStatusStore.put(applicationId, "PENDING_HEAD_HR_REVIEW");
                }
            }
            
            result.put("success", true);
            result.put("message", "Application approved successfully by " + role);
            result.put("applicationId", applicationId);
            result.put("role", role);
            result.put("decision", "approve");
            result.put("comments", comments);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("newStatus", appData.get("applicationStatus"));
            
            logger.info("Application {} approved by {}: {}", applicationId, role, comments);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to approve application {} by {}: {}", applicationId, role, e.getMessage());
            throw new RuntimeException("Failed to approve application: " + e.getMessage());
        }
    }
    
    /**
     * Reject an application by a specific role
     */
    public Map<String, Object> rejectApplication(String applicationId, String role, String comments) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get application data
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            // Find the Camunda task for this application and role (optional for Company Manager)
            String processInstanceId = (String) appData.get("processInstanceId");
            Task activeTask = null;
            
            if (processInstanceId != null) {
                activeTask = findTaskForRole(processInstanceId, role);
            }
            
            // For Company Manager, allow rejection even without Camunda task (referral applications)
            if (activeTask == null && !"companymanager".equals(role.toLowerCase())) {
                if (processInstanceId == null) {
                    throw new RuntimeException("No process instance found for application: " + applicationId);
                } else {
                    throw new RuntimeException("No active task found for role: " + role + " in application: " + applicationId);
                }
            }
            
            // Prepare task variables for rejection
            Map<String, Object> taskVariables = new HashMap<>();
            String rejectionStatus;
            
            switch (role.toLowerCase()) {
                case "hr":
                    taskVariables.put("hrDecision", "reject");
                    taskVariables.put("hrComments", comments != null ? comments : "");
                    appData.put("hrDecision", "reject");
                    appData.put("hrComments", comments);
                    rejectionStatus = "REJECTED_BY_HR";
                    break;
                    
                case "teamlead":
                case "tl":
                    taskVariables.put("tlDecision", "reject");
                    taskVariables.put("tlComments", comments != null ? comments : "");
                    appData.put("tlDecision", "reject");
                    appData.put("tlComments", comments);
                    rejectionStatus = "REJECTED_BY_TL_PM";
                    break;
                    
                case "projectmanager":
                case "pm":
                    taskVariables.put("pmDecision", "reject");
                    taskVariables.put("pmComments", comments != null ? comments : "");
                    appData.put("pmDecision", "reject");
                    appData.put("pmComments", comments);
                    rejectionStatus = "REJECTED_BY_TL_PM";
                    break;
                    
                case "headhr":
                    taskVariables.put("headHRDecision", "reject");
                    taskVariables.put("headHRComments", comments != null ? comments : "");
                    appData.put("headHRDecision", "reject");
                    appData.put("headHRComments", comments);
                    rejectionStatus = "REJECTED_BY_HEAD_HR";
                    break;
                    
                case "companymanager":
                    taskVariables.put("companyManagerDecision", "reject");
                    taskVariables.put("companyManagerComments", comments != null ? comments : "");
                    appData.put("companyManagerDecision", "reject");
                    appData.put("companyManagerComments", comments);
                    rejectionStatus = "REJECTED_BY_COMPANY_MANAGER";
                    break;
                    
                default:
                    throw new RuntimeException("Invalid role: " + role);
            }
            
            // Complete the Camunda task (optional for Company Manager with referral applications)
            if (activeTask != null) {
                taskService.complete(activeTask.getId(), taskVariables);
            } else if ("companymanager".equals(role.toLowerCase())) {
                // For Company Manager, allow rejection without Camunda task (referral applications)
                logger.info("Company Manager rejection for application {} without Camunda task (likely referral application)", applicationId);
            } else {
                throw new RuntimeException("No active task found for role: " + role + " in application: " + applicationId);
            }
            
            // Update application status to rejected
            appData.put("applicationStatus", rejectionStatus);
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            applicationStatusStore.put(applicationId, rejectionStatus);
            applicationDataStore.put(applicationId, appData);
            
            result.put("success", true);
            result.put("message", "Application rejected by " + role);
            result.put("applicationId", applicationId);
            result.put("role", role);
            result.put("decision", "reject");
            result.put("comments", comments);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("newStatus", rejectionStatus);
            
            logger.info("Application {} rejected by {}: {}", applicationId, role, comments);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to reject application {} by {}: {}", applicationId, role, e.getMessage());
            throw new RuntimeException("Failed to reject application: " + e.getMessage());
        }
    }
    
    /**
     * Get application status with approval details
     */
    public Map<String, Object> getApplicationStatus(String applicationId) {
        try {
            // Sync with Camunda first
            syncApplicationStatusWithCamunda();
            
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            Map<String, Object> status = new HashMap<>();
            status.put("applicationId", applicationId);
            status.put("currentStatus", appData.get("applicationStatus"));
            status.put("lastUpdated", appData.get("lastUpdatedTimestamp"));
            
            // Add approval details
            Map<String, Object> approvals = new HashMap<>();
            
            // HR approval
            if (appData.containsKey("hrDecision")) {
                Map<String, Object> hrApproval = new HashMap<>();
                hrApproval.put("decision", appData.get("hrDecision"));
                hrApproval.put("comments", appData.get("hrComments"));
                approvals.put("hr", hrApproval);
            }
            
            // Team Lead approval
            if (appData.containsKey("tlDecision")) {
                Map<String, Object> tlApproval = new HashMap<>();
                tlApproval.put("decision", appData.get("tlDecision"));
                tlApproval.put("comments", appData.get("tlComments"));
                approvals.put("teamLead", tlApproval);
            }
            
            // Project Manager approval
            if (appData.containsKey("pmDecision")) {
                Map<String, Object> pmApproval = new HashMap<>();
                pmApproval.put("decision", appData.get("pmDecision"));
                pmApproval.put("comments", appData.get("pmComments"));
                approvals.put("projectManager", pmApproval);
            }
            
            // Head HR approval
            if (appData.containsKey("headHRDecision")) {
                Map<String, Object> headHRApproval = new HashMap<>();
                headHRApproval.put("decision", appData.get("headHRDecision"));
                headHRApproval.put("comments", appData.get("headHRComments"));
                if (appData.containsKey("offerCTC")) {
                    headHRApproval.put("offerCTC", appData.get("offerCTC"));
                }
                approvals.put("headHR", headHRApproval);
            }
            
            // Company Manager approval
            if (appData.containsKey("companyManagerDecision")) {
                Map<String, Object> companyManagerApproval = new HashMap<>();
                companyManagerApproval.put("decision", appData.get("companyManagerDecision"));
                companyManagerApproval.put("comments", appData.get("companyManagerComments"));
                if (appData.containsKey("finalOfferCTC")) {
                    companyManagerApproval.put("finalOfferCTC", appData.get("finalOfferCTC"));
                }
                approvals.put("companyManager", companyManagerApproval);
            }
            
            status.put("approvals", approvals);
            status.put("applicationData", appData);
            
            return status;
            
        } catch (Exception e) {
            logger.error("Failed to get application status for {}: {}", applicationId, e.getMessage());
            throw new RuntimeException("Failed to get application status: " + e.getMessage());
        }
    }
    
    /**
     * Find the active Camunda task for a specific role
     */
    private Task findTaskForRole(String processInstanceId, String role) {
        try {
            List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
            
            for (Task task : activeTasks) {
                String taskName = task.getName();
                
                switch (role.toLowerCase()) {
                    case "hr":
                        if (taskName.contains("HR Application Review") || taskName.contains("HR Review")) {
                            return task;
                        }
                        break;
                    case "teamlead":
                    case "tl":
                        if (taskName.contains("Team Lead Review") || taskName.contains("TL Review")) {
                            return task;
                        }
                        break;
                    case "projectmanager":
                    case "pm":
                        if (taskName.contains("Project Manager Review") || taskName.contains("PM Review")) {
                            return task;
                        }
                        break;
                    case "headhr":
                        if (taskName.contains("Head HR Final Review") || taskName.contains("Head HR Review")) {
                            return task;
                        }
                        break;
                    case "companymanager":
                        if (taskName.contains("Company Manager Final Review") || taskName.contains("Company Manager Review")) {
                            return task;
                        }
                        break;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to find task for role {} in process {}: {}", role, processInstanceId, e.getMessage());
            return null;
        }
    }

    /**
     * HR hires a candidate (final step that sends congratulations email)
     */
    public Map<String, Object> hireCandidateByHR(String applicationId, String hrComments, String joiningDate, String department) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get application data
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            // Update application status to HIRED
            appData.put("applicationStatus", "HIRED");
            appData.put("hrHiringComments", hrComments);
            appData.put("joiningDate", joiningDate);
            appData.put("department", department);
            appData.put("hiredByHR", true);
            appData.put("hiredTimestamp", LocalDateTime.now().toString());
            applicationStatusStore.put(applicationId, "HIRED");
            
            // Send congratulations email with onboarding link
            try {
                boolean emailSent = emailService.sendHireNotificationEmail(appData);
                if (emailSent) {
                    appData.put("hireNotificationEmailSent", true);
                    appData.put("hireNotificationEmailSentAt", LocalDateTime.now().toString());
                    logger.info("Hire notification email sent successfully for application: {}", applicationId);
                } else {
                    appData.put("hireNotificationEmailSent", false);
                    logger.warn("Failed to send hire notification email for application: {}", applicationId);
                }
            } catch (Exception e) {
                logger.error("Error sending hire notification email for application {}: {}", applicationId, e.getMessage());
                appData.put("hireNotificationEmailSent", false);
            }
            
            // Update application data
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            applicationDataStore.put(applicationId, appData);
            
            result.put("success", true);
            result.put("message", "Candidate hired successfully by HR and email sent");
            result.put("applicationId", applicationId);
            result.put("newStatus", "HIRED");
            result.put("emailSent", appData.get("hireNotificationEmailSent"));
            result.put("joiningDate", joiningDate);
            result.put("department", department);
            result.put("timestamp", LocalDateTime.now().toString());
            
            logger.info("Candidate hired by HR for application {}: joining={}, dept={}", applicationId, joiningDate, department);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to hire candidate by HR for application {}: {}", applicationId, e.getMessage());
            throw new RuntimeException("Failed to hire candidate: " + e.getMessage());
        }
    }

    /**
     * Manually update application status to ACCEPTED and send email (for testing/fixing)
     */
    public Map<String, Object> markApplicationAsAcceptedAndSendEmail(String applicationId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get application data
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            // Update status to ACCEPTED
            appData.put("applicationStatus", "ACCEPTED");
            applicationStatusStore.put(applicationId, "ACCEPTED");
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            
            // Send congratulations email with onboarding link
            try {
                boolean emailSent = emailService.sendHireNotificationEmail(appData);
                if (emailSent) {
                    appData.put("hireNotificationEmailSent", true);
                    appData.put("hireNotificationEmailSentAt", LocalDateTime.now().toString());
                    logger.info("Hire notification email sent successfully for application: {}", applicationId);
                } else {
                    appData.put("hireNotificationEmailSent", false);
                    logger.warn("Failed to send hire notification email for application: {}", applicationId);
                }
            } catch (Exception e) {
                logger.error("Error sending hire notification email for application {}: {}", applicationId, e.getMessage());
                appData.put("hireNotificationEmailSent", false);
            }
            
            // Update application data
            applicationDataStore.put(applicationId, appData);
            
            result.put("success", true);
            result.put("message", "Application marked as ACCEPTED and email sent");
            result.put("applicationId", applicationId);
            result.put("newStatus", "ACCEPTED");
            result.put("emailSent", appData.get("hireNotificationEmailSent"));
            result.put("timestamp", LocalDateTime.now().toString());
            
            logger.info("Application {} manually marked as ACCEPTED and email sent", applicationId);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to mark application {} as ACCEPTED: {}", applicationId, e.getMessage());
            throw new RuntimeException("Failed to mark application as ACCEPTED: " + e.getMessage());
        }
    }

    private String generateApplicationId() {
        return "APP-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Initiate onboarding process for an application
     */
    public Map<String, Object> initiateOnboarding(String applicationId, String joiningDate, 
                                                 String reportingManager, String department, String comments) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get application data
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            // Find the HR Review task for this application
            String processInstanceId = (String) appData.get("processInstanceId");
            if (processInstanceId == null) {
                throw new RuntimeException("No process instance found for application: " + applicationId);
            }
            
            Task activeTask = findTaskForRole(processInstanceId, "hr");
            if (activeTask == null) {
                throw new RuntimeException("No active HR task found for application: " + applicationId);
            }
            
            // Prepare task variables for onboarding
            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("hrDecision", "onboarding");
            taskVariables.put("hrComments", comments != null ? comments : "");
            taskVariables.put("joiningDate", joiningDate);
            taskVariables.put("reportingManager", reportingManager);
            taskVariables.put("department", department);
            
            // Update application data
            appData.put("hrDecision", "onboarding");
            appData.put("hrComments", comments);
            appData.put("joiningDate", joiningDate);
            appData.put("reportingManager", reportingManager);
            appData.put("department", department);
            appData.put("applicationStatus", "ONBOARDING_INITIATED");
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            
            // Complete the Camunda task
            taskService.complete(activeTask.getId(), taskVariables);
            
            // Update stores
            applicationDataStore.put(applicationId, appData);
            applicationStatusStore.put(applicationId, "ONBOARDING_INITIATED");
            
            result.put("success", true);
            result.put("message", "Onboarding process initiated successfully");
            result.put("applicationId", applicationId);
            result.put("joiningDate", joiningDate);
            result.put("reportingManager", reportingManager);
            result.put("department", department);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("newStatus", "ONBOARDING_INITIATED");
            
            logger.info("Onboarding initiated for application {}: joining={}, manager={}, dept={}", 
                       applicationId, joiningDate, reportingManager, department);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to initiate onboarding for application {}: {}", applicationId, e.getMessage());
            throw new RuntimeException("Failed to initiate onboarding: " + e.getMessage());
        }
    }
    
    /**
     * Get application data by ID
     */
    public Map<String, Object> getApplicationById(String applicationId) {
        return applicationDataStore.get(applicationId);
    }
    
    /**
     * Complete the candidate onboarding process
     */
    public Map<String, Object> completeOnboarding(String applicationId, Map<String, Object> onboardingData) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get application data
            Map<String, Object> appData = applicationDataStore.get(applicationId);
            if (appData == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            
            // Find the Candidate Onboarding task for this application
            String processInstanceId = (String) appData.get("processInstanceId");
            
            // Update application data with comprehensive onboarding information
            appData.putAll(onboardingData);
            appData.put("applicationStatus", "ONBOARDING_COMPLETED");
            appData.put("onboardingCompletedDate", LocalDateTime.now().toString());
            appData.put("lastUpdatedTimestamp", LocalDateTime.now().toString());
            appData.put("processCompleted", true);
            
            // Complete the Camunda process if it exists
            if (processInstanceId != null) {
                try {
                    // Find any active tasks for this process
                    List<Task> activeTasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .active()
                        .list();
                    
                    // Complete all active tasks to end the process
                    for (Task task : activeTasks) {
                        Map<String, Object> taskVariables = new HashMap<>();
                        taskVariables.put("onboardingCompleted", true);
                        taskVariables.put("onboardingCompletionDate", LocalDateTime.now().toString());
                        taskVariables.putAll(onboardingData);
                        
                        taskService.complete(task.getId(), taskVariables);
                        logger.info("Completed Camunda task: {} for application: {}", task.getId(), applicationId);
                    }
                    
                    // Verify process has ended
                    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                    
                    if (processInstance == null) {
                        logger.info("Camunda process successfully ended for application: {}", applicationId);
                        appData.put("camundaProcessEnded", true);
                    } else {
                        logger.warn("Camunda process still active for application: {}", applicationId);
                        appData.put("camundaProcessEnded", false);
                    }
                    
                } catch (Exception e) {
                    logger.warn("Failed to complete Camunda process for application {}: {}", applicationId, e.getMessage());
                    // Continue without Camunda - the onboarding is still complete
                    appData.put("camundaProcessEnded", false);
                    appData.put("camundaError", e.getMessage());
                }
            } else {
                logger.info("No Camunda process found for application: {}", applicationId);
                appData.put("camundaProcessEnded", true);
            }
            
            // Update stores
            applicationDataStore.put(applicationId, appData);
            applicationStatusStore.put(applicationId, "ONBOARDING_COMPLETED");
            
            // Create comprehensive result
            result.put("success", true);
            result.put("message", "Professional onboarding completed successfully! Welcome to the team!");
            result.put("applicationId", applicationId);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("newStatus", "ONBOARDING_COMPLETED");
            result.put("processCompleted", true);
            result.put("candidateName", onboardingData.get("fullName"));
            result.put("joiningDate", onboardingData.get("expectedJoiningDate"));
            result.put("camundaProcessEnded", appData.get("camundaProcessEnded"));
            
            logger.info("Comprehensive onboarding completed for application {} - Candidate: {} - Camunda Process Ended: {}", 
                       applicationId, onboardingData.get("fullName"), appData.get("camundaProcessEnded"));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to complete onboarding for application {}: {}", applicationId, e.getMessage());
            throw new RuntimeException("Failed to complete onboarding: " + e.getMessage());
        }
    }
}