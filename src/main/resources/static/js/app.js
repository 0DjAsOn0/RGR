import { loadChats, handleSearch, startChatWithUser, clearSearch, initResizer } from './ui.js';
import { connectWebSocket, subscribeToChat, sendWsMessage, isConnected } from './websocket.js';
import { loadMessages, appendMessage, updateMessageStatus, startReadObserver } from './chat.js';
import { fetchOrCreateChat } from './api.js';
import { escapeHtml, collectErrorMessage } from './utils.js';
import { initGroupModal } from './group.js';
import { viewMyProfile, closeCreateWindow, toggleEmailNotifications } from './profile.js';
import { loadCurrentUser, stopHeartbeat, logout } from './user.js';
import { initAttachments, uploadFiles, initLightbox } from './attachments.js';
import { initChatInfo, openUserProfile, openGroupInfo } from './chat-info.js';
import { t, translateDOM, setLanguage, currentLang } from './i18n.js'; // ✅ Добавлено t

const DEFAULT_AVATAR = '/avatars/default.png';

export const state = {
    currentUser: null,
    currentChatId: null,
    currentChatUserId: null,
    replyToId: null,
    editMessageId: null,
};

let attachmentManager = null;
let chatsRefreshTimer = null;
let chatsRefreshInFlight = false;

const processedMessageIds = new Set();
const PROCESSED_LIMIT = 500;

// ========================
// СТАРТ
// ========================

document.addEventListener('DOMContentLoaded', async () => {
    // Переводим статический HTML при загрузке
    translateDOM();

    // Настраиваем переключатель языка, если он есть на странице
    const langSelect = document.getElementById('langSelect');
    if (langSelect) {
        langSelect.value = currentLang;
        langSelect.addEventListener('change', (e) => setLanguage(e.target.value));
    }

    await loadCurrentUser();

    if (!state.currentUser) {
        return;
    }

    await refreshChatsNow();

    connectWebSocket(onMessageReceived);

    initResizer();
    initEventListeners();
    initGroupModal();
    initLightbox();
    initChatInfo();
});

