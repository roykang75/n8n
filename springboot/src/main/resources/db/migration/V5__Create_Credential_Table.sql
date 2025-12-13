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
    owner_id VARCHAR(36),
    project_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

CREATE TABLE shared_credentials (
    credentials_id VARCHAR(21) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role ENUM('OWNER', 'EDITOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER',
    created_at DATETIME NOT NULL,
    PRIMARY KEY (credentials_id, user_id),
    FOREIGN KEY (credentials_id) REFERENCES credentials(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_credentials_owner ON credentials(owner_id);
CREATE INDEX idx_credentials_project ON credentials(project_id);
CREATE INDEX idx_credentials_type ON credentials(type);
CREATE INDEX idx_credentials_global ON credentials(is_global);
CREATE INDEX idx_shared_credentials_user ON shared_credentials(user_id);