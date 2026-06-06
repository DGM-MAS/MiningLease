-- ============================================================
-- Migration: Expand t_manual_mining_entry for unified
--            manual entry (Mining, Quarry, SC, Stock Lifting)
-- Date: 2026-06-06
-- ============================================================

-- -------------------------------------------------------
-- DROP obsolete review-flow columns (no longer needed)
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    DROP COLUMN IF EXISTS assigned_chief_id,
    DROP COLUMN IF EXISTS assigned_chief_remarks,
    DROP COLUMN IF EXISTS assigned_director_id,
    DROP COLUMN IF EXISTS assigned_director_remarks;

-- -------------------------------------------------------
-- ADD manual entry flag
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS is_manual_entry BOOLEAN NOT NULL DEFAULT TRUE;

-- -------------------------------------------------------
-- ADD applicant / company fields  (all types)
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS applicant_type              VARCHAR(50),
    ADD COLUMN IF NOT EXISTS applicant_cid               VARCHAR(11),
    ADD COLUMN IF NOT EXISTS applicant_name              VARCHAR(255),
    ADD COLUMN IF NOT EXISTS applicant_contact           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS applicant_email             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS postal_address              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS telephone_no                VARCHAR(20),
    ADD COLUMN IF NOT EXISTS license_no                  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS business_license_no         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS company_registration_no     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS company_name                VARCHAR(255),
    ADD COLUMN IF NOT EXISTS company_type                VARCHAR(255);

-- -------------------------------------------------------
-- ADD location fields  (all types)
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS dzongkhag      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gewog          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS nearest_village VARCHAR(100),
    ADD COLUMN IF NOT EXISTS place_of_activity VARCHAR(255),
    ADD COLUMN IF NOT EXISTS dungkhag       VARCHAR(100);

-- -------------------------------------------------------
-- ADD Mining Lease / Quarry Lease — mine details
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS type_of_mines                       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS type_of_minerals                    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS required_investment                 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source_of_finance                   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS technical_competence_experience     TEXT,
    ADD COLUMN IF NOT EXISTS workforce_requirement_recruitment   TEXT,
    ADD COLUMN IF NOT EXISTS proposed_lease_period               VARCHAR(50),
    ADD COLUMN IF NOT EXISTS srf                                 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS land_private                        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS total_land                          VARCHAR(100);

-- -------------------------------------------------------
-- ADD Mining Lease / Quarry Lease — approved / final details
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS approved_area           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approved_erb            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approved_lease_period   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS approved_mineral        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS lease_start_date        DATE,
    ADD COLUMN IF NOT EXISTS lease_end_date          DATE,
    ADD COLUMN IF NOT EXISTS lease_period_years      INTEGER,
    ADD COLUMN IF NOT EXISTS upfront_payment_amount  NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS fmfs_status             VARCHAR(30),
    ADD COLUMN IF NOT EXISTS fmfs_id                 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ec_status               VARCHAR(30),
    ADD COLUMN IF NOT EXISTS ec_expiry_date          DATE,
    ADD COLUMN IF NOT EXISTS mla_status              VARCHAR(30),
    ADD COLUMN IF NOT EXISTS geological_report_status VARCHAR(30);

-- -------------------------------------------------------
-- ADD Mining Lease / Quarry Lease — documents
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS pfs_doc_id                    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location_map_doc_id           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS financial_capability_doc_id   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS exploration_report_doc_id     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS consent_letter_doc_id         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS geological_report_doc_id      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS fmfs_doc_id                   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS llc_doc_id                    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS notesheet_doc_id              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS mla_doc_id                    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS file_upload_id_gr             BIGINT,
    ADD COLUMN IF NOT EXISTS file_upload_id_kmz            BIGINT,
    ADD COLUMN IF NOT EXISTS file_upload_id_pa             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_upload_id_fc             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_upload_id_public_clearance VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mpcd_file_upload_id_pa        BIGINT,
    ADD COLUMN IF NOT EXISTS mpcd_file_upload_id_ma        BIGINT,
    ADD COLUMN IF NOT EXISTS signed_pfs_id                 BIGINT,
    ADD COLUMN IF NOT EXISTS bank_guarantor_doc_id         BIGINT,
    ADD COLUMN IF NOT EXISTS work_order_doc_id             BIGINT;

-- -------------------------------------------------------
-- ADD Surface Collection / Stock Lifting — activity details
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS type_of_activity            TEXT,
    ADD COLUMN IF NOT EXISTS type_of_materials           TEXT,
    ADD COLUMN IF NOT EXISTS collection_site             TEXT,
    ADD COLUMN IF NOT EXISTS proposed_area_srf           DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS proposed_area_state_land    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS proposed_area_private       DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS proposed_area_row           DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS permit_no                   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ec_no                       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS security_clearance_validity VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tax_clearance_validity      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_state_owned              BOOLEAN,
    ADD COLUMN IF NOT EXISTS is_rp_based                 BOOLEAN;

-- -------------------------------------------------------
-- ADD Surface Collection / Stock Lifting — documents
-- -------------------------------------------------------
ALTER TABLE mas_db.t_manual_mining_entry
    ADD COLUMN IF NOT EXISTS attachment_map_file_id          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS recommendation_letter_file_id   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sc_consent_letter_file_id       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fc_file_id                      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS iee_file_id                     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS emp_file_id                     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS adm_approval_file_id            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS undertaking_file_id             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS bg_file_id                      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mpcd_report_file_id             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS iom_file_id                     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS rc_report_file_id               VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mi_report_file_id               VARCHAR(255),
    ADD COLUMN IF NOT EXISTS me_report_file_id               VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sc_ec_file_id                   VARCHAR(255);
