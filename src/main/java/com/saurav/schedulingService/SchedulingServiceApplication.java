package com.saurav.schedulingService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages = "com.saurav.schedulingService")
@SpringBootApplication()
@EnableCassandraRepositories(basePackages = "com.saurav.schedulingService.repository")
@EnableScheduling
public class SchedulingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(SchedulingServiceApplication.class, args);
	}
}
