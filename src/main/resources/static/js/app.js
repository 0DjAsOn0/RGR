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
import { t, translateDOM, setLanguage, currentLang } from './i18n.js';

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
    // переводим страницу при загрузке
    translateDOM();

    // настраиваем переключатель языка, если он есть на странице
    const langSelect = document.getElementById('langSelect');
    if (langSelect) {
        langSelect.value = currentLang;
        langSelect.addEventListener('change', (e) => setLanguage(e.target.value));
    }

    //получаем данные текущего пользователя
    await loadCurrentUser();
    //проверка правильно ли загрузился пользователь, если нет, то на логин кидает
    if (!state.currentUser) {
        return;
    }

    //получаем чатики пользователя
    await refreshChatsNow();


    //(WebSocket) постоянное двустороннее соединение с сервером
    // теперь сервер может отправлять новые сообщения без обновления страницы
    connectWebSocket(onMessageReceived);

    //прогрузка всяких приколюх
    initResizer(); //растягивание списка чатов
    initEventListeners(); // обработчик событий
    initGroupModal(); //иконки кнопчки
    initLightbox(); //открытие фоток больше и кароч чтобы фон затемнялся круто
    initChatInfo(); //открытие инфы о чатике в окошке
});

// функция в которой анализируем куда кликает пользователь и выполняем действия в зависимости от этого
function initEventListeners() {

    //если на профильбатон тыкнул, то показывается профиль пользователя
    document.getElementById('profileBtn')
        ?.addEventListener('click', viewMyProfile);


    //если на логаут тыкнул, то выходим с аккаунта
    document.getElementById('logoutBtn')
        ?.addEventListener('click', logout);


    //если в поле поиска вводит то ищем че он там ввел
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            handleSearch(e.target.value);
        });

        //нажал ентр заходим в чат с первым в списке
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                const firstResult = document.querySelector('#searchResults .card');
                if (firstResult) firstResult.click();
            }

            //нажал ескейп то выходим с поиска
            if (e.key === 'Escape') {
                e.preventDefault();
                e.stopPropagation();
                clearSearch();
            }
        });
    }

    //анализируем куда кликает пользователь и выполняем действия в зависимости от этого
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

        //Обработка кнопки ОТВЕТИТЬ на сообщение
        const replyBtn = e.target.closest('.reply-btn');
        if (replyBtn) {
            const msgId = replyBtn.dataset.id;
            const text = replyBtn.dataset.text;
            setReply(msgId, text);
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

        // Кнопка закрытия панели ответа или редактирования
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

        //клик отправить сообщение
        const sendBtn = e.target.closest('.send-btn');
        if (sendBtn) {
            sendMessage();
            return;
        }

        //клик создания беседы
        const profilePage = document.getElementById('createWindow');
        if (profilePage && e.target === profilePage) {
            closeCreateWindow();
            return;
        }
    });


    //отправка сообщения на энтр
    document.addEventListener('keydown', (e) => {
        if (!e.target.matches('#messageInput')) return;

        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });


    //обработчик кнопки esc
    document.addEventListener('keydown', (e) => {
        if (e.key !== 'Escape') return;


        //если открыты всплывающие окна то в первую очередь закрываем их
        const profilePage = document.getElementById('createWindow');
        if (profilePage && profilePage.style.display !== 'none') {
            closeCreateWindow();
            return;
        }


        //потом по приоритету отмена поиска
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


        //потом уже закрытие чата
        if (state.currentChatId) {
            if (history.state?.chatOpen) {
                history.back();
            } else {
                closeChatView();
            }
        }
    });


    //обработчик кнопки назад
    window.addEventListener('popstate', () => {
        if (state.currentChatId) {
            closeChatView();
        }
    });


    //выход из онлайна при закрытии вкладки например
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

    //проверка на то что вообще чето открыто)
    if (!state.currentChatId) return;

    //определяем заметки или прост чат с челиксами
    const card = document.querySelector(`.card[data-chat-id="${state.currentChatId}"]`);
    const isNotes = card?.dataset.chatType === 'notes';


    //спрашиваем точно ли мы хотим удалить чатикс
    const confirmMsg = isNotes
        ? t('chat.confirmClearNotes')
        : t('chat.confirmDeleteChat');

    if (!confirm(confirmMsg)) {
        return;
    }


    //отправляем запрос на сервер
    try {
        const response = await fetch(`/api/v1/chats/${state.currentChatId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Ошибка удаления');
        }


        //если заметки, то остается чат в списке чатов, но он прост без сообщений
        if (isNotes) {
            const container = document.getElementById('messagesContainer');
            if (container) {
                container.innerHTML = `<div class="no-messages">${t('chat.notesCleared')}</div>`;
                container.classList.add('empty');
            }
        } else {

            //если обычный чат, то удаляется все и закрывается чат, удаляется со списка чатов
            closeChatView();
        }

        //обновляем список чатов (который слева)
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


        clearSearch(false);  //очищаем поиск
        await refreshChatsNow(); //перезагружаем список чатов
        openChat(cardElement);  //открываем найденный чат

    } catch (error) {
        console.error(error);
        alert(t('group.errorJoin'));
    }
}

// ========================
// ИНФО О ТЕКУЩЕМ ЧАТЕ
// ========================

function openCurrentChatInfo() {

    if (!state.currentChatId) return; //опять проверка на то что существует или нет

    const card = document.querySelector(`.card[data-chat-id="${state.currentChatId}"]`);
    const chatType = card?.dataset.chatType ?? 'private';


    //если заметки то не открываем инфу о чате (че там показывать?)))
    if (chatType === 'notes') {
        return;
    }

    //ну тут открываем инфу о группе
    if (chatType === 'group') {
        openGroupInfo(state.currentChatId);
        return;
    }


    //тут открываем профиль собеседника
    const userId = card?.dataset.userId || state.currentChatUserId;
    if (userId) {
        openUserProfile(userId);
    }
}

// ========================
// ЧАТЫ: СИНХРОНИЗАЦИЯ
// ========================


//функции обновления списка чатиков(слева)

//ждем 200мс пока сообщения не прекратят поступать и обновляем если больше ничего не приходит
function refreshChatsDebounced(delay = 200) {
    if (chatsRefreshTimer) {
        clearTimeout(chatsRefreshTimer);
    }

    chatsRefreshTimer = setTimeout(() => {
        chatsRefreshTimer = null;
        refreshChatsNow();
    }, delay);
}


//защита от наложения запросов друг на друга
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

//обман браузера, чтобы при нажатии кнопки назад не выкидывало никуда
function pushChatState(chatId) {
    history.pushState(
        { chatOpen: true, chatId },
        '',
        window.location.pathname
    );
}


//сама функция закрытия чата, вызывалась выше
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

//формирование надписи в списке чатов
function getPreviewText(msg) {

    //если текст или текст + файл то выводит текст
    const text = typeof msg?.text === 'string' ? msg.text.trim() : '';
    if (text) return text;

    const type = String(msg?.type || '').toLowerCase();


    //если чисто файл то определяем что за тип файла фото видео и т.д.
    if (type === 'image' || type === 'images') return t('chat.previewPhoto');
    if (type === 'video') return t('chat.previewVideo');
    if (type === 'audio') return t('chat.previewAudio');
    if (type === 'file') {
        const fileName = msg?.attachments?.[0]?.fileName;
        return fileName ? `📎 ${fileName}` : t('chat.previewFile');
    }


    //если не получилось тип определить то смотрим имя папки в которой оно лежит
    if (Array.isArray(msg?.attachments) && msg.attachments.length > 0) {
        const attachment = msg.attachments[0];
        const mime = String(attachment?.mimeType || '').toLowerCase();

        if (mime.startsWith('image/')) return t('chat.previewPhoto');
        if (mime.startsWith('video/')) return t('chat.previewVideo');
        if (mime.startsWith('audio/')) return t('chat.previewAudio');

        return attachment?.fileName ? `📎 ${attachment.fileName}` : t('chat.previewFile');
    }


    //если непонятно что то выводим нет сообщений
    return t('chat.noMessages');
}

//перенос чата вверх при поступлении сообщения
function moveChatCard(card) {
    const container = card?.parentElement;
    if (!container) return;

    const isNotes = card.dataset.chatType === 'notes';

    //заметки всегда сверху
    if (isNotes) {
        container.prepend(card);
        return;
    }

    const notesCard = container.querySelector('.card[data-chat-type="notes"]');


    //находим карточку заметок и ставим чат под ними
    if (notesCard && notesCard !== card) {
        notesCard.insertAdjacentElement('afterend', card);
    } else {
        container.prepend(card);
    }
}

function updateChatPreview(msg) {

    //ищем карточку чата
    const card = document.querySelector(`.card[data-chat-id="${msg.chatId}"]`);
    if (!card) return false;

    const previewEl = card.querySelector('.user-message');
    const timeEl = card.querySelector('.message-time');


    //обновляем превью чата(последнее сообщение)
    if (previewEl) {
        previewEl.textContent = getPreviewText(msg);
    }


    //обновляем время когда прислали(тут функция берет время из сообщения и прост переводит в часы минуты)
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

    //счетчик непрочитаных

    if (String(state.currentChatId) !== String(msg.chatId)) { //проверям открыт чат прям щас или нет
        let badge = card.querySelector('.unread-badge');
        const previewRow = card.querySelector('.message-preview');


        //создаем индикатор непрочитаных
        if (!badge && previewRow) {
            badge = document.createElement('span');
            badge.className = 'unread-badge';
            previewRow.appendChild(badge);
        }

        //прибавляем к индикатору сообщения
        if (badge) {
            const current = Number(badge.textContent || '0');
            badge.textContent = String(current + 1);
        }
    }

    moveChatCard(card);  //отправляем вверх списка
    return true;
}

// ========================
// WS ОБРАБОТЧИК
// ========================

//обработка событий, которые присылает сервер

function onMessageReceived(frame) {
    const msg = JSON.parse(frame.body);


    //обновить статус сообщения
    if (msg.type === 'STATUS_UPDATE') {
        updateMessageStatus(msg.messageId, msg.status);
        return;
    }

    //обновить список чатов
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


    //стирает удаленное сообщение
    if (msg.type === 'MESSAGE_DELETED') {
        const msgEl = document.querySelector(`.message[data-id="${msg.messageId}"]`);
        if (msgEl) msgEl.remove();
        return;
    }

    //меняет текст сообщение которое отредачили
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


    //пришло новое сообщение мы его проверяем на дубликат
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

    //проверка чье сообщение
    msg.own = Number(msg.senderId) === Number(state.currentUser?.id);


    //тут уже добавление присланного сообщения на экран
    const isCurrentChat = String(msg.chatId) === String(state.currentChatId);


    //это проверка чтобы чат в который пришло был открыт
    if (isCurrentChat) {
        const container = document.getElementById('messagesContainer');
        if (container && !container.querySelector(`[data-id="${msg.id}"]`)) {
            appendMessage(msg);
            startReadObserver();
        }
    }


    //действия со списком чатов
    const updated = updateChatPreview(msg);
    if (!updated) {
        refreshChatsDebounced();
    }
}

// ========================
// ОТКРЫТИЕ ЧАТА
// ========================

export async function openChat(card) {

    //подсветка открытого чата
    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));
    card.classList?.add('active');


    //удаление индикатора непрочитанных
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

    //создается чат с пользователем из поиска
    if (!chatId && userId) {
        const data = await fetchOrCreateChat(userId);
        chatId = data.chatId;
        card.dataset.chatId = chatId;
    }

    if (!chatId) {
        console.error('Не удалось открыть чат: отсутствует chatId');
        return;
    }

    //запоминаем что открыт определенный чат
    state.currentChatId = chatId;
    state.currentChatUserId = userId || null;
    state.replyToId = null;
    state.editMessageId = null;


    pushChatState(chatId);

    //запрос на сервер что надо прочитать все сообщения
    fetch(`/api/v1/messages/chat/${chatId}/read`, {
        method: 'POST',
        credentials: 'include'
    }).catch(err => console.error('Ошибка отметки прочитанных:', err));


    //отрисовка чата на странице
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


    //смотрим когда пользователь был в сети
    if (!isNotes && !isGroup && userId && Number(userId) !== 0) {
        loadUserStatus(userId);
    }


    const openedChatId = String(chatId);

    //скачивает с сервера всю историю переписки
    await loadMessages(chatId);

    //защита от быстрых кликов чтобы сообщения не прогрузились в чужой чат
    if (String(state.currentChatId) !== openedChatId) {
        return;
    }

    //подключение вебсокета чтобы онлайн отрисовывались сообщения
    subscribeToChat(chatId, onMessageReceived);
}

// ========================
// СТАТУС ПОЛЬЗОВАТЕЛЯ
// ========================

async function loadUserStatus(userId) {
    try {
        //запрос информации о пользователе чтобы получить статус его
        const response = await fetch(`/api/v1/users/${userId}`, {
            credentials: 'include'
        });

        if (!response.ok) return;

        //получаем ответ
        const user = await response.json();
        const statusEl = document.getElementById('dialogStatus');

        //если есть текст в ответе то выводим, если нет то оффлайн
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

    //запоминает id сообщения на которое отвечаем
    state.replyToId = messageId;
    state.editMessageId = null;

    //рисуем его поверх окна ввода
    const label = document.querySelector('.reply-preview-label');
    if (label) label.textContent = t('chat.replyTo');

    const replyPreview = document.getElementById('replyPreview');
    const replyText = document.getElementById('replyPreviewText');

    //защита чтоб отвечать нормально на нетекстовое сообщение
    if (replyPreview && replyText) {
        const safeText = (messageText && messageText.trim())
            ? messageText
            : t('chat.messageWithoutText');

        //обрезаем длинный текст
        replyText.textContent =
            safeText.slice(0, 80) + (safeText.length > 80 ? '...' : '');

        replyPreview.style.display = 'flex';
    }

    //очищаем поле ввода
    const input = document.getElementById('messageInput');
    if (input) {
        input.value = '';
        input.focus();
    }
}

//закрывает плашку ответа на соощение
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

//функция которая решает каким способом отправлять
async function sendMessage() {
    const input = document.getElementById('messageInput');
    if (!input || !state.currentChatId) return;

    const content = input.value.trim();

    //проверка новое сообщение или редактирование
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

    //есть ли файлы
    const hasFiles = attachmentManager?.hasFiles() ?? false;

    if (!content && !hasFiles) return;

    if (hasFiles) {
        await sendMessageWithFiles(content, input);
        return;
    }


    //этим отправляем только текст
    if (isConnected()) {
        sendWsMessage(state.currentChatId, content, state.replyToId);
        input.value = '';
        input.style.height = 'auto';
        clearReply();
    } else {
        await sendMessageHttp(content, input);
    }
}

//отправка вложений
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


//отправляет обычный текст классическим способом, если современные технологии (Вебсокет) дали сбой
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