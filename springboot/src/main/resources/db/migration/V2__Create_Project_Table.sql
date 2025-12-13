CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type ENUM('PERSONAL', 'TEAM') NOT NULL DEFAULT 'PERSONAL',
    icon JSON,
    description VARCHAR(512),
    home_workflow_id VARCHAR(36),
    creator_id VARCHAR(36),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE project_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_project_user (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_type ON projects(type);
CREATE INDEX idx_projects_creator ON projects(creator_id);
CREATE INDEX idx_project_relations_user ON project_relations(user_id);