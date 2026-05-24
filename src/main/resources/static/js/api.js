import { currentLang, t } from './i18n.js';

async function request(url, options = {}) {
    // Подготавливаем заголовки, добавляя Accept-Language
    const headers = {
        'Accept-Language': currentLang,
        ...(options.headers || {})
    };

    const res = await fetch(url, {
        credentials: 'include',
        ...options,
        headers
    });

    if (res.status === 401) {
        throw new Error('401');
    }

    if (!res.ok) {
        let message = `${t('api.error')}: ${res.status}`;
        try {
            const data = await res.json();
            message = data.error || data.message || message;
        } catch (_) {}
        throw new Error(message);
    }

    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return res.json();
    }

    return null;
}

export async function fetchCurrentUser() {
    return request('/api/v1/users/me');
}

export async function fetchChats() {
    return request('/api/v1/chats');
}

export async function fetchMessages(chatId) {
    return request(`/api/v1/messages/chat/${chatId}`);
}

export async function fetchOrCreateChat(userId) {
    return request(`/api/v1/messages/private/${userId}`, {
        method: 'POST'
    });
}

export async function searchUsers(query) {
    return request(`/api/v1/users/search?username=${encodeURIComponent(query)}`);
}

export async function sendHeartbeat() {
    return request('/api/v1/users/me/heartbeat', {
        method: 'POST'
    });
}

export async function setOffline() {
    if (navigator.sendBeacon) {
        navigator.sendBeacon('/api/v1/users/me/offline');
        return;
    }

    try {
        await fetch('/api/v1/users/me/offline', {
            method: 'POST',
            credentials: 'include',
            keepalive: true
        });
    } catch (_) {}
}

export async function searchPublicGroups(query) {
    // В этой функции тоже используем базовый запрос,
    // чтобы она подхватила логику Accept-Language и обработку ошибок
    try {
        return await request(`/api/v1/chats/search?query=${encodeURIComponent(query)}`);
    } catch (e) {
        return [];
    }
}