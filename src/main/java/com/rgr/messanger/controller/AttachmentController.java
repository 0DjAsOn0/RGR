package com.rgr.messanger.controller;

import com.rgr.messanger.entity.attachment.Attachment;
import com.rgr.messanger.entity.message.Message;
import com.rgr.messanger.entity.message.Status;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.repository.AttachmentRepo;
import com.rgr.messanger.service.FileStorageService;
import com.rgr.messanger.service.MessageService;
import com.rgr.messanger.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final FileStorageService fileStorageService;
    private final AttachmentRepo attachmentRepo;
    private final MessageService messageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/upload/{chatId}")
    public ResponseEntity<?> uploadFiles(
            @PathVariable Long chatId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "text", required = false, defaultValue = "") String text,
            @RequestParam(value = "replyToId", required = false) Long replyToId,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        User sender = userService.getByUsername(principal.getName());

        String messageType = determineMessageType(files);

        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(sender.getId());
        message.setReplyToId(replyToId);
        message.setText(text.isBlank() ? null : text);
        message.setType(messageType);
        message.setStatus(Status.SENT);

        messageService.create(message, sender.getId());

        log.info("Создано сообщение id={} chatId={} type={}", message.getId(), chatId, messageType);

        List<Map<String, Object>> savedAttachments = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String folder  = getFolderByMime(file.getContentType());
                String fileUrl = fileStorageService.store(file, folder);

                Attachment attachment = new Attachment();
                attachment.setMessageId(message.getId());
                attachment.setFileUrl(fileUrl);
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileSize(file.getSize());
                attachment.setMimeType(file.getContentType());

                attachmentRepo.save(attachment);

                log.info("Сохранён файл: {} -> {}", file.getOriginalFilename(), fileUrl);

                savedAttachments.add(Map.of(
                        "id",       attachment.getId(),
                        "fileUrl",  fileUrl,
                        "fileName", file.getOriginalFilename() != null
                                ? file.getOriginalFilename() : "file",
                        "fileSize", file.getSize(),
                        "mimeType", file.getContentType() != null
                                ? file.getContentType() : "application/octet-stream"
                ));

            } catch (Exception e) {
                log.error("Ошибка загрузки файла {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }

        //Отправляем WS уведомление всем участникам чата
        Map<String, Object> wsMessage = new java.util.LinkedHashMap<>();
        wsMessage.put("id",          message.getId());
        wsMessage.put("chatId",      chatId);
        wsMessage.put("senderId",    sender.getId());
        wsMessage.put("senderName",  sender.getUsername());
        wsMessage.put("type",        messageType);
        wsMessage.put("text",        message.getText());
        wsMessage.put("status",      "SENT");
        wsMessage.put("sendDate",    message.getSendDate());
        wsMessage.put("time",        message.getSendDate() != null
                ? message.getSendDate().toLocalTime()
                  .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                : "");
        wsMessage.put("attachments", savedAttachments);

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, (Object) wsMessage);

        return ResponseEntity.ok(Map.of(
                "messageId",   message.getId(),
                "status",      "SENT",
                "attachments", savedAttachments
        ));
    }

    @GetMapping("/message/{messageId}")
    public ResponseEntity<List<Attachment>> getAttachments(
            @PathVariable Long messageId
    ) {
        return ResponseEntity.ok(attachmentRepo.findByMessageId(messageId));
    }

    private String determineMessageType(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return "text";
        String mime = files.get(0).getContentType();
        if (mime == null) return "file";
        if (mime.startsWith("image/")) return files.size() > 1 ? "images" : "image";
        if (mime.startsWith("video/")) return "video";
        if (mime.startsWith("audio/")) return "audio";
        return "file";
    }

    private String getFolderByMime(String mimeType) {
        if (mimeType == null) return "files";
        if (mimeType.startsWith("image/")) return "images";
        if (mimeType.startsWith("video/")) return "videos";
        if (mimeType.startsWith("audio/")) return "audio";
        return "files";
    }
}