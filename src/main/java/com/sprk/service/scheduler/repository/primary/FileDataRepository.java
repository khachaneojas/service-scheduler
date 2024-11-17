package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.common.FileDataModel;
import org.springframework.data.jpa.repository.JpaRepository;



public interface FileDataRepository extends JpaRepository<FileDataModel, Long> {}
