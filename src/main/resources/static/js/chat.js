import { fetchMessages } from './api.js';
import { escapeHtml, formatStatus } from './utils.js';
import { sendReadReceipt } from './websocket.js';
import { state } from './app.js';
import { renderAttachments } from './attachments.js';
import { t } from './i18n.js';

let readObserver = null;

// ========================
// ЗАГРУЗКА
// ========================

export async function loadMessages(chatId) {
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    try {
        const messages = await fetchMessages(chatId);
        renderMessages(messages);
        startReadObserver();
    } catch (error) {
        console.error('Ошибка загрузки сообщений:', error);
        container.classList.add('empty');
        container.innerHTML = `<div class="no-messages">${t('chat.errorLoadMessages')}</div>`;
    }
}

// ========================
// HELPERS
// ========================

function getMessageText(msg) {
    const text = msg?.text ?? msg?.content ?? '';
    return typeof text === 'string' ? text.trim() : '';
}

function getAttachmentFallback(type) {
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
            return t('chat.previewAttachment');
    }
}

// ========================
// РЕНДЕР СООБЩЕНИЯ
// ========================

// ✅ ТЕПЕРЬ ФУНКЦИЯ ПРИНИМАЕТ МАССИВ ВСЕХ СООБЩЕНИЙ ДЛЯ ПОИСКА ОРИГИНАЛА
function buildMessage(msg, allMessages = []) {
    const isOwn = Number(msg.senderId) === Number(state.currentUser?.id);
    const attachments = Array.isArray(msg.attachments) ? msg.attachments : [];
    const text = getMessageText(msg);

    const attachmentsHtml = attachments.length > 0 ? renderAttachments(attachments) : '';
    const textHtml = text ? `<div class="msg-text">${escapeHtml(text)}</div>` : '';
    const needsAttachmentFallback = !text && attachments.length === 0 && msg.type && msg.type !== 'text';
    const attachmentFallbackHtml = needsAttachmentFallback ? `<div class="msg-text">${escapeHtml(getAttachmentFallback(msg.type))}</div>` : '';

    // ==========================================
    // ✅ УМНЫЙ БЛОК ОТВЕТА (ИЩЕТ ОРИГИНАЛЬНЫЙ ТЕКСТ)
    // ==========================================
    let replyHtml = '';
    if (msg.replyToId) {
        let replySender = '';
        let replyText = t('chat.replyToMessage'); // Дефолтный текст

        // 1. Пытаемся найти оригинальное сообщение в массиве (при загрузке истории)
        const parentMsg = allMessages.find(m => String(m.id) === String(msg.replyToId));

        if (parentMsg) {
            replySender = parentMsg.senderName || '';
            const pText = getMessageText(parentMsg);

            if (pText) {
                replyText = pText; // Если есть текст - берем его
            } else if (parentMsg.type && parentMsg.type !== 'text') {
                replyText = getAttachmentFallback(parentMsg.type); // Если файл - берем тип (Фото/Видео)
            } else if (parentMsg.attachments && parentMsg.attachments.length > 0) {
                replyText = t('chat.previewAttachment');
            }
        } else {
            // 2. Если в массиве нет, ищем прямо в HTML (для новых сообщений по WebSockets)
            const parentEl = document.querySelector(`.message[data-id="${msg.replyToId}"]`);
            if (parentEl) {
                const senderEl = parentEl.querySelector('.message-sender');
                if (senderEl) replySender = senderEl.textContent;

                const textEl = parentEl.querySelector('.msg-text');
                if (textEl) replyText = textEl.textContent;
            }
        }

        // Если это свои заметки, не пишем имя отправителя
        const isNotes = document.getElementById('dialogStatus')?.textContent.toLowerCase().includes(t('chat.notes').toLowerCase());
        const senderLabel = (!replySender || isNotes) ? t('chat.replyToMessage') : replySender;

        // Генерируем HTML ответа с функцией прокрутки (scrollIntoView)
        replyHtml = `
            <div class="reply-snippet" onclick="document.querySelector('.message[data-id=\\'${msg.replyToId}\\']')?.scrollIntoView({behavior: 'smooth', block: 'center'})">
                <span class="reply-label">
                    <svg viewBox="0 0 24 24" width="12" height="12" style="margin-right: 4px; vertical-align: -1px;"><path fill="currentColor" d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/></svg>
                    ${escapeHtml(senderLabel)}
                </span>
                <span class="reply-text">${escapeHtml(replyText)}</span>
            </div>
        `;
    }

    const editedHtml = msg.isEdited ? `<span class="msg-edited-mark">(${t('chat.editedShort')})</span>` : '';

    const hasVisibleContent = Boolean(replyHtml || textHtml || attachmentsHtml || attachmentFallbackHtml);
    if (!hasVisibleContent) return '';

    // ==========================================
    // БЛОК КНОПОК ДЕЙСТВИЙ
    // ==========================================
    let actionsHtml = `<div class="msg-actions">`;
    const replyDataText = text || getAttachmentFallback(msg.type);
    actionsHtml += `
        <button class="msg-action-btn reply-btn" data-id="${msg.id}" data-text="${escapeHtml(replyDataText)}" title="${t('chat.replyBtn') || 'Ответить'}">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/></svg>
        </button>
    `;

    if (isOwn) {
        if (text) {
            actionsHtml += `
                <button class="msg-action-btn edit-btn" data-id="${msg.id}" data-text="${escapeHtml(text)}" title="${t('app.edit')}">
                    <svg viewBox="0 0 24 24"><path fill="currentColor" d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                </button>
            `;
        }
        actionsHtml += `
            <button class="msg-action-btn delete-btn" data-id="${msg.id}" title="${t('app.delete')}">
                <svg viewBox="0 0 24 24"><path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
            </button>
        `;
    }
    actionsHtml += `</div>`;

    return `
        <div class="message ${isOwn ? 'message-out' : 'message-in'}" data-id="${msg.id}">
            <div class="message-bubble">
                ${actionsHtml}
                
                ${!isOwn && msg.senderName ? `<span class="message-sender">${escapeHtml(msg.senderName)}</span>` : ''}
                
                ${replyHtml}
                ${attachmentsHtml}
                ${textHtml}
                ${attachmentFallbackHtml}

                <span class="message-meta">
                    ${editedHtml}
                    <span class="message-time-small">${msg.time ?? ''}</span>
                    ${isOwn ? `<span class="message-status status-icon ${msg.status === 'READ' ? 'read' : ''}">${formatStatus(msg.status)}</span>` : ''}
                </span>
            </div>
        </div>
    `;
}

