package com.sprk.service.scheduler.repository.primary;

import com.sprk.commons.entity.primary.examination.CertificateModel;
import org.springframework.data.jpa.repository.JpaRepository;



public interface CertificateRepository extends JpaRepository<CertificateModel, Long> {
    Boolean existsByCertificateUid(String certificateId);
}
