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
    owner_id VARCHAR(36),
    project_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

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

CREATE INDEX idx_workflows_owner ON workflows(owner_id);
CREATE INDEX idx_workflows_project ON workflows(project_id);
CREATE INDEX idx_workflows_name ON workflows(name);
CREATE INDEX idx_workflows_archived ON workflows(is_archived);
CREATE INDEX idx_workflow_workflow ON workflow_history(workflow_id);