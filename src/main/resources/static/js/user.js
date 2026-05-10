import { fetchCurrentUser, sendHeartbeat } from './api.js';
import { escapeHtml }                       from './utils.js';
import { state }                            from './app.js';

let heartbeatInterval = null;

// ========================
// ЗАГРУЗКА ПОЛЬЗОВАТЕЛЯ
// ========================

export async function loadCurrentUser() {
    try {
        const user = await fetchCurrentUser();
        state.currentUser = user;
        console.log('Пользователь загружен:', user);
        updateNavbar(user);
        startHeartbeat();
    } catch (error) {
        if (error.message === '401') {
            window.location.href = '/login';
        }
        console.error('Ошибка загрузки пользователя:', error);
    }
}

function updateNavbar(user) {
    const profileLabel = document.querySelector('.profile-button .base-inscription');
    if (profileLabel) profileLabel.textContent = user.username;

    const navAvatar = document.querySelector('.profile-button .avatar-img');
    if (navAvatar && user.avatarUrl) navAvatar.src = user.avatarUrl;
}

// ========================
// HEARTBEAT
// ========================

export function startHeartbeat() {
    clearInterval(heartbeatInterval);
    heartbeatInterval = setInterval(async () => {
        try {
            await sendHeartbeat();
        } catch (e) {
            console.error('Heartbeat ошибка:', e);
        }
    }, 10000);
}

export function stopHeartbeat() {
    clearInterval(heartbeatInterval);
}

// ========================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ========================

function showError(el, message) {
    if (!el) return;
    el.textContent   = message;
    el.style.display = 'block';
}

function hideError(el) {
    if (!el) return;
    el.textContent   = '';
    el.style.display = 'none';
}

function updatePasswordStrength(password) {
    const el = document.getElementById('passwordStrength');
    if (!el) return;

    if (!password) {
        el.innerHTML = '';
        return;
    }

    let strength = 0;
    if (password.length >= 6)          strength++;
    if (password.length >= 10)         strength++;
    if (/[A-Z]/.test(password))        strength++;
    if (/[0-9]/.test(password))        strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    const levels = [
        { label: 'Очень слабый', color: '#ff4444' },
        { label: 'Слабый',       color: '#ff8800' },
        { label: 'Средний',      color: '#ffcc00' },
        { label: 'Хороший',      color: '#88cc00' },
        { label: 'Отличный',     color: '#00cc44' },
    ];

    const level = levels[Math.min(strength - 1, 4)];

    el.innerHTML = `
        <div class="strength-bar">
            ${Array.from({ length: 5 }, (_, i) => `
                <div class="strength-segment ${i < strength ? 'active' : ''}"
                     style="${i < strength
        ? `background:${level.color}`
        : ''}">
                </div>
            `).join('')}
        </div>
        <span class="strength-label" style="color:${level.color}">
            ${level.label}
        </span>
    `;
}

// ========================
// АВАТАР
// ========================

async function handleAvatarChange(e) {
    const file    = e.target.files[0];
    if (!file) return;

    const preview = document.getElementById('avatarPreview');
    const errorEl = document.getElementById('editError');

    hideError(errorEl);

    if (!file.type.startsWith('image/')) {
        showError(errorEl, 'Только изображения');
        return;
    }

    if (file.size > 5 * 1024 * 1024) {
        showError(errorEl, 'Файл слишком большой (макс 5MB)');
        return;
    }

    // Превью сразу
    const reader = new FileReader();
    reader.onload = (ev) => {
        if (preview) preview.src = ev.target.result;
    };
    reader.readAsDataURL(file);

    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch('/api/v1/users/me/avatar', {
            method:      'POST',
            credentials: 'include',
            body:        formData
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error ?? 'Ошибка загрузки');
        }

        const data = await response.json();

        // Обновляем state
        state.currentUser.avatarUrl = data.avatarUrl;

        // Обновляем навбар
        const navAvatar = document.querySelector('.profile-button .avatar-img');
        if (navAvatar) navAvatar.src = data.avatarUrl;

        console.log('Аватар загружен:', data.avatarUrl);

    } catch (error) {
        console.error('Ошибка загрузки аватара:', error);
        if (errorEl) showError(errorEl, error.message);
    }
}

// ========================
// СОХРАНЕНИЕ ПРОФИЛЯ
// ========================

