-- Add entity_version column for optimistic locking
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS entity_version BIGINT DEFAULT 0;

-- Update existing rows to have a default version
UPDATE workflows SET entity_version = 0 WHERE entity_version IS NULL;
