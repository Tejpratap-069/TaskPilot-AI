package com.taskpilot.dto;

import java.time.LocalDateTime;

public class AttachmentDtos {
    public record AttachmentResponse(Long id, String fileName, String contentType,
                                     long sizeBytes, LocalDateTime createdAt) {}

    /** Decoded file payload returned by the content endpoint (never serialized to JSON). */
    public record AttachmentContent(byte[] data, String contentType, String fileName) {}
}
