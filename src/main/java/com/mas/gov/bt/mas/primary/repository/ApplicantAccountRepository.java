package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.ApplicantAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantAccountRepository extends JpaRepository<ApplicantAccount, Long> {

    boolean existsByCid(String cid);

    boolean existsByLicenseNo(String licenseNo);

    boolean existsByCompanyRegistrationNumber(String companyRegistrationNumber);

    boolean existsByEmail(String email);

    Optional<ApplicantAccount> findByCid(String cid);

    Optional<ApplicantAccount> findByLicenseNo(String licenseNo);

    Optional<ApplicantAccount> findByCompanyRegistrationNumber(String companyRegistrationNumber);

    @Modifying
    @Query(value = """
        INSERT INTO mas_db.citizen_roles (citizen_id, role_id)
        SELECT :citizenId, r.id FROM mas_db.roles r WHERE r.name = :roleName
        ON CONFLICT DO NOTHING
    """, nativeQuery = true)
    void assignRole(@Param("citizenId") Long citizenId, @Param("roleName") String roleName);
}
