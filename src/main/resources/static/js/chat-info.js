import { escapeHtml } from './utils.js';
import { state } from './app.js';
import { loadChats } from './ui.js';
import { searchUsers as apiSearchUsers } from './api.js';

const DEFAULT_AVATAR = '/avatars/default.png';

let modalEl = null;
let currentGroupChatId = null;
let currentGroupChat = null;
let currentGroupMembers = [];
let isCurrentUserCreator = false;
let searchTimer = null;

export function initChatInfo() {
    if (modalEl) return;

    modalEl = document.createElement('div');
    modalEl.className = 'chat-info-modal';
    modalEl.style.display = 'none';

    modalEl.innerHTML = `
        <div class="chat-info-backdrop"></div>
        <div class="chat-info-window">
            <button class="chat-info-close" type="button">✕</button>
            <div class="chat-info-content" id="chatInfoContent">
                <div class="chat-info-loading">Загрузка...</div>
            </div>
        </div>
    `;

    document.body.appendChild(modalEl);

    modalEl.querySelector('.chat-info-backdrop')?.addEventListener('click', closeChatInfo);
    modalEl.querySelector('.chat-info-close')?.addEventListener('click', closeChatInfo);

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && isOpen()) {
            e.stopPropagation();
            closeChatInfo();
        }
    });
}

export function closeChatInfo() {
    if (!modalEl) return;
    modalEl.style.display = 'none';
    currentGroupChatId = null;
    currentGroupChat = null;
    currentGroupMembers = [];
    isCurrentUserCreator = false;
}

function isOpen() {
    return modalEl && modalEl.style.display !== 'none';
}

function openModal() {
    initChatInfo();
    modalEl.style.display = 'flex';
}

function setContent(html) {
    const content = document.getElementById('chatInfoContent');
    if (content) content.innerHTML = html;
}

function setLoading() {
    setContent('<div class="chat-info-loading">Загрузка...</div>');
}

function setError(message) {
    setContent(`<div class="chat-info-error">${escapeHtml(message)}</div>`);
}

// ========================
// USER PROFILE
// ========================

export async function openUserProfile(userId) {
    if (!userId) return;
    openModal();
    setLoading();

    try {
        const user = await request(`/api/v1/users/${userId}`);
        renderUserProfile(user);
    } catch (error) {
        console.error('Ошибка загрузки профиля:', error);
        setError('Не удалось загрузить профиль');
    }
}

function renderUserProfile(user) {
    const avatar = user.avatarUrl || DEFAULT_AVATAR;
    const username = user.username || 'Без имени';
    const email = user.email || '';
    const status = user.status === 'online' ? 'в сети' : (user.lastSeen || 'не в сети');

    setContent(`
        <div class="chat-info-header">
            <img class="chat-info-avatar" src="${escapeHtml(avatar)}" alt="">
            <div class="chat-info-title">
                <div class="chat-info-name">${escapeHtml(username)}</div>
                <div class="chat-info-status ${user.status === 'online' ? 'online' : ''}">
                    ${escapeHtml(status)}
                </div>
            </div>
        </div>

        <div class="chat-info-section">
            <div class="chat-info-row">
                <span class="chat-info-label">Email</span>
                <span class="chat-info-value">${escapeHtml(email)}</span>
            </div>
            <div class="chat-info-row">
                <span class="chat-info-label">ID</span>
            </div>
        </div>
    `);
}

// ========================
// GROUP INFO
// ========================

export async function openGroupInfo(chatId) {
    if (!chatId) return;
    openModal();
    setLoading();

    try {
        const [chat, members] = await Promise.all([
            request(`/api/v1/chats/${chatId}`),
            request(`/api/v1/chats/${chatId}/members`)
        ]);

        currentGroupChatId = chatId;
        currentGroupChat = chat;
        currentGroupMembers = Array.isArray(members) ? members : [];
        isCurrentUserCreator = Number(chat.creatorId) === Number(state.currentUser?.id);

        renderGroupInfo();
    } catch (error) {
        console.error('Ошибка загрузки группы:', error);
        setError('Не удалось загрузить информацию о группе');
    }
}

