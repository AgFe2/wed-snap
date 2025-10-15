package me.agfe.wedsnap.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponse {
    private String eventId;
    private String uploaderName;
    private int totalFiles;
    private int successCount;
    private int failCount;
    private List<String> failedFiles;
    private String message;
    private LocalDateTime timestamp;
}