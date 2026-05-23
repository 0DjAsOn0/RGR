import { escapeHtml } from './utils.js';
import { fetchChats, searchUsers as apiSearchUsers, fetchOrCreateChat } from './api.js';
import { openChat, state } from './app.js';

const DEFAULT_AVATAR = '/avatars/default.png';

let searchTimeout = null;
let searchRequestId = 0;

// ========================
// СПИСОК ЧАТОВ
// ========================

export async function loadChats() {
    try {
        const chats = await fetchChats();
        renderChatList(chats);
        return chats;
    } catch (error) {
        console.error('Ошибка загрузки чатов:', error);
        return [];
    }
}

function getLastMessagePreview(chat) {
    const text = typeof chat.lastMessage === 'string' ? chat.lastMessage.trim() : '';
    const type = String(chat.lastMessageType || '').toLowerCase();

    if (text) return escapeHtml(text);

    switch (type) {
        case 'image':
        case 'images':
            return '🖼 Фото';
        case 'video':
            return '🎥 Видео';
        case 'audio':
            return '🎵 Аудио';
        case 'file':
            return '📎 Файл';
        default:
            if (chat.hasAttachment) return '📎 Вложение';
            return 'Нет сообщений';
    }
}

function normalizeChatType(chat) {
    if (chat.type === 'notes') return 'notes';
    if (chat.type === 'group') return 'group';
    if (chat.name === 'Заметки' && !chat.interlocutorId) return 'notes';
    return 'private';
}

export function renderChatList(chats) {
    const container = document.getElementById('chatsContainer');
    if (!container) return;

    if (!chats || chats.length === 0) {
        container.innerHTML = '<li class="no-chats">Нет чатов</li>';
        return;
    }

    container.innerHTML = chats.map(chat => {
        const chatType = normalizeChatType(chat);
        const isNotes = chatType === 'notes';
        const isGroup = chatType === 'group';

        const rawName = isNotes
            ? 'Заметки'
            : isGroup
                ? (chat.name ?? 'Группа')
                : (chat.interlocutorName ?? chat.name ?? 'Чат');

        const escapedName = escapeHtml(rawName);

        const groupAvatar = isGroup ? (chat.avatarUrl ?? null) : null;
        const privateAvatar = !isNotes && !isGroup
            ? (chat.interlocutorAvatar ?? DEFAULT_AVATAR)
            : null;

        const datasetAvatar = isGroup
            ? (groupAvatar ?? '')
            : (privateAvatar ?? '');

        const preview = getLastMessagePreview(chat);

        return `
            <li class="card"
                data-chat-id="${chat.id}"
                data-user-id="${chat.interlocutorId ?? ''}"
                data-user-name="${escapeHtml(rawName)}"
                data-user-avatar="${escapeHtml(datasetAvatar)}"
                data-chat-type="${chatType}">
                <div class="chat-card">
                    <div class="avatar">
                        ${isNotes
            ? `<div class="notes-avatar">📝</div>`
            : isGroup
                ? (groupAvatar
                    ? `<img class="avatar-img" src="${escapeHtml(groupAvatar)}" alt="">`
                    : `<div class="notes-avatar">👥</div>`)
                : `<img class="avatar-img" src="${escapeHtml(privateAvatar)}" alt="">`
        }
                    </div>
                    <div class="card-content">
                        <div class="name-time">
                            <span class="user-name">${escapedName}</span>
                            <time class="message-time">${chat.lastMessageTime ?? ''}</time>
                        </div>
                        <div class="message-preview">
                            <span class="user-message">${preview}</span>
                            ${chat.unreadCount > 0
            ? `<span class="unread-badge">${chat.unreadCount}</span>`
            : ''
        }
                        </div>
                    </div>
                </div>
            </li>
        `;
    }).join('');
}

// ========================
// ПОИСК
// ========================

export function handleSearch(value) {
    clearTimeout(searchTimeout);

    const searchResults = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    if (!searchResults || !chatsContainer) return;

    const query = value?.trim() ?? '';

    if (query.length < 2) {
        searchResults.style.display = 'none';
        searchResults.innerHTML = '';
        chatsContainer.style.display = 'block';
        return;
    }

    searchTimeout = setTimeout(async () => {
        await doSearch(query);
    }, 400);
}

