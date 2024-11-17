package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.user.RequestModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EmployeeRequestRepository extends JpaRepository<RequestModel, Long> {}
