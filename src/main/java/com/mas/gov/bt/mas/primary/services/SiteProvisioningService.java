package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.SiteMaster;
import com.mas.gov.bt.mas.primary.repository.SiteMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Creates the promotor's site in shared mas_db.site_master when a mining
 * lease reaches final approval. The site is what Mine TP / balances key on
 * and what the profile site switch lists.
 */
@Service
@Slf4j
public class SiteProvisioningService {

    private static final String LEASE_TYPE = "MINING_LEASE";

    private final SiteMasterRepository siteMasterRepository;

    public SiteProvisioningService(SiteMasterRepository siteMasterRepository) {
        this.siteMasterRepository = siteMasterRepository;
    }

    /** Idempotent — safe to call again if the approval step is re-run. */
    public void provisionSiteForApprovedLease(MiningLeaseApplication app) {
        if (siteMasterRepository.existsByLeaseTypeAndLeaseApplicationId(LEASE_TYPE, app.getId())) {
            return;
        }
        SiteMaster site = new SiteMaster();
        site.setSiteName(app.getNameOfMine() != null && !app.getNameOfMine().isBlank()
                ? app.getNameOfMine()
                : "Mine - " + app.getApplicationNumber());
        site.setApplicantUserId(app.getApplicantUserId());
        site.setLeaseType(LEASE_TYPE);
        site.setLeaseApplicationId(app.getId());
        site.setLeaseApplicationNumber(app.getApplicationNumber());
        site.setDzongkhagId(app.getDzongkhag() != null ? app.getDzongkhag().getId() : null);
        site.setGewogNameId(app.getGewog() != null ? String.valueOf(app.getGewog().getGewogSerialNo()) : null);
        site.setDungkhagName(app.getDungkhag());
        site.setNearestVillageId(app.getNearestVillage() != null
                ? String.valueOf(app.getNearestVillage().getVillageSerialNo()) : null);
        site.setPlace(app.getPlaceOfMiningActivity());
        site.setCreatedBy("system-lease-approval");
        siteMasterRepository.save(site);
        log.info("Provisioned site '{}' for approved mining lease {}", site.getSiteName(), app.getApplicationNumber());
    }
}
