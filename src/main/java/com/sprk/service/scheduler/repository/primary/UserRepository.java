package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.user.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<UserModel, Long> {}
