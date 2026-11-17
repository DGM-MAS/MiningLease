-- Tracks which environmental clearance the "renew your EC" reminder was last sent
-- for (EnvironmentalClearanceRenewalReminderScheduler). Stores ec_expiry_date at
-- the time of sending, so an EC renewal that moves ec_expiry_date re-arms the
-- reminder. Same type as ec_expiry_date so the two compare exactly.

ALTER TABLE t_mining_lease_application
    ADD COLUMN IF NOT EXISTS ec_renewal_reminder_sent_for_expiry_date TIMESTAMP;
