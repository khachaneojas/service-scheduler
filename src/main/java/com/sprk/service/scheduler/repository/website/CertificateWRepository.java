package com.sprk.service.scheduler.repository.website;


import com.sprk.commons.entity.website.CertificateWModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateWRepository extends JpaRepository<CertificateWModel, Long> {
    CertificateWModel findByCertificateUid(String certUid);
}
