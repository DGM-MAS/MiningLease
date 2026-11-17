package com.mas.gov.bt.mas.primary.scheduler;

import com.mas.gov.bt.mas.primary.entity.TemporaryClosureEntity;
import com.mas.gov.bt.mas.primary.services.TemporaryClosureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Makes a temporarily closed licence (mining lease, quarry lease, surface
 * collection permit or stock lifting) active again once the closure duration
 * the promoter declared has passed.
 *
 * The closure period is counted from the date the RC approved the closure:
 * end date = approvedAt + numberOfMonthsForClosure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemporaryClosureReinstatementScheduler {

    private final TemporaryClosureService temporaryClosureService;

    /*
     * Runs every day at 12:30 AM (server time).
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void reinstateEndedClosures() {

        log.info("Starting temporary closure reinstatement job");

        LocalDate today = LocalDate.now();

        List<TemporaryClosureEntity> closures =
                temporaryClosureService.findApprovedClosuresNotReinstated();

        int reinstated = 0;

        for (TemporaryClosureEntity closure : closures) {

            if (!isClosurePeriodOver(closure, today)) {
                continue;
            }

            try {
                // Each closure runs in its own transaction, so one failure does not undo the others.
                if (temporaryClosureService.reinstateAfterClosure(closure.getId())) {
                    reinstated++;
                }
            } catch (Exception e) {
                log.error(
                        "Failed to reinstate licence for temporary closure {} (application {})",
                        closure.getId(),
                        closure.getApplicationId(),
                        e
                );
            }
        }

        log.info(
                "Completed temporary closure reinstatement job: {} of {} open closures reinstated",
                reinstated,
                closures.size()
        );
    }

    private boolean isClosurePeriodOver(TemporaryClosureEntity closure, LocalDate today) {

        if (closure.getApprovedAt() == null
                || closure.getNumberOfMonthsForClosure() == null
                || closure.getNumberOfMonthsForClosure() <= 0) {

            log.warn(
                    "Skipping temporary closure {} (application {}): approval date or closure duration is missing",
                    closure.getId(),
                    closure.getApplicationId()
            );
            return false;
        }

        LocalDate closureEndDate = closure.getApprovedAt()
                .toLocalDate()
                .plusMonths(closure.getNumberOfMonthsForClosure());

        return !today.isBefore(closureEndDate);
    }
}
