package com.app.nonstop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.app.nonstop.mapper")
public class NonstopApplication {

	public static void main(String[] args) {
		SpringApplication.run(NonstopApplication.class, args);
	}

}
