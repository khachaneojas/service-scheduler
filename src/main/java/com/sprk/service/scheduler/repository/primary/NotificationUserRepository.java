package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.user.mapping.NotificationUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationUserRepository extends JpaRepository<NotificationUserMapping, Long> {
}
