import { state } from './app.js';

let stompClient = null;

//Map — храним все подписки одновременно
const subscriptions    = new Map();
const pendingChatIds   = new Set();
let   globalOnMessage  = null;

export function connectWebSocket(onMessage) {
    globalOnMessage = onMessage;

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({},
        () => {
            console.log('WebSocket подключён');

            if (state.currentUser?.id) {
                subscribeToUserNotifications(onMessage);
            }

            // Подписываемся на все чаты из очереди
            pendingChatIds.forEach(chatId => {
                doSubscribe(chatId, onMessage);
            });
            pendingChatIds.clear();
        },
        (error) => {
            console.error('WebSocket ошибка:', error);
            setTimeout(() => connectWebSocket(onMessage), 5000);
        }
    );
}

function doSubscribe(chatId, onMessage) {
    const key = String(chatId);
    if (subscriptions.has(key)) return; // уже подписан

    const sub = stompClient.subscribe(
        `/topic/chat/${chatId}`,
        onMessage ?? globalOnMessage
    );
    subscriptions.set(key, sub);
    console.log('Подписан на чат:', chatId);
}

export function subscribeToChat(chatId, onMessage) {
    if (!stompClient?.connected) {
        // WS ещё не подключён — добавляем в очередь
        pendingChatIds.add(String(chatId));
        return;
    }
    doSubscribe(chatId, onMessage ?? globalOnMessage);
}

export function sendWsMessage(chatId, content) {
    if (!stompClient?.connected) return false;

    stompClient.send(
        `/app/chat/${chatId}`,
        {},
        JSON.stringify({ content })
    );
    return true;
}

export function sendReadReceipt(messageId, chatId) {
    if (!stompClient?.connected) return;

    stompClient.send(
        '/app/message/read',
        {},
        JSON.stringify({ messageId, chatId })
    );
}

function subscribeToUserNotifications(onMessage) {
    if (!stompClient?.connected) return;

    stompClient.subscribe(
        `/topic/user/${state.currentUser.id}`,
        onMessage
    );
    console.log('Подписан на уведомления пользователя:', state.currentUser.id);
}

export function isConnected() {
    return stompClient?.connected ?? false;
}