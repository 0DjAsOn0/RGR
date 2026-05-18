import { escapeHtml }        from './utils.js';
import { fetchChats, searchUsers as apiSearchUsers, fetchOrCreateChat } from './api.js';
import { openChat }          from './app.js';

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

// определяем превью последнего сообщения
function getLastMessagePreview(chat) {
    const text = chat.lastMessage;
    const type = chat.lastMessageType;

    if (text && text.trim()) return escapeHtml(text);

    // Нет текста — смотрим на тип
    switch (type) {
        case 'image':
        case 'images': return '🖼 Фото';
        case 'video':  return '🎥 Видео';
        case 'audio':  return '🎵 Аудио';
        case 'file':   return '📎 Файл';
        default:
            // Если тип неизвестен но нет текста — проверяем attachments
            if (chat.hasAttachment) return '📎 Вложение';
            return 'Нет сообщений';
    }
}

export function renderChatList(chats) {
    const container = document.getElementById('chatsContainer');
    if (!container) return;

    if (chats.length === 0) {
        container.innerHTML = '<li class="no-chats">Нет чатов</li>';
        return;
    }

    container.innerHTML = chats.map(chat => {

        const isNotes = chat.name === 'Заметки' && !chat.interlocutorId;
        const isGroup = chat.type === 'group' && chat.name !== 'Заметки';

        const name = isNotes
            ? 'Заметки'
            : isGroup
                ? escapeHtml(chat.name)
                : escapeHtml(chat.interlocutorName ?? chat.name ?? 'Чат');

        const avatar = (isNotes || isGroup)
            ? null
            : (chat.interlocutorAvatar ?? '/avatars/avatar.png');

        // превью последнего сообщения
        const preview = getLastMessagePreview(chat);

        return `
            <li class="card"
                data-chat-id="${chat.id}"
                data-user-id="${chat.interlocutorId ?? ''}"
                data-user-name="${name}"
                data-user-avatar="${avatar ?? ''}"
                data-chat-type="${chat.type ?? 'private'}">
                <div class="chat-card">
                    <div class="avatar">
                        ${isNotes
            ? `<div class="notes-avatar">📝</div>`
            : isGroup
                ? `<div class="notes-avatar">👥</div>`
                : `<img class="avatar-img" src="${avatar}" alt="">`
        }
                    </div>
                    <div class="card-content">
                        <div class="name-time">
                            <span class="user-name">${name}</span>
                            <time class="message-time">
                                ${chat.lastMessageTime ?? ''}
                            </time>
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

let searchTimeout = null;

export function handleSearch(value) {
    clearTimeout(searchTimeout);

    const searchResults  = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    if (value.length < 2) {
        searchResults.style.display  = 'none';
        searchResults.innerHTML      = '';
        chatsContainer.style.display = 'block';
        return;
    }

    searchTimeout = setTimeout(async () => {
        await doSearch(value);
    }, 400);
}

async function doSearch(query) {
    const searchResults  = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    try {
        searchResults.style.display  = 'block';
        searchResults.innerHTML      = '<li class="search-loading">Поиск...</li>';
        chatsContainer.style.display = 'none';

        const users = await apiSearchUsers(query);
        renderSearchResults(users);

    } catch (error) {
        console.error('Ошибка поиска:', error);
        searchResults.innerHTML = '<li class="search-error">Ошибка поиска</li>';
    }
}

export function renderSearchResults(users) {
    const searchResults = document.getElementById('searchResults');

    if (users.length === 0) {
        searchResults.innerHTML = `<li class="no-results">Пользователи не найдены</li>`;
        return;
    }

    searchResults.innerHTML = users.map(user => `
        <li class="card search-card"
            data-user-id="${user.id}"
            data-user-name="${escapeHtml(user.username)}"
            data-user-avatar="${user.avatarUrl ?? '/avatars/avatar.png'}">
            <div class="chat-card">
                <div class="avatar">
                    <img class="avatar-img"
                         src="${user.avatarUrl ?? '/avatars/avatar.png'}"
                         alt="">
                </div>
                <div class="card-content">
                    <div class="name-time">
                        <span class="user-name">${escapeHtml(user.username)}</span>
                        <span class="user-status-badge ${user.status === 'online' ? 'online' : ''}">
                            ${user.status === 'online' ? 'в сети' : ''}
                        </span>
                    </div>
                    <span class="user-message">Нажмите чтобы написать</span>
                </div>
            </div>
        </li>
    `).join('');
}

export async function startChatWithUser(userId, username, avatarUrl) {
    try {
        const data   = await fetchOrCreateChat(userId);
        const chatId = data.chatId;

        clearSearch();

        const tempCard = document.createElement('li');
        tempCard.dataset.chatId     = chatId;
        tempCard.dataset.userId     = userId;
        tempCard.dataset.userName   = username;
        tempCard.dataset.userAvatar = avatarUrl;

        await openChat(tempCard);

    } catch (error) {
        console.error('Ошибка:', error);
    }
}

export function clearSearch() {
    const searchInput    = document.getElementById('searchInput');
    const searchResults  = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    if (searchInput)   searchInput.value         = '';
    if (searchResults) {
        searchResults.style.display = 'none';
        searchResults.innerHTML     = '';
    }
    if (chatsContainer) chatsContainer.style.display = 'block';

    loadChats();
}

// ========================
// RESIZER
// ========================

export function initResizer() {
    const resizer   = document.querySelector('.resizer');
    const chatsList = document.querySelector('.chats-list');
    let isResizing  = false;

    resizer.addEventListener('mousedown', (e) => {
        isResizing = true;
        document.body.style.cursor = 'col-resize';
        e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
        if (!isResizing) return;
        const containerLeft = chatsList.getBoundingClientRect().left;
        let newWidth = e.clientX - containerLeft;
        if (newWidth < 260)  newWidth = 260;
        if (newWidth > 1160) newWidth = 1160;
        chatsList.style.width = newWidth + 'px';
    });

    document.addEventListener('mouseup', () => {
        if (isResizing) {
            isResizing = false;
            document.body.style.cursor = 'default';
        }
    });
}