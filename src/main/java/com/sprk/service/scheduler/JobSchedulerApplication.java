package com.sprk.service.scheduler;

import com.sprk.commons.annotation.EnableMultiModuleData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableMultiModuleData
@EnableJpaAuditing
@EnableDiscoveryClient
@EnableCaching
@EnableScheduling
public class JobSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobSchedulerApplication.class, args);
	}

}
