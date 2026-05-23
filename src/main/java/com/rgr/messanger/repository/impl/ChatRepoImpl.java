package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.chat.Chat;
import com.rgr.messanger.repository.ChatRepo;
import com.rgr.messanger.web.dto.chat.ChatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRepoImpl implements ChatRepo {

    private static final String NOTES_CHAT_NAME = "Заметки";
    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_MEMBER = "member";

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_BY_USER_ID = """
        SELECT
            c.id AS chat_id,
            CASE
                WHEN c.type = 'private'
                     AND c.name = ?
                     AND NOT EXISTS (
                         SELECT 1
                         FROM chat_members cmx
                         WHERE cmx.chat_id = c.id
                           AND cmx.user_id <> ?
                     )
                THEN 'notes'
                ELSE c.type
            END AS chat_type,
            CASE
                WHEN c.type = 'private'
                     AND c.name = ?
                     AND NOT EXISTS (
                         SELECT 1
                         FROM chat_members cmx
                         WHERE cmx.chat_id = c.id
                           AND cmx.user_id <> ?
                     )
                THEN ?
                ELSE c.name
            END AS chat_name,
            c.avatar_url AS chat_avatar_url,
            c.created_at AS chat_created_at,
            c.updated_at AS chat_updated_at,

            lm.text AS last_message_text,
            lm.type AS last_message_type,
            lm.send_date AS last_message_time,

            CASE
                WHEN c.type = 'private' AND other_user.id IS NOT NULL THEN other_user.id
                ELSE NULL
            END AS interlocutor_id,

            CASE
                WHEN c.type = 'private' AND other_user.username IS NOT NULL THEN other_user.username
                ELSE NULL
            END AS interlocutor_name,

            CASE
                WHEN c.type = 'private' AND other_user.avatar_url IS NOT NULL THEN other_user.avatar_url
                ELSE NULL
            END AS interlocutor_avatar,

            COUNT(unread.id) AS unread_count

        FROM chats c
        JOIN chat_members cm
            ON cm.chat_id = c.id
           AND cm.user_id = ?

        LEFT JOIN users other_user
            ON other_user.id = (
                SELECT cm2.user_id
                FROM chat_members cm2
                WHERE cm2.chat_id = c.id
                  AND cm2.user_id <> ?
                ORDER BY cm2.user_id
                LIMIT 1
            )

        LEFT JOIN messages lm
            ON lm.id = (
                SELECT m2.id
                FROM messages m2
                WHERE m2.chat_id = c.id
                  AND m2.is_deleted = FALSE
                ORDER BY m2.send_date DESC, m2.id DESC
                LIMIT 1
            )

        LEFT JOIN messages unread
            ON unread.chat_id = c.id
           AND unread.sender_id <> ?
           AND unread.is_deleted = FALSE
           AND unread.id > COALESCE((
                SELECT mr.last_read_msg
                FROM message_reads mr
                WHERE mr.chat_id = c.id
                  AND mr.user_id = ?
           ), 0)

        GROUP BY
            c.id, c.type, c.name, c.avatar_url, c.created_at, c.updated_at,
            lm.id, lm.text, lm.type, lm.send_date,
            other_user.id, other_user.username, other_user.avatar_url

        ORDER BY
            CASE
                WHEN c.type = 'private'
                     AND c.name = ?
                     AND NOT EXISTS (
                         SELECT 1
                         FROM chat_members cmx
                         WHERE cmx.chat_id = c.id
                           AND cmx.user_id <> ?
                     )
                THEN 0
                ELSE 1
            END,
            COALESCE(lm.send_date, c.created_at) DESC,
            c.id DESC
        """;

    @Override
    public List<ChatDto> findByUserId(Long userId) {
        return jdbcTemplate.query(
                FIND_BY_USER_ID,
                (rs, rowNum) -> {
                    ChatDto dto = new ChatDto();
                    dto.setId(rs.getLong("chat_id"));
                    dto.setType(rs.getString("chat_type"));
                    dto.setName(rs.getString("chat_name"));
                    dto.setAvatarUrl(rs.getString("chat_avatar_url"));

                    dto.setLastMessage(rs.getString("last_message_text"));
                    dto.setLastMessageType(rs.getString("last_message_type"));

                    dto.setInterlocutorId(rs.getObject("interlocutor_id", Long.class));
                    dto.setInterlocutorName(rs.getString("interlocutor_name"));
                    dto.setInterlocutorAvatar(rs.getString("interlocutor_avatar"));

                    dto.setUnreadCount(rs.getInt("unread_count"));

                    Timestamp lastTime = rs.getTimestamp("last_message_time");
                    if (lastTime != null) {
                        dto.setLastMessageTime(lastTime.toLocalDateTime());
                    }

                    return dto;
                },
                NOTES_CHAT_NAME, userId,
                NOTES_CHAT_NAME, userId, NOTES_CHAT_NAME,
                userId,
                userId,
                userId,
                userId,
                NOTES_CHAT_NAME, userId
        );
    }

    private static final String FIND_BY_ID = """
        SELECT id         AS chat_id,
               type       AS chat_type,
               name       AS chat_name,
               avatar_url AS chat_avatar_url,
               creator_id AS chat_creator_id,
               created_at AS chat_created_at,
               updated_at AS chat_updated_at
        FROM chats
        WHERE id = ?
        """;

    @Override
    public Optional<Chat> findById(Long chatId) {
        List<Chat> result = jdbcTemplate.query(
                FIND_BY_ID,
                (rs, rowNum) -> mapChat(rs),
                chatId
        );
        return result.stream().findFirst();
    }

    private static final String FIND_PRIVATE_CHAT = """
        SELECT c.id         AS chat_id,
               c.type       AS chat_type,
               c.name       AS chat_name,
               c.avatar_url AS chat_avatar_url,
               c.creator_id AS chat_creator_id,
               c.created_at AS chat_created_at,
               c.updated_at AS chat_updated_at
        FROM chats c
        WHERE c.type = 'private'
          AND EXISTS (
              SELECT 1 FROM chat_members
              WHERE chat_id = c.id AND user_id = ?
          )
          AND EXISTS (
              SELECT 1 FROM chat_members
              WHERE chat_id = c.id AND user_id = ?
          )
          AND (
              SELECT COUNT(*) FROM chat_members WHERE chat_id = c.id
          ) = CASE WHEN ? = ? THEN 1 ELSE 2 END
        LIMIT 1
        """;

    @Override
    public Optional<Chat> findPrivateChat(Long userId1, Long userId2) {
        List<Chat> result = jdbcTemplate.query(
                FIND_PRIVATE_CHAT,
                (rs, rowNum) -> mapChat(rs),
                userId1, userId2, userId1, userId2
        );
        return result.stream().findFirst();
    }

    private static final String CREATE_PRIVATE_CHAT = """
        INSERT INTO chats (type, creator_id)
        VALUES ('private', ?)
        """;

    @Override
    public Long createPrivateChat(Long creatorId) {
        return insertAndGetId(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE_PRIVATE_CHAT, new String[]{"id"}
            );
            stmt.setLong(1, creatorId);
            return stmt;
        });
    }

    private static final String CREATE_NOTES_CHAT = """
        INSERT INTO chats (type, name, creator_id)
        VALUES ('private', ?, ?)
        """;

    @Override
    public Long createNotesChat(Long userId) {
        Long chatId = insertAndGetId(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE_NOTES_CHAT, new String[]{"id"}
            );
            stmt.setString(1, NOTES_CHAT_NAME);
            stmt.setLong(2, userId);
            return stmt;
        });
        addMemberWithRole(chatId, userId, ROLE_OWNER);
        return chatId;
    }

    private static final String CREATE_GROUP_CHAT = """
        INSERT INTO chats (type, name, creator_id)
        VALUES ('group', ?, ?)
        """;

    @Override
    public Long createGroupChat(String name, Long creatorId) {
        return insertAndGetId(connection -> {
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE_GROUP_CHAT, new String[]{"id"}
            );
            stmt.setString(1, name);
            stmt.setLong(2, creatorId);
            return stmt;
        });
    }

    private static final String UPDATE_CHAT = """
        UPDATE chats
        SET name       = COALESCE(?, name),
            avatar_url = COALESCE(?, avatar_url),
            updated_at = NOW()
        WHERE id = ?
        """;

    @Override
    public void updateChat(Long chatId, String name, String avatarUrl) {
        String safeName = isBlank(name) ? null : name;
        String safeAvatar = isBlank(avatarUrl) ? null : avatarUrl;
        jdbcTemplate.update(UPDATE_CHAT, safeName, safeAvatar, chatId);
    }

    private static final String DELETE_CHAT = """
        DELETE FROM chats WHERE id = ?
        """;

    @Override
    public void deleteChat(Long chatId) {
        jdbcTemplate.update(DELETE_CHAT, chatId);
    }

    private static final String GET_CHAT_MEMBER_IDS = """
        SELECT user_id FROM chat_members
        WHERE chat_id = ?
        """;

    @Override
    public List<Long> getChatMemberIds(Long chatId) {
        return jdbcTemplate.query(
                GET_CHAT_MEMBER_IDS,
                (rs, rowNum) -> rs.getLong("user_id"),
                chatId
        );
    }

    private static final String IS_MEMBER = """
        SELECT COUNT(1) FROM chat_members
        WHERE chat_id = ? AND user_id = ?
        """;

    @Override
    public boolean isMember(Long chatId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                IS_MEMBER, Integer.class, chatId, userId
        );
        return count != null && count > 0;
    }

    private static final String ADD_MEMBER_WITH_ROLE = """
        INSERT INTO chat_members (chat_id, user_id, role)
        VALUES (?, ?, ?)
        ON CONFLICT (chat_id, user_id) DO NOTHING
        """;

    @Override
    public void addMember(Long chatId, Long userId) {
        addMemberWithRole(chatId, userId, ROLE_MEMBER);
    }

    @Override
    public void addMemberWithRole(Long chatId, Long userId, String role) {
        jdbcTemplate.update(ADD_MEMBER_WITH_ROLE, chatId, userId, role);
    }

    private static final String REMOVE_MEMBER = """
        DELETE FROM chat_members
        WHERE chat_id = ? AND user_id = ?
        """;

    @Override
    public void removeMember(Long chatId, Long userId) {
        jdbcTemplate.update(REMOVE_MEMBER, chatId, userId);
    }

    private static final String GET_MEMBER_ROLE = """
        SELECT role FROM chat_members
        WHERE chat_id = ? AND user_id = ?
        """;

    @Override
    public Optional<String> getMemberRole(Long chatId, Long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    GET_MEMBER_ROLE, String.class, chatId, userId
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Chat mapChat(java.sql.ResultSet rs) throws java.sql.SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getLong("chat_id"));
        chat.setType(rs.getString("chat_type"));
        chat.setName(rs.getString("chat_name"));
        chat.setAvatarUrl(rs.getString("chat_avatar_url"));
        chat.setCreatorId(rs.getObject("chat_creator_id", Long.class));

        Timestamp createdAt = rs.getTimestamp("chat_created_at");
        if (createdAt != null) {
            chat.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("chat_updated_at");
        if (updatedAt != null) {
            chat.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return chat;
    }

    private Long insertAndGetId(
            org.springframework.jdbc.core.PreparedStatementCreator psc
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(psc, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated id");
        }
        return key.longValue();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }


    private static final String GET_MEMBERS_DETAILED = """
    SELECT u.id          AS user_id,
           u.username    AS user_username,
           u.avatar_url  AS user_avatar_url,
           u.status      AS user_status,
           cm.role       AS member_role
    FROM chat_members cm
    JOIN users u ON u.id = cm.user_id
    WHERE cm.chat_id = ?
    ORDER BY
        CASE cm.role
            WHEN 'owner' THEN 0
            WHEN 'admin' THEN 1
            ELSE 2
        END,
        u.username
    """;

    @Override
    public List<com.rgr.messanger.web.dto.chat.ChatMemberResponse> getMembersDetailed(Long chatId) {
        return jdbcTemplate.query(
                GET_MEMBERS_DETAILED,
                (rs, rowNum) -> {
                    com.rgr.messanger.web.dto.chat.ChatMemberResponse dto =
                            new com.rgr.messanger.web.dto.chat.ChatMemberResponse();
                    dto.setId(rs.getLong("user_id"));
                    dto.setUsername(rs.getString("user_username"));
                    dto.setAvatarUrl(rs.getString("user_avatar_url"));
                    dto.setStatus(rs.getString("user_status"));
                    dto.setRole(rs.getString("member_role"));
                    return dto;
                },
                chatId
        );
    }
}