-- Latest-remarks fields added by commit 5b77698 ("changes from mining lease and
-- renewal latest remarks added"), fd0273d (PR #101) and the later PRs #102-#107
-- (temporary closure, termination, director assign task, work order upload, PA/FC
-- approval). The entities were merged without a matching migration, so image :53
-- fails Hibernate schema validation on startup with "missing column
-- [latest_remarks] in table [t_environment_clearance_renewal]" and the pod
-- crash-loops.

ALTER TABLE t_environment_clearance_renewal
    ADD COLUMN IF NOT EXISTS latest_remarks TEXT;

ALTER TABLE t_immediate_suspension_application
    ADD COLUMN IF NOT EXISTS latest_remarks TEXT;

ALTER TABLE t_sample_transport_clearance
    ADD COLUMN IF NOT EXISTS latest_remarks TEXT;

ALTER TABLE t_mining_lease_application
    ADD COLUMN IF NOT EXISTS latest_remark_focal TEXT;

ALTER TABLE t_mining_lease_renewal_application
    ADD COLUMN IF NOT EXISTS latest_remark_focal TEXT;

ALTER TABLE t_temporary_closure
    ADD COLUMN IF NOT EXISTS latest_remarks TEXT;

ALTER TABLE t_termination_application
    ADD COLUMN IF NOT EXISTS latest_remarks TEXT;
