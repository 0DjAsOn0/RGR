package com.rgr.messanger.repository.impl;

import com.rgr.messanger.repository.AdminRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminRepoImpl implements AdminRepo {

    private final JdbcTemplate jdbcTemplate;

    private static final String COUNT_MESSAGES = """
        SELECT COUNT(*)
        FROM messages
        WHERE is_deleted = FALSE
        """;

    @Override
    public long countMessages() {
        Long value = jdbcTemplate.queryForObject(COUNT_MESSAGES, Long.class);
        return value == null ? 0L : value;
    }
}