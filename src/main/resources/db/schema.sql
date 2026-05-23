-- USERS
CREATE TABLE IF NOT EXISTS users (
                                     id                   BIGSERIAL PRIMARY KEY,
                                     username             VARCHAR(50)  UNIQUE NOT NULL,
                                     email                VARCHAR(255) UNIQUE NOT NULL,
                                     email_verified       BOOLEAN      NOT NULL DEFAULT FALSE,
                                     email_notifications  BOOLEAN      NOT NULL DEFAULT TRUE,
                                     blocked              BOOLEAN      NOT NULL DEFAULT FALSE,
                                     password             VARCHAR(255) NOT NULL,
                                     avatar_url           TEXT,
                                     status               VARCHAR(20)  NOT NULL DEFAULT 'offline'
                                         CHECK (status IN ('online', 'offline')),
                                     last_seen            TIMESTAMP,
                                     created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
                                     updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_blocked  ON users(blocked);

-- ROLES
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          role    VARCHAR(50) NOT NULL,
                                          CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
                                          CONSTRAINT chk_user_roles_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role);

-- CHATS
CREATE TABLE IF NOT EXISTS chats (
                                     id         BIGSERIAL PRIMARY KEY,
                                     type       VARCHAR(20) NOT NULL DEFAULT 'private'
                                         CHECK (type IN ('private', 'group')),
                                     is_public  BOOLEAN NOT NULL DEFAULT FALSE, -- <== ДОБАВЛЕН ФЛАГ (публичный или приватный чат)
                                     name       VARCHAR(100),
                                     avatar_url TEXT,
                                     creator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chats_type       ON chats(type);
CREATE INDEX IF NOT EXISTS idx_chats_creator_id ON chats(creator_id);
CREATE INDEX IF NOT EXISTS idx_chats_is_public  ON chats(is_public); -- <== ДОБАВЛЕН ИНДЕКС для быстрого поиска публичных групп

-- CHAT MEMBERS
CREATE TABLE IF NOT EXISTS chat_members (
                                            id          BIGSERIAL PRIMARY KEY,
                                            chat_id     BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
                                            user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                            role        VARCHAR(20) NOT NULL DEFAULT 'member'
                                                CHECK (role IN ('member', 'admin', 'owner')),
                                            joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                                            muted_until TIMESTAMP,
                                            CONSTRAINT uq_chat_members_chat_user UNIQUE(chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_members_chat ON chat_members(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_members_user ON chat_members(user_id);

-- MESSAGES
CREATE TABLE IF NOT EXISTS messages (
                                        id          BIGSERIAL PRIMARY KEY,
                                        chat_id     BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
                                        sender_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                        reply_to_id BIGINT REFERENCES messages(id) ON DELETE SET NULL,
                                        type        VARCHAR(20) NOT NULL DEFAULT 'text'
                                            CHECK (type IN ('text', 'image', 'images', 'video', 'audio', 'file')),
                                        text        TEXT,
                                        is_edited   BOOLEAN   NOT NULL DEFAULT FALSE,
                                        is_deleted  BOOLEAN   NOT NULL DEFAULT FALSE,
                                        send_date   TIMESTAMP NOT NULL DEFAULT NOW(),
                                        edited_at   TIMESTAMP,
                                        status      VARCHAR(20) NOT NULL DEFAULT 'SENT'
                                            CHECK (status IN ('SENDING', 'SENT', 'DELIVERED', 'READ', 'NOT_SENDING'))
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages(chat_id, send_date DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender       ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_reply_to     ON messages(reply_to_id);

-- MESSAGE READS
CREATE TABLE IF NOT EXISTS message_reads (
                                             id            BIGSERIAL PRIMARY KEY,
                                             chat_id       BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
                                             user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             last_read_msg BIGINT REFERENCES messages(id) ON DELETE SET NULL,
                                             last_read_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                                             CONSTRAINT uq_message_reads_chat_user UNIQUE(chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_reads_chat_user ON message_reads(chat_id, user_id);

-- ATTACHMENTS
CREATE TABLE IF NOT EXISTS attachments (
                                           id         BIGSERIAL PRIMARY KEY,
                                           message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                                           file_url   TEXT NOT NULL,
                                           file_name  VARCHAR(255),
                                           file_size  BIGINT,
                                           mime_type  VARCHAR(100),
                                           created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attachments_message ON attachments(message_id);