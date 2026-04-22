package com.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry-point for the Order Management System (OMS) project.
 * <p>
 * Following Hexagonal Architecture principles, the framework (Spring) is maintained 
 * at the periphery of the business core. This class initializes adapters and 
 * registers beans within the Inversion of Control (IoC) container.
 */
@SpringBootApplication
public class OmsApplication {

    public static void main(String[] args) {
        System.out.println("====== STARTING ORDER MANAGEMENT SYSTEM (ARCH: HEXAGONAL) ======");
        SpringApplication.run(OmsApplication.class, args);
    }
}
