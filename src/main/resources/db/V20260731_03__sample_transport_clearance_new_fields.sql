-- UAT feedback (Sample Shipping Clearance): Applicant Scope (In-country/Ex-country),
-- destination country for Ex-country applications, a "specify" field for Rock/Mineral
-- Name = Other, and mandatory Sample Photo + optional Others attachments.

ALTER TABLE t_sample_transport_clearance
    ADD COLUMN IF NOT EXISTS applicant_scope VARCHAR(50),
    ADD COLUMN IF NOT EXISTS destination_country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rock_mineral_name_specify VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sample_photo_file_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS others_file_id VARCHAR(100);
