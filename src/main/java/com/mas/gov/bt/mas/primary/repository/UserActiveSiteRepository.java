package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.UserActiveSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActiveSiteRepository extends JpaRepository<UserActiveSite, Long> {
}
