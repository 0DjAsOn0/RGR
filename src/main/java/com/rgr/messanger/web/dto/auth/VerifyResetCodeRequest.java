package com.rgr.messanger.web.dto.auth;

import lombok.Data;

@Data
public class VerifyResetCodeRequest {
    private String email;
    private String code;
}