package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Shared mas_db.t_user_active_site table (owned by mas-backend-masters) —
 * one row per citizen, pointing at the site they're currently working under.
 * Mapped here read-only so promotor-facing pickers in this repo can be
 * scoped to "my own active site" instead of returning every promotor's data.
 */
@Entity
@Data
@Table(name = "t_user_active_site", schema = "mas_db")
public class UserActiveSite {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "site_id")
    private Long siteId;
}
