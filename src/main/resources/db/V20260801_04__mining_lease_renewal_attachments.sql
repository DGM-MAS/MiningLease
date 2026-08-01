-- Mining Lease Renewal (UAT Paro, Day 4 "Lease renewal"):
-- 4 new attachments on the renewal application, and a Geologist "Approval Letter"
-- attachment required when approving the Geological Assessment Report.
ALTER TABLE t_mining_lease_renewal_application
    ADD COLUMN IF NOT EXISTS geological_assessment_report_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS geological_maps_cross_sections_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS kmz_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS valid_tax_clearance_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS geologist_approval_letter_doc_id VARCHAR(255);
