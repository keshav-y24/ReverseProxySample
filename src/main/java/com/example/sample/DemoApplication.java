package com.example.sample;

import com.example.sample.httpServer.HttpServer;
import com.example.sample.model.Host;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/*
 * DemoApplication class is the main class of the reverse_proxy
 * this class starts all nodes/hosts by fetching all available nodes using ApplicationContext
 * ApplicationContext is used to get singleton bean of hostsList
 */
@EnableCaching
@SpringBootApplication
@Log4j2
public class DemoApplication {

	public static void main(String[] args) {
		ApplicationContext context
				= new AnnotationConfigApplicationContext(DemoApplication.class);

		List<Host> hostList = (List<Host>) context.getBean("hostList");
		log.info("Starting Servers!!!");
		for(Host host: hostList) {
			HttpServer httpServer = new HttpServer();
			httpServer.run(host.getServiceName(), host.getServiceIP(), host.getPort());

		}
		log.info("All Servers Started!!!");
		SpringApplication.run(DemoApplication.class, args);
	}
}
