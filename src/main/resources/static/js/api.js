export async function fetchCurrentUser() {
    const res = await fetch('/api/v1/users/me', {
        credentials: 'include'
    });
    if (res.status === 401) throw new Error('401');
    if (!res.ok) throw new Error('Ошибка: ' + res.status);
    return res.json();
}

export async function fetchChats() {
    const res = await fetch('/api/v1/chats', {
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Ошибка загрузки чатов');
    return res.json();
}

export async function fetchMessages(chatId) {
    const res = await fetch(`/api/v1/messages/chat/${chatId}`, {
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Ошибка загрузки сообщений');
    return res.json();
}

export async function fetchOrCreateChat(userId) {
    const res = await fetch(`/api/v1/messages/private/${userId}`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Ошибка создания чата');
    return res.json();
}

export async function searchUsers(query) {
    const res = await fetch(
        `/api/v1/users/search?username=${encodeURIComponent(query)}`,
        { credentials: 'include' }
    );
    if (!res.ok) throw new Error('Ошибка поиска');
    return res.json();
}

export async function sendHeartbeat() {
    await fetch('/api/v1/users/me/heartbeat', {
        method: 'POST',
        credentials: 'include'
    });
}

export async function setOffline() {
    navigator.sendBeacon('/api/v1/users/me/offline');
}