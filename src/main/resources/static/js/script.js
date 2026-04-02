const resizer = document.querySelector('.resizer');
const chatsList = document.querySelector('.chats-list');

let isResizing = false;

resizer.addEventListener('mousedown', (e) => {
    isResizing = true;
    document.body.style.cursor = 'col-resize';
    e.preventDefault();
});

document.addEventListener('mousemove', (e) => {
    if (!isResizing) return;

    const containerLeft = chatsList.getBoundingClientRect().left;
    let newWidth = e.clientX - containerLeft;

    if (newWidth < 260) newWidth = 260;
    if (newWidth > 1160) newWidth = 1160;

    chatsList.style.width = newWidth + 'px';
});

document.addEventListener('mouseup', () => {
    if (isResizing) {
        isResizing = false;
        document.body.style.cursor = 'default';
    }
});

function viewMyProfile(){
    console.log('открытие окна профиля пользователя');
    

    let profilePage = document.getElementById('createWindow');
    
    if (!profilePage) {
        console.log('создание окна профиля пользователя');
        profilePage = document.createElement('div');
        profilePage.id = 'createWindow';
        profilePage.className = 'profilePage';
        document.body.appendChild(profilePage);
    }

    profilePage.innerHTML = `
        <div class="profilePage-content">
            <div class="header-profile">
                <span class="edit" onclick="editProfile()">Редактировать</span>
                <div class="main-info-profile">
                    <img class="avatar-img avatar-profile" src="./img/avatar1.JPG" alt="">
                    <span class="nickname" ">Никнейм тута</span>
                    <span class="time-activity">был(а) в сети дата епта</span>
                </div>
                <span class="close" onclick="closeCreateWindow()">&times;</span>
            </div>
            <div class="profile-contacts">
                <span class="contacts-link" >djeson2006@mail.ru</span>
                <span class="contacts-text" >Почта</span>
                <span class="contacts-link" >devil666prada</span>
                <span class="contacts-text" >Имя пользователя</span>
            </div>
            <div class="email-refuse">
                <span class="refuse-agree">Отказаться от рассылки почты</span>
                <label class="custom-checkbox">
                    <input type="checkbox" id="agree" name="message-refuse" value="no">
                    <span class="checkmark"></span>
                </label>
            </div>
        </div>
    `;

    profilePage.classList.add('show');
    profilePage.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function closeCreateWindow() {
    console.log('закрываем окно профиля');
    
    const profilePage = document.getElementById('createWindow');
    if (profilePage) {
        profilePage.classList.remove('show');
        profilePage.style.display = 'none';

        document.body.style.overflow = 'auto';
    }
}

$('#registerBtn').click(function() {
    const login = $('#login').val().trim();
    const password = $('#password').val();
    const repeat = $('#repeatPassword').val();
    const email = $('#email').val().trim();

    if (!login || !password || !repeat || !email) {
        alert('Заполните все поля');
        return;
    }

    if (password !== repeat) {
        alert('Пароли не совпадают');
        return;
    }

    $.ajax({
        url: 'https://твой-сервер.com/api/register',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ login, password, email }),
        success: function(res) {
            alert('Регистрация успешна');
            console.log(res);
        },
        error: function(err) {
            alert('Ошибка');
            console.error(err);
        }
    });
});



















function openChat(card) {
    // подсветка активного чата
    document.querySelectorAll('.card').forEach(c => c.classList.remove('active'));
    card.classList.add('active');

    const dialog = document.getElementById('mainDialog');
    dialog.classList.remove('empty-dialog');

    dialog.innerHTML = `
        <div class="dialog-header">
            <div class="dialog-header-info">
                <img class="avatar-img" src="../../static/img/avatar1.JPG" alt="">
                <div class="dialog-header-text">
                    <span class="dialog-name">Имя пользователя</span>
                    <span class="dialog-status">был(а) недавно</span>
                </div>
            </div>
            <div class="dialog-header-actions">
                <button class="icon-btn">🔍</button>
                <button class="icon-btn">⋮</button>
            </div>
        </div>

        <div class="dialog-messages">
            <div class="message message-in">
                <div class="message-bubble">
                    <p class="message-text">Привет!</p>
                    <span class="message-time-small">12:00</span>
                </div>
            </div>
            <div class="message message-out">
                <div class="message-bubble">
                    <p class="message-text">Привет! Как дела?</p>
                    <span class="message-time-small">12:01</span>
                </div>
            </div>
            <div class="message message-in">
                <div class="message-bubble">
                    <p class="message-text">Всё отлично, спасибо!</p>
                    <span class="message-time-small">12:02</span>
                </div>
            </div>
        </div>

        <div class="dialog-input-area">
            <button class="icon-btn">📎</button>
            <textarea class="message-input" placeholder="Написать сообщение..." rows="1"></textarea>
            <button class="icon-btn">😊</button>
            <button class="icon-btn send-btn">➤</button>
        </div>
    `;
}