package com.rgr.messanger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PasswordResetStore {

    private static final int CODE_TTL_MINUTES = 15;

    private record ResetEntry(
            String code,
            LocalDateTime expiresAt,
            boolean verified
    ) {
        ResetEntry markVerified() {
            return new ResetEntry(code, expiresAt, true);
        }
    }

    private final Map<String, ResetEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code) {
        store.put(normalize(email), new ResetEntry(
                code,
                LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES),
                false
        ));
    }

    public boolean verify(String email, String code) {
        String key = normalize(email);
        ResetEntry entry = store.get(key);

        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return false;
        }
        if (!entry.code().equals(code)) return false;

        // помечаем как верифицированный
        store.put(key, entry.markVerified());
        return true;
    }

    public boolean isVerified(String email) {
        String key = normalize(email);
        ResetEntry entry = store.get(key);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return false;
        }
        return entry.verified();
    }

    public void remove(String email) {
        store.remove(normalize(email));
    }

    public boolean hasActiveCode(String email) {
        String key = normalize(email);
        ResetEntry entry = store.get(key);
        return entry != null && LocalDateTime.now().isBefore(entry.expiresAt());
    }

    private String normalize(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    /**
     * Каждые 5 минут чистим истёкшие коды, чтобы не было утечки памяти.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;

        Iterator<Map.Entry<String, ResetEntry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ResetEntry> entry = it.next();
            if (now.isAfter(entry.getValue().expiresAt())) {
                it.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Очищено истёкших кодов сброса: {}", removed);
        }
    }
}