async function saveProfile() {
    const username        = document.getElementById('editUsername')?.value.trim();
    const oldPassword     = document.getElementById('editOldPassword')?.value;
    const password        = document.getElementById('editPassword')?.value;
    const passwordConfirm = document.getElementById('editPasswordConfirm')?.value;
    const errorEl         = document.getElementById('editError');
    const successEl       = document.getElementById('editSuccess');
    const saveBtn         = document.getElementById('saveProfileBtn');

    hideError(errorEl);
    if (successEl) successEl.style.display = 'none';

    // ========================
    // ВАЛИДАЦИЯ ПАРОЛЯ
    // ========================

    if (password || oldPassword || passwordConfirm) {

        if (!oldPassword) {
            showError(errorEl, 'Введите текущий пароль');
            return;
        }

        if (!password) {
            showError(errorEl, 'Введите новый пароль');
            return;
        }

        if (password.length < 6) {
            showError(errorEl, 'Новый пароль минимум 6 символов');
            return;
        }

        if (password !== passwordConfirm) {
            showError(errorEl, 'Пароли не совпадают');
            return;
        }

        if (password === oldPassword) {
            showError(errorEl, 'Новый пароль совпадает с текущим');
            return;
        }
    }

    // ========================
    // ФОРМИРУЕМ ТЕЛО ЗАПРОСА
    // ========================

    const body = {};

    if (username && username !== state.currentUser?.username) {
        body.username = username;
    }

    if (password && oldPassword) {
        body.oldPassword = oldPassword;
        body.password    = password;
    }

    if (Object.keys(body).length === 0) {
        showError(errorEl, 'Нет изменений');
        return;
    }

    // ========================
    // ОТПРАВКА
    // ========================

    try {
        saveBtn.disabled    = true;
        saveBtn.textContent = 'Сохранение...';

        const response = await fetch('/api/v1/users/me', {
            method:      'PATCH',
            credentials: 'include',
            headers:     { 'Content-Type': 'application/json' },
            body:        JSON.stringify(body)
        });

        // Неверный текущий пароль
        if (response.status === 400) {
            const data = await response.json();
            showError(errorEl, data.error ?? 'Неверный текущий пароль');
            return;
        }

        if (!response.ok) {
            throw new Error('Ошибка сохранения');
        }

        const updated = await response.json();

        // Обновляем state
        state.currentUser = { ...state.currentUser, ...updated };

        // Обновляем навбар
        const profileLabel = document.querySelector(
            '.profile-button .base-inscription'
        );
        if (profileLabel) profileLabel.textContent = updated.username;

        // Очищаем поля пароля
        const oldPwd = document.getElementById('editOldPassword');
        const newPwd = document.getElementById('editPassword');
        const cfmPwd = document.getElementById('editPasswordConfirm');
        if (oldPwd) oldPwd.value = '';
        if (newPwd) newPwd.value = '';
        if (cfmPwd) cfmPwd.value = '';

        if (successEl) successEl.style.display = 'block';

        // Через 1.5 сек возвращаемся к профилю
        setTimeout(() => viewMyProfile(), 1500);

    } catch (error) {
        console.error('Ошибка обновления:', error);
        showError(errorEl, error.message);
    } finally {
        saveBtn.disabled    = false;
        saveBtn.textContent = 'Сохранить';
    }
}

// ========================
// ФОРМА РЕДАКТИРОВАНИЯ
// ========================

export function showEditForm() {
    const content = document.querySelector('.profilePage-content');
    if (!content) return;

    const user      = state.currentUser;
    const avatarUrl = user?.avatarUrl ?? '/img/avatar1.JPG';

    content.innerHTML = `
        <div class="header-profile">
            <span class="edit" id="backToProfileBtn">← Назад</span>
            <div class="main-info-profile">
                <span class="nickname">Редактирование</span>
            </div>
            <span class="close" id="closeProfileBtn">&times;</span>
        </div>

        <div class="edit-form">

            <div class="avatar-upload" id="avatarUploadArea">
                <img class="avatar-img avatar-edit-preview"
                     id="avatarPreview"
                     src="${avatarUrl}"
                     alt="Аватар">
                <div class="avatar-overlay">
                    <span>Изменить фото</span>
                </div>
                <input type="file"
                       id="avatarFileInput"
                       accept="image/*"
                       style="display:none">
            </div>

            <div class="edit-field">
                <label class="edit-label">Имя пользователя</label>
                <input class="edit-input"
                       id="editUsername"
                       type="text"
                       value="${escapeHtml(user?.username ?? '')}"
                       placeholder="Новое имя">
            </div>

            <div class="edit-divider">Смена пароля</div>

            <div class="edit-field">
                <label class="edit-label">Текущий пароль</label>
                <div class="password-input-wrap">
                    <input class="edit-input"
                           id="editOldPassword"
                           type="password"
                           placeholder="Введите текущий пароль">
                    <button class="toggle-password"
                            data-target="editOldPassword"
                            type="button">👁</button>
                </div>
            </div>

            <div class="edit-field">
                <label class="edit-label">Новый пароль</label>
                <div class="password-input-wrap">
                    <input class="edit-input"
                           id="editPassword"
                           type="password"
                           placeholder="Минимум 6 символов">
                    <button class="toggle-password"
                            data-target="editPassword"
                            type="button">👁</button>
                </div>
                <div class="password-strength" id="passwordStrength"></div>
            </div>

            <div class="edit-field">
                <label class="edit-label">Подтвердите новый пароль</label>
                <div class="password-input-wrap">
                    <input class="edit-input"
                           id="editPasswordConfirm"
                           type="password"
                           placeholder="Повторите новый пароль">
                    <button class="toggle-password"
                            data-target="editPasswordConfirm"
                            type="button">👁</button>
                </div>
            </div>

            <div id="editError"
                 class="edit-error"
                 style="display:none"></div>

            <div id="editSuccess"
                 class="edit-success"
                 style="display:none">
                Профиль обновлён ✓
            </div>

            <button class="edit-save-btn" id="saveProfileBtn">
                Сохранить
            </button>
        </div>
    `;

    document.getElementById('backToProfileBtn')
        ?.addEventListener('click', viewMyProfile);

    document.getElementById('closeProfileBtn')
        ?.addEventListener('click', closeCreateWindow);

    document.getElementById('avatarUploadArea')
        ?.addEventListener('click', () => {
            document.getElementById('avatarFileInput')?.click();
        });

    document.getElementById('avatarFileInput')
        ?.addEventListener('change', handleAvatarChange);

    document.querySelectorAll('.toggle-password').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.target);
            if (!input) return;
            input.type      = input.type === 'password' ? 'text' : 'password';
            btn.textContent = input.type === 'password' ? '👁' : '🙈';
        });
    });

    document.getElementById('editPassword')
        ?.addEventListener('input', (e) => {
            updatePasswordStrength(e.target.value);
        });

    document.getElementById('saveProfileBtn')
        ?.addEventListener('click', saveProfile);
}

