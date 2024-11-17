package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.common.OrganizationModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrganizationRepository extends JpaRepository<OrganizationModel, Long> {}
