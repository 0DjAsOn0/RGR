import { t, translateDOM } from './i18n.js';

const state = {
    users: [],
    stats: null
};

document.addEventListener('DOMContentLoaded', async () => {
    translateDOM();
    bindEvents();
    await loadAdminPage();
});

function bindEvents() {
    //кнопка обновления
    document.getElementById('refreshBtn')?.addEventListener('click', () => {
        loadAdminPage();
    });

    //Обработчик кликов по таблице пользователей
    document.getElementById('usersTableBody')?.addEventListener('click', async (e) => {

        //кнопка блокировать
        const blockBtn = e.target.closest('[data-action="toggle-block"]');
        if (blockBtn) {
            const userId = Number(blockBtn.dataset.userId);
            const blocked = blockBtn.dataset.blocked === 'true';
            await toggleBlock(userId, blocked);
            return;
        }

        //кнопка назначить администратора
        const roleBtn = e.target.closest('[data-action="toggle-admin"]');
        if (roleBtn) {
            const userId = Number(roleBtn.dataset.userId);
            const isAdmin = roleBtn.dataset.admin === 'true';
            await toggleAdminRole(userId, isAdmin);
        }
    });
}

//основная загрузка страницы
async function loadAdminPage() {
    hideError();
    try {
        await Promise.all([
            loadStats(),
            loadUsers()
        ]);
    } catch (error) {
        console.error('Ошибка загрузки админки:', error);
        showError(error.message || t('admin.errorLoadPanel'));
    }
}

//статистика
async function loadStats() {
    const stats = await request('/api/v1/admin/stats');
    state.stats = stats;
    renderStats();
}

//загрузка пользователей
async function loadUsers() {
    const users = await request('/api/v1/admin/users');
    state.users = Array.isArray(users) ? users : [];
    renderUsers();
}

//отрисовка статитсткик
function renderStats() {
    document.getElementById('totalUsers').textContent = state.stats?.totalUsers ?? '0';
    document.getElementById('totalMessages').textContent = state.stats?.totalMessages ?? '0';
}

//отрисовка пользователей
function renderUsers() {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    if (!state.users.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="loading-row">${t('admin.noUsersFound')}</td>
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
                        ${isBlocked ? t('admin.statusBlocked') : t('admin.statusActive')}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        <button
                            class="action-btn ${isBlocked ? 'success' : 'danger'}"
                            data-action="toggle-block"
                            data-user-id="${user.id}"
                            data-blocked="${isBlocked}">
                            ${isBlocked ? t('admin.actionUnblock') : t('admin.actionBlock')}
                        </button>

                        <button
                            class="action-btn secondary"
                            data-action="toggle-admin"
                            data-user-id="${user.id}"
                            data-admin="${isAdmin}">
                            ${isAdmin ? t('admin.actionRemoveAdmin') : t('admin.actionMakeAdmin')}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

//логика блокировки или разблокировки пользователя
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
        showError(error.message || t('admin.errorBlock'));
    }
}

//логика назначения админом пользователя
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
        showError(error.message || t('admin.errorRole'));
    }
}

// показывает online/offline в таблице
function renderStatus(status) {
    const normalized = String(status || '').toLowerCase();

    if (normalized === 'online') {
        return `<span class="online-status">${t('status.online')}</span>`;
    }

    return `<span class="offline-status">${t('status.offline')}</span>`;
}

//функция для запросов
async function request(url, options = {}) {
    // Импортируем текущий язык, чтобы сервер понимал, какие ошибки отдавать (задел на будущее)
    const { currentLang } = await import('./i18n.js');

    const response = await fetch(url, {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            'Accept-Language': currentLang,
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        let message = `${t('admin.errorPrefix')} ${response.status}`;
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

//показ ошибки
function showError(message) {
    const box = document.getElementById('adminError');
    if (!box) return;

    box.style.display = 'block';
    box.textContent = message;
}

//скрытие ошибки
function hideError() {
    const box = document.getElementById('adminError');
    if (!box) return;

    box.style.display = 'none';
    box.textContent = '';
}

//защита от XSS
function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}