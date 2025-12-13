-- Fix foreign key constraints to allow NULL values
-- This migration fixes the issue where owner_id was created as NOT NULL
-- but foreign key constraint uses ON DELETE SET NULL

-- Drop foreign keys first
ALTER TABLE credentials DROP FOREIGN KEY IF EXISTS credentials_ibfk_1;
ALTER TABLE credentials DROP FOREIGN KEY IF EXISTS credentials_ibfk_2;
ALTER TABLE workflows DROP FOREIGN KEY IF EXISTS workflows_ibfk_1;
ALTER TABLE workflows DROP FOREIGN KEY IF EXISTS workflows_ibfk_2;

-- Modify columns to allow NULL (this may fail if column is already nullable, that's okay)
ALTER TABLE credentials MODIFY COLUMN owner_id VARCHAR(36) NULL;
ALTER TABLE credentials MODIFY COLUMN project_id BIGINT NULL;
ALTER TABLE workflows MODIFY COLUMN owner_id VARCHAR(36) NULL;
ALTER TABLE workflows MODIFY COLUMN project_id BIGINT NULL;

-- Re-add foreign key constraints with ON DELETE SET NULL
ALTER TABLE credentials ADD CONSTRAINT credentials_ibfk_1 FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE credentials ADD CONSTRAINT credentials_ibfk_2 FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;
ALTER TABLE workflows ADD CONSTRAINT workflows_ibfk_1 FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE workflows ADD CONSTRAINT workflows_ibfk_2 FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;