function initEventListeners() {
    document.getElementById('profileBtn')
        ?.addEventListener('click', viewMyProfile);

    document.getElementById('logoutBtn')
        ?.addEventListener('click', logout);

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            handleSearch(e.target.value);
        });

        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                const firstResult = document.querySelector('#searchResults .card');
                if (firstResult) firstResult.click();
            }

            if (e.key === 'Escape') {
                e.preventDefault();
                e.stopPropagation();
                clearSearch();
            }
        });
    }

    document.addEventListener('click', (e) => {

        // 1. Открытие/закрытие меню опций чата (кнопка ⋮)
        const chatOptionsBtn = e.target.closest('#chatOptionsBtn');
        const chatOptionsMenu = document.getElementById('chatOptionsMenu');

        if (chatOptionsBtn) {
            e.stopPropagation();
            if (chatOptionsMenu) {
                const isHidden = chatOptionsMenu.style.display === 'none';
                chatOptionsMenu.style.display = isHidden ? 'flex' : 'none';
            }
            return;
        }

        // Если кликнули мимо меню — закрываем его
        if (chatOptionsMenu && chatOptionsMenu.style.display !== 'none') {
            const isClickInside = e.target.closest('#chatOptionsMenu');
            if (!isClickInside) {
                chatOptionsMenu.style.display = 'none';
            }
        }

        // 2. Нажатие на "Удалить чат" / "Очистить заметки"
        const deleteChatBtn = e.target.closest('#deleteChatBtn');
        if (deleteChatBtn) {
            e.stopPropagation();
            if (chatOptionsMenu) chatOptionsMenu.style.display = 'none';
            deleteCurrentChat();
            return;
        }

        // Обработка кнопки УДАЛИТЬ сообщение
        const deleteBtn = e.target.closest('.delete-btn');
        if (deleteBtn && !e.target.closest('.chat-info-remove-btn')) {
            const msgId = deleteBtn.dataset.id;
            if (confirm(t('chat.confirmDeleteMessage'))) {
                fetch(`/api/v1/messages/${msgId}`, { method: 'DELETE', credentials: 'include' })
                    .catch(err => alert(t('chat.errorDeleteMessage')));
            }
            return;
        }

        // Обработка кнопки РЕДАКТИРОВАТЬ сообщение
        const editBtn = e.target.closest('.edit-btn');
        if (editBtn) {
            state.editMessageId = editBtn.dataset.id;
            const text = editBtn.dataset.text;

            const messageInput = document.getElementById('messageInput');
            if (messageInput) {
                messageInput.value = text;
                messageInput.focus();
            }

            const replyPreview = document.getElementById('replyPreview');
            const replyText = document.getElementById('replyPreviewText');
            if (replyPreview && replyText) {
                const label = document.querySelector('.reply-preview-label');
                if (label) label.textContent = t('chat.editing');
                replyText.textContent = text.length > 50 ? text.slice(0, 50) + '...' : text;
                replyPreview.style.display = 'flex';
            }
            return;
        }

        // Кнопка закрытия панели Reply / Edit
        const cancelReply = e.target.closest('.cancel-reply-btn');
        if (cancelReply) {
            clearReply();
            state.editMessageId = null;
            const label = document.querySelector('.reply-preview-label');
            if (label) label.textContent = t('chat.replyTo');
            const messageInput = document.getElementById('messageInput');
            if (messageInput) messageInput.value = '';
            return;
        }

        // Клик по пользователю в поиске
        const searchCard = e.target.closest('.search-card');
        if (searchCard) {
            const userId = searchCard.dataset.userId;
            const username = searchCard.dataset.userName;
            const avatar = searchCard.dataset.userAvatar;
            startChatWithUser(userId, username, avatar);
            return;
        }

        // КЛИК ПО ПУБЛИЧНОЙ ГРУППЕ В ПОИСКЕ (ВСТУПЛЕНИЕ)
        const publicGroupCard = e.target.closest('.search-public-group');
        if (publicGroupCard) {
            const chatId = publicGroupCard.dataset.chatId;
            const groupName = publicGroupCard.dataset.userName;

            if (confirm(`${t('group.confirmJoin')} "${groupName}"?`)) {
                joinPublicGroup(chatId, publicGroupCard);
            }
            return;
        }

        // Клик по шапке чата (для открытия инфы)
        const dialogHeader = e.target.closest('.dialog-header');
        if (dialogHeader && state.currentChatId) {
            if (!e.target.closest('.dialog-header-actions')) {
                openCurrentChatInfo();
                return;
            }
        }

        // Клик по карточке чата (или группы в поиске)
        const chatCard = e.target.closest('.card[data-chat-id]');
        if (chatCard) {
            openChat(chatCard);
            return;
        }

        const sendBtn = e.target.closest('.send-btn');
        if (sendBtn) {
            sendMessage();
            return;
        }

        const profilePage = document.getElementById('createWindow');
        if (profilePage && e.target === profilePage) {
            closeCreateWindow();
            return;
        }
    });

    document.addEventListener('keydown', (e) => {
        if (!e.target.matches('#messageInput')) return;

        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key !== 'Escape') return;

        const profilePage = document.getElementById('createWindow');
        if (profilePage && profilePage.style.display !== 'none') {
            closeCreateWindow();
            return;
        }

        const searchInputEl = document.getElementById('searchInput');
        const searchResults = document.getElementById('searchResults');
        const hasSearchValue = !!searchInputEl?.value?.trim();
        const searchVisible = !!(
            searchResults &&
            searchResults.style.display !== 'none' &&
            searchResults.innerHTML.trim()
        );

        if (hasSearchValue || searchVisible) {
            clearSearch(false);
            return;
        }

        if (state.currentChatId) {
            if (history.state?.chatOpen) {
                history.back();
            } else {
                closeChatView();
            }
        }
    });

    window.addEventListener('popstate', () => {
        if (state.currentChatId) {
            closeChatView();
        }
    });

    window.addEventListener('beforeunload', () => {
        stopHeartbeat();

        if (navigator.sendBeacon) {
            navigator.sendBeacon('/api/v1/users/me/offline');
        }
    });
}

