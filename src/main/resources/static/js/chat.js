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
        ? `<div class="msg-text">${escapeHtml(text)}</div>`
        : '';

    const needsAttachmentFallback = !text && attachments.length === 0 && msg.type && msg.type !== 'text';

    const attachmentFallbackHtml = needsAttachmentFallback
        ? `<div class="msg-text">${escapeHtml(getAttachmentFallback(msg.type))}</div>`
        : '';

    const replyHtml = msg.replyToId
        ? `
            <div class="reply-snippet">
                <span class="reply-label">Ответ на сообщение</span>
            </div>
        `
        : '';

    const editedHtml = msg.isEdited
        ? `<span class="msg-edited-mark">(изм.)</span>`
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

    // Кнопки действий (редактировать, удалить)
    const actionsHtml = text ? `
        <div class="msg-actions">
            <button class="msg-action-btn edit-btn" data-id="${msg.id}" data-text="${escapeHtml(text)}" title="Редактировать">
                <svg viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
            </button>
            <button class="msg-action-btn delete-btn" data-id="${msg.id}" title="Удалить">
                <svg viewBox="0 0 24 24"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
            </button>
        </div>
    ` : `
        <div class="msg-actions">
            <button class="msg-action-btn delete-btn" data-id="${msg.id}" title="Удалить">
                <svg viewBox="0 0 24 24"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
            </button>
        </div>
    `;

    return `
        <div class="message ${isOwn ? 'message-out' : 'message-in'}"
             data-id="${msg.id}">
            <div class="message-bubble">
                ${actionsHtml}
                
                ${!isOwn && msg.senderName
        ? `<span class="message-sender">${escapeHtml(msg.senderName)}</span>`
        : ''
    }

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