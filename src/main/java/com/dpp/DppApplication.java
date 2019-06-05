package com.dpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
//@SpringBootTest
public class DppApplication {
 
	protected static final Logger logger = LoggerFactory.getLogger(DppApplication.class);
	
	public static ConfigurableApplicationContext context = null;
	
    public static void main(String[] args) {
    	context = SpringApplication.run(DppApplication.class, args);
    	logger.info("start spring boot ===============================");
    }
    
     
}