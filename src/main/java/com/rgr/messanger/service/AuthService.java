package com.rgr.messanger.service;

import com.rgr.messanger.web.dto.auth.JwtRequest;
import com.rgr.messanger.web.dto.auth.JwtResponse;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    JwtResponse login(JwtRequest loginRequest);

    JwtResponse refresh(String refreshToken);
}
