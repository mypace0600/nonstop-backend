package com.app.nonstop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NonstopApplication {

	public static void main(String[] args) {
		SpringApplication.run(NonstopApplication.class, args);
	}

}
