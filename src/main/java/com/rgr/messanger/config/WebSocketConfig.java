package com.rgr.messanger.config;

import com.rgr.messanger.web.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String JWT_COOKIE_NAME = "accessToken";
    private static final String AUTH_ATTR_KEY = "auth";

    private final JwtTokenProvider tokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor())
                .withSockJS();
    }

    private HandshakeInterceptor jwtHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    Cookie[] cookies = servletRequest.getServletRequest().getCookies();

                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                                tryAuthenticate(cookie.getValue(), attributes);
                                break;
                            }
                        }
                    }
                }

                if (attributes.get(AUTH_ATTR_KEY) instanceof Authentication auth) {
                    log.debug("WS handshake authenticated: {}", auth.getName());
                } else {
                    log.warn("WS handshake without valid JWT cookie");
                }

                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Exception exception) {
                // no-op
            }
        };
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> attrs = accessor.getSessionAttributes();

                    if (attrs != null && attrs.get(AUTH_ATTR_KEY) instanceof Authentication auth) {
                        accessor.setUser(auth);
                        log.debug("WS CONNECT authenticated: {}", auth.getName());
                        return message;
                    }

                    log.warn("WS CONNECT rejected: no valid authentication");
                    throw new IllegalArgumentException("WS CONNECT without valid authentication");
                }

                Principal user = accessor.getUser();
                if ((StompCommand.SEND.equals(accessor.getCommand())
                        || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))
                        && user == null) {
                    log.warn("WS {} rejected: unauthenticated session", accessor.getCommand());
                    throw new IllegalArgumentException("Unauthenticated WebSocket session");
                }

                return message;
            }
        });
    }

    private void tryAuthenticate(String token, Map<String, Object> attributes) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            if (tokenProvider.isValid(token)) {
                Authentication auth = tokenProvider.getAuthentication(token);
                attributes.put(AUTH_ATTR_KEY, auth);
            }
        } catch (Exception e) {
            log.warn("WS handshake auth failed: {}", e.getMessage());
        }
    }
}