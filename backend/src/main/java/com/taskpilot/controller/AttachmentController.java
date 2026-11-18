package com.taskpilot.controller;

import com.taskpilot.dto.AttachmentDtos.AttachmentContent;
import com.taskpilot.dto.AttachmentDtos.AttachmentResponse;
import com.taskpilot.service.AttachmentService;
import com.taskpilot.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final SecurityUtil securityUtil;

    public AttachmentController(AttachmentService attachmentService, SecurityUtil securityUtil) {
        this.attachmentService = attachmentService;
        this.securityUtil = securityUtil;
    }

    @PostMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<AttachmentResponse> upload(@PathVariable Long taskId,
                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(securityUtil.currentUser(), taskId, file));
    }

    @GetMapping("/api/tasks/{taskId}/attachments")
    public List<AttachmentResponse> list(@PathVariable Long taskId) {
        return attachmentService.list(securityUtil.currentUser(), taskId);
    }

    @GetMapping("/api/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long attachmentId) {
        AttachmentContent content = attachmentService.content(securityUtil.currentUser(), attachmentId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (content.contentType() != null && !content.contentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(content.contentType());
            } catch (InvalidMediaTypeException ignored) {
                // fall back to application/octet-stream for unparseable stored types
            }
        }
        String safeName = content.fileName() == null ? "file"
                : content.fileName().replaceAll("[\"\\r\\n]", "_");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                .body(content.data());
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long attachmentId) {
        attachmentService.delete(securityUtil.currentUser(), attachmentId);
        return ResponseEntity.noContent().build();
    }
}
