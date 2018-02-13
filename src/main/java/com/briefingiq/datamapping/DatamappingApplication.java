package com.briefingiq.datamapping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@Import(ApplicationConfig.class)
@ComponentScan(basePackages= {"com.briefingiq.datamapping"})
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class DatamappingApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatamappingApplication.class, args);
	}
}