function renderGroupInfo() {
    const chat = currentGroupChat;
    if (!chat) return;

    const avatarHtml = chat.avatarUrl
        ? `<img class="chat-info-avatar" src="${escapeHtml(withCacheBust(chat.avatarUrl))}" alt="">`
        : `<div class="chat-info-avatar group-placeholder">👥</div>`;

    const editControls = isCurrentUserCreator ? `
        <div class="chat-info-edit-row">
            <input type="text"
                   class="chat-info-name-input"
                   id="chatInfoNameInput"
                   value="${escapeHtml(chat.name || '')}"
                   placeholder="Название беседы">
            <button class="chat-info-btn primary" id="chatInfoSaveNameBtn" type="button">
                Сохранить
            </button>
        </div>

        <div class="chat-info-edit-row">
            <label class="chat-info-btn secondary" for="chatInfoAvatarInput">
                Сменить аватар
            </label>
            <input type="file"
                   id="chatInfoAvatarInput"
                   accept="image/*"
                   style="display:none;">
        </div>
        
        <div class="chat-info-edit-row" style="margin-top: 15px;">
            <label style="display: flex; align-items: center; cursor: pointer; font-size: 14px; color: #ddd;">
                <input type="checkbox" id="chatInfoPrivacyToggle" ${chat.isPublic ? 'checked' : ''} style="margin-right: 10px; width: 16px; height: 16px;">
                Публичная группа (доступна в поиске)
            </label>
        </div>
    ` : '';

    const addMemberBlock = isCurrentUserCreator ? `
        <div class="chat-info-add-member">
            <input type="text"
                   class="chat-info-name-input"
                   id="chatInfoAddUserSearch"
                   placeholder="Поиск пользователя для добавления">
            <ul class="chat-info-search-results" id="chatInfoAddSearchResults"></ul>
        </div>
    ` : '';

    const leaveGroupBlock = !isCurrentUserCreator ? `
        <div class="chat-info-edit-row" style="margin-top: 20px; justify-content: center;">
            <button class="chat-info-btn" id="chatInfoLeaveBtn" type="button" style="background: #c62828; color: white; border: none; width: 100%;">
                Покинуть группу
            </button>
        </div>
    ` : '';

    setContent(`
        <div class="chat-info-header">
            ${avatarHtml}
            <div class="chat-info-title">
                <div class="chat-info-name">${escapeHtml(chat.name || 'Группа')}</div>
                <div class="chat-info-status">Участников: ${currentGroupMembers.length}</div>
            </div>
        </div>

        ${editControls}

        <div class="chat-info-section">
            <div class="chat-info-section-title">Участники</div>
            <ul class="chat-info-members">
                ${currentGroupMembers.map(renderMember).join('')}
            </ul>
        </div>

        ${addMemberBlock}
        ${leaveGroupBlock}
    `);

    bindGroupControls();
}

function renderMember(member) {
    const avatar = member.avatarUrl || DEFAULT_AVATAR;
    const username = member.username || 'Пользователь';
    const role = member.role || 'member';

    const isMe = Number(member.id) === Number(state.currentUser?.id);
    const roleLabel = role === 'admin' || role === 'owner' ? 'admin' : '';
    const isCreator = Number(member.id) === Number(currentGroupChat?.creatorId);

    const canRemove = isCurrentUserCreator && !isCreator && !isMe;

    return `
        <li class="chat-info-member">
            <img class="chat-info-member-avatar" src="${escapeHtml(avatar)}" alt="">
            <div class="chat-info-member-text">
                <span class="chat-info-member-name">
                    ${escapeHtml(username)}${isMe ? ' (вы)' : ''}
                </span>
                ${roleLabel
        ? `<span class="chat-info-member-role">${escapeHtml(roleLabel)}</span>`
        : ''}
            </div>
            ${canRemove
        ? `<button class="chat-info-remove-btn"
                           data-user-id="${member.id}"
                           type="button"
                           title="Удалить из беседы">✕</button>`
        : ''}
        </li>
    `;
}

// ========================
// EVENT BINDING
// ========================

function bindGroupControls() {
    const saveBtn = document.getElementById('chatInfoSaveNameBtn');
    if (saveBtn) {
        saveBtn.addEventListener('click', saveName);
    }

    const avatarInput = document.getElementById('chatInfoAvatarInput');
    if (avatarInput) {
        avatarInput.addEventListener('change', async (e) => {
            const file = e.target.files?.[0];
            if (file) {
                await uploadAvatar(file);
                e.target.value = '';
            }
        });
    }

    document.querySelectorAll('.chat-info-remove-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const userId = Number(btn.dataset.userId);
            removeMember(userId);
        });
    });

    if (isCurrentUserCreator) {
        bindAddMemberSearch();

        const privacyToggle = document.getElementById('chatInfoPrivacyToggle');
        if (privacyToggle) {
            privacyToggle.addEventListener('change', async (e) => {
                await toggleGroupPrivacy(e.target.checked);
            });
        }
    }

    const leaveBtn = document.getElementById('chatInfoLeaveBtn');
    if (leaveBtn) {
        leaveBtn.addEventListener('click', leaveGroup);
    }
}