// ========================
// РЕНДЕР СПИСКА
// ========================

export function renderMessages(messages) {
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    // ✅ ПЕРЕДАЕМ ВЕСЬ МАССИВ messages ВНУТРЬ buildMessage
    const html = (messages ?? [])
        .map(msg => buildMessage(msg, messages))
        .filter(Boolean)
        .join('');

    if (!html) {
        container.classList.add('empty');
        const dialogStatus = document.getElementById('dialogStatus');
        const isNotes = dialogStatus && dialogStatus.textContent.toLowerCase().includes(t('chat.notes').toLowerCase());
        const emptyText = isNotes ? t('chat.noNotesYet') : t('chat.start');
        container.innerHTML = `<div class="no-messages">${emptyText}</div>`;
        return;
    }

    container.classList.remove('empty');
    container.innerHTML = html;
    container.scrollTop = container.scrollHeight;
}

// ========================
// ДОБАВИТЬ СООБЩЕНИЕ
// ========================

export function appendMessage(msg) {
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    container.classList.remove('empty');
    container.querySelector('.no-messages')?.remove();

    if (container.querySelector(`[data-id="${msg.id}"]`)) return;

    // Для одиночного сообщения передаем пустой массив (функция найдет оригинал в HTML)
    const html = buildMessage(msg, []);
    if (!html) return;

    container.insertAdjacentHTML('beforeend', html);
    container.scrollTop = container.scrollHeight;
}

// ========================
// ОБНОВИТЬ СТАТУС
// ========================

export function updateMessageStatus(messageId, status) {
    const msgEl = document.querySelector(`[data-id="${messageId}"]`);
    if (!msgEl) return;

    const statusEl = msgEl.querySelector('.message-status');
    if (statusEl) {
        statusEl.innerHTML = formatStatus(status);
        if (status === 'READ') {
            statusEl.classList.add('read');
        }
    }
}

// ========================
// READ OBSERVER
// ========================

export function startReadObserver() {
    if (readObserver) {
        readObserver.disconnect();
    }

    const container = document.getElementById('messagesContainer');
    if (!container) return;

    readObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) return;

            const msgEl = entry.target;
            const isOwn = msgEl.classList.contains('message-out');

            if (!isOwn && msgEl.dataset.read !== 'true') {
                msgEl.dataset.read = 'true';
                readObserver.unobserve(msgEl);
                sendReadReceipt(msgEl.dataset.id, state.currentChatId);
            }
        });
    }, {
        root: container,
        threshold: 0.5
    });

    container.querySelectorAll('.message-in').forEach(el => {
        readObserver.observe(el);
    });
}