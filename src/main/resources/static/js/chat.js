import { fetchMessages }   from './api.js';
import { escapeHtml, formatStatus } from './utils.js';
import { subscribeToChat, sendReadReceipt } from './websocket.js';
import { state }           from './app.js';

export async function loadMessages(chatId) {
    const messages = await fetchMessages(chatId);
    renderMessages(messages);
    startReadObserver();
}

export function renderMessages(messages) {
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    if (messages.length === 0) {
        container.classList.add('empty');
        container.innerHTML = `<div class="no-messages">Начните переписку!</div>`;
        return;
    }

    container.classList.remove('empty');

    container.innerHTML = messages.map(msg => {
        const isOwn = Number(msg.senderId) === Number(state.currentUser?.id);
        return `
            <div class="message ${isOwn ? 'message-out' : 'message-in'}"
                 data-id="${msg.id}">
                <div class="message-bubble">
                    <p class="message-text">${escapeHtml(msg.text)}</p>
                    <div class="message-meta">
                        <span class="message-time-small">${msg.time ?? ''}</span>
                        ${isOwn
            ? `<span class="message-status">${formatStatus(msg.status)}</span>`
            : ''}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    container.scrollTop = container.scrollHeight;
}

export function appendMessage(msg) {
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    container.classList.remove('empty');
    container.querySelector('.no-messages')?.remove();

    if (container.querySelector(`[data-id="${msg.id}"]`)) return;

    const isOwn = Number(msg.senderId) === Number(state.currentUser?.id);

    const div = document.createElement('div');
    div.className  = `message ${isOwn ? 'message-out' : 'message-in'}`;
    div.dataset.id = msg.id;
    div.innerHTML  = `
        <div class="message-bubble">
            <p class="message-text">${escapeHtml(msg.text)}</p>
            <div class="message-meta">
                <span class="message-time-small">${msg.time ?? ''}</span>
                ${isOwn
        ? `<span class="message-status">${formatStatus(msg.status)}</span>`
        : ''}
            </div>
        </div>
    `;

    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

export function updateMessageStatus(messageId, status) {
    const msgEl = document.querySelector(`[data-id="${messageId}"]`);
    if (!msgEl) return;

    const statusEl = msgEl.querySelector('.message-status');
    if (statusEl) statusEl.textContent = formatStatus(status);
}

let readObserver = null;

export function startReadObserver() {
    if (readObserver) readObserver.disconnect();

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
    }, { root: container, threshold: 0.5 });

    container.querySelectorAll('.message-in').forEach(el => {
        readObserver.observe(el);
    });
}