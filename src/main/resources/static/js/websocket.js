import { state } from './app.js';

let stompClient = null;
let reconnectDelay = 1000;
const MAX_DELAY = 30000;

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

export function connectWebSocket(onMessage) {
    globalOnMessage = onMessage;
    manualDisconnect = false;

    if (stompClient?.connected || connecting) {
        return;
    }

    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    connecting = true;

    const socket = new SockJS('/ws');
    const client = Stomp.over(socket);
    client.debug = null;

    client.connect(
        {},
        () => {
            stompClient = client;
            connecting = false;
            reconnectDelay = 1000;

            console.log('WebSocket подключён');

            const oldChats = Array.from(subscriptions.keys());
            subscriptions.forEach(sub => {
                try {
                    sub.unsubscribe();
                } catch {
                    // ignore
                }
            });
            subscriptions.clear();

            if (userSubscription) {
                try {
                    userSubscription.unsubscribe();
                } catch {
                    // ignore
                }
                userSubscription = null;
            }

            if (state.currentUser?.id) {
                subscribeToUserNotifications(onMessage);
            }

            const chatsToRestore = new Set([
                ...oldChats,
                ...pendingChatIds
            ]);
            pendingChatIds.clear();

            chatsToRestore.forEach(chatId => {
                doSubscribe(chatId, onMessage);
            });
        },
        (error) => {
            console.error('WebSocket ошибка:', error);

            connecting = false;
            stompClient = null;

            if (manualDisconnect) {
                return;
            }

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

    if (!stompClient?.connected || subscriptions.has(key)) {
        return;
    }

    const sub = stompClient.subscribe(
        `/topic/chat/${key}`,
        onMessage ?? globalOnMessage
    );

    subscriptions.set(key, sub);
    console.log('Подписан на чат:', key);
}

export function subscribeToChat(chatId, onMessage) {
    const key = String(chatId);

    if (!stompClient?.connected) {
        pendingChatIds.add(key);
        return;
    }

    doSubscribe(key, onMessage ?? globalOnMessage);
}

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
}

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
// ОТПРАВКА
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
}