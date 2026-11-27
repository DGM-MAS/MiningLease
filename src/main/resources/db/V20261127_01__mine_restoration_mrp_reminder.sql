-- Tracks which lease / permit end date the "submit your Mine Restoration Plan"
-- reminder was last sent for (MineRestorationPlanReminderScheduler). Stores the
-- end date at the time of sending, so a renewal that moves the end date re-arms
-- the reminder for the new term. Each column has the same type as the end-date
-- column it mirrors so the two compare exactly.

ALTER TABLE mas_db.t_mining_lease_application
    ADD COLUMN IF NOT EXISTS mrp_reminder_sent_for_end_date DATE;           -- mirrors lease_end_date

ALTER TABLE mas_db.t_quarry_lease_application
    ADD COLUMN IF NOT EXISTS mrp_reminder_sent_for_end_date DATE;           -- mirrors lease_end_date

ALTER TABLE mas_db.surface_collection_permit
    ADD COLUMN IF NOT EXISTS mrp_reminder_sent_for_end_date TIMESTAMP;      -- mirrors permit_validity_to

ALTER TABLE mas_db.stock_lifting_application
    ADD COLUMN IF NOT EXISTS mrp_reminder_sent_for_end_date TIMESTAMP;      -- mirrors validity_date
