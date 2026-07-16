package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseRenewalApplication;
import com.mas.gov.bt.mas.primary.entity.SiteMaster;
import com.mas.gov.bt.mas.primary.repository.SiteMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    /**
     * Idempotent — safe to call again if the approval step is re-run. Returns
     * the site (existing or newly created) so the caller can retroactively
     * stamp site_id onto the originating lease application's own
     * t_application_master row.
     */
    public SiteMaster provisionSiteForApprovedLease(MiningLeaseApplication app) {
        Optional<SiteMaster> existing = siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(LEASE_TYPE, app.getApplicationNumber());
        if (existing.isPresent()) {
            return existing.get();
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
        return site;
    }

    /**
     * Refreshes the existing site_master row's location fields when a renewal is approved.
     * Renewal applications share the original lease's applicationNumber (not its id), so the
     * row is matched by leaseApplicationNumber. Renewals don't carry a mine-name field, so
     * site_name is intentionally left untouched here — only location can drift at renewal.
     */
    public void refreshSiteLocationForRenewal(MiningLeaseRenewalApplication app) {
        siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(LEASE_TYPE, app.getApplicationNumber())
                .ifPresent(site -> {
                    site.setDzongkhagId(app.getDzongkhag() != null ? app.getDzongkhag().getId() : site.getDzongkhagId());
                    site.setGewogNameId(app.getGewog() != null ? String.valueOf(app.getGewog().getGewogSerialNo()) : site.getGewogNameId());
                    site.setDungkhagName(app.getDungkhag() != null ? app.getDungkhag() : site.getDungkhagName());
                    site.setNearestVillageId(app.getNearestVillage() != null
                            ? String.valueOf(app.getNearestVillage().getVillageSerialNo()) : site.getNearestVillageId());
                    site.setPlace(app.getPlaceOfMiningActivity() != null ? app.getPlaceOfMiningActivity() : site.getPlace());
                    siteMasterRepository.save(site);
                    log.info("Refreshed site_master location for renewed mining lease {}", app.getApplicationNumber());
                });
    }

    /**
     * Flips the shared site_master row's is_active flag when a lease enters or leaves a
     * SUSPENDED/TERMINATED/TEMPORARY CLOSURE APPROVED state. ActiveSiteService (mas-royalty-service)
     * already rejects an inactive site, so this alone blocks Mine TP and every other site-scoped
     * application (balances, other TP variants) without touching those services directly.
     * leaseType is passed explicitly (not the class's own LEASE_TYPE constant) since Termination/
     * ImmediateSuspension/TemporaryClosure cover both Mining Lease and Quarry Lease from one call site.
     */
    public void setSiteActive(String leaseType, String applicationNumber, boolean active) {
        if (leaseType == null || applicationNumber == null) {
            return;
        }
        siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(leaseType, applicationNumber)
                .ifPresent(site -> {
                    site.setIsActive(active);
                    siteMasterRepository.save(site);
                    log.info("Set site_master.is_active={} for {} lease {}", active, leaseType, applicationNumber);
                });
    }
}