async function doSearch(query) {
    const currentRequestId = ++searchRequestId;

    const searchResults = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    if (!searchResults || !chatsContainer) return;

    try {
        searchResults.style.display = 'block';
        searchResults.innerHTML = '<li class="search-loading">Поиск...</li>';
        chatsContainer.style.display = 'none';

        const users = await apiSearchUsers(query);

        if (currentRequestId !== searchRequestId) {
            return;
        }

        renderSearchResults(users);

    } catch (error) {
        if (currentRequestId !== searchRequestId) {
            return;
        }

        console.error('Ошибка поиска:', error);
        searchResults.innerHTML = '<li class="search-error">Ошибка поиска</li>';
    }
}

export function renderSearchResults(users) {
    const searchResults = document.getElementById('searchResults');
    if (!searchResults) return;

    if (!users || users.length === 0) {
        searchResults.innerHTML = '<li class="no-results">Пользователи не найдены</li>';
        return;
    }

    searchResults.innerHTML = users.map(user => {
        const username = user.username ?? 'Пользователь';
        const avatarUrl = user.avatarUrl ?? DEFAULT_AVATAR;

        return `
            <li class="card search-card"
                data-user-id="${user.id}"
                data-user-name="${escapeHtml(username)}"
                data-user-avatar="${escapeHtml(avatarUrl)}">
                <div class="chat-card">
                    <div class="avatar">
                        <img class="avatar-img"
                             src="${escapeHtml(avatarUrl)}"
                             alt="">
                    </div>
                    <div class="card-content">
                        <div class="name-time">
                            <span class="user-name">${escapeHtml(username)}</span>
                            <span class="user-status-badge ${user.status === 'online' ? 'online' : ''}">
                                ${user.status === 'online' ? 'в сети' : ''}
                            </span>
                        </div>
                        <span class="user-message">Нажмите чтобы написать</span>
                    </div>
                </div>
            </li>
        `;
    }).join('');
}

export async function startChatWithUser(userId, username, avatarUrl) {
    try {
        const data = await fetchOrCreateChat(userId);
        const chatId = data.chatId;

        clearSearch(false);

        await loadChats();

        const realCard = document.querySelector(`.card[data-chat-id="${chatId}"]`);
        if (realCard) {
            await openChat(realCard);
            return;
        }

        const tempCard = document.createElement('li');
        tempCard.className = 'card';
        tempCard.dataset.chatId = chatId;
        tempCard.dataset.userId = userId;
        tempCard.dataset.userName = username ?? 'Чат';
        tempCard.dataset.userAvatar = avatarUrl ?? DEFAULT_AVATAR;
        tempCard.dataset.chatType =
            Number(userId) === Number(state.currentUser?.id) ? 'notes' : 'private';

        await openChat(tempCard);

    } catch (error) {
        console.error('Ошибка создания чата:', error);
    }
}

export function clearSearch(reloadChats = true) {
    const searchInput = document.getElementById('searchInput');
    const searchResults = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    clearTimeout(searchTimeout);
    searchRequestId++;

    if (searchInput) searchInput.value = '';

    if (searchResults) {
        searchResults.style.display = 'none';
        searchResults.innerHTML = '';
    }

    if (chatsContainer) {
        chatsContainer.style.display = 'block';
    }

    if (reloadChats) {
        loadChats();
    }
}

// ========================
// RESIZER
// ========================

export function initResizer() {
    const resizer = document.querySelector('.resizer');
    const chatsList = document.querySelector('.chats-list');

    if (!resizer || !chatsList) return;

    let isResizing = false;

    resizer.addEventListener('mousedown', (e) => {
        isResizing = true;
        document.body.style.cursor = 'col-resize';
        e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
        if (!isResizing) return;

        const containerLeft = chatsList.getBoundingClientRect().left;
        let newWidth = e.clientX - containerLeft;

        if (newWidth < 260) newWidth = 260;
        if (newWidth > 1160) newWidth = 1160;

        chatsList.style.width = `${newWidth}px`;
    });

    document.addEventListener('mouseup', () => {
        if (!isResizing) return;

        isResizing = false;
        document.body.style.cursor = 'default';
    });
}