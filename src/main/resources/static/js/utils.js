//защита от XSS
export function escapeHtml(text) {
    if (text == null) return '';

    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function wrapStatusIcon(svg) {
    return `<span class="status-icon">${svg}</span>`;
}

//отрисовка статусов отправки в сообщениях
export function formatStatus(status) {
    switch (status) {
        case 'SENDING':
            return wrapStatusIcon(`
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                </svg>
            `);

        case 'SENT':
            return wrapStatusIcon(`
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="12" viewBox="0 0 24 24" fill="none" stroke="#4fc3f7" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                </svg>
            `);

        case 'READ':
            return wrapStatusIcon(`
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="12" viewBox="0 0 32 24" fill="none" stroke="#4fc3f7" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                    <polyline points="28 6 17 17 14 14"/>
                </svg>
            `);

        case 'NOT_SENDING':
            return wrapStatusIcon(`
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#e53935" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
            `);

        default:
            return '';
    }
}

export function collectErrorMessage(data) {
    if (!data) return '';

    if (typeof data === 'string') {
        return data;
    }

    if (typeof data.message === 'string' && data.message.trim()) {
        return data.message.trim();
    }

    if (typeof data.error === 'string' && data.error.trim()) {
        return data.error.trim();
    }

    if (Array.isArray(data.errors) && data.errors.length > 0) {
        return data.errors
            .filter(Boolean)
            .join(', ');
    }

    if (data.details && typeof data.details === 'object') {
        const values = Object.values(data.details)
            .flat()
            .filter(Boolean);

        if (values.length > 0) {
            return values.join(', ');
        }
    }

    return '';
}