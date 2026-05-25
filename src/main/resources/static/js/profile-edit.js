import { escapeHtml } from './utils.js';
import { state } from './app.js';
import { closeCreateWindow, viewMyProfile } from './profile.js';
import { updateNavbar } from './user.js';
import { t } from './i18n.js'; // ✅ Добавлен импорт перевода

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
    if (!data) return t('profile.errorSave');
    if (typeof data.error === 'string') return data.error;

    const values = Object.values(data).filter(v => typeof v === 'string');
    if (values.length > 0) return values.join(', ');

    return t('profile.errorSave');
}

function validateUsername(username) {
    if (!username) return null;
    if (username.length < 3 || username.length > 30) {
        return t('profile.errUsernameLength');
    }
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return t('profile.errUsernameChars');
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
        { label: t('pass.veryWeak'), color: '#ff4444' },
        { label: t('pass.weak'),     color: '#ff8800' },
        { label: t('pass.medium'),   color: '#ffcc00' },
        { label: t('pass.good'),     color: '#88cc00' },
        { label: t('pass.excellent'),color: '#00cc44' },
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
        showError(errorEl, t('profile.errOnlyImages'));
        return;
    }

    if (file.size > 5 * 1024 * 1024) {
        showError(errorEl, t('profile.errFileSize'));
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
            throw new Error(data?.error ?? t('profile.errUpload'));
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
            showError(errorEl, t('profile.errEnterOldPass'));
            return;
        }
        if (!password) {
            showError(errorEl, t('profile.errEnterNewPass'));
            return;
        }
        if (password.length < 6) {
            showError(errorEl, t('profile.errPassMinLength'));
            return;
        }
        if (/[А-Яа-яЁё]/.test(password)) {
            showError(errorEl, t('js.errPassCyrillic'));
            return;
        }
        if (password !== passwordConfirm) {
            showError(errorEl, t('js.errPassMatch')); // Используем тот же ключ, что и при регистрации
            return;
        }
        if (password === oldPassword) {
            showError(errorEl, t('profile.errPassSame'));
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
        showError(errorEl, t('profile.errNoChanges'));
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
            saveBtn.textContent = t('js.btnSaving'); // Используем тот же ключ
        }

        // Подключаем текущий язык
        const { currentLang } = await import('./i18n.js');

        const response = await fetch('/api/v1/users/me', {
            method: 'PATCH',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept-Language': currentLang
            },
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
            saveBtn.textContent = t('app.save');
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
            <span class="edit" id="backToProfileBtn">← ${t('profile.backBtn')}</span>
            <div class="main-info-profile">
                <span class="nickname">${t('profile.editingTitle')}</span>
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
                    <span>${t('profile.changePhoto')}</span>
                </div>
                <input type="file"
                       id="avatarFileInput"
                       accept="image/*"
                       style="display:none">
            </div>

            <div class="edit-field">
                <label class="edit-label">${t('profile.username')}</label>
                <input class="edit-input"
                       id="editUsername"
                       type="text"
                       value="${escapeHtml(user?.username ?? '')}"
                       placeholder="${t('profile.newName')}">
            </div>

            <div class="edit-divider">${t('profile.changePassTitle')}</div>

            <div class="edit-field">
                <label class="edit-label">${t('profile.oldPass')}</label>
                <div class="password-input-wrap">
                    <input class="edit-input" id="editOldPassword"
                           type="password" placeholder="${t('profile.enterOldPass')}">
                    <button class="toggle-password"
                            data-target="editOldPassword" type="button">👁</button>
                </div>
            </div>

            <div class="edit-field">
                <label class="edit-label">${t('profile.newPass')}</label>
                <div class="password-input-wrap">
                    <input class="edit-input" id="editPassword"
                           type="password" placeholder="${t('profile.passMinLenPlaceholder')}">
                    <button class="toggle-password"
                            data-target="editPassword" type="button">👁</button>
                </div>
                <div class="password-strength" id="passwordStrength"></div>
            </div>

            <div class="edit-field">
                <label class="edit-label">${t('profile.confirmNewPass')}</label>
                <div class="password-input-wrap">
                    <input class="edit-input" id="editPasswordConfirm"
                           type="password" placeholder="${t('profile.repeatNewPass')}">
                    <button class="toggle-password"
                            data-target="editPasswordConfirm" type="button">👁</button>
                </div>
            </div>

            <div id="editError" class="edit-error" style="display:none"></div>
            <div id="editSuccess" class="edit-success" style="display:none">
                ${t('profile.profileUpdated')} ✓
            </div>

            <button class="edit-save-btn" id="saveProfileBtn">${t('app.save')}</button>
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