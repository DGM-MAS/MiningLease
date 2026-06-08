-- Add manual entry tracking fields to the three regular permit tables
-- and make the attachment table's manual_entry_id column nullable (now
-- uses application_no as the reference for new-style records).

-- ── Mining Lease ─────────────────────────────────────────────────────────────
ALTER TABLE mas_db.t_mining_lease_application
    ADD COLUMN IF NOT EXISTS manual_entry_by  BIGINT,
    ADD COLUMN IF NOT EXISTS manual_entry_on  TIMESTAMP;

-- ── Quarry Lease ─────────────────────────────────────────────────────────────
ALTER TABLE mas_db.t_quarry_lease_application
    ADD COLUMN IF NOT EXISTS is_manual_entry  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS manual_entry_by  BIGINT,
    ADD COLUMN IF NOT EXISTS manual_entry_on  TIMESTAMP;

-- ── Surface Collection Permit ────────────────────────────────────────────────
ALTER TABLE mas_db.surface_collection_permit
    ADD COLUMN IF NOT EXISTS is_manual_entry  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS manual_entry_by  BIGINT,
    ADD COLUMN IF NOT EXISTS manual_entry_on  TIMESTAMP;

-- ── Manual Mining Attachment ─────────────────────────────────────────────────
-- Drop the NOT NULL constraint so existing records are unaffected
ALTER TABLE mas_db.t_manual_mining_attachment
    ALTER COLUMN manual_entry_id DROP NOT NULL;

ALTER TABLE mas_db.t_manual_mining_attachment
    ADD COLUMN IF NOT EXISTS application_no VARCHAR(50);