// ========================
// УДАЛЕНИЕ ЧАТА / ОЧИСТКА ЗАМЕТОК
// ========================
async function deleteCurrentChat() {
    if (!state.currentChatId) return;

    const card = document.querySelector(`.card[data-chat-id="${state.currentChatId}"]`);
    const isNotes = card?.dataset.chatType === 'notes';

    const confirmMsg = isNotes
        ? t('chat.confirmClearNotes')
        : t('chat.confirmDeleteChat');

    if (!confirm(confirmMsg)) {
        return;
    }

    try {
        const response = await fetch(`/api/v1/chats/${state.currentChatId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Ошибка удаления');
        }

        if (isNotes) {
            const container = document.getElementById('messagesContainer');
            if (container) {
                container.innerHTML = `<div class="no-messages">${t('chat.notesCleared')}</div>`;
                container.classList.add('empty');
            }
        } else {
            closeChatView();
        }

        refreshChatsDebounced();

    } catch (error) {
        console.error('Ошибка удаления чата:', error);
        alert(t('chat.errorDeleteChat'));
    }
}

// ФУНКЦИЯ ВСТУПЛЕНИЯ В ПУБЛИЧНУЮ ГРУППУ
async function joinPublicGroup(chatId, cardElement) {
    try {
        const response = await fetch(`/api/v1/chats/${chatId}/join`, {
            method: 'POST',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Не удалось вступить в группу');
        }

        clearSearch(false);
        await refreshChatsNow();
        openChat(cardElement);

    } catch (error) {
        console.error(error);
        alert(t('group.errorJoin'));
    }
}

// ========================
// ИНФО О ТЕКУЩЕМ ЧАТЕ
// ========================

function openCurrentChatInfo() {
    if (!state.currentChatId) return;

    const card = document.querySelector(`.card[data-chat-id="${state.currentChatId}"]`);
    const chatType = card?.dataset.chatType ?? 'private';

    if (chatType === 'notes') {
        return;
    }

    if (chatType === 'group') {
        openGroupInfo(state.currentChatId);
        return;
    }

    const userId = card?.dataset.userId || state.currentChatUserId;
    if (userId) {
        openUserProfile(userId);
    }
}

// ========================
// ЧАТЫ: СИНХРОНИЗАЦИЯ
// ========================

function refreshChatsDebounced(delay = 200) {
    if (chatsRefreshTimer) {
        clearTimeout(chatsRefreshTimer);
    }

    chatsRefreshTimer = setTimeout(() => {
        chatsRefreshTimer = null;
        refreshChatsNow();
    }, delay);
}

async function refreshChatsNow() {
    if (chatsRefreshInFlight) return;

    try {
        chatsRefreshInFlight = true;
        await loadChats();
    } catch (error) {
        console.error('Ошибка обновления списка чатов:', error);
    } finally {
        chatsRefreshInFlight = false;
    }
}

// ========================
// HISTORY API
// ========================

function pushChatState(chatId) {
    history.pushState(
        { chatOpen: true, chatId },
        '',
        window.location.pathname
    );
}

function closeChatView() {
    state.currentChatId = null;
    state.currentChatUserId = null;
    state.replyToId = null;
    state.editMessageId = null;
    attachmentManager = null;

    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));

    const dialog = document.getElementById('mainDialog');
    if (dialog) {
        dialog.classList.add('empty-dialog');
        dialog.innerHTML = `<p class="main-dialog-inscription">${t('chat.emptyState')}</p>`;
    }
}

// ========================
// PREVIEW ДЛЯ СПИСКА ЧАТОВ
// ========================

function getPreviewText(msg) {
    const text = typeof msg?.text === 'string' ? msg.text.trim() : '';
    if (text) return text;

    const type = String(msg?.type || '').toLowerCase();

    if (type === 'image' || type === 'images') return t('chat.previewPhoto');
    if (type === 'video') return t('chat.previewVideo');
    if (type === 'audio') return t('chat.previewAudio');
    if (type === 'file') {
        const fileName = msg?.attachments?.[0]?.fileName;
        return fileName ? `📎 ${fileName}` : t('chat.previewFile');
    }

    if (Array.isArray(msg?.attachments) && msg.attachments.length > 0) {
        const attachment = msg.attachments[0];
        const mime = String(attachment?.mimeType || '').toLowerCase();

        if (mime.startsWith('image/')) return t('chat.previewPhoto');
        if (mime.startsWith('video/')) return t('chat.previewVideo');
        if (mime.startsWith('audio/')) return t('chat.previewAudio');

        return attachment?.fileName ? `📎 ${attachment.fileName}` : t('chat.previewFile');
    }

    return t('chat.noMessages');
}

function moveChatCard(card) {
    const container = card?.parentElement;
    if (!container) return;

    const isNotes = card.dataset.chatType === 'notes';

    if (isNotes) {
        container.prepend(card);
        return;
    }

    const notesCard = container.querySelector('.card[data-chat-type="notes"]');

    if (notesCard && notesCard !== card) {
        notesCard.insertAdjacentElement('afterend', card);
    } else {
        container.prepend(card);
    }
}

function updateChatPreview(msg) {
    const card = document.querySelector(`.card[data-chat-id="${msg.chatId}"]`);
    if (!card) return false;

    const previewEl = card.querySelector('.user-message');
    const timeEl = card.querySelector('.message-time');

    if (previewEl) {
        previewEl.textContent = getPreviewText(msg);
    }

    if (timeEl) {
        const rawTime = msg.createdAt || msg.time;
        if (rawTime) {
            const date = new Date(rawTime);
            if (!Number.isNaN(date.getTime())) {
                timeEl.textContent = date.toLocaleTimeString('ru-RU', {
                    hour: '2-digit',
                    minute: '2-digit'
                });
            }
        }
    }

    if (String(state.currentChatId) !== String(msg.chatId)) {
        let badge = card.querySelector('.unread-badge');
        const previewRow = card.querySelector('.message-preview');

        if (!badge && previewRow) {
            badge = document.createElement('span');
            badge.className = 'unread-badge';
            previewRow.appendChild(badge);
        }

        if (badge) {
            const current = Number(badge.textContent || '0');
            badge.textContent = String(current + 1);
        }
    }

    moveChatCard(card);
    return true;
}

// ========================
// WS ОБРАБОТЧИК
// ========================

function onMessageReceived(frame) {
    const msg = JSON.parse(frame.body);

    if (msg.type === 'STATUS_UPDATE') {
        updateMessageStatus(msg.messageId, msg.status);
        return;
    }

    if (msg.type === 'CHAT_LIST_UPDATE') {
        const chatId = String(msg.chatId);

        if (msg.action === 'removed' || msg.action === 'deleted') {
            if (String(state.currentChatId) === chatId) {
                closeChatView();
            }
        } else if (msg.action === 'notes_cleared') {
            if (String(state.currentChatId) === chatId) {
                const container = document.getElementById('messagesContainer');
                if (container) {
                    container.innerHTML = `<div class="no-messages">${t('chat.notesCleared')}</div>`;
                    container.classList.add('empty');
                }
            }
        }

        refreshChatsDebounced();
        return;
    }

    if (msg.type === 'MESSAGE_DELETED') {
        const msgEl = document.querySelector(`.message[data-id="${msg.messageId}"]`);
        if (msgEl) msgEl.remove();
        return;
    }

    if (msg.type === 'MESSAGE_EDITED') {
        const msgEl = document.querySelector(`.message[data-id="${msg.messageId}"]`);
        if (msgEl) {
            const textDiv = msgEl.querySelector('.msg-text');
            if (textDiv) textDiv.textContent = msg.text;

            const editBtn = msgEl.querySelector('.edit-btn');
            if (editBtn) editBtn.dataset.text = msg.text;

            const metaDiv = msgEl.querySelector('.message-meta');
            if (metaDiv && !metaDiv.querySelector('.msg-edited-mark')) {
                metaDiv.insertAdjacentHTML('afterbegin', `<span class="msg-edited-mark">(${t('chat.editedShort')})</span>`);
            }
        }
        return;
    }

    if (msg.id != null) {
        const key = String(msg.id);
        if (processedMessageIds.has(key)) {
            return;
        }
        processedMessageIds.add(key);

        if (processedMessageIds.size > PROCESSED_LIMIT) {
            const iterator = processedMessageIds.values();
            for (let i = 0; i < 100; i++) {
                const next = iterator.next();
                if (next.done) break;
                processedMessageIds.delete(next.value);
            }
        }
    }

    msg.own = Number(msg.senderId) === Number(state.currentUser?.id);

    const isCurrentChat = String(msg.chatId) === String(state.currentChatId);

    if (isCurrentChat) {
        const container = document.getElementById('messagesContainer');
        if (container && !container.querySelector(`[data-id="${msg.id}"]`)) {
            appendMessage(msg);
            startReadObserver();
        }
    }

    const updated = updateChatPreview(msg);
    if (!updated) {
        refreshChatsDebounced();
    }
}

// ========================
// ОТКРЫТИЕ ЧАТА
// ========================

export async function openChat(card) {
    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));
    card.classList?.add('active');

    const badge = card.querySelector('.unread-badge');
    if (badge) {
        badge.remove();
    }

    const userId = card.dataset.userId;
    const userName = card.dataset.userName ?? t('chat.defaultInterlocutor');
    const userAvatar = card.dataset.userAvatar ?? '';
    const chatType = card.dataset.chatType ?? 'private';
    let chatId = card.dataset.chatId;

    const isNotes = chatType === 'notes';
    const isGroup = chatType === 'group';

    if (!chatId && userId) {
        const data = await fetchOrCreateChat(userId);
        chatId = data.chatId;
        card.dataset.chatId = chatId;
    }

    if (!chatId) {
        console.error('Не удалось открыть чат: отсутствует chatId');
        return;
    }

    state.currentChatId = chatId;
    state.currentChatUserId = userId || null;
    state.replyToId = null;
    state.editMessageId = null;

    pushChatState(chatId);

    fetch(`/api/v1/messages/chat/${chatId}/read`, {
        method: 'POST',
        credentials: 'include'
    }).catch(err => console.error('Ошибка отметки прочитанных:', err));

    const dialog = document.getElementById('mainDialog');
    if (!dialog) return;

    const avatarBlock = isNotes
        ? `<div class="notes-avatar" style="flex-shrink:0">📝</div>`
        : isGroup
            ? (userAvatar
                ? `<img class="avatar-img" src="${escapeHtml(userAvatar)}" alt="">`
                : `<div class="notes-avatar" style="flex-shrink:0">👥</div>`)
            : `<img class="avatar-img" src="${escapeHtml(userAvatar || DEFAULT_AVATAR)}" alt="">`;

    dialog.classList.remove('empty-dialog');
    dialog.innerHTML = `
        <div class="dialog-header" data-chat-type="${chatType}">
            <div class="dialog-header-info">
                ${avatarBlock}
                <div class="dialog-header-text">
                    <span class="dialog-name">
                        ${isNotes ? t('chat.notes') : escapeHtml(userName)}
                    </span>
                    <span class="dialog-status" id="dialogStatus">
                        ${isNotes ? t('chat.personalNotes') : isGroup ? t('chat.group') : t('app.loading')}
                    </span>
                </div>
            </div>
            <div class="dialog-header-actions" style="position: relative;">
                <button class="icon-btn" id="chatOptionsBtn" type="button">⋮</button>
                <div class="chat-options-menu" id="chatOptionsMenu" style="display: none;">
                    <button class="chat-option-btn danger" id="deleteChatBtn" type="button">
                        ${isNotes ? t('chat.clearNotes') : t('chat.deleteChat')}
                    </button>
                </div>
            </div>
        </div>

        <div class="dialog-messages" id="messagesContainer">
            <div class="messages-loading">${t('app.loading')}</div>
        </div>

        <div class="reply-preview" id="replyPreview" style="display:none;">
            <div class="reply-preview-content">
                <span class="reply-preview-label">${t('chat.replyTo')}</span>
                <span class="reply-preview-text" id="replyPreviewText"></span>
            </div>
            <button class="cancel-reply-btn" type="button">✕</button>
        </div>

        <div class="attachments-preview" id="attachmentsPreview" style="display:none;"></div>

        <div class="dialog-input-area">
            <button class="attach-btn" id="attachBtn" type="button" title="${t('chat.attachFile')}">
                <img src="/icons/attachment.svg" alt="${t('chat.attachment')}">
            </button>
            <input type="file" id="fileInput" multiple hidden>

            <textarea
                class="message-input"
                id="messageInput"
                placeholder="${isNotes ? t('chat.writeNote') : t('chat.writeMessage')}"
                rows="1"></textarea>

            <button class="icon-btn send-btn" type="button">➤</button>
        </div>
    `;

    attachmentManager = initAttachments();

    if (!isNotes && !isGroup && userId && Number(userId) !== 0) {
        loadUserStatus(userId);
    }

    const openedChatId = String(chatId);

    await loadMessages(chatId);

    if (String(state.currentChatId) !== openedChatId) {
        return;
    }

    subscribeToChat(chatId, onMessageReceived);
}

// ========================
// СТАТУС ПОЛЬЗОВАТЕЛЯ
// ========================

async function loadUserStatus(userId) {
    try {
        const response = await fetch(`/api/v1/users/${userId}`, {
            credentials: 'include'
        });

        if (!response.ok) return;

        const user = await response.json();
        const statusEl = document.getElementById('dialogStatus');

        if (statusEl) {
            statusEl.textContent = user.lastSeen ?? t('status.offline');
            statusEl.style.color = user.status === 'online' ? '#4caf50' : '';
        }
    } catch (error) {
        console.error('Ошибка загрузки статуса:', error);
    }
}

// ========================
// ОТВЕТ НА СООБЩЕНИЕ И РЕДАКТИРОВАНИЕ
// ========================

export function setReply(messageId, messageText) {
    state.replyToId = messageId;
    state.editMessageId = null;

    const label = document.querySelector('.reply-preview-label');
    if (label) label.textContent = t('chat.replyTo');

    const replyPreview = document.getElementById('replyPreview');
    const replyText = document.getElementById('replyPreviewText');

    if (replyPreview && replyText) {
        const safeText = (messageText && messageText.trim())
            ? messageText
            : t('chat.messageWithoutText');

        replyText.textContent =
            safeText.slice(0, 80) + (safeText.length > 80 ? '...' : '');

        replyPreview.style.display = 'flex';
    }

    const input = document.getElementById('messageInput');
    if (input) {
        input.value = '';
        input.focus();
    }
}

function clearReply() {
    state.replyToId = null;
    const replyPreview = document.getElementById('replyPreview');
    if (replyPreview) {
        replyPreview.style.display = 'none';
    }
}

// ========================
// ОТПРАВКА СООБЩЕНИЯ (И РЕДАКТИРОВАНИЕ)
// ========================

async function sendMessage() {
    const input = document.getElementById('messageInput');
    if (!input || !state.currentChatId) return;

    const content = input.value.trim();

    if (state.editMessageId) {
        if (!content) return;

        try {
            await fetch(`/api/v1/messages/${state.editMessageId}`, {
                method: 'PATCH',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: content })
            });

            input.value = '';
            input.style.height = 'auto';
            clearReply();
            state.editMessageId = null;
            const label = document.querySelector('.reply-preview-label');
            if (label) label.textContent = t('chat.replyTo');

        } catch (error) {
            console.error('Ошибка редактирования сообщения:', error);
            alert(t('chat.errorEditMessage'));
        }
        return;
    }

    const hasFiles = attachmentManager?.hasFiles() ?? false;

    if (!content && !hasFiles) return;

    if (hasFiles) {
        await sendMessageWithFiles(content, input);
        return;
    }

    if (isConnected()) {
        sendWsMessage(state.currentChatId, content, state.replyToId);
        input.value = '';
        input.style.height = 'auto';
        clearReply();
    } else {
        await sendMessageHttp(content, input);
    }
}

async function sendMessageWithFiles(text, input) {
    const files = attachmentManager?.getFiles?.() ?? [];

    try {
        const sendBtn = document.querySelector('.send-btn');
        if (sendBtn) {
            sendBtn.disabled = true;
            sendBtn.innerHTML = '⏳';
        }

        const createdMessage = await uploadFiles(
            state.currentChatId,
            files,
            text,
            state.replyToId
        );

        if (createdMessage) {
            createdMessage.own = Number(createdMessage.senderId) === Number(state.currentUser?.id);

            if (String(createdMessage.chatId) === String(state.currentChatId)) {
                appendMessage(createdMessage);
                startReadObserver();
            }

            refreshChatsDebounced();
        }

        input.value = '';
        input.style.height = 'auto';
        attachmentManager?.clearFiles?.();
        clearReply();

    } catch (error) {
        console.error('Ошибка загрузки файлов:', error);
        alert(t('chat.errorUpload') + (error.message ?? ''));
    } finally {
        const sendBtn = document.querySelector('.send-btn');
        if (sendBtn) {
            sendBtn.disabled = false;
            sendBtn.innerHTML = '➤';
        }
    }
}

async function sendMessageHttp(content, input) {
    try {
        const response = await fetch(`/api/v1/messages/chat/${state.currentChatId}`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                content,
                replyToId: state.replyToId
            })
        });

        if (!response.ok) {
            let message = t('chat.errorSend');

            try {
                const data = await response.json();
                message = collectErrorMessage(data) || message;
            } catch {
                // ignore
            }

            throw new Error(message);
        }

        input.value = '';
        input.style.height = 'auto';
        clearReply();

        refreshChatsDebounced();

    } catch (error) {
        console.error('Ошибка HTTP отправки:', error);
        alert(error.message ?? t('chat.errorSend'));
    }
}

// ========================
// ГЛОБАЛЬНЫЕ ФУНКЦИИ
// ========================

window.handleOpenChat = openChat;
window.handleStartChat = startChatWithUser;
window.handleSearch = handleSearch;
window.sendMessage = sendMessage;
window.viewMyProfile = viewMyProfile;
window.closeCreateWindow = closeCreateWindow;
window.toggleEmailNotifications = toggleEmailNotifications;
window.logout = logout;
window.setReply = setReply;