package com.rgr.messanger.repository.impl;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.ResourceMappingException;
import com.rgr.messanger.repository.DataSourceConfig;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.web.mappers.UserRowMapper;
import jakarta.activation.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepoImpl implements UserRepo {

    private final DataSourceConfig dataSourceConfig;

    private static final String FIND_BY_ID = """
        SELECT u.id          as user_id,
               u.username    as user_username,
               u.email       as user_email,
               u.password    as user_password,
               u.avatar_url  as user_avatar_url,
               u.status      as user_status,
               u.last_seen   as user_last_seen,
               u.created_at  as user_created_at,
               u.updated_at  as user_updated_at,
               ur.role       as user_role
        FROM users u
        LEFT JOIN user_roles ur ON ur.user_id = u.id
        WHERE u.id = ?
        """;

    @Override
    public Optional<User> findById(Long id) {
        try{
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            stmt.setLong(1,id);
            try (ResultSet rs = stmt.executeQuery()){
                return  Optional.ofNullable(UserRowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResourceMappingException(
                    "Exception while finding user by id: " + e.getMessage()
            );
        }
    }

    private static final String FIND_BY_USERNAME = """
        SELECT u.id          as user_id,
               u.username    as user_username,
               u.email       as user_email,
               u.password    as user_password,
               u.avatar_url  as user_avatar_url,
               u.status      as user_status,
               u.last_seen   as user_last_seen,
               u.created_at  as user_created_at,
               u.updated_at  as user_updated_at,
               ur.role       as user_role
        FROM users u
        LEFT JOIN user_roles ur ON ur.user_id = u.id
        WHERE u.username = ?
        """;

    @Override
    public Optional<User> findByUsername(String username) {
        try{
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_USERNAME,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1,username);
            try (ResultSet rs = stmt.executeQuery()){
                return  Optional.ofNullable(UserRowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResourceMappingException(
                    "Exception while finding user by username: " + e.getMessage()
            );
        }
    }

    private static final String UPDATE = """
            UPDATE users
            SET username   = ?,
                email      = ?,
                password   = ?,
                avatar_url = ?,
                status     = ?,
                last_seen  = ?,
                updated_at = NOW()
            WHERE id = ?
            """;

    @Override
    public void update(User user) {
        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(UPDATE);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getAvatarUrl());
            stmt.setString(5, user.getStatus() != null ? user.getStatus() : "offline");
            stmt.setTimestamp(6, user.getLastSeen() != null
                    ? Timestamp.valueOf(user.getLastSeen())
                    : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(7, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResourceMappingException("Exception while updating user: " + e.getMessage());
        }
    }

    private static final String CREATE = """
            INSERT INTO users (username, email, password, avatar_url, status, last_seen)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void create(User user) {
        try {
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    CREATE,
                    PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getAvatarUrl());
            stmt.setString(5, user.getStatus() != null ? user.getStatus() : "offline");

            stmt.setTimestamp(6, user.getLastSeen() != null
                    ? Timestamp.valueOf(user.getLastSeen())
                    : Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResourceMappingException("Exception while creating user: " + e.getMessage());
        }
    }

    private static final String INSERT_USER_ROLE = """
            INSERT INTO user_roles (user_id, role)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

    @Override
    public void insertUserRole(Long userId, Role role) {
        try{
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(INSERT_USER_ROLE);
            stmt.setLong(1, userId);
            stmt.setString(2, role.name());
            stmt.executeUpdate();
        } catch (SQLException throwable){
            throwable.printStackTrace();
            throw new ResourceMappingException("Exception while inserting user role"+ throwable.getMessage());
        }
    }

    private static final String IS_MESSAGE_OWNER = """
            SELECT COUNT(1)
            FROM messages
            WHERE id = ? AND sender_id = ?
            """;

    @Override
    public boolean isMessageOwner(Long userId, Long messageId) {
        try{
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(IS_MESSAGE_OWNER);
            stmt.setLong(1, userId);
            stmt.setLong(2, messageId);
            try (ResultSet rs = stmt.executeQuery()){
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException throwable){
            throwable.printStackTrace();
            throw new ResourceMappingException("Exception while checking if user is message owner" + throwable.getMessage());
        }
    }

    private static final String DELETE = """
            DELETE FROM users
            WHERE id = ?
            """;

    @Override
    public void delete(Long id) {
        try{
            Connection connection = dataSourceConfig.getConnection();
            PreparedStatement stmt = connection.prepareStatement(DELETE);
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException throwable){
            throw new ResourceMappingException("Exception while deleting user");
        }
    }
}
