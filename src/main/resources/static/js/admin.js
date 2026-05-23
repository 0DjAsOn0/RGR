const state = {
    users: [],
    stats: null
};

document.addEventListener('DOMContentLoaded', async () => {
    bindEvents();
    await loadAdminPage();
});

function bindEvents() {
    document.getElementById('refreshBtn')?.addEventListener('click', () => {
        loadAdminPage();
    });

    document.getElementById('usersTableBody')?.addEventListener('click', async (e) => {
        const blockBtn = e.target.closest('[data-action="toggle-block"]');
        if (blockBtn) {
            const userId = Number(blockBtn.dataset.userId);
            const blocked = blockBtn.dataset.blocked === 'true';
            await toggleBlock(userId, blocked);
            return;
        }

        const roleBtn = e.target.closest('[data-action="toggle-admin"]');
        if (roleBtn) {
            const userId = Number(roleBtn.dataset.userId);
            const isAdmin = roleBtn.dataset.admin === 'true';
            await toggleAdminRole(userId, isAdmin);
        }
    });
}

async function loadAdminPage() {
    hideError();
    try {
        await Promise.all([
            loadStats(),
            loadUsers()
        ]);
    } catch (error) {
        console.error('Ошибка загрузки админки:', error);
        showError(error.message || 'Не удалось загрузить админ-панель');
    }
}

async function loadStats() {
    const stats = await request('/api/v1/admin/stats');
    state.stats = stats;
    renderStats();
}

async function loadUsers() {
    const users = await request('/api/v1/admin/users');
    state.users = Array.isArray(users) ? users : [];
    renderUsers();
}

function renderStats() {
    document.getElementById('totalUsers').textContent = state.stats?.totalUsers ?? '0';
    document.getElementById('totalMessages').textContent = state.stats?.totalMessages ?? '0';
}

function renderUsers() {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    if (!state.users.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="loading-row">Пользователи не найдены</td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = state.users.map(user => {
        const roles = Array.isArray(user.roles) ? user.roles : [];
        const isAdmin = roles.includes('ROLE_ADMIN');
        const isBlocked = !!user.blocked;

        return `
            <tr>
                <td>${user.id ?? ''}</td>
                <td>${escapeHtml(user.username ?? '')}</td>
                <td>${escapeHtml(user.email ?? '')}</td>
                <td>${renderStatus(user.status)}</td>
                <td>
                    <div class="roles-cell">
                        ${roles.map(role => `<span class="role-badge">${escapeHtml(role)}</span>`).join('')}
                    </div>
                </td>
                <td>
                    <span class="${isBlocked ? 'blocked-badge' : 'active-badge'}">
                        ${isBlocked ? 'Заблокирован' : 'Активен'}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        <button
                            class="action-btn ${isBlocked ? 'success' : 'danger'}"
                            data-action="toggle-block"
                            data-user-id="${user.id}"
                            data-blocked="${isBlocked}">
                            ${isBlocked ? 'Разблокировать' : 'Заблокировать'}
                        </button>

                        <button
                            class="action-btn secondary"
                            data-action="toggle-admin"
                            data-user-id="${user.id}"
                            data-admin="${isAdmin}">
                            ${isAdmin ? 'Снять admin' : 'Сделать admin'}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function toggleBlock(userId, currentlyBlocked) {
    try {
        await request(`/api/v1/admin/users/${userId}/block`, {
            method: 'PATCH',
            body: JSON.stringify({
                blocked: !currentlyBlocked
            })
        });

        await loadUsers();
    } catch (error) {
        console.error('Ошибка блокировки:', error);
        showError(error.message || 'Не удалось изменить блокировку');
    }
}

async function toggleAdminRole(userId, isAdminNow) {
    try {
        const roles = isAdminNow ? ['ROLE_USER'] : ['ROLE_ADMIN'];

        await request(`/api/v1/admin/users/${userId}/roles`, {
            method: 'PATCH',
            body: JSON.stringify({ roles })
        });

        await loadUsers();
    } catch (error) {
        console.error('Ошибка смены роли:', error);
        showError(error.message || 'Не удалось изменить роль');
    }
}

function renderStatus(status) {
    const normalized = String(status || '').toLowerCase();

    if (normalized === 'online') {
        return '<span class="online-status">online</span>';
    }

    return '<span class="offline-status">offline</span>';
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        let message = `Ошибка ${response.status}`;
        try {
            const text = await response.text();
            if (text) message = text;
        } catch {
            // ignore
        }
        throw new Error(message);
    }

    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
        return response.json();
    }

    return null;
}

function showError(message) {
    const box = document.getElementById('adminError');
    if (!box) return;

    box.style.display = 'block';
    box.textContent = message;
}

function hideError() {
    const box = document.getElementById('adminError');
    if (!box) return;

    box.style.display = 'none';
    box.textContent = '';
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}