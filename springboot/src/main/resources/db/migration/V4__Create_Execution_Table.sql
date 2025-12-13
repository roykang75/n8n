CREATE TABLE executions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    mode ENUM('MANUAL', 'WEBHOOK', 'RETRY', 'CLI', 'TRIGGER', 'STATIC_TRIGGER') NOT NULL DEFAULT 'WEBHOOK',
    retry_of VARCHAR(36),
    retry_success_id VARCHAR(36),
    status ENUM('NEW', 'WAITING', 'PREPARING', 'RUNNING', 'SUCCESS', 'ERROR', 'CANCELED', 'CRASHED', 'UNKNOWN', 'KILLED', 'UNEXPECTED') NOT NULL DEFAULT 'WAITING',
    workflow_id VARCHAR(21) NOT NULL,
    user_id VARCHAR(36),
    started_at DATETIME,
    stopped_at DATETIME,
    wait_till DATETIME,
    deleted_at DATETIME,
    retry_of_execution_id VARCHAR(36),
    workflow_data TEXT,
    data JSON,
    workflow_id_path VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_executions_workflow ON executions(workflow_id);
CREATE INDEX idx_executions_user ON executions(user_id);
CREATE INDEX idx_executions_status ON executions(status);
CREATE INDEX idx_executions_mode ON executions(mode);
CREATE INDEX idx_executions_started ON executions(started_at);
CREATE INDEX idx_executions_deleted ON executions(deleted_at);