-- UAT feedback: ME dashboard had no way to attach an ERB Utilization Letter when a
-- restoration completion report is decided "not satisfactory" (ERB_UTILIZED) — only the
-- ERB_RELEASED (satisfactory) path had a letter-upload column.

ALTER TABLE mas_db.t_mine_restoration_application
    ADD COLUMN IF NOT EXISTS erb_utilization_letter_doc_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS erb_utilization_letter_issued_at TIMESTAMP;
