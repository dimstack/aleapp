CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    avatar_url TEXT,
    role TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ONLINE',
    server_id TEXT NOT NULL,
    is_approved INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    last_seen_at TEXT,
    lockout_until TEXT,
    UNIQUE(server_id, username),
    FOREIGN KEY(server_id) REFERENCES servers(id)
);

CREATE TABLE IF NOT EXISTS invite_tokens (
    id TEXT PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL,
    server_id TEXT NOT NULL,
    created_by TEXT,
    max_uses INTEGER NOT NULL DEFAULT 0,
    current_uses INTEGER NOT NULL DEFAULT 0,
    granted_role TEXT NOT NULL DEFAULT 'MEMBER',
    require_approval INTEGER NOT NULL DEFAULT 0,
    expires_at TEXT,
    is_revoked INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    FOREIGN KEY(server_id) REFERENCES servers(id),
    FOREIGN KEY(created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS join_requests (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    avatar_url TEXT,
    invite_token_id TEXT NOT NULL,
    server_id TEXT NOT NULL,
    requested_role TEXT NOT NULL DEFAULT 'MEMBER',
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    reviewed_at TEXT,
    reviewed_by TEXT,
    FOREIGN KEY(invite_token_id) REFERENCES invite_tokens(id),
    FOREIGN KEY(server_id) REFERENCES servers(id),
    FOREIGN KEY(reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS favorites (
    user_id TEXT NOT NULL,
    favorite_user_id TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    PRIMARY KEY(user_id, favorite_user_id),
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(favorite_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    server_name TEXT NOT NULL,
    message TEXT NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS login_attempts (
    server_id TEXT NOT NULL,
    username TEXT NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TEXT,
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    PRIMARY KEY(server_id, username),
    FOREIGN KEY(server_id) REFERENCES servers(id)
);

CREATE INDEX IF NOT EXISTS idx_users_server_id ON users(server_id);
CREATE INDEX IF NOT EXISTS idx_invite_tokens_server_id ON invite_tokens(server_id);
CREATE INDEX IF NOT EXISTS idx_join_requests_server_id_status ON join_requests(server_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
