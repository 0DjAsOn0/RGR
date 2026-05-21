import { fetchCurrentUser, sendHeartbeat, setOffline } from './api.js';
import { state } from './app.js';
import { disconnect } from './websocket.js';

let heartbeatInterval = null;
const HEARTBEAT_INTERVAL_MS = 10000;
const DEFAULT_AVATAR = '/avatars/default.png';

export async function loadCurrentUser() {
    try {
        const user = await fetchCurrentUser();
        state.currentUser = user;
        console.log('Пользователь загружен:', user);
        updateNavbar(user);
        startHeartbeat();
    } catch (error) {
        if (error.message === '401') {
            handleUnauthorized();
            return;
        }

        console.error('Ошибка загрузки пользователя:', error);
    }
}

export function updateNavbar(user) {
    const profileLabel = document.querySelector('.profile-button .base-inscription');
    if (profileLabel) {
        profileLabel.textContent = user.username ?? 'Профиль';
    }

    const navAvatar = document.querySelector('.profile-button .avatar-img');
    if (navAvatar) {
        navAvatar.src = user.avatarUrl
            ? `${user.avatarUrl}?t=${Date.now()}`
            : DEFAULT_AVATAR;
    }
}

export function startHeartbeat() {
    stopHeartbeat();

    const beat = async () => {
        try {
            await sendHeartbeat();

            if (state.currentUser) {
                state.currentUser.status = 'online';
            }
        } catch (error) {
            if (error.message === '401') {
                console.warn('Heartbeat 401: пользователь больше не авторизован');
                handleUnauthorized();
                return;
            }

            console.error('Heartbeat ошибка:', error);
        }
    };

    beat();
    heartbeatInterval = setInterval(beat, HEARTBEAT_INTERVAL_MS);
}

export function stopHeartbeat() {
    if (heartbeatInterval) {
        clearInterval(heartbeatInterval);
        heartbeatInterval = null;
    }
}

export async function logout() {
    try {
        stopHeartbeat();
        disconnect();

        await setOffline();

        await fetch('/api/v1/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });

    } catch (error) {
        console.error('Ошибка logout:', error);
    } finally {
        clearUserState();
        window.location.href = '/login';
    }
}

function handleUnauthorized() {
    stopHeartbeat();
    disconnect();
    clearUserState();
    window.location.href = '/login';
}

function clearUserState() {
    state.currentUser = null;
    state.currentChatId = null;
    state.currentChatUserId = null;
    state.replyToId = null;
}