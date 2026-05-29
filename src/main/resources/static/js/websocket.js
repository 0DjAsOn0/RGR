import { state } from './app.js';

let stompClient = null;
let reconnectDelay = 1000;
const MAX_DELAY = 30000;

// подписки
const subscriptions = new Map();
const pendingChatIds = new Set();

let globalOnMessage = null;
let userSubscription = null;
let reconnectTimer = null;
let manualDisconnect = false;
let connecting = false;

// ========================
// ПОДКЛЮЧЕНИЕ
// ========================


//SockJS — библиотека для организации клиентского соединения с сервером в real-time.
// STOMP — протокол поверх этого соединения, который позволяет удобно подписываться на каналы и отправлять сообщения.

export function connectWebSocket(onMessage) {

    //сохранение обработчика сообщений
    globalOnMessage = onMessage;
    manualDisconnect = false;

    //проверка подключены уже или нет
    if (stompClient?.connected || connecting) {
        return;
    }

    //сброс таймера переподключения
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    connecting = true;

    //после флага на connecting подключаемся к вебсокет
    const socket = new SockJS('/ws');

    //поверх вебсокета открываем стомп клиент
    const client = Stomp.over(socket);
    client.debug = null;

    client.connect(
        {},  //header
        () => {         //успешное подключение
            stompClient = client;
            connecting = false;
            reconnectDelay = 1000;

            console.log('WebSocket подключён');

            //запоминаем старые подписки
            const oldChats = Array.from(subscriptions.keys());

            //в соединении их сбрасываем
            subscriptions.forEach(sub => {
                try {
                    sub.unsubscribe();
                } catch {
                    // ignore
                }
            });
            subscriptions.clear();


            //Очищается подписка на персональные уведомления
            if (userSubscription) {
                try {
                    userSubscription.unsubscribe();
                } catch {
                    // ignore
                }
                userSubscription = null;
            }

            //Если текущий пользователь известен — подписываемся на личные уведомления
            if (state.currentUser?.id) {
                subscribeToUserNotifications(onMessage);
            }


            //Восстанавливаются подписки на чаты
            const chatsToRestore = new Set([
                ...oldChats,
                ...pendingChatIds
            ]);
            pendingChatIds.clear();

            //Для каждого чата делается подписка
            chatsToRestore.forEach(chatId => {
                doSubscribe(chatId, onMessage);
            });
        },
        (error) => {   //ошибка
            console.error('WebSocket ошибка:', error);

            connecting = false;
            stompClient = null;

            //если например чел сам вышел то мы не переподключаемся
            if (manualDisconnect) {
                return;
            }

            //если таймера переподключения еще нет — запускаем его
            if (!reconnectTimer) {
                reconnectTimer = setTimeout(() => {
                    reconnectTimer = null;
                    connectWebSocket(globalOnMessage);
                }, reconnectDelay);

                reconnectDelay = Math.min(reconnectDelay * 2, MAX_DELAY);
            }
        }
    );
}

// ========================
// ПОДПИСКИ
// ========================

function doSubscribe(chatId, onMessage) {
    const key = String(chatId);


    //проверка соединения и проверка не подписаны ли уже
    if (!stompClient?.connected || subscriptions.has(key)) {
        return;
    }


    //подписываемся на канал
    const sub = stompClient.subscribe(
        `/topic/chat/${key}`,
        onMessage ?? globalOnMessage
    );

    //сохраняем подписку
    subscriptions.set(key, sub);
    console.log('Подписан на чат:', key);
}


//публичная функция подписки на чат
export function subscribeToChat(chatId, onMessage) {
    const key = String(chatId);


    //проверка соединения
    if (!stompClient?.connected) {
        pendingChatIds.add(key);
        return;
    }

    //подписываемся
    doSubscribe(key, onMessage ?? globalOnMessage);
}

//отписка от чата например когда пользователь удаляет чатикс
export function unsubscribeFromChat(chatId) {
    const key = String(chatId);
    const sub = subscriptions.get(key);

    if (sub) {
        try {
            sub.unsubscribe();
        } catch {
            // ignore
        }
        subscriptions.delete(key);
    }

    pendingChatIds.delete(key);
    //хз че сказать ну прост удаляется
}

//подписка на персональные уведомления пользователя
function subscribeToUserNotifications(onMessage) {

    if (!stompClient?.connected || !state.currentUser?.id) {
        return;
    }

    if (userSubscription) {
        try {
            userSubscription.unsubscribe();
        } catch {
            // ignore
        }
    }

    userSubscription = stompClient.subscribe(
        `/topic/user/${state.currentUser.id}`,
        onMessage ?? globalOnMessage
    );

    console.log('Подписан на уведомления пользователя:', state.currentUser.id);
}

// ========================
// ОТПРАВКА СООБЩЕНИЙ
// ========================

export function sendWsMessage(chatId, content, replyToId = null) {

    if (!stompClient?.connected) {
        return false;
    }

    stompClient.send(
        `/app/chat/${chatId}`,
        {},
        JSON.stringify({ content, replyToId })
    );

    return true;
}

//отправляем что сообщение прочитано
export function sendReadReceipt(messageId, chatId) {

    if (!stompClient?.connected) {
        return;
    }

    stompClient.send(
        '/app/message/read',
        {},
        JSON.stringify({ messageId, chatId })
    );
}

// ========================
// СОСТОЯНИЕ
// ========================

export function isConnected() {
    return stompClient?.connected ?? false;
}

//отключение
export function disconnect() {
    manualDisconnect = true;
    connecting = false;

    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    subscriptions.forEach(sub => {
        try {
            sub.unsubscribe();
        } catch {
            // ignore
        }
    });
    subscriptions.clear();
    pendingChatIds.clear();

    if (userSubscription) {
        try {
            userSubscription.unsubscribe();
        } catch {
            // ignore
        }
        userSubscription = null;
    }

    if (stompClient?.connected) {
        stompClient.disconnect(() => {
            stompClient = null;
        });
    } else {
        stompClient = null;
    }
    //отключаемся очищаемся
}