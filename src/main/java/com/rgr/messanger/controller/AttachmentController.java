package com.rgr.messanger.controller;

import com.rgr.messanger.entity.attachment.Attachment;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.exception.AccessDeniedException;
import com.rgr.messanger.repository.AttachmentRepo;
import com.rgr.messanger.service.ChatService;
import com.rgr.messanger.service.FileStorageService;
import com.rgr.messanger.service.MessageService;
import com.rgr.messanger.web.dto.message.MessageResponse;
import com.rgr.messanger.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private static final String TYPE_TEXT = "text";
    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_IMAGES = "images";
    private static final String TYPE_VIDEO = "video";
    private static final String TYPE_AUDIO = "audio";
    private static final String TYPE_FILE = "file";

    private final FileStorageService fileStorageService;
    private final AttachmentRepo attachmentRepo;
    private final MessageService messageService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/upload/{chatId}")
    public ResponseEntity<?> uploadFiles(
            @PathVariable Long chatId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "text", required = false, defaultValue = "") String text,
            @RequestParam(value = "replyToId", required = false) Long replyToId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        if (!chatService.isMember(chatId, user.getId())) {
            throw new AccessDeniedException("Вы не являетесь участником чата");
        }

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Файлы не переданы"));
        }

        String messageType = determineMessageType(files);

        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(user.getId());
        message.setReplyToId(replyToId);
        message.setText(text != null && !text.isBlank() ? text.trim() : null);
        message.setType(messageType);
        message.setStatus(Status.SENT);
        message.setSendDate(java.time.LocalDateTime.now());

        messageService.create(message, user.getId());

        log.info("Создано сообщение id={} chatId={} type={}",
                message.getId(), chatId, messageType);

        int savedCount = 0;

        for (MultipartFile file : files) {
            try {
                String folder = getFolderByMime(file.getContentType());
                String fileUrl = fileStorageService.store(file, folder);

                Attachment attachment = new Attachment();
                attachment.setMessageId(message.getId());
                attachment.setFileUrl(fileUrl);
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileSize(file.getSize());
                attachment.setMimeType(file.getContentType());

                attachmentRepo.save(attachment);
                savedCount++;

                log.info("Сохранён файл: {} -> {}",
                        file.getOriginalFilename(), fileUrl);

            } catch (Exception e) {
                log.error("Ошибка загрузки файла {}",
                        file != null ? file.getOriginalFilename() : "unknown", e);
            }
        }

        if (savedCount == 0) {
            messageService.delete(message.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Не удалось загрузить ни один файл"));
        }

        List<Attachment> savedAttachments = attachmentRepo.findByMessageId(message.getId());

        message.setSenderName(user.getUsername());
        MessageResponse response = MessageResponse.from(message, savedAttachments);

        // 1. Сообщение для открытого чата
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

        // 2. Уведомление всем участникам для левого списка чатов / неоткрытых диалогов
        List<Long> memberIds = chatService.getChatMemberIds(chatId);
        for (Long memberId : memberIds) {
            messagingTemplate.convertAndSend("/topic/user/" + memberId, response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/message/{messageId}")
    public ResponseEntity<List<Attachment>> getAttachments(
            @PathVariable Long messageId,
            @AuthenticationPrincipal JwtEntity user
    ) {
        Message message = messageService.getById(messageId);

        if (!chatService.isMember(message.getChatId(), user.getId())) {
            throw new AccessDeniedException("Вы не являетесь участником чата");
        }

        return ResponseEntity.ok(attachmentRepo.findByMessageId(messageId));
    }

    private String determineMessageType(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return TYPE_TEXT;

        String mime = files.get(0).getContentType();
        if (mime == null) return TYPE_FILE;

        if (mime.startsWith("image/")) {
            return files.size() > 1 ? TYPE_IMAGES : TYPE_IMAGE;
        }
        if (mime.startsWith("video/")) return TYPE_VIDEO;
        if (mime.startsWith("audio/")) return TYPE_AUDIO;

        return TYPE_FILE;
    }

    private String getFolderByMime(String mimeType) {
        if (mimeType == null) return "files";
        if (mimeType.startsWith("image/")) return "images";
        if (mimeType.startsWith("video/")) return "videos";
        if (mimeType.startsWith("audio/")) return "audio";
        return "files";
    }
}