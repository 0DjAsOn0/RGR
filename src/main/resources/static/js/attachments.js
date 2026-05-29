import { escapeHtml } from './utils.js';
import { t } from './i18n.js';

const MAX_FILES = 10;
const MAX_SIZE_MB = 50;

export function initAttachments() {
    const attachBtn = document.getElementById('attachBtn');
    const fileInput = document.getElementById('fileInput');
    const preview = document.getElementById('attachmentsPreview');

    if (!attachBtn || !fileInput || !preview) return null;

    let selectedFiles = [];
    let previewUrls = [];

    attachBtn.addEventListener('click', () => fileInput.click());


    //обработка файлов которые скинули
    fileInput.addEventListener('change', (e) => {
        handleFiles(e.target.files);
        fileInput.value = '';
    });


    //работа с подсветкой и дизайном области ввода
    const inputArea = document.querySelector('.dialog-input-area');
    if (inputArea) {
        inputArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            inputArea.classList.add('drag-over');
        });

        inputArea.addEventListener('dragleave', () => {
            inputArea.classList.remove('drag-over');
        });

        inputArea.addEventListener('drop', (e) => {
            e.preventDefault();
            inputArea.classList.remove('drag-over');
            handleFiles(e.dataTransfer.files);
        });
    }


    //обработка файлов
    function handleFiles(fileList) {

        //перебираем файлы
        Array.from(fileList).forEach(file => {

            //проверка на количество файлов
            if (selectedFiles.length >= MAX_FILES) {
                alert(t('attach.maxFilesAlert')(MAX_FILES));
                return;
            }

            //проверка на размер файлов
            if (file.size > MAX_SIZE_MB * 1024 * 1024) {
                alert(t('attach.tooBigAlert')(file.name, MAX_SIZE_MB));
                return;
            }

            //если все норм то кидаем на отправку
            selectedFiles.push(file);
        });


        //предпросмотр файлов
        renderPreview();
    }


    //очищаем память браузера
    function clearPreviewUrls() {
        previewUrls.forEach(url => URL.revokeObjectURL(url));
        previewUrls = [];
    }


    //превью вложений
    function renderPreview() {
        clearPreviewUrls();
        preview.innerHTML = '';

        if (selectedFiles.length === 0) {
            preview.style.display = 'none';
            return;
        }

        preview.style.display = 'flex';

        selectedFiles.forEach((file, index) => {
            const item = document.createElement('div');
            item.className = 'preview-item';

            if (file.type.startsWith('image/')) {
                const img = document.createElement('img');
                const url = URL.createObjectURL(file);
                previewUrls.push(url);

                img.src = url;
                img.className = 'preview-img';
                item.appendChild(img);

            } else if (file.type.startsWith('video/')) {
                const video = document.createElement('video');
                const url = URL.createObjectURL(file);
                previewUrls.push(url);

                video.src = url;
                video.className = 'preview-img';
                video.muted = true;
                item.appendChild(video);

            } else {
                const icon = document.createElement('div');
                icon.className = 'preview-file-icon';
                icon.innerHTML = `
                    <span class="file-ext">${escapeHtml(getExtension(file.name))}</span>
                    <span class="file-name">${escapeHtml(truncate(file.name, 15))}</span>
                    <span class="file-size">${formatSize(file.size)}</span>
                `;
                item.appendChild(icon);
            }


            //кнопка удаления
            const removeBtn = document.createElement('button');
            removeBtn.className = 'preview-remove';
            removeBtn.type = 'button';
            removeBtn.textContent = '✕';
            removeBtn.addEventListener('click', () => {
                selectedFiles.splice(index, 1);
                renderPreview();
            });

            item.appendChild(removeBtn);
            preview.appendChild(item);
        });
    }

    // возвращает копию выбранных файлов
    // Очищает список файлов и превью после отправки
    // Проверяет, есть ли вообще прикрепленные файлы
    return {
        getFiles: () => [...selectedFiles],
        clearFiles: () => {
            selectedFiles = [];
            clearPreviewUrls();
            renderPreview();
        },
        hasFiles: () => selectedFiles.length > 0
    };
}

