CREATE TABLE users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(32),
    last_name VARCHAR(32),
    password VARCHAR(255),
    role ENUM('ADMIN', 'USER', 'OWNER') NOT NULL DEFAULT 'USER',
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    mfa_recovery_codes JSON,
    last_active_at DATETIME,
    settings JSON,
    personalization_answers JSON,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE user_auth_identities (
    user_id VARCHAR(36) NOT NULL,
    id VARCHAR(36) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_data JSON,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_disabled ON users(disabled);
CREATE INDEX idx_user_auth_provider ON user_auth_identities(provider_type, provider_id);