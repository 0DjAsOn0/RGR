import { state } from './app.js';
import { searchUsers } from './api.js';
import { escapeHtml, collectErrorMessage } from './utils.js';
import { loadChats } from './ui.js';

const DEFAULT_AVATAR = 'avatars/default.png';

const selectedUsers = new Map(); // id -> { id, username }
let initialized = false;
let searchRequestId = 0;

// ========================
// INIT
// ========================

export function initGroupModal() {
    if (initialized) return;
    initialized = true;

    const folderBtn = document.getElementById('folderBtn');
    const modal = document.getElementById('createGroupModal');
    const closeBtn = document.getElementById('closeGroupModal');
    const createBtn = document.getElementById('createGroupBtn');
    const memberSearch = document.getElementById('groupMemberSearch');

    if (!modal) return;

    folderBtn?.addEventListener('click', () => {
        resetModal();
        modal.style.display = 'flex';
    });

    closeBtn?.addEventListener('click', () => {
        modal.style.display = 'none';
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });

    let searchTimeout = null;

    memberSearch?.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);

        const query = e.target.value.trim();
        const results = document.getElementById('groupSearchResults');

        if (!query || query.length < 2) {
            searchRequestId++;
            if (results) {
                results.innerHTML = '';
            }
            return;
        }

        searchTimeout = setTimeout(async () => {
            await handleMemberSearch(query);
        }, 400);
    });

    createBtn?.addEventListener('click', createGroup);
}

// ========================
// STATE HELPERS
// ========================

function resetModal() {
    selectedUsers.clear();
    renderChips();

    const groupName = document.getElementById('groupName');
    const memberSearch = document.getElementById('groupMemberSearch');
    const results = document.getElementById('groupSearchResults');
    const isPublicCheckbox = document.getElementById('isPublicCheckbox'); // ✅ Сброс чекбокса

    if (groupName) groupName.value = '';
    if (memberSearch) memberSearch.value = '';
    if (results) results.innerHTML = '';
    if (isPublicCheckbox) isPublicCheckbox.checked = false; // ✅ Сброс чекбокса
}

// ========================
// SEARCH
// ========================

async function handleMemberSearch(query) {
    const currentRequestId = ++searchRequestId;
    const results = document.getElementById('groupSearchResults');

    if (!results) return;

    try {
        results.innerHTML = '<li style="padding:8px;color:#888">Поиск...</li>';

        const users = await searchUsers(query);

        if (currentRequestId !== searchRequestId) {
            return;
        }

        renderGroupSearchResults(users);

    } catch (error) {
        if (currentRequestId !== searchRequestId) {
            return;
        }

        console.error('Ошибка поиска участников:', error);
        results.innerHTML = '<li style="padding:8px;color:#c62828">Ошибка поиска</li>';
    }
}

function renderGroupSearchResults(users) {
    const list = document.getElementById('groupSearchResults');
    if (!list) return;

    const filteredUsers = (users ?? []).filter(u => u.id !== state.currentUser?.id);

    if (filteredUsers.length === 0) {
        list.innerHTML = '<li style="padding:8px;color:#888">Не найдено</li>';
        return;
    }

    list.innerHTML = filteredUsers.map(u => {
        const username = u.username ?? 'Пользователь';
        const avatarUrl = u.avatarUrl ?? DEFAULT_AVATAR;

        return `
            <li data-id="${u.id}" data-name="${escapeHtml(username)}">
                <img class="avatar-img"
                     style="width:32px;height:32px"
                     src="${escapeHtml(avatarUrl)}"
                     alt="">
                <span>${escapeHtml(username)}</span>
            </li>
        `;
    }).join('');

    list.querySelectorAll('li').forEach(li => {
        li.addEventListener('click', () => {
            const id = Number(li.dataset.id);
            const user = filteredUsers.find(u => Number(u.id) === id);

            if (!user) return;

            selectedUsers.set(id, {
                id,
                username: user.username ?? 'Пользователь'
            });

            renderChips();

            const memberSearch = document.getElementById('groupMemberSearch');
            if (memberSearch) {
                memberSearch.value = '';
            }

            list.innerHTML = '';
        });
    });
}

// ========================
// CHIPS
// ========================

function renderChips() {
    const container = document.getElementById('selectedMembers');
    if (!container) return;

    container.innerHTML = [...selectedUsers.values()].map(u => `
        <div class="member-chip" data-id="${u.id}">
            <span>${escapeHtml(u.username)}</span>
            <button type="button" data-id="${u.id}">✕</button>
        </div>
    `).join('');

    container.querySelectorAll('button').forEach(btn => {
        btn.addEventListener('click', () => {
            selectedUsers.delete(Number(btn.dataset.id));
            renderChips();
        });
    });
}

// ========================
// CREATE GROUP
// ========================

async function createGroup() {
    const groupNameInput = document.getElementById('groupName');
    const modal = document.getElementById('createGroupModal');
    const createBtn = document.getElementById('createGroupBtn');
    const isPublicCheckbox = document.getElementById('isPublicCheckbox'); // ✅ Читаем чекбокс

    const name = groupNameInput?.value.trim() ?? '';
    const isPublic = isPublicCheckbox ? isPublicCheckbox.checked : false; // ✅ Статус публичности

    if (!name) {
        alert('Введите название группы');
        return;
    }

    // Если группа приватная — заставляем добавить хоть кого-то. Если публичная — можно создать пустую!
    if (selectedUsers.size === 0 && !isPublic) {
        alert('Добавьте хотя бы одного участника для приватной группы');
        return;
    }

    try {
        if (createBtn) {
            createBtn.disabled = true;
        }

        const response = await fetch('/api/v1/chats/group', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name,
                memberIds: [...selectedUsers.keys()],
                isPublic: isPublic // ✅ Отправляем флаг на сервер
            })
        });

        if (!response.ok) {
            let message = 'Ошибка создания группы';

            try {
                const data = await response.json();
                message = collectErrorMessage(data) || message;
            } catch {
                // ignore
            }

            throw new Error(message);
        }

        if (modal) {
            modal.style.display = 'none';
        }

        resetModal();
        await loadChats();

    } catch (error) {
        console.error('Ошибка создания группы:', error);
        alert(error.message ?? 'Не удалось создать группу');
    } finally {
        if (createBtn) {
            createBtn.disabled = false;
        }
    }
}