// ========================
// ОТПРАВКА ФАЙЛОВ на сервер
// ========================
export async function uploadFiles(chatId, files, text, replyToId) {
    const formData = new FormData();

    files.forEach(file => formData.append('files', file));
    formData.append('text', text || '');

    if (replyToId != null) {
        formData.append('replyToId', replyToId);
    }

    // Подключаем текущий язык для отправки на сервер
    const { currentLang } = await import('./i18n.js');

    //запрос
    const response = await fetch(`/api/v1/attachments/upload/${chatId}`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Accept-Language': currentLang
        },
        body: formData
    });

    //ждем JSON ответ
    let data = null;
    try {
        data = await response.json();
    } catch {
        data = null;
    }

    if (!response.ok) {
        throw new Error(data?.error || data?.message || t('attach.uploadError'));
    }

    return data;
}

// ========================
// РЕНДЕР ВЛОЖЕНИЙ
// ========================
export function renderAttachments(attachments) {
    if (!attachments || attachments.length === 0) return '';

    return attachments.map(a => {
        const mime = a.mimeType || a.type || '';
        const fileName = escapeHtml(a.fileName || a.name || t('attach.defaultFileName'));
        const rawUrl = a.fileUrl || a.url || '';
        const fileUrl = escapeHtml(rawUrl);
        const fileSize = a.fileSize || a.size || 0;

        if (!rawUrl) {
            return `
                <div class="msg-attachment msg-file">
                    <div class="file-download">
                        <span class="file-download-icon">📎</span>
                        <div class="file-info">
                            <span class="file-download-name">${fileName}</span>
                            <span class="file-download-size">${formatSize(fileSize)}</span>
                        </div>
                    </div>
                </div>
            `;
        }

        if (mime.startsWith('image/')) {
            return `
                <div class="msg-attachment">
                    <img class="msg-image"
                         src="${fileUrl}"
                         alt="${fileName}"
                         loading="lazy"
                         data-lightbox-src="${fileUrl}">
                </div>
            `;
        }

        if (mime.startsWith('video/')) {
            return `
                <div class="msg-attachment">
                    <video class="msg-video" controls preload="metadata">
                        <source src="${fileUrl}" type="${escapeHtml(mime)}">
                        ${t('attach.noVideoSupport')}
                    </video>
                </div>
            `;
        }

        if (mime.startsWith('audio/')) {
            return `
                <div class="msg-attachment">
                    <audio class="msg-audio" controls>
                        <source src="${fileUrl}" type="${escapeHtml(mime)}">
                    </audio>
                </div>
            `;
        }

        return `
            <div class="msg-attachment msg-file">
                <a href="${fileUrl}" download="${fileName}" class="file-download">
                    <span class="file-download-icon">📎</span>
                    <div class="file-info">
                        <span class="file-download-name">${fileName}</span>
                        <span class="file-download-size">${formatSize(fileSize)}</span>
                    </div>
                </a>
            </div>
        `;
    }).join('');
}

// ========================
// LIGHTBOX
// ========================
export function initLightbox() {
    if (document.getElementById('lightbox')) return;

    const overlay = document.createElement('div');
    overlay.id = 'lightbox';
    overlay.className = 'lightbox-overlay';
    overlay.innerHTML = `
        <div class="lightbox-content">
            <button class="lightbox-close" type="button">✕</button>
            <img class="lightbox-img" src="" alt="">
        </div>
    `;

    document.body.appendChild(overlay);

    const image = overlay.querySelector('.lightbox-img');

    overlay.querySelector('.lightbox-close')
        .addEventListener('click', () => {
            image.src = '';
            overlay.style.display = 'none';
        });

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            image.src = '';
            overlay.style.display = 'none';
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            image.src = '';
            overlay.style.display = 'none';
        }
    });

    document.addEventListener('click', (e) => {
        const target = e.target.closest('[data-lightbox-src]');
        if (!target) return;

        image.src = target.dataset.lightboxSrc;
        overlay.style.display = 'flex';
    });
}

// ========================
// HELPERS
// ========================
function formatSize(bytes) {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getExtension(filename) {
    if (!filename || !filename.includes('.')) return 'FILE';
    return filename.split('.').pop().toUpperCase().slice(0, 4);
}

function truncate(str, max) {
    if (!str) return '';
    return str.length > max ? `${str.slice(0, max)}…` : str;
}