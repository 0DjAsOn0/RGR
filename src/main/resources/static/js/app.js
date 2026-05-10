import { loadCurrentUser, stopHeartbeat,
    viewMyProfile, closeCreateWindow,
    toggleEmailNotifications, logout }   from './user.js';
import { loadChats, renderChatList,
    handleSearch, startChatWithUser,
    clearSearch, initResizer }           from './ui.js';
import { connectWebSocket, subscribeToChat,
    sendWsMessage, isConnected }         from './websocket.js';
import { loadMessages, appendMessage,
    updateMessageStatus,
    startReadObserver }                  from './chat.js';
import { fetchOrCreateChat }                  from './api.js';
import { escapeHtml }                         from './utils.js';

// ========================
// ГЛОБАЛЬНОЕ СОСТОЯНИЕ
// ========================

export const state = {
    currentUser:      null,
    currentChatId:    null,
    currentChatUserId: null,
};

// ========================
// СТАРТ
// ========================

document.addEventListener('DOMContentLoaded', async () => {
    history.replaceState({ chatOpen: false }, '', window.location.pathname);

    await loadCurrentUser();
    await loadChats();
    connectWebSocket(onMessageReceived);
    initResizer();
    initEventListeners();
});

function initEventListeners() {

    // Кнопка профиля
    document.getElementById('profileBtn')
        ?.addEventListener('click', viewMyProfile);

    // Кнопка выхода
    document.getElementById('logoutBtn')
        ?.addEventListener('click', logout);

    // ✅ Поиск — по вводу текста
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        // Поиск при вводе
        searchInput.addEventListener('input', (e) => {
            console.log('Поиск:', e.target.value);
            handleSearch(e.target.value);
        });

        // ✅ Enter — открываем первый результат
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                const firstResult = document.querySelector(
                    '#searchResults .card'
                );
                if (firstResult) firstResult.click();
            }
        });

        // Escape — очищаем поиск
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                clearSearch();
            }
        });
    }

    // Делегирование кликов
    document.addEventListener('click', (e) => {

        // Карточка чата
        const chatCard = e.target.closest('.card[data-chat-id]');
        if (chatCard) {
            openChat(chatCard);
            return;
        }

        // Карточка поиска
        const searchCard = e.target.closest('.search-card');
        if (searchCard) {
            const userId   = searchCard.dataset.userId;
            const username = searchCard.dataset.userName;
            const avatar   = searchCard.dataset.userAvatar;
            startChatWithUser(userId, username, avatar);
            return;
        }

        // Кнопка отправки
        const sendBtn = e.target.closest('.send-btn');
        if (sendBtn) {
            sendMessage();
            return;
        }

        // Закрытие профиля по фону
        const profilePage = document.getElementById('createWindow');
        if (profilePage && e.target === profilePage) {
            closeCreateWindow();
            return;
        }
    });

    // Клавиатура в поле ввода сообщения
    document.addEventListener('keydown', (e) => {
        if (!e.target.matches('#messageInput')) return;
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // История браузера
    window.addEventListener('popstate', () => {
        if (state.currentChatId) closeChatView();
    });

    // Оффлайн при закрытии
    window.addEventListener('beforeunload', () => {
        stopHeartbeat();
        navigator.sendBeacon('/api/v1/users/me/offline');
    });
}

// ========================
// HISTORY API
// ========================

window.addEventListener('popstate', () => {
    if (state.currentChatId) closeChatView();
});

function pushChatState(chatId) {
    history.pushState(
        { chatOpen: true, chatId },
        '',
        window.location.pathname
    );
}

function closeChatView() {
    state.currentChatId      = null;
    state.currentChatUserId  = null;

    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));

    const dialog = document.getElementById('mainDialog');
    if (dialog) {
        dialog.classList.add('empty-dialog');
        dialog.innerHTML = '<p class="main-dialog-inscription">Выберите, кому хотели бы написать </p>'
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

    const container = document.getElementById('messagesContainer');
    if (container?.querySelector(`[data-id="${msg.id}"]`)) return;

    appendMessage(msg);
    startReadObserver();
    loadChats();
}

// ========================
// ОТКРЫТИЕ ЧАТА
// ========================

export async function openChat(card) {
    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));
    card.classList?.add('active');

    const userId     = card.dataset.userId;
    const userName   = card.dataset.userName   ?? 'Собеседник';
    const userAvatar = card.dataset.userAvatar ?? '/img/avatar1.JPG';
    let   chatId     = card.dataset.chatId;

    if (!chatId && userId) {
        const data = await fetchOrCreateChat(userId);
        chatId = data.chatId;
        card.dataset.chatId = chatId;
    }

    state.currentChatId      = chatId;
    state.currentChatUserId  = userId;

    pushChatState(chatId);

    const dialog = document.getElementById('mainDialog');
    dialog.classList.remove('empty-dialog');
    dialog.innerHTML = `
        <div class="dialog-header">
            <div class="dialog-header-info">
                <img class="avatar-img" src="${userAvatar}" alt="">
                <div class="dialog-header-text">
                    <span class="dialog-name">${escapeHtml(userName)}</span>
                    <span class="dialog-status" id="dialogStatus">...</span>
                </div>
            </div>
            <div class="dialog-header-actions">
                <button class="icon-btn">🔍</button>
                <button class="icon-btn">⋮</button>
            </div>
        </div>

        <div class="dialog-messages" id="messagesContainer">
            <div class="messages-loading">Загрузка...</div>
        </div>

        <div class="dialog-input-area">
            <button class="icon-btn">📎</button>
            <textarea
                class="message-input"
                id="messageInput"
                placeholder="Написать сообщение..."
                rows="1"
                onkeydown="handleMessageKeydown(event)">
            </textarea>
            <button class="icon-btn">😊</button>
            <button class="icon-btn send-btn" onclick="sendMessage()">➤</button>
        </div>
    `;

    await loadMessages(chatId);
    subscribeToChat(chatId, onMessageReceived);
}

// ========================
// ОТПРАВКА СООБЩЕНИЯ
// ========================

async function sendMessage() {
    const input = document.getElementById('messageInput');
    if (!input || !state.currentChatId) return;

    const content = input.value.trim();
    if (!content) return;

    if (isConnected()) {
        sendWsMessage(state.currentChatId, content);
        input.value = '';
        input.style.height = 'auto';
    } else {
        await sendMessageHttp(content, input);
    }
}

async function sendMessageHttp(content, input) {
    try {
        const response = await fetch(
            `/api/v1/messages/chat/${state.currentChatId}`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content })
            });

        if (!response.ok) throw new Error('Ошибка отправки');
        input.value = '';
        await loadChats();

    } catch (error) {
        console.error('Ошибка HTTP отправки:', error);
    }
}

function handleMessageKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// ========================
// ГЛОБАЛЬНЫЕ ФУНКЦИИ для HTML onclick
// ========================

window.handleOpenChat      = openChat;
window.handleStartChat     = startChatWithUser;
window.handleSearch        = handleSearch;
window.sendMessage         = sendMessage;
window.handleMessageKeydown = handleMessageKeydown;
window.viewMyProfile       = viewMyProfile;
window.closeCreateWindow   = closeCreateWindow;
window.toggleEmailNotifications = toggleEmailNotifications;
window.logout              = logout;