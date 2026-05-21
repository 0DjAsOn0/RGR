import { loadChats, handleSearch, startChatWithUser, clearSearch, initResizer } from './ui.js';
import { connectWebSocket, subscribeToChat, sendWsMessage, isConnected } from './websocket.js';
import { loadMessages, appendMessage, updateMessageStatus, startReadObserver } from './chat.js';
import { fetchOrCreateChat } from './api.js';
import { escapeHtml, collectErrorMessage } from './utils.js';
import { initGroupModal } from './group.js';
import { viewMyProfile, closeCreateWindow, toggleEmailNotifications } from './profile.js';
import { loadCurrentUser, stopHeartbeat, logout } from './user.js';
import { initAttachments, uploadFiles, initLightbox } from './attachments.js';

const DEFAULT_AVATAR = '/avatars/default.png';

export const state = {
    currentUser: null,
    currentChatId: null,
    currentChatUserId: null,
    replyToId: null,
};

let attachmentManager = null;

// ========================
// СТАРТ
// ========================

document.addEventListener('DOMContentLoaded', async () => {
    await loadCurrentUser();

    if (!state.currentUser) {
        return;
    }

    await loadChats();

    connectWebSocket(onMessageReceived);

    initResizer();
    initEventListeners();
    initGroupModal();
    initLightbox();
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
                clearSearch();
            }
        });
    }

    document.addEventListener('click', (e) => {
        const searchCard = e.target.closest('.search-card');
        if (searchCard) {
            const userId = searchCard.dataset.userId;
            const username = searchCard.dataset.userName;
            const avatar = searchCard.dataset.userAvatar;
            startChatWithUser(userId, username, avatar);
            return;
        }

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

        const cancelReply = e.target.closest('.cancel-reply-btn');
        if (cancelReply) {
            clearReply();
        }
    });

    document.addEventListener('keydown', (e) => {
        if (!e.target.matches('#messageInput')) return;

        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
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
    attachmentManager = null;

    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));

    const dialog = document.getElementById('mainDialog');
    if (dialog) {
        dialog.classList.add('empty-dialog');
        dialog.innerHTML = '<p class="main-dialog-inscription">Выберите, кому хотели бы написать</p>';
    }
}

// ========================
// ПРЕВЬЮ СООБЩЕНИЯ
// ========================

function getMessagePreview(msg) {
    const text = msg?.text ?? msg?.content ?? '';

    if (typeof text === 'string' && text.trim()) {
        return text.trim();
    }

    const type = (msg?.type ?? '').toLowerCase();

    if (type === 'image' || type === 'images') return '🖼 Фото';
    if (type === 'video') return '🎥 Видео';
    if (type === 'audio') return '🎵 Аудио';
    if (type === 'file') return '📎 Файл';

    if (Array.isArray(msg?.attachments) && msg.attachments.length > 0) {
        const first = msg.attachments[0];
        const mime = first?.mimeType ?? '';

        if (mime.startsWith('image/')) return '🖼 Фото';
        if (mime.startsWith('video/')) return '🎥 Видео';
        if (mime.startsWith('audio/')) return '🎵 Аудио';
        return '📎 Файл';
    }

    return 'Новое сообщение';
}

