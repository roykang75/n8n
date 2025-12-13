-- Reset the database for fresh migration
-- Run this if Flyway migration V7 failed and you need to reset

DROP DATABASE IF EXISTS n8n_dev;
CREATE DATABASE n8n_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Clean Flyway history
USE n8n_dev;
DROP TABLE IF EXISTS flyway_schema_history;