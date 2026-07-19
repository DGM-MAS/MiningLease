package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseRenewalApplication;
import com.mas.gov.bt.mas.primary.entity.QuarryLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.SiteMaster;
import com.mas.gov.bt.mas.primary.entity.StockLiftingApplication;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAuctionApplication;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermitEntity;
import com.mas.gov.bt.mas.primary.repository.SiteMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates the promotor's site in shared mas_db.site_master when a mining
 * lease reaches final approval. The site is what Mine TP / balances key on
 * and what the profile site switch lists.
 */
@Service
@Slf4j
public class SiteProvisioningService {

    private static final String LEASE_TYPE = "MINING_LEASE";
    private static final String QUARRY_LEASE_TYPE = "QUARRY_LEASE";
    private static final String SURFACE_COLLECTION_TYPE = "SURFACE_COLLECTION";
    private static final String STOCK_LIFTING_TYPE = "STOCK_LIFTING";

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
     * Mirrors Quarrying-Lease's own SiteProvisioningService.provisionSiteForApprovedLease —
     * needed here too because ManualMiningEntryServiceImpl writes quarry lease manual
     * entries directly into the shared t_quarry_lease_application table from this service,
     * bypassing the Quarrying-Lease microservice's real approval flow entirely.
     */
    public SiteMaster provisionSiteForApprovedLease(QuarryLeaseApplication app) {
        Optional<SiteMaster> existing = siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(QUARRY_LEASE_TYPE, app.getApplicationNumber());
        if (existing.isPresent()) {
            return existing.get();
        }
        SiteMaster site = new SiteMaster();
        site.setSiteName(app.getNameOfQuarry() != null && !app.getNameOfQuarry().isBlank()
                ? app.getNameOfQuarry()
                : "Quarry - " + app.getApplicationNumber());
        site.setApplicantUserId(app.getApplicantUserId());
        site.setLeaseType(QUARRY_LEASE_TYPE);
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
        log.info("Provisioned site '{}' for approved quarry lease {}", site.getSiteName(), app.getApplicationNumber());
        return site;
    }

    /**
     * Mirrors mas-royalty-service's own SiteProvisioningService.provisionSiteForSurfaceCollection —
     * needed here too because ManualMiningEntryServiceImpl writes surface collection manual
     * entries directly into the shared surface_collection_permit table from this service.
     */
    public SiteMaster provisionSiteForSurfaceCollection(SurfaceCollectionPermitEntity entity) {
        Optional<SiteMaster> existing = siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(SURFACE_COLLECTION_TYPE, entity.getApplicationNo());
        if (existing.isPresent()) {
            return existing.get();
        }
        SiteMaster site = new SiteMaster();
        site.setSiteName(entity.getNameOfSurfaceCollection() != null && !entity.getNameOfSurfaceCollection().isBlank()
                ? entity.getNameOfSurfaceCollection()
                : "Surface Collection - " + entity.getApplicationNo());
        site.setApplicantUserId(entity.getCreatedBy());
        site.setLeaseType(SURFACE_COLLECTION_TYPE);
        site.setLeaseApplicationId(entity.getId());
        site.setLeaseApplicationNumber(entity.getApplicationNo());
        site.setPlace(joinLocation(entity.getPlaceVillage(), entity.getGewog(), entity.getDzongkhag()));
        site.setCreatedBy("system-scp-approval");
        siteMasterRepository.save(site);
        log.info("Provisioned site '{}' for approved surface collection permit {}", site.getSiteName(), entity.getApplicationNo());
        return site;
    }

    /**
     * Mirrors mas-royalty-service's own SiteProvisioningService.provisionSiteForStockLifting —
     * needed here too because ManualMiningEntryServiceImpl writes stock lifting manual
     * entries directly into the shared stock_lifting_application table from this service.
     */
    public SiteMaster provisionSiteForStockLifting(StockLiftingApplication app) {
        Optional<SiteMaster> existing = siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(STOCK_LIFTING_TYPE, app.getApplicationNo());
        if (existing.isPresent()) {
            return existing.get();
        }
        SiteMaster site = new SiteMaster();
        site.setSiteName(app.getNameOfStockLifting() != null && !app.getNameOfStockLifting().isBlank()
                ? app.getNameOfStockLifting()
                : "Stock Lifting - " + app.getApplicationNo());
        site.setApplicantUserId(app.getCreatedBy());
        site.setLeaseType(STOCK_LIFTING_TYPE);
        site.setLeaseApplicationId(app.getId());
        site.setLeaseApplicationNumber(app.getApplicationNo());
        site.setPlace(joinLocation(app.getPlaceVillage(), app.getGewog(), app.getDzongkhag()));
        site.setCreatedBy("system-stocklifting-approval");
        siteMasterRepository.save(site);
        log.info("Provisioned site '{}' for approved stock lifting application {}", site.getSiteName(), app.getApplicationNo());
        return site;
    }

    private String joinLocation(String... parts) {
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(", "));
    }

    public SiteMaster provisionSiteForApprovedLeaseSurfaceAuction(SurfaceCollectionAuctionApplication app) {
        Optional<SiteMaster> existing = siteMasterRepository.findByLeaseTypeAndLeaseApplicationNumber(SURFACE_COLLECTION_TYPE, app.getApplicationNo());
        if (existing.isPresent()) {
            return existing.get();
        }
        SiteMaster site = new SiteMaster();

        site.setSiteName(app.getSiteName() != null && !app.getSiteName().isBlank()
                ? app.getSiteName()
                : "Surface Collection Auction - " + app.getApplicationNo());

        site.setApplicantUserId(app.getBidWinner() != null ? app.getBidWinner().getPromoterId() : null);
        site.setLeaseType(SURFACE_COLLECTION_TYPE);
        site.setLeaseApplicationId(app.getId());
        site.setLeaseApplicationNumber(app.getApplicationNo());
        site.setDzongkhagId(app.getDzongkhagId() != null ? app.getDzongkhagId().getId() : null);
        site.setGewogNameId(app.getGewogId() != null ? String.valueOf(app.getGewogId().getGewogSerialNo()) : null);
        site.setNearestVillageId(app.getVillageId() != null
                ? String.valueOf(app.getVillageId().getVillageSerialNo()) : null);
        site.setPlace(app.getDzongkhagId().getDzongkhagName());
        site.setCreatedBy("system-lease-approval");
        siteMasterRepository.save(site);

        log.info(
                "Provisioned site '{}' for approved surface collection auction {}",
                site.getSiteName(),
                app.getApplicationNo()
        );

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
