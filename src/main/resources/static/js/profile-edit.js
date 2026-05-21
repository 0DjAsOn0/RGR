import { escapeHtml } from './utils.js';
import { state } from './app.js';
import { closeCreateWindow, viewMyProfile } from './profile.js';
import { updateNavbar } from './user.js';

const DEFAULT_AVATAR = 'avatars/default.png';
let avatarChanged = false;

// ========================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ========================

function showError(el, message) {
    if (!el) return;
    el.textContent = message;
    el.style.display = 'block';
}

function hideError(el) {
    if (!el) return;
    el.textContent = '';
    el.style.display = 'none';
}

function collectErrorMessage(data) {
    if (!data) return 'Ошибка сохранения';
    if (typeof data.error === 'string') return data.error;

    const values = Object.values(data).filter(v => typeof v === 'string');
    if (values.length > 0) return values.join(', ');

    return 'Ошибка сохранения';
}

function validateUsername(username) {
    if (!username) return null;
    if (username.length < 3 || username.length > 30) {
        return 'Имя пользователя должно быть от 3 до 30 символов';
    }
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return 'Имя пользователя может содержать только латинские буквы, цифры и _';
    }
    return null;
}

function updatePasswordStrength(password) {
    const el = document.getElementById('passwordStrength');
    if (!el) return;

    if (!password) {
        el.innerHTML = '';
        return;
    }

    let strength = 0;
    if (password.length >= 6) strength++;
    if (password.length >= 10) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    const levels = [
        { label: 'Очень слабый', color: '#ff4444' },
        { label: 'Слабый',       color: '#ff8800' },
        { label: 'Средний',      color: '#ffcc00' },
        { label: 'Хороший',      color: '#88cc00' },
        { label: 'Отличный',     color: '#00cc44' },
    ];

    const level = levels[Math.min(Math.max(strength - 1, 0), 4)];

    el.innerHTML = `
        <div class="strength-bar">
            ${Array.from({ length: 5 }, (_, i) => `
                <div class="strength-segment ${i < strength ? 'active' : ''}"
                     style="${i < strength ? `background:${level.color}` : ''}">
                </div>
            `).join('')}
        </div>
        <span class="strength-label" style="color:${level.color}">
            ${level.label}
        </span>
    `;
}

// ========================
// ЗАГРУЗКА АВАТАРА
// ========================

async function handleAvatarChange(e) {
    const file = e.target.files[0];
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
            method: 'POST',
            credentials: 'include',
            body: formData
        });

        let data = null;
        try {
            data = await response.json();
        } catch (_) {}

        if (!response.ok) {
            throw new Error(data?.error ?? 'Ошибка загрузки');
        }

        const url = `${data.avatarUrl}?t=${Date.now()}`;

        state.currentUser.avatarUrl = data.avatarUrl;
        avatarChanged = true;

        if (preview) preview.src = url;
        updateNavbar(state.currentUser);

        const profileAvatar = document.getElementById('profileAvatar');
        if (profileAvatar) profileAvatar.src = url;

    } catch (error) {
        console.error('Ошибка загрузки аватара:', error);
        showError(errorEl, error.message);
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

    // Валидация имени
    if (username && username !== state.currentUser?.username) {
        const usernameError = validateUsername(username);
        if (usernameError) {
            showError(errorEl, usernameError);
            return;
        }
    }

    // Валидация пароля
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

    const body = {};

    if (username && username !== state.currentUser?.username) {
        body.username = username;
    }

    if (password && oldPassword) {
        body.oldPassword = oldPassword;
        body.password = password;
    }

    if (Object.keys(body).length === 0 && !avatarChanged) {
        showError(errorEl, 'Нет изменений');
        return;
    }

    if (Object.keys(body).length === 0 && avatarChanged) {
        avatarChanged = false;
        if (successEl) successEl.style.display = 'block';
        setTimeout(viewMyProfile, 1500);
        return;
    }

    try {
        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.textContent = 'Сохранение...';
        }

        const response = await fetch('/api/v1/users/me', {
            method: 'PATCH',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        let data = null;
        try {
            data = await response.json();
        } catch (_) {}

        if (!response.ok) {
            showError(errorEl, collectErrorMessage(data));
            return;
        }

        state.currentUser = { ...state.currentUser, ...data };
        avatarChanged = false;

        updateNavbar(state.currentUser);

        ['editOldPassword', 'editPassword', 'editPasswordConfirm'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });

        if (successEl) successEl.style.display = 'block';
        setTimeout(viewMyProfile, 1500);

    } catch (error) {
        console.error('Ошибка обновления:', error);
        showError(errorEl, error.message);
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.textContent = 'Сохранить';
        }
    }
}

// ========================
// ФОРМА РЕДАКТИРОВАНИЯ
// ========================

export function showEditForm() {
    avatarChanged = false;

    const content = document.querySelector('.profilePage-content');
    if (!content) return;

    const user = state.currentUser;
    const avatarUrl = user?.avatarUrl
        ? `${user.avatarUrl}?t=${Date.now()}`
        : DEFAULT_AVATAR;

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
                    <input class="edit-input" id="editOldPassword"
                           type="password" placeholder="Введите текущий пароль">
                    <button class="toggle-password"
                            data-target="editOldPassword" type="button">👁</button>
                </div>
            </div>

            <div class="edit-field">
                <label class="edit-label">Новый пароль</label>
                <div class="password-input-wrap">
                    <input class="edit-input" id="editPassword"
                           type="password" placeholder="Минимум 6 символов">
                    <button class="toggle-password"
                            data-target="editPassword" type="button">👁</button>
                </div>
                <div class="password-strength" id="passwordStrength"></div>
            </div>

            <div class="edit-field">
                <label class="edit-label">Подтвердите новый пароль</label>
                <div class="password-input-wrap">
                    <input class="edit-input" id="editPasswordConfirm"
                           type="password" placeholder="Повторите новый пароль">
                    <button class="toggle-password"
                            data-target="editPasswordConfirm" type="button">👁</button>
                </div>
            </div>

            <div id="editError" class="edit-error" style="display:none"></div>
            <div id="editSuccess" class="edit-success" style="display:none">
                Профиль обновлён ✓
            </div>

            <button class="edit-save-btn" id="saveProfileBtn">Сохранить</button>
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
            input.type = input.type === 'password' ? 'text' : 'password';
            btn.textContent = input.type === 'password' ? '👁' : '🙈';
        });
    });

    document.getElementById('editPassword')
        ?.addEventListener('input', (e) => updatePasswordStrength(e.target.value));

    document.getElementById('saveProfileBtn')
        ?.addEventListener('click', saveProfile);
}