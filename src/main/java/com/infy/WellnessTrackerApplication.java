package com.infy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WellnessTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WellnessTrackerApplication.class, args);
	}

}
