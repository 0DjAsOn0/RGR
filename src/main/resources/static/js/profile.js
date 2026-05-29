import { escapeHtml } from './utils.js';
import { state } from './app.js';
import { showEditForm } from './profile-edit.js';
import { t } from './i18n.js';

const DEFAULT_AVATAR = 'avatars/default.png';

//функция показа профиля
export function viewMyProfile() {
    let profilePage = document.getElementById('createWindow');

    //создаем страницу профиля если не нашло
    if (!profilePage) {
        profilePage = document.createElement('div');
        profilePage.id = 'createWindow';
        profilePage.className = 'profilePage';
        document.body.appendChild(profilePage);

        // Обработчик фона вешаем ОДИН раз
        profilePage.addEventListener('click', (e) => {
            if (e.target === profilePage) closeCreateWindow();
        });
    }

    const user = state.currentUser;
    const username = user?.username ?? t('app.loading');
    const email = user?.email ?? t('app.loading');
    const avatarUrl = user?.avatarUrl
        ? `${user.avatarUrl}?t=${Date.now()}`
        : DEFAULT_AVATAR;

    const status = user?.lastSeen ?? (user?.status === 'online' ? t('status.online') : t('status.offline'));
    const emailNotifications = user?.emailNotifications ?? true;

    //отрисовка
    profilePage.innerHTML = `
        <div class="profilePage-content">
            <div class="header-profile">
                <span class="edit" id="editProfileBtn">${t('app.edit')}</span>
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
                <span class="contacts-text">${t('profile.email')}</span>
                <span class="contacts-link">${escapeHtml(username)}</span>
                <span class="contacts-text">${t('profile.username')}</span>
            </div>

            <div class="email-refuse">
                <span class="refuse-agree">${t('profile.unsubscribe')}</span>
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
    profilePage.style.display = 'flex';
    document.body.style.overflow = 'hidden';

    document.getElementById('editProfileBtn')
        ?.addEventListener('click', showEditForm);

    document.getElementById('closeProfileBtn')
        ?.addEventListener('click', closeCreateWindow);

    document.getElementById('agree')
        ?.addEventListener('change', (e) => toggleEmailNotifications(e.target));
}

//закрыть окно профиля
export function closeCreateWindow() {
    const profilePage = document.getElementById('createWindow');
    if (profilePage) {
        profilePage.classList.remove('show');
        profilePage.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
}

//сохранение настройки email-уведомлений
export async function toggleEmailNotifications(checkbox) {
    const previousChecked = checkbox.checked;
    const emailNotifications = !checkbox.checked;

    try {
        // Подключаем текущий язык для отправки на сервер
        const { currentLang } = await import('./i18n.js');

        const response = await fetch('/api/v1/users/me/email-notifications', {
            method: 'PATCH',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept-Language': currentLang
            },
            body: JSON.stringify({ emailNotifications })
        });

        if (!response.ok) {
            throw new Error('Ошибка сохранения');
        }

        if (state.currentUser) {
            state.currentUser.emailNotifications = emailNotifications;
        }

    } catch (error) {
        console.error('Ошибка сохранения email-уведомлений:', error);
        checkbox.checked = previousChecked;
    }
}