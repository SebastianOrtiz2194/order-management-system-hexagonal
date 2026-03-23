-- =============================================================================
-- init.sql — Script de inicialización de la base de datos OMS
-- =============================================================================
-- Este script se ejecuta AUTOMÁTICAMENTE por el contenedor de PostgreSQL
-- la primera vez que se crea el volumen (docker-entrypoint-initdb.d/).
--
-- En proyectos con Flyway o Liquibase, este archivo solo configura extensiones
-- y roles. Las migraciones DDL se manejarán desde la app Spring Boot.
-- =============================================================================

-- Extensión UUID: permite usar gen_random_uuid() para generar PKs tipo UUID.
-- Preferimos UUID sobre SERIAL/BIGSERIAL porque:
--   - Son globalmente únicos (no hay colisión entre microservicios)
--   - Ocultan información de negocio (no revelan volumen de pedidos)
--   - Facilitan distribución futura de datos
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Extensión para full-text search (útil en búsquedas futuras de productos)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Confirmación de inicialización
DO $$
BEGIN
    RAISE NOTICE 'OMS Database initialized successfully at %', NOW();
END $$;
