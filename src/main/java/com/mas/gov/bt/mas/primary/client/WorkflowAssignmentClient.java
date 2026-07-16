package com.mas.gov.bt.mas.primary.client;

import com.mas.gov.bt.mas.primary.dto.workflow.ResolveAssigneeApiRequest;
import com.mas.gov.bt.mas.primary.dto.workflow.ResolvedAssigneeDto;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for the admin-configured application auto-assignment resolver in
 * mas-backend-masters (/api/workflow-assignment/resolve). Replaces the
 * hardcoded "least busy user" native queries this service used to run
 * itself — the role/permission/region rule for each application status now
 * lives in mas_db.t_workflow_step, editable from the admin dashboard.
 *
 * Follows this repo's existing RestTemplate convention (see
 * MastersPaymentClient) rather than introducing WebClient — this repo has no
 * webflux dependency today. The internal-API-key header is set explicitly
 * per-request (this endpoint is guarded by InternalApiKeyFilter, not a user
 * JWT), rather than routing through the shared AppConfig RestTemplate
 * interceptor, which is scoped to /api/notifications only.
 */
@Component
@Slf4j
public class WorkflowAssignmentClient {

    private final RestTemplate restTemplate;

    @Value("${app.masters.base-url}")
    private String mastersBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public WorkflowAssignmentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Resolves the least-busy eligible user for the given application status,
     * per the matching t_workflow_step rule (region, then HQ region, then
     * role/permission-only). regionId is honored only when the matched rule
     * is region-scoped — always pass the application's real region regardless
     * of whether you know the rule is region-scoped, so flipping that setting
     * on the admin screen takes effect without a code change here.
     * applicationNumber is required so masters can record an escalation task
     * when no eligible candidate is found.
     *
     * When no eligible candidate exists (or dynamic assignment is globally
     * off), the returned DTO has escalated=true and userId=null — this is a
     * normal outcome, not an error. Masters has already written the
     * unassigned task to the escalation queue; callers must NOT create their
     * own task in that case.
     *
     * @throws BusinessException(RECORD_NOT_FOUND) if no rule is configured for
     *         this service/status, or the call to masters otherwise failed.
     */
    public ResolvedAssigneeDto resolve(String serviceCode, String triggerStatus, Long regionId, String applicationNumber) {
        String url = mastersBaseUrl + "/api/workflow-assignment/resolve";
        ResolveAssigneeApiRequest request = new ResolveAssigneeApiRequest(serviceCode, triggerStatus, regionId, applicationNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalApiKey);

        try {
            ResponseEntity<SuccessResponse<ResolvedAssigneeDto>> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<SuccessResponse<ResolvedAssigneeDto>>() {});

            if (response.getBody() == null || response.getBody().getData() == null) {
                log.warn("Workflow assignment resolve returned no data for {}/{}", serviceCode, triggerStatus);
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No workflow assignment rule configured");
            }
            return response.getBody().getData();
        } catch (HttpStatusCodeException e) {
            log.warn("Workflow assignment resolve failed for {}/{}: status={}, body={}",
                    serviceCode, triggerStatus, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No eligible assignee could be resolved");
        }
    }
}
