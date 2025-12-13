CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(36) NOT NULL UNIQUE,
    created_by VARCHAR(36),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE workflow_tags (
    workflow_id VARCHAR(21) NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (workflow_id, tag_id),
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_workflow_tags_tag ON workflow_tags(tag_id);