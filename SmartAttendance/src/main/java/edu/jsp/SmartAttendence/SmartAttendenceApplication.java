package edu.jsp.SmartAttendence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartAttendenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartAttendenceApplication.class, args);
		System.out.println("http://localhost:8080/login");
	}

}
