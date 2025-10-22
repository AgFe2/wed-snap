package me.agfe.wedsnap.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponse {

    private String eventName;
    private String uploaderName;
    private int totalFiles;
    private int successCount;
    private int failCount;
    private List<String> failedFiles;
    private String message;
    private LocalDateTime timestamp;
}