function updateChatPreview(msg) {
    const chatId = String(msg.chatId);
    const chatCard = document.querySelector(`.card[data-chat-id="${chatId}"]`);

    if (!chatCard) {
        loadChats();
        return;
    }

    const preview = getMessagePreview(msg);

    const timeEl = chatCard.querySelector('.message-time');
    if (timeEl && msg.time) {
        timeEl.textContent = msg.time;
    }

    const previewEl = chatCard.querySelector('.user-message');
    if (previewEl) {
        previewEl.textContent = preview;
    }

    const isOwn = Number(msg.senderId) === Number(state.currentUser?.id);

    if (chatId !== String(state.currentChatId) && !isOwn) {
        let badge = chatCard.querySelector('.unread-badge');

        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'unread-badge';
            chatCard.querySelector('.message-preview')?.appendChild(badge);
        }

        badge.textContent = String(parseInt(badge.textContent || '0', 10) + 1);
    }

    const container = document.getElementById('chatsContainer');
    if (container && chatCard.parentElement === container) {
        const firstCard = container.querySelector('.card:first-child');
        const firstType = firstCard?.dataset?.chatType;

        if (firstType === 'notes' && firstCard !== chatCard) {
            firstCard.after(chatCard);
        } else {
            container.prepend(chatCard);
        }
    }
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

    msg.own = Number(msg.senderId) === Number(state.currentUser?.id);

    console.log('WS preview message:', msg);

    const isCurrentChat = String(msg.chatId) === String(state.currentChatId);

    if (isCurrentChat) {
        const container = document.getElementById('messagesContainer');
        if (!container) return;

        if (!container.querySelector(`[data-id="${msg.id}"]`)) {
            appendMessage(msg);
            startReadObserver();
        }

        updateChatPreview(msg);
        return;
    }

    loadChats();
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
    const userName = card.dataset.userName ?? 'Собеседник';
    const userAvatar = card.dataset.userAvatar ?? DEFAULT_AVATAR;
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

    pushChatState(chatId);

    fetch(`/api/v1/messages/chat/${chatId}/read`, {
        method: 'POST',
        credentials: 'include'
    }).catch(err => console.error('Ошибка отметки прочитанных:', err));

    const dialog = document.getElementById('mainDialog');
    if (!dialog) return;

    dialog.classList.remove('empty-dialog');
    dialog.innerHTML = `
        <div class="dialog-header">
            <div class="dialog-header-info">
                ${isNotes
        ? `<div class="notes-avatar" style="flex-shrink:0">📝</div>`
        : isGroup
            ? `<div class="notes-avatar" style="flex-shrink:0">👥</div>`
            : `<img class="avatar-img" src="${userAvatar}" alt="">`
    }
                <div class="dialog-header-text">
                    <span class="dialog-name">
                        ${isNotes ? 'Заметки' : escapeHtml(userName)}
                    </span>
                    <span class="dialog-status" id="dialogStatus">
                        ${isNotes ? 'Личные заметки' : isGroup ? 'Группа' : 'Загрузка...'}
                    </span>
                </div>
            </div>
            <div class="dialog-header-actions">
                <button class="icon-btn" type="button">⋮</button>
            </div>
        </div>

        <div class="dialog-messages" id="messagesContainer">
            <div class="messages-loading">Загрузка...</div>
        </div>

        <div class="reply-preview" id="replyPreview" style="display:none;">
            <div class="reply-preview-content">
                <span class="reply-preview-label">Ответ на:</span>
                <span class="reply-preview-text" id="replyPreviewText"></span>
            </div>
            <button class="cancel-reply-btn" type="button">✕</button>
        </div>

        <div class="attachments-preview" id="attachmentsPreview" style="display:none;"></div>

        <div class="dialog-input-area">
            <button class="attach-btn" id="attachBtn" type="button" title="Прикрепить файл">
                <img src="/icons/attachment.svg" alt="вложение">
            </button>
            <input type="file" id="fileInput" multiple hidden>

            <textarea
                class="message-input"
                id="messageInput"
                placeholder="${isNotes ? 'Написать заметку...' : 'Написать сообщение...'}"
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
            statusEl.textContent = user.lastSeen ?? 'не в сети';
            statusEl.style.color = user.status === 'online' ? '#4caf50' : '';
        }
    } catch (error) {
        console.error('Ошибка загрузки статуса:', error);
    }
}

// ========================
// ОТВЕТ НА СООБЩЕНИЕ
// ========================

export function setReply(messageId, messageText) {
    state.replyToId = messageId;

    const replyPreview = document.getElementById('replyPreview');
    const replyText = document.getElementById('replyPreviewText');

    if (replyPreview && replyText) {
        const safeText = (messageText && messageText.trim())
            ? messageText
            : 'Сообщение без текста';

        replyText.textContent =
            safeText.slice(0, 80) + (safeText.length > 80 ? '...' : '');

        replyPreview.style.display = 'flex';
    }

    document.getElementById('messageInput')?.focus();
}

function clearReply() {
    state.replyToId = null;
    const replyPreview = document.getElementById('replyPreview');
    if (replyPreview) {
        replyPreview.style.display = 'none';
    }
}

// ========================
// ОТПРАВКА СООБЩЕНИЯ
// ========================

async function sendMessage() {
    const input = document.getElementById('messageInput');
    if (!input || !state.currentChatId) return;

    const content = input.value.trim();
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
                updateChatPreview(createdMessage);
                appendMessage(createdMessage);
                startReadObserver();
            } else {
                loadChats();
            }
        }

        input.value = '';
        input.style.height = 'auto';
        attachmentManager?.clearFiles?.();
        clearReply();

    } catch (error) {
        console.error('Ошибка загрузки файлов:', error);
        alert('Ошибка загрузки: ' + (error.message ?? 'Неизвестная ошибка'));
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
            let message = 'Ошибка отправки';

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

    } catch (error) {
        console.error('Ошибка HTTP отправки:', error);
        alert(error.message ?? 'Ошибка отправки');
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