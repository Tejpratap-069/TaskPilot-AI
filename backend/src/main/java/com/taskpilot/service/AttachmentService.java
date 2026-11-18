package com.taskpilot.service;

import com.taskpilot.dto.AttachmentDtos.AttachmentContent;
import com.taskpilot.dto.AttachmentDtos.AttachmentResponse;
import com.taskpilot.entity.Attachment;
import com.taskpilot.entity.Task;
import com.taskpilot.entity.User;
import com.taskpilot.exception.BadRequestException;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class AttachmentService {
    /** Per-file upload limit: 2 MB. */
    public static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;

    private final AttachmentRepository attachments;
    private final TaskService taskService;

    public AttachmentService(AttachmentRepository attachments, TaskService taskService) {
        this.attachments = attachments;
        this.taskService = taskService;
    }

    @Transactional
    public AttachmentResponse upload(User user, Long taskId, MultipartFile file) {
        Task task = taskService.ownedTask(user, taskId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A non-empty file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the 2 MB size limit");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }
        String name = file.getOriginalFilename();
        Attachment attachment = Attachment.builder()
                .task(task)
                .fileName(name == null || name.isBlank() ? "file" : name)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .dataBase64(Base64.getEncoder().encodeToString(bytes))
                .build();
        attachments.save(attachment);
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(User user, Long taskId) {
        Task task = taskService.ownedTask(user, taskId);
        return attachments.findByTaskOrderByCreatedAtAsc(task).stream()
                .map(AttachmentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttachmentContent content(User user, Long attachmentId) {
        Attachment attachment = ownedAttachment(user, attachmentId);
        byte[] data = Base64.getDecoder().decode(attachment.getDataBase64());
        return new AttachmentContent(data, attachment.getContentType(), attachment.getFileName());
    }

    @Transactional
    public void delete(User user, Long attachmentId) {
        attachments.delete(ownedAttachment(user, attachmentId));
    }

    /** An attachment owned by someone else behaves exactly like a missing one. */
    private Attachment ownedAttachment(User user, Long attachmentId) {
        Attachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        if (!attachment.getTask().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Attachment not found");
        }
        return attachment;
    }

    public static AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(),
                a.getSizeBytes(), a.getCreatedAt());
    }
}
