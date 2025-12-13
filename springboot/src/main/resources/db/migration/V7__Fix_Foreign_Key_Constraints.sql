-- Fix foreign key constraints to allow NULL values
-- This migration fixes the issue where owner_id was created as NOT NULL
-- but foreign key constraint uses ON DELETE SET NULL

-- Since MySQL doesn't support DROP FOREIGN KEY IF EXISTS,
-- we'll use a stored procedure approach or ignore errors

DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DELIMITER //
CREATE PROCEDURE drop_fk_if_exists(
    IN table_name VARCHAR(64),
    IN constraint_name VARCHAR(64)
)
BEGIN
    DECLARE fk_exists INT;

    SELECT COUNT(1) INTO fk_exists
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
    AND table_name = table_name
    AND constraint_name = constraint_name
    AND constraint_type = 'FOREIGN KEY';

    IF fk_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', table_name, ' DROP FOREIGN KEY ', constraint_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- Drop foreign keys using the procedure
CALL drop_fk_if_exists('credentials', 'credentials_ibfk_1');
CALL drop_fk_if_exists('credentials', 'credentials_ibfk_2');
CALL drop_fk_if_exists('workflows', 'workflows_ibfk_1');
CALL drop_fk_if_exists('workflows', 'workflows_ibfk_2');

-- Clean up the procedure
DROP PROCEDURE IF EXISTS drop_fk_if_exists;

-- Modify columns to allow NULL
ALTER TABLE credentials MODIFY COLUMN owner_id VARCHAR(36) NULL;
ALTER TABLE credentials MODIFY COLUMN project_id BIGINT NULL;
ALTER TABLE workflows MODIFY COLUMN owner_id VARCHAR(36) NULL;
ALTER TABLE workflows MODIFY COLUMN project_id BIGINT NULL;

-- Re-add foreign key constraints with ON DELETE SET NULL
ALTER TABLE credentials ADD CONSTRAINT credentials_ibfk_1 FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE credentials ADD CONSTRAINT credentials_ibfk_2 FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;
ALTER TABLE workflows ADD CONSTRAINT workflows_ibfk_1 FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE workflows ADD CONSTRAINT workflows_ibfk_2 FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;