package com.rgr.messanger.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasswordResetStore {

    private record ResetEntry(String code, LocalDateTime expiresAt) {}

    private final Map<String, ResetEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code) {
        store.put(email.toLowerCase(),
                new ResetEntry(code, LocalDateTime.now().plusMinutes(15)));
    }

    public boolean verify(String email, String code) {
        ResetEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        return entry.code().equals(code);
    }

    public void remove(String email) {
        store.remove(email.toLowerCase());
    }
}