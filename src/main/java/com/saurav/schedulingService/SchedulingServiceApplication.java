package com.saurav.schedulingService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@ComponentScan(basePackages = "com.saurav.schedulingService")
@SpringBootApplication()
@EnableCassandraRepositories(basePackages = "com.saurav.schedulingService.repository")
public class SchedulingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(SchedulingServiceApplication.class, args);
	}
}
