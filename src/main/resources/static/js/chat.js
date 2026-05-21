import { fetchMessages } from './api.js';
import { escapeHtml, formatStatus } from './utils.js';
import { sendReadReceipt } from './websocket.js';
import { state } from './app.js';
import { renderAttachments } from './attachments.js';

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
        container.innerHTML = `<div class="no-messages">Ошибка загрузки сообщений</div>`;
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
            return '🖼 Изображение';
        case 'video':
            return '🎥 Видео';
        case 'audio':
            return '🎵 Аудио';
        case 'file':
            return '📎 Файл';
        default:
            return '📎 Вложение';
    }
}

// ========================
// РЕНДЕР СООБЩЕНИЯ
// ========================

function buildMessage(msg) {
    const isOwn = Number(msg.senderId) === Number(state.currentUser?.id);
    const attachments = Array.isArray(msg.attachments) ? msg.attachments : [];
    const text = getMessageText(msg);

    const attachmentsHtml = attachments.length > 0
        ? renderAttachments(attachments)
        : '';

    const textHtml = text
        ? `<span class="message-text">${escapeHtml(text)}</span>`
        : '';

    const needsAttachmentFallback = !text && attachments.length === 0 && msg.type && msg.type !== 'text';

    const attachmentFallbackHtml = needsAttachmentFallback
        ? `<span class="message-text">${escapeHtml(getAttachmentFallback(msg.type))}</span>`
        : '';

    const replyHtml = msg.replyToId
        ? `
            <div class="reply-snippet">
                <span class="reply-label">Ответ на сообщение</span>
            </div>
        `
        : '';

    const editedHtml = msg.isEdited
        ? `<span class="message-edited">(изменено)</span>`
        : '';

    const hasVisibleContent = Boolean(
        replyHtml ||
        textHtml ||
        attachmentsHtml ||
        attachmentFallbackHtml
    );

    if (!hasVisibleContent) {
        return '';
    }

    return `
        <div class="message ${isOwn ? 'message-out' : 'message-in'}"
             data-id="${msg.id}">
            <div class="message-bubble">
                ${!isOwn && msg.senderName
        ? `<span class="message-sender">${escapeHtml(msg.senderName)}</span>`
        : ''
    }

                ${replyHtml}
                ${textHtml}
                ${attachmentFallbackHtml}
                ${attachmentsHtml}

                <span class="message-meta">
                    ${editedHtml}
                    <span class="message-time-small">${msg.time ?? ''}</span>
                    ${isOwn
        ? `<span class="message-status">${formatStatus(msg.status)}</span>`
        : ''
    }
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

    const html = (messages ?? [])
        .map(buildMessage)
        .filter(Boolean)
        .join('');

    if (!html) {
        container.classList.add('empty');
        container.innerHTML = `<div class="no-messages">Начните переписку!</div>`;
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

    const html = buildMessage(msg);
    if (!html) {
        console.warn('Сообщение не содержит отображаемого контента:', msg);
        return;
    }

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