// ========================
// ПРОСМОТР ПРОФИЛЯ
// ========================

export function viewMyProfile() {
    let profilePage = document.getElementById('createWindow');

    if (!profilePage) {
        profilePage = document.createElement('div');
        profilePage.id = 'createWindow';
        profilePage.className = 'profilePage';
        document.body.appendChild(profilePage);
    }

    const user               = state.currentUser;
    const username           = user?.username           ?? 'Загрузка...';
    const email              = user?.email              ?? 'Загрузка...';
    const avatarUrl          = user?.avatarUrl          ?? './img/avatar1.JPG';
    const status             = user?.status             ?? 'оффлайн';
    const emailNotifications = user?.emailNotifications ?? true;

    profilePage.innerHTML = `
        <div class="profilePage-content">
            <div class="header-profile">
                <span class="edit" id="editProfileBtn">Редактировать</span>
                <div class="main-info-profile">
                    <img class="avatar-img avatar-profile"
                         id="profileAvatar"
                         src="${avatarUrl}" alt="">
                    <span class="nickname">${escapeHtml(username)}</span>
                    <span class="time-activity">${escapeHtml(status)}</span>
                </div>
                <span class="close" id="closeProfileBtn">&times;</span>
            </div>
            <div class="profile-contacts">
                <span class="contacts-link">${escapeHtml(email)}</span>
                <span class="contacts-text">Почта</span>
                <span class="contacts-link">${escapeHtml(username)}</span>
                <span class="contacts-text">Имя пользователя</span>
            </div>
            <div class="email-refuse">
                <span class="refuse-agree">Отказаться от рассылки почты</span>
                <label class="custom-checkbox">
                    <input type="checkbox"
                           id="agree"
                           name="message-refuse"
                           ${!emailNotifications ? 'checked' : ''}>
                    <span class="checkmark"></span>
                </label>
            </div>
        </div>
    `;

    profilePage.classList.add('show');
    profilePage.style.display    = 'flex';
    document.body.style.overflow = 'hidden';

    document.getElementById('editProfileBtn')
        ?.addEventListener('click', showEditForm);

    document.getElementById('closeProfileBtn')
        ?.addEventListener('click', closeCreateWindow);

    document.getElementById('agree')
        ?.addEventListener('change', (e) => toggleEmailNotifications(e.target));

    profilePage.addEventListener('click', (e) => {
        if (e.target === profilePage) closeCreateWindow();
    });
}

// ========================
// ЗАКРЫТИЕ ПРОФИЛЯ
// ========================

export function closeCreateWindow() {
    const profilePage = document.getElementById('createWindow');
    if (profilePage) {
        profilePage.classList.remove('show');
        profilePage.style.display    = 'none';
        document.body.style.overflow = 'auto';
    }
}

// ========================
// EMAIL УВЕДОМЛЕНИЯ
// ========================

export async function toggleEmailNotifications(checkbox) {
    const emailNotifications = !checkbox.checked;
    try {
        const response = await fetch('/api/v1/users/me/email-notifications', {
            method:  'PATCH',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ emailNotifications })
        });

        if (!response.ok) throw new Error('Ошибка сохранения');
        state.currentUser.emailNotifications = emailNotifications;

    } catch (error) {
        console.error('Ошибка:', error);
        checkbox.checked = !checkbox.checked;
    }
}

// ========================
// LOGOUT
// ========================

export async function logout() {
    try {
        stopHeartbeat();
        await fetch('/api/v1/auth/logout', {
            method:      'POST',
            credentials: 'include'
        });
    } catch (e) {
        console.error('Ошибка logout:', e);
    } finally {
        window.location.href = '/login';
    }
}