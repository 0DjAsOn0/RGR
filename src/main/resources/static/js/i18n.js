const translations = {
    ru: {
        "app.title": "Мессенджер",
        "app.loading": "Загрузка...",
        "app.cancel": "Отмена",
        "app.save": "Сохранить",
        "app.delete": "Удалить",
        "app.edit": "Редактировать",

        "nav.profile": "Профиль",
        "nav.createChat": "Создать чат",

        "search.placeholder": "Поиск",
        "search.searching": "Поиск...",
        "search.error": "Ошибка при поиске",
        "search.users": "Пользователи",
        "search.publicGroups": "Публичные группы",
        "search.notFound": "Ничего не найдено",
        "search.clickToWrite": "Нажмите, чтобы написать",
        "search.clickToJoin": "Нажмите, чтобы вступить",

        "chat.emptyState": "Выберите, кому хотели бы написать",
        "chat.noChats": "Нет чатов",
        "chat.noMessages": "Нет сообщений",
        "chat.start": "Начните переписку!",
        "chat.noNotesYet": "Здесь пока нет заметок",
        "chat.errorLoadMessages": "Ошибка загрузки сообщений",
        "chat.replyToMessage": "Ответ на сообщение",
        "chat.confirmDeleteMessage": "Точно удалить сообщение?",
        "chat.errorDeleteMessage": "Нет прав для удаления",
        "chat.editing": "Редактирование:",
        "chat.replyTo": "Ответ на:",
        "chat.confirmClearNotes": "Вы уверены, что хотите очистить все заметки? Восстановить их будет невозможно.",
        "chat.confirmDeleteChat": "Вы уверены, что хотите удалить этот чат? Это действие необратимо.",
        "chat.notesCleared": "Заметки очищены",
        "chat.errorDeleteChat": "Нет прав для удаления этого чата или произошла ошибка.",
        "chat.notes": "Заметки",
        "chat.personalNotes": "Личные заметки",
        "chat.group": "Группа",
        "chat.defaultChat": "Чат",
        "chat.clearNotes": "Очистить заметки",
        "chat.deleteChat": "Удалить чат",
        "chat.attachFile": "Прикрепить файл",
        "chat.attachment": "вложение",
        "chat.writeNote": "Написать заметку...",
        "chat.writeMessage": "Написать сообщение...",
        "chat.messageWithoutText": "Сообщение без текста",
        "chat.errorEditMessage": "Ошибка при редактировании сообщения",
        "chat.errorUpload": "Ошибка загрузки: ",
        "chat.errorSend": "Ошибка отправки",
        "chat.defaultInterlocutor": "Собеседник",
        "chat.editedShort": "изм.",
        "chat.previewPhoto": "🖼 Фото",
        "chat.previewVideo": "🎥 Видео",
        "chat.previewAudio": "🎵 Аудио",
        "chat.previewFile": "📎 Файл",
        "chat.previewAttachment": "📎 Вложение",

        "group.createTitle": "Создать группу",
        "group.namePlaceholder": "Название группы",
        "group.makePublic": "Сделать группу публичной (доступна в поиске)",
        "group.addMembersPlaceholder": "Добавить участников",
        "group.createBtn": "Создать",
        "group.confirmJoin": "Вы хотите вступить в группу",
        "group.errorJoin": "Произошла ошибка при попытке вступить в группу.",

        "status.offline": "не в сети",
        "status.online": "в сети",

        // АДМИН-ПАНЕЛЬ
        "admin.title": "Админ-панель",
        "admin.header": "Админ-панель",
        "admin.subtitle": "Управление пользователями и статистика",
        "admin.backToChat": "← Назад в чат",
        "admin.statUsers": "Пользователей зарегистрировано",
        "admin.statMessages": "Сообщений отправлено",
        "admin.usersList": "Пользователи",
        "admin.refreshBtn": "Обновить",
        "admin.thId": "ID",
        "admin.thName": "Имя",
        "admin.thEmail": "Email",
        "admin.thStatus": "Статус",
        "admin.thRoles": "Роли",
        "admin.thBlock": "Блокировка",
        "admin.thActions": "Действия",
        "admin.errorLoadPanel": "Не удалось загрузить админ-панель",
        "admin.noUsersFound": "Пользователи не найдены",
        "admin.statusBlocked": "Заблокирован",
        "admin.statusActive": "Активен",
        "admin.actionUnblock": "Разблокировать",
        "admin.actionBlock": "Заблокировать",
        "admin.actionMakeAdmin": "Сделать admin",
        "admin.actionRemoveAdmin": "Снять admin",
        "admin.errorBlock": "Не удалось изменить блокировку",
        "admin.errorRole": "Не удалось изменить роль",
        "admin.errorPrefix": "Ошибка HTTP",
        "group.defaultUser": "Пользователь",
        "group.enterNameAlert": "Введите название группы",
        "group.addMembersAlert": "Добавьте хотя бы одного участника для приватной группы",
        "group.createError": "Ошибка создания группы",
        "group.createFailed": "Не удалось создать группу",

        "api.error": "Ошибка",

        "profile.email": "Почта",
        "profile.username": "Имя пользователя",
        "profile.unsubscribe": "Отказаться от рассылки почты",

        "attach.maxFilesAlert": (max) => `Максимум ${max} файлов`,
        "attach.tooBigAlert": (name, max) => `Файл "${name}" слишком большой (макс. ${max}MB)`,
        "attach.uploadError": "Ошибка загрузки файлов",
        "attach.defaultFileName": "файл",
        "attach.noVideoSupport": "Ваш браузер не поддерживает видео",

        "pass.veryWeak": "Очень слабый",
        "pass.weak": "Слабый",
        "pass.medium": "Средний",
        "pass.good": "Хороший",
        "pass.excellent": "Отличный",

        "profile.errorSave": "Ошибка сохранения",
        "profile.errUsernameLength": "Имя пользователя должно быть от 3 до 30 символов",
        "profile.errUsernameChars": "Имя пользователя может содержать только латинские буквы, цифры и _",
        "profile.errOnlyImages": "Только изображения",
        "profile.errFileSize": "Файл слишком большой (макс 5MB)",
        "profile.errUpload": "Ошибка загрузки",
        "profile.errEnterOldPass": "Введите текущий пароль",
        "profile.errEnterNewPass": "Введите новый пароль",
        "profile.errPassMinLength": "Новый пароль минимум 6 символов",
        "profile.errPassSame": "Новый пароль совпадает с текущим",
        "profile.errNoChanges": "Нет изменений",
        "profile.backBtn": "Назад",
        "profile.editingTitle": "Редактирование",
        "profile.changePhoto": "Изменить фото",
        "profile.newName": "Новое имя",
        "profile.changePassTitle": "Смена пароля",
        "profile.oldPass": "Текущий пароль",
        "profile.enterOldPass": "Введите текущий пароль",
        "profile.newPass": "Новый пароль",
        "profile.passMinLenPlaceholder": "Минимум 6 символов",
        "profile.confirmNewPass": "Подтвердите новый пароль",
        "profile.repeatNewPass": "Повторите новый пароль",
        "profile.profileUpdated": "Профиль обновлён"
    },
    en: {
        "app.title": "Messenger",
        "app.loading": "Loading...",
        "app.cancel": "Cancel",
        "app.save": "Save",
        "app.delete": "Delete",
        "app.edit": "Edit",

        "nav.profile": "Profile",
        "nav.createChat": "Create Chat",

        "search.placeholder": "Search",
        "search.searching": "Searching...",
        "search.error": "Search error",
        "search.users": "Users",
        "search.publicGroups": "Public Groups",
        "search.notFound": "Nothing found",
        "search.clickToWrite": "Click to write",
        "search.clickToJoin": "Click to join",

        "chat.emptyState": "Select who you would like to write to",
        "chat.noChats": "No chats",
        "chat.noMessages": "No messages",
        "chat.start": "Start a conversation!",
        "chat.noNotesYet": "No notes yet",
        "chat.errorLoadMessages": "Error loading messages",
        "chat.replyToMessage": "Reply to message",
        "chat.confirmDeleteMessage": "Delete message?",
        "chat.errorDeleteMessage": "No permission to delete",
        "chat.editing": "Editing:",
        "chat.replyTo": "Reply to:",
        "chat.confirmClearNotes": "Are you sure you want to clear all notes? This cannot be undone.",
        "chat.confirmDeleteChat": "Are you sure you want to delete this chat? This action is irreversible.",
        "chat.notesCleared": "Notes cleared",
        "chat.errorDeleteChat": "Error or no permission to delete chat.",
        "chat.notes": "Notes",
        "chat.personalNotes": "Personal notes",
        "chat.group": "Group",
        "chat.defaultChat": "Chat",
        "chat.clearNotes": "Clear notes",
        "chat.deleteChat": "Delete chat",
        "chat.attachFile": "Attach file",
        "chat.attachment": "attachment",
        "chat.writeNote": "Write a note...",
        "chat.writeMessage": "Write a message...",
        "chat.messageWithoutText": "Message without text",
        "chat.errorEditMessage": "Error editing message",
        "chat.errorUpload": "Upload error: ",
        "chat.errorSend": "Send error",
        "chat.defaultInterlocutor": "Interlocutor",
        "chat.editedShort": "edited",
        "chat.previewPhoto": "🖼 Photo",
        "chat.previewVideo": "🎥 Video",
        "chat.previewAudio": "🎵 Audio",
        "chat.previewFile": "📎 File",
        "chat.previewAttachment": "📎 Attachment",

        "group.createTitle": "Create Group",
        "group.namePlaceholder": "Group Name",
        "group.makePublic": "Make group public (available in search)",
        "group.addMembersPlaceholder": "Add members",
        "group.createBtn": "Create",
        "group.confirmJoin": "Do you want to join the group",
        "group.errorJoin": "Error joining the group.",

        "status.offline": "offline",
        "status.online": "online",

        // АДМИН-ПАНЕЛЬ
        "admin.title": "Admin Panel",
        "admin.header": "Admin Panel",
        "admin.subtitle": "User management and statistics",
        "admin.backToChat": "← Back to chat",
        "admin.statUsers": "Registered users",
        "admin.statMessages": "Messages sent",
        "admin.usersList": "Users",
        "admin.refreshBtn": "Refresh",
        "admin.thId": "ID",
        "admin.thName": "Name",
        "admin.thEmail": "Email",
        "admin.thStatus": "Status",
        "admin.thRoles": "Roles",
        "admin.thBlock": "Ban Status",
        "admin.thActions": "Actions",
        "admin.errorLoadPanel": "Failed to load admin panel",
        "admin.noUsersFound": "No users found",
        "admin.statusBlocked": "Blocked",
        "admin.statusActive": "Active",
        "admin.actionUnblock": "Unblock",
        "admin.actionBlock": "Block",
        "admin.actionMakeAdmin": "Make admin",
        "admin.actionRemoveAdmin": "Remove admin",
        "admin.errorBlock": "Failed to change block status",
        "admin.errorRole": "Failed to change role",
        "admin.errorPrefix": "HTTP Error",

        "group.defaultUser": "User",
        "group.enterNameAlert": "Enter a group name",
        "group.addMembersAlert": "Add at least one member for a private group",
        "group.createError": "Group creation error",
        "group.createFailed": "Failed to create group",

        "api.error": "Error",

        "profile.email": "Email",
        "profile.username": "Username",
        "profile.unsubscribe": "Unsubscribe from emails",

        "attach.maxFilesAlert": (max) => `Maximum ${max} files allowed`,
        "attach.tooBigAlert": (name, max) => `File "${name}" is too large (max ${max}MB)`,
        "attach.uploadError": "Error uploading files",
        "attach.defaultFileName": "file",
        "attach.noVideoSupport": "Your browser does not support video",

        "pass.veryWeak": "Very weak",
        "pass.weak": "Weak",
        "pass.medium": "Medium",
        "pass.good": "Good",
        "pass.excellent": "Excellent",

        "profile.errorSave": "Save error",
        "profile.errUsernameLength": "Username must be between 3 and 30 characters",
        "profile.errUsernameChars": "Username can only contain Latin letters, numbers, and _",
        "profile.errOnlyImages": "Images only",
        "profile.errFileSize": "File too large (max 5MB)",
        "profile.errUpload": "Upload error",
        "profile.errEnterOldPass": "Enter current password",
        "profile.errEnterNewPass": "Enter new password",
        "profile.errPassMinLength": "New password must be at least 6 characters",
        "profile.errPassSame": "New password is the same as the current one",
        "profile.errNoChanges": "No changes made",
        "profile.backBtn": "Back",
        "profile.editingTitle": "Editing",
        "profile.changePhoto": "Change photo",
        "profile.newName": "New name",
        "profile.changePassTitle": "Change password",
        "profile.oldPass": "Current password",
        "profile.enterOldPass": "Enter current password",
        "profile.newPass": "New password",
        "profile.passMinLenPlaceholder": "At least 6 characters",
        "profile.confirmNewPass": "Confirm new password",
        "profile.repeatNewPass": "Repeat new password",
        "profile.profileUpdated": "Profile updated"
    }
};

export let currentLang = localStorage.getItem('app_lang') || 'ru';

export function t(key) {
    const langDict = translations[currentLang];
    if (langDict && langDict[key]) {
        return langDict[key];
    }
    return key;
}

export function setLanguage(lang) {
    if (translations[lang]) {
        currentLang = lang;
        localStorage.setItem('app_lang', lang);
        translateDOM();
        location.reload();
    }
}

export function translateDOM() {
    const elements = document.querySelectorAll('[data-i18n]');
    elements.forEach(el => {
        const key = el.getAttribute('data-i18n');

        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
            el.placeholder = t(key);
        } else if (el.tagName === 'TITLE') {
            document.title = t(key);
        } else {
            el.textContent = t(key);
        }
    });
}