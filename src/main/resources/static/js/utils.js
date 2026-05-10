export function escapeHtml(text) {
    if (text == null) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

export function formatStatus(status) {
    switch (status) {
        case 'SENT': return '✓';
        case 'READ': return '✓✓';
        default:     return '';
    }
}