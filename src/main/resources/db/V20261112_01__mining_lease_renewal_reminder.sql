-- Tracks which lease term the "renew your lease" reminder was last sent for
-- (MiningLeaseRenewalReminderScheduler). Stores the lease_end_date at the time
-- of sending, so a renewal that moves lease_end_date re-arms the reminder.

ALTER TABLE t_mining_lease_application
    ADD COLUMN IF NOT EXISTS renewal_reminder_sent_for_end_date DATE;
