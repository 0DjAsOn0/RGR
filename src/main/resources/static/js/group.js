import { state } from './app.js';
import { searchUsers } from './api.js';
import { escapeHtml } from './utils.js';
import { loadChats } from './ui.js';

// Выбранные участники
const selectedUsers = new Map(); // id -> { id, username }

export function initGroupModal() {
    const folderBtn        = document.getElementById('folderBtn');
    const modal            = document.getElementById('createGroupModal');
    const closeBtn         = document.getElementById('closeGroupModal');
    const createBtn        = document.getElementById('createGroupBtn');
    const memberSearch     = document.getElementById('groupMemberSearch');

    // Открыть модалку
    folderBtn?.addEventListener('click', () => {
        modal.style.display = 'flex';
        selectedUsers.clear();
        renderChips();
        document.getElementById('groupName').value = '';
        document.getElementById('groupSearchResults').innerHTML = '';
        memberSearch.value = '';
    });

    // Закрыть модалку
    closeBtn?.addEventListener('click', () => {
        modal.style.display = 'none';
    });

    // Закрыть по фону
    modal?.addEventListener('click', (e) => {
        if (e.target === modal) modal.style.display = 'none';
    });

    // Поиск участников
    let searchTimeout = null;
    memberSearch?.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        const query = e.target.value.trim();
        if (query.length < 2) {
            document.getElementById('groupSearchResults').innerHTML = '';
            return;
        }
        searchTimeout = setTimeout(async () => {
            const users = await searchUsers(query);
            renderGroupSearchResults(users);
        }, 400);
    });

    // Создать группу
    createBtn?.addEventListener('click', createGroup);
}

function renderGroupSearchResults(users) {
    const list = document.getElementById('groupSearchResults');

    if (users.length === 0) {
        list.innerHTML = '<li style="padding:8px;color:#888">Не найдено</li>';
        return;
    }

    list.innerHTML = users
        .filter(u => u.id !== state.currentUser?.id) // исключаем себя
        .map(u => `
            <li data-id="${u.id}" data-name="${escapeHtml(u.username)}">
                <img class="avatar-img" style="width:32px;height:32px"
                     src="${u.avatarUrl ?? '/avatars/avatar.png'}" alt="">
                <span>${escapeHtml(u.username)}</span>
            </li>
        `).join('');

    // Клик — добавить участника
    list.querySelectorAll('li').forEach(li => {
        li.addEventListener('click', () => {
            const id   = Number(li.dataset.id);
            const name = li.dataset.name;
            selectedUsers.set(id, { id, username: name });
            renderChips();
            document.getElementById('groupMemberSearch').value = '';
            list.innerHTML = '';
        });
    });
}

function renderChips() {
    const container = document.getElementById('selectedMembers');
    container.innerHTML = [...selectedUsers.values()].map(u => `
        <div class="member-chip" data-id="${u.id}">
            <span>${escapeHtml(u.username)}</span>
            <button data-id="${u.id}">✕</button>
        </div>
    `).join('');

    // Удалить участника
    container.querySelectorAll('button').forEach(btn => {
        btn.addEventListener('click', () => {
            selectedUsers.delete(Number(btn.dataset.id));
            renderChips();
        });
    });
}

async function createGroup() {
    const name = document.getElementById('groupName').value.trim();

    if (!name) {
        alert('Введите название группы');
        return;
    }

    if (selectedUsers.size === 0) {
        alert('Добавьте хотя бы одного участника');
        return;
    }

    try {
        const response = await fetch('/api/v1/chats/group', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name,
                memberIds: [...selectedUsers.keys()]
            })
        });

        if (!response.ok) throw new Error('Ошибка создания группы');

        document.getElementById('createGroupModal').style.display = 'none';
        await loadChats(); // обновляем список чатов

    } catch (error) {
        console.error('Ошибка:', error);
        alert('Не удалось создать группу');
    }
}