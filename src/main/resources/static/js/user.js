import { fetchCurrentUser, sendHeartbeat } from './api.js';
import { state } from './app.js';

let heartbeatInterval = null;

export async function loadCurrentUser() {
    try {
        const user = await fetchCurrentUser();
        state.currentUser = user;
        console.log('Пользователь загружен:', user);
        updateNavbar(user);
        startHeartbeat();
    } catch (error) {
        if (error.message === '401') {
            window.location.href = '/login';
        }
        console.error('Ошибка загрузки пользователя:', error);
    }
}

export function updateNavbar(user) {
    const profileLabel = document.querySelector('.profile-button .base-inscription');
    if (profileLabel) profileLabel.textContent = user.username;

    const navAvatar = document.querySelector('.profile-button .avatar-img');
    if (navAvatar && user.avatarUrl) {
        navAvatar.src = user.avatarUrl + '?t=' + Date.now();
    }
}

export function startHeartbeat() {
    clearInterval(heartbeatInterval);

    fetch('/api/v1/users/me/heartbeat', {
        method: 'POST',
        credentials: 'include'
    }).then(() => {
        if (state.currentUser) {
            state.currentUser.status = 'online';
        }
    }).catch(e => console.error('Heartbeat ошибка:', e));

    heartbeatInterval = setInterval(async () => {
        try {
            await sendHeartbeat();
            if (state.currentUser) {
                state.currentUser.status = 'online';
            }
        } catch (e) {
            console.error('Heartbeat ошибка:', e);
        }
    }, 10000);
}

export function stopHeartbeat() {
    clearInterval(heartbeatInterval);
}

export async function logout() {
    try {
        stopHeartbeat();

        await fetch('/api/v1/users/me/offline', {
            method: 'POST',
            credentials: 'include'
        });
        await fetch('/api/v1/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });
    } catch (e) {
        console.error('Ошибка logout:', e);
    } finally {
        window.location.href = '/login';
    }
}