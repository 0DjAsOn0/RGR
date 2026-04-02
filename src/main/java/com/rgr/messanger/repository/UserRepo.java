package com.rgr.messanger.repository;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepo {
    static final String SQL_GET_PROFILE_BY_ID =
            "select id, first_name, last_name, age from profile where id = :id";

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    void update(User user);

    void create(User user);

    void insertUserRole(Long userId, Role role);

    boolean isMessageOwner(Long userId, Long messageId);

    void delete(Long id);
}
