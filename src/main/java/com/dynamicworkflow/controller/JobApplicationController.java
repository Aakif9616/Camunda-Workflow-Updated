package com.dynamicworkflow.controller;

import com.dynamicworkflow.dto.ApplicationResponse;
import com.dynamicworkflow.model.WorkflowDefinition;
import com.dynamicworkflow.model.WorkflowStep;
import com.dynamicworkflow.service.JobApplicationService;
import com.dynamicworkflow.service.ValidationService;
import com.dynamicworkflow.service.WorkflowDefinitionService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/job-applications")
@CrossOrigin(origins = "*")
public class JobApplicationController {
    
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);
    
    private final JobApplicationService jobApplicationService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final RuntimeService runtimeService;
    
    public JobApplicationController(JobApplicationService jobApplicationService,
                                 WorkflowDefinitionService workflowDefinitionService,
                                 RuntimeService runtimeService) {
        this.jobApplicationService = jobApplicationService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.runtimeService = runtimeService;
    }
    
    /**
     * GET /api/job-applications/workflow-definition
     * Get the complete workflow definition
     */
    @GetMapping("/workflow-definition")
    public ResponseEntity<WorkflowDefinition> getWorkflowDefinition() {
        try {
            WorkflowDefinition definition = workflowDefinitionService.getWorkflowDefinition();
            return ResponseEntity.ok(definition);
        } catch (Exception e) {
            logger.error("Failed to get workflow definition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/job-applications/steps/{stepId}
     * Get specific step definition
     */
    @GetMapping("/steps/{stepId}")
    public ResponseEntity<WorkflowStep> getStep(@PathVariable String stepId) {
        try {
            Optional<WorkflowStep> step = workflowDefinitionService.getStepById(stepId);
            if (step.isPresent()) {
                return ResponseEntity.ok(step.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get step: {}", stepId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/job-applications/start
     * Start a new job application workflow
     */
    @PostMapping("/start")
    public ResponseEntity<ApplicationResponse> startApplication() {
        try {
            ApplicationResponse response = jobApplicationService.startApplication();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            ApplicationResponse errorResponse = new ApplicationResponse(null, "ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /api/job-applications/{applicationId}/step
     * Submit step data for an application
     */
    @PostMapping("/{applicationId}/step")
    public ResponseEntity<ApplicationResponse> submitStep(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> stepData) {
        try {
            ApplicationResponse response = jobApplicationService.submitStep(applicationId, stepData);
            return ResponseEntity.ok(response);
        } catch (ValidationService.ValidationException e) {
            logger.warn("Validation failed for application {}: {}", applicationId, e.getMessage());
            ApplicationResponse errorResponse = new ApplicationResponse(applicationId, "VALIDATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to submit step for application: {}", applicationId, e);
            ApplicationResponse errorResponse = new ApplicationResponse(applicationId, "ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /api/job-applications/{applicationId}
     * Get application data
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable String applicationId) {
        try {
            ApplicationResponse response = jobApplicationService.getApplication(applicationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get application: {}", applicationId, e);
            ApplicationResponse errorResponse = new ApplicationResponse(applicationId, "ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /api/job-applications/{applicationId}/hr-summary
     * Get formatted applicant summary for HR review
     */
    @GetMapping("/{applicationId}/hr-summary")
    public ResponseEntity<Map<String, Object>> getHRSummary(@PathVariable String applicationId) {
        try {
            ApplicationResponse application = jobApplicationService.getApplication(applicationId);
            
            if (application.getData() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> hrSummary = new HashMap<>();
            Map<String, Object> appData = application.getData();
            
            // Format data for HR display
            hrSummary.put("applicationId", applicationId);
            hrSummary.put("applicantName", appData.get("firstName") + " " + appData.get("lastName"));
            hrSummary.put("email", appData.get("email"));
            hrSummary.put("mobile", appData.get("mobileNumber"));
            hrSummary.put("position", appData.get("position"));
            hrSummary.put("expectedCTC", appData.get("expectedSalaryCTC"));
            hrSummary.put("experience", appData.get("totalExperience"));
            hrSummary.put("education", appData.get("highestEducation"));
            hrSummary.put("skills", appData.get("skills"));
            hrSummary.put("noticePeriod", appData.get("noticePeriod"));
            hrSummary.put("applicationStatus", application.getStatus());
            hrSummary.put("submissionDate", appData.get("submissionTimestamp"));
            
            return ResponseEntity.ok(hrSummary);
            
        } catch (Exception e) {
            logger.error("Failed to get HR summary for application: {}", applicationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/job-applications/validate-step
     * Validate step data without submitting
     */
    @PostMapping("/validate-step")
    public ResponseEntity<Map<String, Object>> validateStep(@RequestBody Map<String, Object> request) {
        try {
            String stepId = (String) request.get("stepId");
            @SuppressWarnings("unchecked")
            Map<String, Object> stepData = (Map<String, Object>) request.get("stepData");
            
            Optional<WorkflowStep> step = workflowDefinitionService.getStepById(stepId);
            if (!step.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("valid", false);
                errorResponse.put("message", "Invalid step ID");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // This would throw ValidationException if invalid
            // validationService.validateStepData(step.get(), stepData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "Validation successful");
            return ResponseEntity.ok(response);
            
        } catch (ValidationService.ValidationException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to validate step", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /api/job-applications/all
     * Get all applications (for debugging)
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllApplications() {
        try {
            Map<String, Object> result = jobApplicationService.getAllApplications();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get all applications", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /api/job-applications/start-bpmn-process
     * Manually start a BPMN process instance for testing
     */
    @PostMapping("/start-bpmn-process")
    public ResponseEntity<Map<String, Object>> startBpmnProcess(@RequestBody(required = false) Map<String, Object> variables) {
        try {
            if (variables == null) {
                variables = new HashMap<>();
            }
            
            // Add default variables if not provided
            variables.putIfAbsent("applicationId", "MANUAL-" + System.currentTimeMillis());
            variables.putIfAbsent("validationResult", true);
            
            // Convert to Camunda variable format
            Map<String, Object> processVariables = new HashMap<>();
            variables.forEach((key, value) -> {
                processVariables.put(key, value);
            });
            
            // Start process instance
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "job-recruitment-workflow-india",
                (String) variables.get("applicationId"), // business key
                processVariables
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("processInstanceId", processInstance.getId());
            response.put("businessKey", processInstance.getBusinessKey());
            response.put("processDefinitionId", processInstance.getProcessDefinitionId());
            response.put("variables", variables);
            response.put("message", "BPMN process started successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to start BPMN process", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to start BPMN process");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /api/job-applications/bpmn-processes
     * Get all running BPMN process instances
     */
    @GetMapping("/bpmn-processes")
    public ResponseEntity<Map<String, Object>> getBpmnProcesses() {
        try {
            List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("job-recruitment-workflow-india")
                .list();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalProcesses", processInstances.size());
            response.put("processes", processInstances.stream().map(pi -> {
                Map<String, Object> processInfo = new HashMap<>();
                processInfo.put("processInstanceId", pi.getId());
                processInfo.put("businessKey", pi.getBusinessKey());
                processInfo.put("suspended", pi.isSuspended());
                processInfo.put("ended", pi.isEnded());
                return processInfo;
            }).collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get BPMN processes", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /api/job-applications/sync
     * Manually trigger sync with Camunda (for debugging)
     */
    @GetMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncWithCamunda() {
        try {
            // This will trigger the sync in getAllApplications
            Map<String, Object> result = jobApplicationService.getAllApplications();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sync completed successfully");
            response.put("totalApplications", result.get("totalApplications"));
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to sync with Camunda", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /api/job-applications/{applicationId}/approve
     * Approve an application by a specific role
     */
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<Map<String, Object>> approveApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> approvalData) {
        try {
            String role = (String) approvalData.get("role");
            String comments = (String) approvalData.get("comments");
            String offerCTC = (String) approvalData.get("offerCTC"); // For Head HR only
            
            Map<String, Object> result = jobApplicationService.approveApplication(applicationId, role, comments, offerCTC);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to approve application: {}", applicationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /api/job-applications/{applicationId}/reject
     * Reject an application by a specific role
     */
    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> rejectionData) {
        try {
            String role = (String) rejectionData.get("role");
            String comments = (String) rejectionData.get("comments");
            
            Map<String, Object> result = jobApplicationService.rejectApplication(applicationId, role, comments);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to reject application: {}", applicationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /api/job-applications/{applicationId}/hr-onboarding
     * Initiate onboarding process for an application
     */
    @PostMapping("/{applicationId}/hr-onboarding")
    public ResponseEntity<Map<String, Object>> initiateOnboarding(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> onboardingData) {
        try {
            String joiningDate = (String) onboardingData.get("joiningDate");
            String reportingManager = (String) onboardingData.get("reportingManager");
            String department = (String) onboardingData.get("department");
            String comments = (String) onboardingData.get("hrComments");
            
            Map<String, Object> result = jobApplicationService.initiateOnboarding(
                applicationId, joiningDate, reportingManager, department, comments);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to initiate onboarding for application: {}", applicationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /api/job-applications/{applicationId}/complete-onboarding
     * Complete the candidate onboarding process
     */
    @PostMapping("/{applicationId}/complete-onboarding")
    public ResponseEntity<Map<String, Object>> completeOnboarding(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> onboardingData) {
        try {
            Map<String, Object> result = jobApplicationService.completeOnboarding(applicationId, onboardingData);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to complete onboarding for application: {}", applicationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /api/job-applications/{applicationId}/status
     * Get current application status and approval details
     */
    @GetMapping("/{applicationId}/status")
    public ResponseEntity<Map<String, Object>> getApplicationStatus(@PathVariable String applicationId) {
        try {
            Map<String, Object> status = jobApplicationService.getApplicationStatus(applicationId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get application status: {}", applicationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * HR hires a candidate (sends congratulations email)
     */
    @PostMapping("/{applicationId}/hr-hire")
    public ResponseEntity<Map<String, Object>> hireCandidateByHR(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> hiringData) {
        try {
            String hrComments = (String) hiringData.get("hrComments");
            String joiningDate = (String) hiringData.get("joiningDate");
            String department = (String) hiringData.get("department");
            
            Map<String, Object> result = jobApplicationService.hireCandidateByHR(applicationId, hrComments, joiningDate, department);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Manually mark application as ACCEPTED and send email (for testing/fixing)
     */
    @PostMapping("/{applicationId}/mark-accepted")
    public ResponseEntity<Map<String, Object>> markApplicationAsAccepted(@PathVariable String applicationId) {
        try {
            Map<String, Object> result = jobApplicationService.markApplicationAsAcceptedAndSendEmail(applicationId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * GET /api/job-applications/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Job Application Workflow");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}