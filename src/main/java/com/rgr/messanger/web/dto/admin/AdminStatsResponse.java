package com.rgr.messanger.web.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalMessages;
}