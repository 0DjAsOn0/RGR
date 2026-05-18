const MAX_FILES = 10;
const MAX_SIZE_MB = 50;

let selectedFiles = [];

export function initAttachments() {
    const attachBtn = document.getElementById('attachBtn');
    const fileInput = document.getElementById('fileInput');
    const preview   = document.getElementById('attachmentsPreview');

    if (!attachBtn || !fileInput || !preview) return null;

    // Клик по кнопке — открывает выбор любого файла
    attachBtn.addEventListener('click', () => fileInput.click());

    // Обработка выбранных файлов
    fileInput.addEventListener('change', (e) => {
        handleFiles(e.target.files);
        fileInput.value = ''; // сброс чтобы можно было выбрать те же файлы снова
    });

    // Drag & Drop на область ввода
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

    // ========================
    // Обработка файлов
    // ========================
    function handleFiles(fileList) {
        Array.from(fileList).forEach(file => {
            if (selectedFiles.length >= MAX_FILES) {
                alert(`Максимум ${MAX_FILES} файлов`);
                return;
            }
            if (file.size > MAX_SIZE_MB * 1024 * 1024) {
                alert(`Файл "${file.name}" слишком большой (макс. ${MAX_SIZE_MB}MB)`);
                return;
            }
            selectedFiles.push(file);
        });

        renderPreview();
    }

    // ========================
    // Предпросмотр файлов
    // ========================
    function renderPreview() {
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
                img.src       = URL.createObjectURL(file);
                img.className = 'preview-img';
                item.appendChild(img);

            } else if (file.type.startsWith('video/')) {
                const video   = document.createElement('video');
                video.src     = URL.createObjectURL(file);
                video.className = 'preview-img';
                video.muted   = true;
                item.appendChild(video);

            } else {
                // Любой другой файл — иконка с именем
                const icon = document.createElement('div');
                icon.className = 'preview-file-icon';
                icon.innerHTML = `
                    <span class="file-ext">${getExtension(file.name)}</span>
                    <span class="file-name">${truncate(file.name, 15)}</span>
                    <span class="file-size">${formatSize(file.size)}</span>
                `;
                item.appendChild(icon);
            }

            // Кнопка удаления превью
            const removeBtn = document.createElement('button');
            removeBtn.className = 'preview-remove';
            removeBtn.innerHTML = '✕';
            removeBtn.addEventListener('click', () => {
                selectedFiles.splice(index, 1);
                renderPreview();
            });
            item.appendChild(removeBtn);

            preview.appendChild(item);
        });
    }

    return {
        getFiles:  () => [...selectedFiles],
        clearFiles: () => {
            selectedFiles = [];
            renderPreview();
        },
        hasFiles: () => selectedFiles.length > 0
    };
}

// ========================
// Отправка файлов на сервер
// ========================
export async function uploadFiles(chatId, files, text, replyToId) {
    const formData = new FormData();

    files.forEach(file => formData.append('files', file));
    formData.append('text', text || '');
    if (replyToId) formData.append('replyToId', replyToId);

    const response = await fetch(`/api/v1/attachments/upload/${chatId}`, {
        method: 'POST',
        body: formData
        // НЕ устанавливаем Content-Type — браузер сам добавит boundary
    });

    if (!response.ok) {
        const err = await response.json();
        throw new Error(err.error || 'Ошибка загрузки файлов');
    }

    return response.json();
}

// ========================
// Рендер вложений в сообщении
// ========================
export function renderAttachments(attachments) {
    if (!attachments || attachments.length === 0) return '';

    return attachments.map(a => {
        const mime     = a.mimeType  || '';
        const fileName = a.fileName  || 'файл';
        const fileUrl  = a.fileUrl   || '';

        if (mime.startsWith('image/')) {
            return `
                <div class="msg-attachment">
                    <img class="msg-image"
                         src="${fileUrl}"
                         alt="${fileName}"
                         loading="lazy"
                         onclick="openLightbox('${fileUrl}')">
                </div>`;
        }

        if (mime.startsWith('video/')) {
            return `
                <div class="msg-attachment">
                    <video class="msg-video" controls preload="metadata">
                        <source src="${fileUrl}" type="${mime}">
                        Ваш браузер не поддерживает видео
                    </video>
                </div>`;
        }

        if (mime.startsWith('audio/')) {
            return `
                <div class="msg-attachment">
                    <audio class="msg-audio" controls>
                        <source src="${fileUrl}" type="${mime}">
                    </audio>
                </div>`;
        }

        // Любой другой файл — кнопка скачивания
        return `
            <div class="msg-attachment msg-file">
                <a href="${fileUrl}" download="${fileName}" class="file-download">
                    <span class="file-download-icon">📎</span>
                    <div class="file-info">
                        <span class="file-download-name">${fileName}</span>
                        <span class="file-download-size">${formatSize(a.fileSize)}</span>
                    </div>
                </a>
            </div>`;
    }).join('');
}

// ========================
// Lightbox для картинок
// ========================
export function initLightbox() {
    // Не создаём повторно
    if (document.getElementById('lightbox')) return;

    const overlay = document.createElement('div');
    overlay.id        = 'lightbox';
    overlay.className = 'lightbox-overlay';
    overlay.innerHTML = `
        <div class="lightbox-content">
            <button class="lightbox-close">✕</button>
            <img class="lightbox-img" src="" alt="">
        </div>`;
    document.body.appendChild(overlay);

    overlay.querySelector('.lightbox-close')
        .addEventListener('click', () => overlay.style.display = 'none');

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) overlay.style.display = 'none';
    });

    // Закрытие по Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') overlay.style.display = 'none';
    });

    window.openLightbox = (src) => {
        overlay.querySelector('.lightbox-img').src = src;
        overlay.style.display = 'flex';
    };
}

// ========================
// Вспомогательные функции
// ========================
function formatSize(bytes) {
    if (!bytes) return '';
    if (bytes < 1024)             return bytes + ' B';
    if (bytes < 1024 * 1024)      return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function getExtension(filename) {
    return filename.split('.').pop().toUpperCase().slice(0, 4);
}

function truncate(str, max) {
    return str.length > max ? str.slice(0, max) + '…' : str;
}