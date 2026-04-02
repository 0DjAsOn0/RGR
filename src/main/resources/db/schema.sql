CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    avatar_url  TEXT,
    status      VARCHAR(20) DEFAULT 'offline',
    last_seen   TIMESTAMP DEFAULT NOW(),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          role    VARCHAR(50) NOT NULL,
                                          CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS chats (
                                     id          BIGSERIAL PRIMARY KEY,
                                     type        VARCHAR(20) NOT NULL DEFAULT 'private',
    name        VARCHAR(100),
    avatar_url  TEXT,
    creator_id  BIGINT REFERENCES users(id),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS chat_members (
                                            id          BIGSERIAL PRIMARY KEY,
                                            chat_id     BIGINT REFERENCES chats(id) ON DELETE CASCADE,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) DEFAULT 'member',
    joined_at   TIMESTAMP DEFAULT NOW(),
    muted_until TIMESTAMP,
    UNIQUE(chat_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_chat_members_chat ON chat_members(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_members_user ON chat_members(user_id);

CREATE TABLE IF NOT EXISTS messages (
    id          BIGSERIAL PRIMARY KEY,
    chat_id     BIGINT REFERENCES chats(id) ON DELETE CASCADE,
    sender_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reply_to_id BIGINT REFERENCES messages(id) ON DELETE SET NULL,
    type        VARCHAR(20) DEFAULT 'text',
    text     TEXT,
    is_edited   BOOLEAN DEFAULT FALSE,
    is_deleted  BOOLEAN DEFAULT FALSE,
    send_date  TIMESTAMP DEFAULT NOW(),
    edited_at   TIMESTAMP,
    status VARCHAR(20) DEFAULT 'NOT_SENDING'
    );

CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages(chat_id, send_date DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);

CREATE TABLE IF NOT EXISTS message_reads (
    id            BIGSERIAL PRIMARY KEY,
    chat_id       BIGINT REFERENCES chats(id) ON DELETE CASCADE,
    user_id       BIGINT REFERENCES users(id) ON DELETE CASCADE,
    last_read_msg BIGINT REFERENCES messages(id),
    last_read_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(chat_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_reads_chat_user ON message_reads(chat_id, user_id);

CREATE TABLE IF NOT EXISTS attachments (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE,
    file_url   TEXT NOT NULL,
    file_name  VARCHAR(255),
    file_size  BIGINT,
    mime_type  VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_attachments_message ON attachments(message_id);