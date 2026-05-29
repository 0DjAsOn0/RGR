import { escapeHtml } from './utils.js';
import { fetchChats, searchUsers, searchPublicGroups, fetchOrCreateChat } from './api.js';
import { openChat, state } from './app.js';
import { t } from './i18n.js';

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

//последнее сообщение чтобы было показано в списке чатов чо там было
function getLastMessagePreview(chat) {
    const text = typeof chat.lastMessage === 'string' ? chat.lastMessage.trim() : '';
    const type = String(chat.lastMessageType || '').toLowerCase();

    if (text) return escapeHtml(text);

    switch (type) {
        case 'image':
        case 'images':
            return t('chat.previewPhoto');
        case 'video':
            return t('chat.previewVideo');
        case 'audio':
            return t('chat.previewAudio');
        case 'file':
            return t('chat.previewFile');
        default:
            if (chat.hasAttachment) return t('chat.previewAttachment');
            return t('chat.noMessages');
    }
}

//типы чатов какие нужны
function normalizeChatType(chat) {
    if (chat.type === 'notes') return 'notes';
    if (chat.type === 'group') return 'group';
    if (chat.name === 'Заметки' && !chat.interlocutorId) return 'notes';
    return 'private';
}

//отрисовка списка чатов
export function renderChatList(chats) {
    const container = document.getElementById('chatsContainer');
    if (!container) return;

    if (!chats || chats.length === 0) {
        container.innerHTML = `<li class="no-chats">${t('chat.noChats')}</li>`;
        return;
    }

    container.innerHTML = chats.map(chat => {
        const chatType = normalizeChatType(chat);
        const isNotes = chatType === 'notes';
        const isGroup = chatType === 'group';

        const rawName = isNotes
            ? t('chat.notes')
            : isGroup
                ? (chat.name ?? t('chat.group'))
                : (chat.interlocutorName ?? chat.name ?? t('chat.defaultChat'));

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
// ПОИСК (ПОЛЬЗОВАТЕЛИ + ГРУППЫ)
// ========================

//поиск по строке
export async function handleSearch(query) {
    const searchResults = document.getElementById('searchResults');
    const chatsContainer = document.getElementById('chatsContainer');

    //сброс таймаута поиска
    clearTimeout(searchTimeout);

    //ограничение 2 символа в поиске
    if (!query || query.trim().length < 2) {
        searchRequestId++;
        if (searchResults) {
            searchResults.innerHTML = '';
            searchResults.style.display = 'none';
        }
        if (chatsContainer) chatsContainer.style.display = 'block';
        return;
    }


    //отложеный запус поиска чтобы не отправлять много запросов если пользователь скорострел
    searchTimeout = setTimeout(async () => {
        const currentRequestId = ++searchRequestId;

        if (searchResults) {
            searchResults.style.display = 'block';
            searchResults.innerHTML = `<div style="padding: 15px; text-align: center; color: #888;">${t('search.searching')}</div>`;
        }
        if (chatsContainer) chatsContainer.style.display = 'none';

        try {
            const [users, groups] = await Promise.all([
                searchUsers(query),
                searchPublicGroups(query)
            ]);

            if (currentRequestId !== searchRequestId) return;

            renderCombinedSearchResults(users, groups);

        } catch (error) {
            if (currentRequestId !== searchRequestId) return;
            console.error('Ошибка поиска:', error);
            if (searchResults) {
                searchResults.innerHTML = `<div style="padding: 15px; text-align: center; color: #c62828;">${t('search.error')}</div>`;
            }
        }
    }, 300);
}

//отрисовка результатов
function renderCombinedSearchResults(users, groups) {

    const searchResults = document.getElementById('searchResults');
    if (!searchResults) return;

    searchResults.innerHTML = '';
    let hasResults = false;

    // 1. Отрисовка пользователей
    const filteredUsers = (users || []).filter(u => u.id !== state.currentUser?.id);

    if (filteredUsers.length > 0) {
        hasResults = true;
        searchResults.innerHTML += `<div style="padding: 8px 15px; font-size: 12px; font-weight: bold; color: #888; text-transform: uppercase;">${t('search.users')}</div>`;

        filteredUsers.forEach(u => {
            const avatarUrl = u.avatarUrl ?? DEFAULT_AVATAR;
            const statusLabel = u.status === 'online' ? t('status.online') : t('search.clickToWrite');
            const statusClass = u.status === 'online' ? 'online' : '';

            searchResults.innerHTML += `
                <div class="card search-card" data-user-id="${u.id}" data-user-name="${escapeHtml(u.username)}" data-user-avatar="${escapeHtml(avatarUrl)}">
                    <div class="chat-card">
                        <div class="avatar">
                            <img class="avatar-img" src="${escapeHtml(avatarUrl)}" alt="">
                        </div>
                        <div class="card-content">
                            <div class="name-time">
                                <span class="user-name">${escapeHtml(u.username)}</span>
                                <span class="user-status-badge ${statusClass}">
                                    ${u.status === 'online' ? t('status.online') : ''}
                                </span>
                            </div>
                            <span class="user-message">${statusLabel}</span>
                        </div>
                    </div>
                </div>
            `;
        });
    }

    // 2. Отрисовка публичных групп
    if (groups && groups.length > 0) {
        hasResults = true;
        searchResults.innerHTML += `<div style="padding: 8px 15px; font-size: 12px; font-weight: bold; color: #888; text-transform: uppercase; margin-top: 10px;">${t('search.publicGroups')}</div>`;

        groups.forEach(g => {
            const avatarUrl = g.avatarUrl ?? DEFAULT_AVATAR;
            const groupName = g.name ?? t('chat.group');

            searchResults.innerHTML += `
                <div class="card search-public-group" data-chat-id="${g.id}" data-chat-type="group" data-user-name="${escapeHtml(groupName)}" data-user-avatar="${escapeHtml(avatarUrl)}">
                    <div class="chat-card">
                        <div class="avatar">
                            <img class="avatar-img" src="${escapeHtml(avatarUrl)}" alt="">
                        </div>
                        <div class="card-content">
                            <div class="name-time">
                                <span class="user-name">${escapeHtml(groupName)}</span>
                            </div>
                            <span class="user-message" style="color: #4caf50;">${t('search.clickToJoin')}</span>
                        </div>
                    </div>
                </div>
            `;
        });
    }

    if (!hasResults) {
        searchResults.innerHTML = `<div style="padding: 15px; text-align: center; color: #888;">${t('search.notFound')}</div>`;
    }
}

// ========================
// ДЕЙСТВИЯ С ЧАТАМИ И ПОИСКОМ
// ========================
//начать чат с пользователем
export async function startChatWithUser(userId, username, avatarUrl) {
    try {
        const data = await fetchOrCreateChat(userId);
        const chatId = data.chatId;

        //очищаем поле поиска
        clearSearch(false);

        //перезагружаем список чатов
        await loadChats();

        //поиск карточки
        const realCard = document.querySelector(`.card[data-chat-id="${chatId}"]`);
        if (realCard) {
            await openChat(realCard);
            return;
        }

        //если че создается временная
        const tempCard = document.createElement('li');
        tempCard.className = 'card';
        tempCard.dataset.chatId = chatId;
        tempCard.dataset.userId = userId;
        tempCard.dataset.userName = username ?? t('chat.defaultChat');
        tempCard.dataset.userAvatar = avatarUrl ?? DEFAULT_AVATAR;
        tempCard.dataset.chatType =
            Number(userId) === Number(state.currentUser?.id) ? 'notes' : 'private';

        //открываем чат
        await openChat(tempCard);

    } catch (error) {
        console.error('Ошибка создания чата:', error);
    }
}

//очистка поля поиска
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
//изменение размера списка чатов
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