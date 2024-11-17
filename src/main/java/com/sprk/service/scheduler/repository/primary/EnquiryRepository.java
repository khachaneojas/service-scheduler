package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.enquiry.EnquiryModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EnquiryRepository extends JpaRepository<EnquiryModel, Long> {}
