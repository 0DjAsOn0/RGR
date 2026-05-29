import { fetchCurrentUser, sendHeartbeat, setOffline } from './api.js';
import { state } from './app.js';
import { disconnect } from './websocket.js';
import { t } from './i18n.js';

let heartbeatInterval = null;
const HEARTBEAT_INTERVAL_MS = 10000;
const DEFAULT_AVATAR = '/avatars/default.png';

export async function loadCurrentUser() {

    //загружаем данные о пользователе
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

//обновляем интерфейс ставим аву пользователя и пишем его ник
export function updateNavbar(user) {
    const profileLabel = document.querySelector('.profile-button .base-inscription');
    if (profileLabel) {
        profileLabel.textContent = user.username ?? t('nav.profile');
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

    //активность пользователя
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

    //отправляем онлайн
    beat();
    heartbeatInterval = setInterval(beat, HEARTBEAT_INTERVAL_MS);
}

//остановка прослушивания
export function stopHeartbeat() {
    if (heartbeatInterval) {
        clearInterval(heartbeatInterval);
        heartbeatInterval = null;
    }
}

//разлогиниваемся
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

//пользователя надо разлогинить
function handleUnauthorized() {
    stopHeartbeat();
    disconnect();
    clearUserState();
    window.location.href = '/login';
}

//очистка состояния пользователя
function clearUserState() {
    state.currentUser = null;
    state.currentChatId = null;
    state.currentChatUserId = null;
    state.replyToId = null;
}