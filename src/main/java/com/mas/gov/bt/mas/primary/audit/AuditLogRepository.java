package com.mas.gov.bt.mas.primary.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = "SELECT EXISTS(SELECT 1 FROM mas_db.users WHERE id = :id)", nativeQuery = true)
    boolean userExists(@Param("id") Long id);
}
