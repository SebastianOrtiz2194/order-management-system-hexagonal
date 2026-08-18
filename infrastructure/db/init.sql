-- =============================================================================
-- init.sql — OMS Database Initialization Script
-- =============================================================================
-- This script is executed AUTOMATICALLY by the PostgreSQL container
-- the first time the volume is created (docker-entrypoint-initdb.d/).
--
-- In projects using Flyway or Liquibase, this file only configures extensions
-- and roles. DDL migrations are managed from the Spring Boot app.
-- =============================================================================

-- UUID extension: enables gen_random_uuid() to generate UUID-type PKs.
-- We prefer UUID over SERIAL/BIGSERIAL because:
--   - They are globally unique (no collisions between microservices)
--   - They hide business information (do not reveal order volume)
--   - They ease future data distribution
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Extension for full-text search (useful for future product searches)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Initialization confirmation
DO $$
BEGIN
    RAISE NOTICE 'OMS Database initialized successfully at %', NOW();
END $$;
