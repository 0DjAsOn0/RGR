import { formatStatus } from './utils.js';
import { state }        from './app.js';

let stompClient        = null;
let currentSubscription = null;

export function connectWebSocket(onMessage) {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({},
        () => {
            console.log('WebSocket подключён ✅');
            if (state.currentChatId) {
                subscribeToChat(state.currentChatId, onMessage);
            }
        },
        (error) => {
            console.error('WebSocket ошибка:', error);
            setTimeout(() => connectWebSocket(onMessage), 5000);
        }
    );
}

export function subscribeToChat(chatId, onMessage) {
    if (!stompClient?.connected) return;

    if (currentSubscription) {
        currentSubscription.unsubscribe();
        currentSubscription = null;
    }

    currentSubscription = stompClient.subscribe(
        `/topic/chat/${chatId}`,
        onMessage
    );

    console.log('Подписан на чат:', chatId);
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

export function isConnected() {
    return stompClient?.connected ?? false;
}