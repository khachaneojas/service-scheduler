package com.sprk.service.scheduler.repository.mq;

import com.sprk.commons.entity.mq.RegistryModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;



public interface RegistryRepository extends JpaRepository<RegistryModel, Long> {
    RegistryModel findByMacAddress(String macAddress);

    @Query("SELECT COUNT(r) FROM RegistryModel r WHERE r.macAddress = :macAddress")
    int countByMacAddress(@Param("macAddress") String macAddress);

    @Query("SELECT r FROM RegistryModel r ORDER BY r.lastUpdateReceived DESC")
    List<RegistryModel> findMostRecent();
}
