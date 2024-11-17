package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.enquiry.EnquiryArchiveModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EnquiryArchiveRepository extends JpaRepository<EnquiryArchiveModel, Long> {}
