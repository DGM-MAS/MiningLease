package com.mas.gov.bt.mas.primary.audit;

import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Every submit/approve/reject/reassign/revision path across this service's ~14 lifecycle
 * service classes (MiningLeaseService, MiningLeaseRenewalService, TerminationService,
 * MineRestorationService, ImmediateSuspensionService, etc. — ~260+ individual call sites)
 * eventually calls ApplicationMasterRepository.save(...) to record the transition in
 * t_application_master. Intercepting that one method gives full audit coverage without
 * touching any of that business logic.
 *
 * The "old status" is read via a raw JDBC query rather than the JPA repository/entity
 * manager: within the same persistence context, findById() would return the identical
 * managed Java object that the caller already mutated in memory (JPA identity map), which
 * would make old == new for every update. A plain SQL SELECT bypasses that identity map and
 * sees the still-persisted value, since Hibernate defers the actual UPDATE statement until
 * flush/commit.
 */
@Aspect
@Component
public class ApplicationMasterAuditAspect {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditLogService auditLogService;

    @Around("execution(* com.mas.gov.bt.mas.primary.repository.ApplicationMasterRepository.save(..)) && args(entity)")
    public Object aroundSave(ProceedingJoinPoint pjp, ApplicationMaster entity) throws Throwable {
        boolean isNew = entity.getId() == null;
        String oldStatus = isNew ? null : fetchPersistedStatus(entity.getId());

        Object result = pjp.proceed();

        String newStatus = entity.getCurrentStatus();
        if (isNew) {
            auditLogService.logAction("APPLICATION_SUBMITTED",
                    "Application " + entity.getApplicationNumber() + " (" + entity.getServiceCode() + ") submitted",
                    entity.getServiceCode(), entity.getId(), null, newStatus);
        } else if (oldStatus == null ? newStatus != null : !oldStatus.equals(newStatus)) {
            auditLogService.logAction("STATUS_CHANGE",
                    "Application " + entity.getApplicationNumber() + " (" + entity.getServiceCode() + ") status changed",
                    entity.getServiceCode(), entity.getId(), oldStatus, newStatus);
        }
        return result;
    }

    private String fetchPersistedStatus(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT current_status FROM mas_db.t_application_master WHERE id = ?",
                    String.class, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
