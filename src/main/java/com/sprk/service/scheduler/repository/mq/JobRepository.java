package com.sprk.service.scheduler.repository.mq;

import com.sprk.commons.entity.mq.JobModel;
import com.sprk.commons.entity.mq.tag.JobType;

import org.springframework.data.jpa.repository.JpaRepository;



public interface JobRepository extends JpaRepository<JobModel, Long> {
    long countByJobType(JobType jobType);
}
