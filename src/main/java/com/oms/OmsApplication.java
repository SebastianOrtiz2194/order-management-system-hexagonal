package com.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry-point principal del proyecto Order Management System.
 * <p>
 * En Arquitectura Hexagonal, el framework (Spring) se mantiene al margen del código de negocio.
 * Esta clase levanta los adaptadores y registra los beans en el Inversor de Control (IoC).
 */
@SpringBootApplication
public class OmsApplication {

    public static void main(String[] args) {
        System.out.println("====== INICIANDO ORDER MANAGEMENT SYSTEM (ARCH: HEXAGONAL) ======");
        SpringApplication.run(OmsApplication.class, args);
    }
}
