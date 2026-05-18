import { escapeHtml } from './utils.js';
import { state }      from './app.js';
import { showEditForm } from './profile-edit.js';

export function viewMyProfile() {
    let profilePage = document.getElementById('createWindow');

    if (!profilePage) {
        profilePage = document.createElement('div');
        profilePage.id        = 'createWindow';
        profilePage.className = 'profilePage';
        document.body.appendChild(profilePage);
    }

    const user               = state.currentUser;
    const username           = user?.username           ?? 'Загрузка...';
    const email              = user?.email              ?? 'Загрузка...';
    const avatarUrl          = (user?.avatarUrl ?? '/avatars/avatar.png') + '?t=' + Date.now();
    const status = state.currentUser?.status === 'online'
        ? 'в сети'
        : 'не в сети';
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

export function closeCreateWindow() {
    const profilePage = document.getElementById('createWindow');
    if (profilePage) {
        profilePage.classList.remove('show');
        profilePage.style.display    = 'none';
        document.body.style.overflow = 'auto';
    }
}

export async function toggleEmailNotifications(checkbox) {
    const emailNotifications = !checkbox.checked;
    try {
        const response = await fetch('/api/v1/users/me/email-notifications', {
            method:      'PATCH',
            credentials: 'include',
            headers:     { 'Content-Type': 'application/json' },
            body:        JSON.stringify({ emailNotifications })
        });
        if (!response.ok) throw new Error('Ошибка сохранения');
        state.currentUser.emailNotifications = emailNotifications;
    } catch (error) {
        console.error('Ошибка:', error);
        checkbox.checked = !checkbox.checked;
    }
}