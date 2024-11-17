package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.user.NotificationModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationModel, Long> {
}
