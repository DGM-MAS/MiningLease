-- Set when the declared temporary closure period has ended and the licence was
-- made active again (TemporaryClosureReinstatementScheduler). NULL = the closure
-- has not been reinstated yet.

ALTER TABLE t_temporary_closure
    ADD COLUMN IF NOT EXISTS reinstated_at TIMESTAMP;