// ========================
// ACTIONS
// ========================

async function saveName() {
    if (!currentGroupChatId) return;

    const input = document.getElementById('chatInfoNameInput');
    const name = (input?.value || '').trim();

    if (!name) {
        alert('Название не может быть пустым');
        return;
    }

    if (name === (currentGroupChat?.name || '').trim()) {
        flashSaved();
        return;
    }

    try {
        await request(`/api/v1/chats/${currentGroupChatId}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });

        currentGroupChat.name = name;
        renderGroupInfo();
        await syncOutsideModal();
        flashSaved();
    } catch (error) {
        console.error('Ошибка сохранения названия:', error);
        alert('Не удалось сохранить название');
    }
}

async function uploadAvatar(file) {
    if (!currentGroupChatId) return;

    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`/api/v1/chats/${currentGroupChatId}/avatar`, {
            method: 'POST',
            credentials: 'include',
            body: formData
        });

        if (!response.ok) throw new Error(`Ошибка ${response.status}`);

        const data = await response.json();
        currentGroupChat.avatarUrl = data.avatarUrl;
        renderGroupInfo();
        await syncOutsideModal();
    } catch (error) {
        console.error('Ошибка загрузки аватара:', error);
        alert('Не удалось загрузить аватар');
    }
}


async function toggleGroupPrivacy(isPublic) {
    if (!currentGroupChatId) return;

    try {
        await request(`/api/v1/chats/${currentGroupChatId}/privacy`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ isPublic })
        });
        currentGroupChat.isPublic = isPublic;
    } catch (error) {
        console.error('Ошибка изменения приватности:', error);
        alert('Не удалось изменить настройки приватности');
        // Если ошибка — возвращаем галочку обратно
        const toggle = document.getElementById('chatInfoPrivacyToggle');
        if (toggle) toggle.checked = !isPublic;
    }
}

async function removeMember(userId) {
    if (!currentGroupChatId || !userId) return;
    if (!confirm('Удалить пользователя из беседы?')) return;

    try {
        await request(`/api/v1/chats/${currentGroupChatId}/members/${userId}`, {
            method: 'DELETE'
        });

        currentGroupMembers = currentGroupMembers.filter(m => Number(m.id) !== Number(userId));
        renderGroupInfo();
        await syncOutsideModal();
    } catch (error) {
        console.error('Ошибка удаления участника:', error);
        alert('Не удалось удалить участника');
    }
}

async function addMember(userId) {
    if (!currentGroupChatId || !userId) return;

    try {
        await request(`/api/v1/chats/${currentGroupChatId}/members`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId })
        });

        const members = await request(`/api/v1/chats/${currentGroupChatId}/members`);
        currentGroupMembers = Array.isArray(members) ? members : [];
        renderGroupInfo();
        await syncOutsideModal();
    } catch (error) {
        console.error('Ошибка добавления участника:', error);
        alert('Не удалось добавить участника');
    }
}

async function leaveGroup() {
    if (!currentGroupChatId || !state.currentUser?.id) return;
    if (!confirm('Вы точно хотите покинуть эту группу?')) return;

    try {
        await request(`/api/v1/chats/${currentGroupChatId}/members/${state.currentUser.id}`, {
            method: 'DELETE'
        });

        closeChatInfo();
        // Принудительно закрываем чат на заднем фоне и обновляем список
        document.getElementById('mainDialog').classList.add('empty-dialog');
        document.getElementById('mainDialog').innerHTML = '<p class="main-dialog-inscription">Выберите, кому хотели бы написать</p>';
        state.currentChatId = null;

        await loadChats();
    } catch (error) {
        console.error('Ошибка выхода из группы:', error);
        alert('Не удалось покинуть группу');
    }
}

// ========================
// SYNC HEADER & CHAT LIST
// ========================

async function syncOutsideModal() {
    updateDialogHeader();
    try {
        await loadChats();
        bustAvatarCacheInChatList();
    } catch (error) {
        console.error('Ошибка обновления чатов:', error);
    }
}

function updateDialogHeader() {
    if (!currentGroupChat) return;
    if (String(state.currentChatId) !== String(currentGroupChatId)) return;

    const dialogName = document.querySelector('.dialog-header .dialog-name');
    if (dialogName) {
        dialogName.textContent = currentGroupChat.name || 'Группа';
    }

    const headerInfo = document.querySelector('.dialog-header .dialog-header-info');
    if (!headerInfo) return;

    const firstChild = headerInfo.firstElementChild;
    if (!firstChild) return;

    const avatarUrl = currentGroupChat.avatarUrl;

    if (avatarUrl) {
        const cacheBusted = withCacheBust(avatarUrl);

        if (firstChild.tagName === 'IMG') {
            firstChild.src = cacheBusted;
        } else {
            const img = document.createElement('img');
            img.className = 'avatar-img';
            img.src = cacheBusted;
            img.alt = '';
            firstChild.replaceWith(img);
        }
    } else {
        if (firstChild.tagName !== 'DIV' || !firstChild.classList.contains('notes-avatar')) {
            const placeholder = document.createElement('div');
            placeholder.className = 'notes-avatar';
            placeholder.style.flexShrink = '0';
            placeholder.textContent = '👥';
            firstChild.replaceWith(placeholder);
        }
    }
}

function bustAvatarCacheInChatList() {
    if (!currentGroupChatId) return;

    const card = document.querySelector(`.card[data-chat-id="${currentGroupChatId}"]`);
    if (!card) return;

    const img = card.querySelector('.avatar-img');
    if (img && currentGroupChat?.avatarUrl) {
        img.src = withCacheBust(currentGroupChat.avatarUrl);
    }
}

function withCacheBust(url) {
    if (!url) return url;
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}t=${Date.now()}`;
}

function flashSaved() {
    const btn = document.getElementById('chatInfoSaveNameBtn');
    if (!btn) return;
    const original = btn.textContent;
    btn.textContent = '✓ Сохранено';
    btn.disabled = true;
    setTimeout(() => {
        btn.textContent = original;
        btn.disabled = false;
    }, 1200);
}

// ========================
// SEARCH FOR ADD
// ========================

function bindAddMemberSearch() {
    const input = document.getElementById('chatInfoAddUserSearch');
    const results = document.getElementById('chatInfoAddSearchResults');
    if (!input || !results) return;

    input.addEventListener('input', () => {
        clearTimeout(searchTimer);
        const query = input.value.trim();

        if (query.length < 1) {
            results.innerHTML = '';
            return;
        }

        searchTimer = setTimeout(async () => {
            try {
                const users = await apiSearchUsers(query);

                const memberIds = new Set(currentGroupMembers.map(m => Number(m.id)));
                const myId = Number(state.currentUser?.id);

                const filtered = (Array.isArray(users) ? users : [])
                    .filter(u => !memberIds.has(Number(u.id)) && Number(u.id) !== myId);

                if (!filtered.length) {
                    results.innerHTML = '<li class="chat-info-empty">Ничего не найдено</li>';
                    return;
                }

                results.innerHTML = filtered.map(u => `
                    <li class="chat-info-search-item" data-user-id="${u.id}">
                        <img class="chat-info-member-avatar"
                             src="${escapeHtml(u.avatarUrl || DEFAULT_AVATAR)}"
                             alt="">
                        <span class="chat-info-member-name">${escapeHtml(u.username || '')}</span>
                        <button class="chat-info-btn primary chat-info-add-user-btn"
                                data-user-id="${u.id}"
                                type="button">
                            Добавить
                        </button>
                    </li>
                `).join('');

                results.querySelectorAll('.chat-info-add-user-btn').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const userId = Number(btn.dataset.userId);
                        addMember(userId);
                    });
                });
            } catch (error) {
                console.error('Ошибка поиска:', error);
                results.innerHTML = '<li class="chat-info-empty">Ошибка поиска</li>';
            }
        }, 300);
    });
}

// ========================
// HELPER
// ========================

async function request(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'include',
        ...options
    });
    if (!response.ok) {
        let msg = `Ошибка ${response.status}`;
        try {
            const text = await response.text();
            if (text) msg = text;
        } catch {}
        throw new Error(msg);
    }
    const ct = response.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
        return response.json();
    }
    return null;
}