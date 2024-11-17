package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.user.HolidayModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface HolidayRepository extends JpaRepository<HolidayModel, Long> {}
