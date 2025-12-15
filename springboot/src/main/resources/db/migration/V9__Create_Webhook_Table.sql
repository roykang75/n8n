CREATE TABLE IF NOT EXISTS webhooks (
    webhook_path VARCHAR(255) NOT NULL,
    method VARCHAR(32) NOT NULL,
    workflow_id VARCHAR(255) NOT NULL,
    node VARCHAR(255) NOT NULL,
    webhook_id VARCHAR(255),
    path_length INT,
    PRIMARY KEY (webhook_path, method)
);
