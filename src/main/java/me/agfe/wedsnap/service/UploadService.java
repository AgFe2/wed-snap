package me.agfe.wedsnap.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.repository.UploadRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "heif");
    private final UploadRepository uploadRepository;
    @Value("${wedsnap.environment}")
    private String environment;

    public UploadResponse processUpload(UploadRequest request) {
        String uniqueUploader = getUniqueUploaderName(request.getEventName(), request.getUploaderName());
        request.setUploaderName(uniqueUploader);

        List<MultipartFile> files = request.getFiles();

        List<String> failedFiles = new ArrayList<>();
        int successCount = 0;

        synchronized (this) {
            for (MultipartFile file : files) {
                try {
                    validateFile(file);
                    uploadRepository.saveFile(request.getEventName(), request.getUploaderName(), file);
                    successCount++;
                } catch (IllegalArgumentException | IOException e) {
                    log.error("[{}] Failed to save file: {} → {}", environment, file.getOriginalFilename(), e.getMessage());
                    failedFiles.add(file.getOriginalFilename());
                }
            }
        }

        int total = files.size();
        int failCount = failedFiles.size();

        log.info("Upload Request processed: eventName={}, Uploader={} files={}, 성공 {}, 실패 {}, ",
                 request.getEventName(), uniqueUploader, total, successCount, failCount);

        return UploadResponse.builder()
                             .eventName(request.getEventName())
                             .uploaderName(request.getUploaderName())
                             .totalFiles(total)
                             .successCount(successCount)
                             .failCount(failCount)
                             .failedFiles(failedFiles)
                             .timestamp(LocalDateTime.now())
                             .message(String.format("%d개 업로드 성공, %d개 실패", successCount, failCount))
                             .build();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드 할 수 없습니다.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 비어있습니다.");
        }

        String ext = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + fileName);
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex + 1) : "";
    }

    private String getUniqueUploaderName(String eventName, String uploaderName) {
        return uploadRepository.findUniqueUploaderName(eventName, uploaderName);
    }
}
