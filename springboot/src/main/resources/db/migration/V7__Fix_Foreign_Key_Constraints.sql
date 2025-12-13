-- Fix foreign key constraints to allow NULL values
-- This migration fixes the issue where owner_id was created as NOT NULL
-- but foreign key constraint uses ON DELETE SET NULL

-- Drop and recreate workflows table with correct constraints
DROP TABLE IF EXISTS workflow_history;
DROP TABLE IF EXISTS workflow_tags;
DROP TABLE IF EXISTS workflows;

-- Recreate workflows table
CREATE TABLE workflows (
    id VARCHAR(21) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    nodes JSON,
    connections JSON,
    settings JSON,
    static_data JSON,
    meta JSON,
    version_id VARCHAR(36),
    active_version_id VARCHAR(36),
    version_counter INT DEFAULT 0,
    trigger_count INT DEFAULT 0,
    parent_folder_id VARCHAR(21),
    pin_data JSON,
    owner_id VARCHAR(36) NULL,
    project_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

-- Drop and recreate credentials table
DROP TABLE IF EXISTS shared_credentials;
DROP TABLE IF EXISTS credentials;

-- Recreate credentials table
CREATE TABLE credentials (
    id VARCHAR(21) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    data TEXT NOT NULL,
    type VARCHAR(128) NOT NULL,
    is_managed BOOLEAN DEFAULT FALSE,
    is_global BOOLEAN DEFAULT FALSE,
    is_resolvable BOOLEAN DEFAULT FALSE,
    resolvable_allow_fallback BOOLEAN DEFAULT FALSE,
    resolver_id VARCHAR(36),
    owner_id VARCHAR(36) NULL,
    project_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

-- Recreate supporting tables
CREATE TABLE workflow_history (
    workflow_id VARCHAR(21) NOT NULL,
    version_id VARCHAR(36) NOT NULL,
    version INT NOT NULL,
    name VARCHAR(128) NOT NULL,
    nodes JSON,
    connections JSON,
    settings JSON,
    created_by VARCHAR(36),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (workflow_id, version_id),
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE workflow_tags (
    workflow_id VARCHAR(21) NOT NULL,
    tag_id VARCHAR(21) NOT NULL,
    PRIMARY KEY (workflow_id, tag_id),
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE shared_credentials (
    credentials_id VARCHAR(21) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (credentials_id, user_id),
    FOREIGN KEY (credentials_id) REFERENCES credentials(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Recreate indexes
CREATE INDEX idx_workflows_owner ON workflows(owner_id);
CREATE INDEX idx_workflows_project ON workflows(project_id);
CREATE INDEX idx_workflows_name ON workflows(name);
CREATE INDEX idx_workflows_archived ON workflows(is_archived);
CREATE INDEX idx_workflow_history ON workflow_history(workflow_id);
CREATE INDEX idx_credentials_owner ON credentials(owner_id);
CREATE INDEX idx_credentials_project ON credentials(project_id);